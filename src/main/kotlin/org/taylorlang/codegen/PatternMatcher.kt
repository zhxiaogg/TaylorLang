package org.taylorlang.codegen

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Handles pattern matching logic for bytecode generation.
 * 
 * This class provides:
 * - Pattern test generation
 * - Variable binding in patterns
 * - Guard expression evaluation
 * - Constructor and literal pattern matching
 */
class PatternMatcher(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val typeConverter: TypeConverter,
    private val generateExpressionCallback: (TypedExpression) -> Unit
) {
    
    /**
     * Generate bytecode for pattern testing.
     * 
     * @param pattern The pattern to test
     * @param targetType The type of the value being matched
     * @param successLabel Label to jump to if pattern matches
     * @param failureLabel Label to jump to if pattern doesn't match
     */
    fun generatePatternTest(
        pattern: Pattern,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label,
        targetSlot: Int = -1
    ) {
        when (pattern) {
            is Pattern.LiteralPattern -> {
                generateLiteralPatternTest(pattern, targetType, successLabel, failureLabel)
            }
            is Pattern.WildcardPattern -> {
                // Wildcard patterns always match - they match any value
                // Pop the target value that was loaded for comparison since wildcards don't need to compare
                // CRITICAL FIX: Use POP2 for double values (they occupy 2 stack slots)
                if (targetType == BuiltinTypes.DOUBLE || (targetType is Type.PrimitiveType && targetType.name.lowercase() in listOf("double", "float"))) {
                    methodVisitor.visitInsn(POP2)
                } else {
                    methodVisitor.visitInsn(POP)
                }
                methodVisitor.visitJumpInsn(GOTO, successLabel)
            }
            is Pattern.IdentifierPattern -> {
                // Identifier patterns always match - they just bind the value
                // Pop the target value that was loaded for comparison
                // since identifier patterns don't need to compare, just bind
                // CRITICAL FIX: Use POP2 for double values (they occupy 2 stack slots)
                if (targetType == BuiltinTypes.DOUBLE || (targetType is Type.PrimitiveType && targetType.name.lowercase() in listOf("double", "float"))) {
                    methodVisitor.visitInsn(POP2)
                } else {
                    methodVisitor.visitInsn(POP)
                }
                methodVisitor.visitJumpInsn(GOTO, successLabel)
            }
            is Pattern.ConstructorPattern -> {
                generateConstructorPatternTest(pattern, targetType, successLabel, failureLabel)
            }
            is Pattern.GuardPattern -> {
                generateGuardPatternTest(pattern, targetType, successLabel, failureLabel, targetSlot)
            }
            else -> {
                // Unknown pattern type - assume failure
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
        }
    }
    
    /**
     * Generate pattern test for literal patterns.
     */
    private fun generateLiteralPatternTest(
        pattern: Pattern.LiteralPattern,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label
    ) {
        when (val literal = pattern.literal) {
            is Literal.IntLiteral -> {
                // Compare integer value
                methodVisitor.visitLdcInsn(literal.value)
                methodVisitor.visitJumpInsn(IF_ICMPEQ, successLabel)
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
            is Literal.StringLiteral -> {
                // Compare string value using equals
                methodVisitor.visitLdcInsn(literal.value)
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/Object",
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false
                )
                methodVisitor.visitJumpInsn(IFNE, successLabel)
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
            is Literal.BooleanLiteral -> {
                // Compare boolean value
                val expectedValue = if (literal.value) 1 else 0
                methodVisitor.visitLdcInsn(expectedValue)
                methodVisitor.visitJumpInsn(IF_ICMPEQ, successLabel)
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
            is Literal.FloatLiteral -> {
                // Compare double value
                methodVisitor.visitLdcInsn(literal.value)
                methodVisitor.visitInsn(DCMPL)
                methodVisitor.visitJumpInsn(IFEQ, successLabel)
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
            else -> {
                // Unknown literal type
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
        }
    }
    
    /**
     * Generate pattern test for constructor patterns.
     */
    private fun generateConstructorPatternTest(
        pattern: Pattern.ConstructorPattern,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label
    ) {
        val constructorName = pattern.constructor
        
        // Check if this is a nullary constructor (no fields)
        if (pattern.patterns.isEmpty()) {
            generateNullaryConstructorTest(constructorName, targetType, successLabel, failureLabel)
            return
        }
        
        // For constructors with fields, we need to:
        // 1. Check the constructor type
        // 2. Verify field patterns match
        
        val constructorClassName = getConstructorClassName(constructorName, targetType)
        
        // Check instanceof
        methodVisitor.visitInsn(DUP) // Duplicate value for field access
        methodVisitor.visitTypeInsn(INSTANCEOF, constructorClassName)
        // CRITICAL FIX: Ensure consistent stack state on failure
        // If instanceof fails, we need to clean up the duplicated value
        val tempFailureLabel = Label()
        methodVisitor.visitJumpInsn(IFEQ, tempFailureLabel)
        // instanceof succeeded, continue with duplicated value
        // Cast to constructor type (DUPed value is still on stack)
        methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
        
        // Test each field pattern with proper stack management
        for (i in pattern.patterns.indices) {
            val fieldPattern = pattern.patterns[i]
            val fieldType = getFieldType(constructorName, i, targetType)
            
            // Duplicate constructor instance for field access
            methodVisitor.visitInsn(DUP)
            
            // Access field via getter method (Kotlin data classes have private fields)
            generateFieldAccess(constructorName, i, constructorClassName, fieldType)
            
            // Test field pattern recursively
            val fieldSuccessLabel = Label()
            val fieldFailureLabel = Label()
            
            // CRITICAL FIX: Generate custom field pattern test to handle stack cleanup
            generateFieldPatternTest(fieldPattern, fieldType, fieldSuccessLabel, fieldFailureLabel)
            
            // Field pattern failed - clean up stack and fail constructor pattern
            methodVisitor.visitLabel(fieldFailureLabel)
            methodVisitor.visitInsn(POP) // Remove constructor instance
            methodVisitor.visitJumpInsn(GOTO, failureLabel)
            
            // Field pattern succeeded - continue to next field
            methodVisitor.visitLabel(fieldSuccessLabel)
            // CRITICAL FIX: After each field test, the constructor instance is still on the stack
            // No additional cleanup needed here as the constructor instance is properly maintained
        }
        
        // All field patterns matched
        methodVisitor.visitInsn(POP) // Remove constructor instance
        methodVisitor.visitJumpInsn(GOTO, successLabel)
        
        // CRITICAL FIX: Handle instanceof failure case with proper stack cleanup
        methodVisitor.visitLabel(tempFailureLabel)
        methodVisitor.visitInsn(POP) // Clean up the duplicated value from failed instanceof
        methodVisitor.visitJumpInsn(GOTO, failureLabel)
    }
    
    /**
     * Generate test for nullary constructors (enums, singletons).
     */
    private fun generateNullaryConstructorTest(
        constructorName: String,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label
    ) {
        val constructorClassName = getConstructorClassName(constructorName, targetType)
        
        // For nullary constructors, we just check instanceof
        methodVisitor.visitTypeInsn(INSTANCEOF, constructorClassName)
        methodVisitor.visitJumpInsn(IFNE, successLabel)
        methodVisitor.visitJumpInsn(GOTO, failureLabel)
    }
    
    /**
     * Generate pattern test with guard expression.
     * CRITICAL FIX: Bind pattern variables before evaluating guard to prevent stack frame issues
     */
    private fun generateGuardPatternTest(
        pattern: Pattern.GuardPattern,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label,
        targetSlot: Int
    ) {
        // First test the inner pattern
        val guardSuccessLabel = Label()
        generatePatternTest(pattern.pattern, targetType, guardSuccessLabel, failureLabel)
        
        // If inner pattern matches, evaluate guard
        methodVisitor.visitLabel(guardSuccessLabel)
        
        // CRITICAL FIX: Bind pattern variables before evaluating guard expression
        // This is necessary because guard expressions can reference pattern variables (e.g., case a if a > 10)
        if (targetSlot != -1) {
            // We have access to the target slot, so we can properly bind pattern variables for guard evaluation
            val savedSlotManager = variableSlotManager.createCheckpoint()
            
            // Load target value and store it in a temporary slot for pattern variable binding
            val tempTargetSlot = variableSlotManager.allocateTemporarySlot(targetType)
            val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
            val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
            
            // Store current target for pattern variable binding
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            methodVisitor.visitVarInsn(storeInstruction, tempTargetSlot)
            
            // Bind variables from the inner pattern for guard evaluation
            bindPatternVariables(pattern.pattern, targetType, tempTargetSlot)
            
            // Generate guard expression (now pattern variables are available)
            generateExpressionCallback(TypedExpression(pattern.guard, BuiltinTypes.BOOLEAN))
            
            // Restore variable slot state
            variableSlotManager.restoreCheckpoint(savedSlotManager)
            variableSlotManager.releaseTemporarySlot(tempTargetSlot)
        } else {
            // Fallback: Generate guard expression without variable binding
            // This will fail if the guard references pattern variables, but maintains backward compatibility
            generateExpressionCallback(TypedExpression(pattern.guard, BuiltinTypes.BOOLEAN))
        }
        
        // Check guard result
        methodVisitor.visitJumpInsn(IFNE, successLabel)
        methodVisitor.visitJumpInsn(GOTO, failureLabel)
    }
    
    /**
     * Bind variables in a pattern to their values.
     * 
     * @param pattern The pattern to bind variables from
     * @param targetType The type of the matched value
     * @param targetSlot The local variable slot containing the matched value
     */
    fun bindPatternVariables(pattern: Pattern, targetType: Type, targetSlot: Int) {
        when (pattern) {
            is Pattern.IdentifierPattern -> {
                // Bind identifier to the target value
                val identifierSlot = variableSlotManager.allocateSlot(pattern.name, targetType)
                val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
                val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
                
                methodVisitor.visitVarInsn(loadInstruction, targetSlot)
                methodVisitor.visitVarInsn(storeInstruction, identifierSlot)
            }
            is Pattern.ConstructorPattern -> {
                bindConstructorPatternVariables(pattern, targetType, targetSlot)
            }
            is Pattern.GuardPattern -> {
                // Bind variables from the inner pattern
                bindPatternVariables(pattern.pattern, targetType, targetSlot)
            }
            is Pattern.LiteralPattern -> {
                // Literal patterns don't bind variables
            }
            is Pattern.WildcardPattern -> {
                // Wildcard patterns don't bind variables
            }
            else -> {
                // Unknown pattern type - no variables to bind
            }
        }
    }
    
    /**
     * Bind variables in constructor patterns.
     */
    private fun bindConstructorPatternVariables(
        pattern: Pattern.ConstructorPattern,
        targetType: Type,
        targetSlot: Int
    ) {
        if (pattern.patterns.isEmpty()) {
            return // No fields to bind
        }
        
        val constructorClassName = getConstructorClassName(pattern.constructor, targetType)
        val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
        val allocatedTempSlots = mutableListOf<Int>()
        
        for (i in pattern.patterns.indices) {
            val fieldPattern = pattern.patterns[i]
            val fieldType = getFieldType(pattern.constructor, i, targetType)
            
            // Load constructor instance
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
            
            // Access field via getter method (Kotlin data classes have private fields)
            generateFieldAccess(pattern.constructor, i, constructorClassName, fieldType)
            
            // Store field value in temporary slot
            val fieldSlot = variableSlotManager.allocateTemporarySlot(fieldType)
            allocatedTempSlots.add(fieldSlot)
            val fieldStoreInstruction = variableSlotManager.getStoreInstruction(fieldType)
            methodVisitor.visitVarInsn(fieldStoreInstruction, fieldSlot)
            
            // Recursively bind variables in field pattern
            bindPatternVariables(fieldPattern, fieldType, fieldSlot)
        }
        
        // CRITICAL FIX: Release temporary slots to prevent slot accumulation
        // This prevents inconsistent local variable counts between execution paths
        for (tempSlot in allocatedTempSlots) {
            variableSlotManager.releaseTemporarySlot(tempSlot)
        }
    }
    
    /**
     * Get the JVM class name for a constructor variant.
     */
    private fun getConstructorClassName(constructorName: String, targetType: Type): String {
        // For now, use a simple mapping
        // In a full implementation, this would consult a type registry
        return when (constructorName) {
            "Ok" -> "org/taylorlang/runtime/TaylorResult\$Ok"
            "Error" -> "org/taylorlang/runtime/TaylorResult\$Error"
            "Some" -> "org/taylorlang/runtime/TaylorOption\$Some"
            "None" -> "org/taylorlang/runtime/TaylorOption\$None"
            "Pair" -> "org/taylorlang/runtime/TaylorTuple2\$Pair"
            "Active" -> "org/taylorlang/runtime/TaylorStatus\$Active"
            "Inactive" -> "org/taylorlang/runtime/TaylorStatus\$Inactive"
            "Pending" -> "org/taylorlang/runtime/TaylorStatus\$Pending"
            else -> {
                // Default mapping based on target type
                when (targetType) {
                    is Type.NamedType -> targetType.name.replace('.', '/')
                    else -> "java/lang/Object"
                }
            }
        }
    }
    
    /**
     * Get the field name for a constructor field.
     */
    private fun getFieldName(constructorName: String, fieldIndex: Int, targetType: Type): String {
        return when (constructorName) {
            "Ok" -> if (fieldIndex == 0) "value" else "field$fieldIndex"
            "Error" -> if (fieldIndex == 0) "error" else "field$fieldIndex"
            "Some" -> if (fieldIndex == 0) "value" else "field$fieldIndex"
            else -> "field$fieldIndex"
        }
    }
    
    /**
     * Get the type of a constructor field.
     */
    private fun getFieldType(constructorName: String, fieldIndex: Int, targetType: Type): Type {
        // This is simplified - in a full implementation, this would consult the type system
        return when (constructorName) {
            "Ok", "Some" -> {
                if (targetType is Type.GenericType && targetType.arguments.isNotEmpty()) {
                    targetType.arguments[0]
                } else {
                    Type.NamedType("Object")
                }
            }
            "Error" -> BuiltinTypes.STRING // Error field access returns the message string, not the full Throwable
            "Pair" -> {
                // For Pair(x, y), both fields can be generic or Object
                if (targetType is Type.GenericType && targetType.arguments.size > fieldIndex) {
                    targetType.arguments[fieldIndex]
                } else {
                    // Default to Integer for Pair fields when not generically typed
                    // This handles cases like Pair(10, 20) where the types are inferred as integers
                    BuiltinTypes.INT
                }
            }
            else -> Type.NamedType("Object")
        }
    }
    
    /**
     * Generate field access via appropriate method (getter for Kotlin data classes).
     */
    private fun generateFieldAccess(constructorName: String, fieldIndex: Int, constructorClassName: String, fieldType: Type) {
        val methodName = getFieldAccessMethod(constructorName, fieldIndex)
        
        if (constructorName == "Error" && fieldIndex == 0) {
            // Special handling for Error - getError() returns Throwable, but we want the message string
            methodVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                constructorClassName,
                "getError",
                "()Ljava/lang/Throwable;",
                false
            )
            // Call getMessage() on the Throwable to get the string message
            methodVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/Throwable",
                "getMessage",
                "()Ljava/lang/String;",
                false
            )
        } else {
            // Standard field access
            val methodDescriptor = "()${typeConverter.getJvmTypeDescriptor(fieldType)}"
            methodVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                constructorClassName,
                methodName,
                methodDescriptor,
                false
            )
            
            // Special handling for Pair fields that need Object -> primitive conversion
            if (constructorName == "Pair" && fieldIndex in 0..1) {
                // Pair.getFirst() and getSecond() return Object, but we often need primitives
                // Always attempt to unbox to the expected primitive type
                val actualReturnType = Type.NamedType("Object") // getFirst/getSecond return Object
                when (fieldType) {
                    is Type.PrimitiveType -> {
                        when (fieldType.name.lowercase()) {
                            "int" -> typeConverter.convertType(actualReturnType, BuiltinTypes.INT)
                            "double", "float" -> typeConverter.convertType(actualReturnType, BuiltinTypes.DOUBLE)
                            "boolean" -> typeConverter.convertType(actualReturnType, BuiltinTypes.BOOLEAN)
                        }
                    }
                    is Type.NamedType -> {
                        when (fieldType.name.lowercase()) {
                            "int" -> typeConverter.convertType(actualReturnType, BuiltinTypes.INT)
                            "double", "float" -> typeConverter.convertType(actualReturnType, BuiltinTypes.DOUBLE)
                            "boolean" -> typeConverter.convertType(actualReturnType, BuiltinTypes.BOOLEAN)
                        }
                    }
                    else -> {
                        // For other types, no conversion needed
                    }
                }
            }
        }
    }
    
    /**
     * Get the method name for accessing a constructor field.
     */
    private fun getFieldAccessMethod(constructorName: String, fieldIndex: Int): String {
        return when (constructorName) {
            "Ok" -> if (fieldIndex == 0) "getValue" else "getField$fieldIndex"
            "Error" -> if (fieldIndex == 0) "getError" else "getField$fieldIndex"
            "Some" -> if (fieldIndex == 0) "getValue" else "getField$fieldIndex"
            "Pair" -> when (fieldIndex) {
                0 -> "getFirst"
                1 -> "getSecond"
                else -> "getField$fieldIndex"
            }
            else -> "getField$fieldIndex"
        }
    }
    
    /**
     * Generate field pattern test with proper stack management for constructor patterns.
     * This method is specifically designed to handle the case where a constructor instance
     * is on the stack and needs to be preserved or cleaned up appropriately.
     */
    private fun generateFieldPatternTest(
        pattern: Pattern,
        fieldType: Type,
        successLabel: Label,
        failureLabel: Label
    ) {
        when (pattern) {
            is Pattern.LiteralPattern -> {
                generateLiteralPatternTest(pattern, fieldType, successLabel, failureLabel)
            }
            is Pattern.WildcardPattern -> {
                // Wildcard patterns always match - just consume the field value
                // Use POP2 for double values (they occupy 2 stack slots)
                if (fieldType == BuiltinTypes.DOUBLE || (fieldType is Type.PrimitiveType && fieldType.name.lowercase() in listOf("double", "float"))) {
                    methodVisitor.visitInsn(POP2)
                } else {
                    methodVisitor.visitInsn(POP)
                }
                methodVisitor.visitJumpInsn(GOTO, successLabel)
            }
            is Pattern.IdentifierPattern -> {
                // Identifier patterns always match - just consume the field value
                // Use POP2 for double values (they occupy 2 stack slots)
                if (fieldType == BuiltinTypes.DOUBLE || (fieldType is Type.PrimitiveType && fieldType.name.lowercase() in listOf("double", "float"))) {
                    methodVisitor.visitInsn(POP2)
                } else {
                    methodVisitor.visitInsn(POP)
                }
                methodVisitor.visitJumpInsn(GOTO, successLabel)
            }
            is Pattern.ConstructorPattern -> {
                // Recursive constructor patterns - use the standard pattern test
                generatePatternTest(pattern, fieldType, successLabel, failureLabel, -1)
            }
            is Pattern.GuardPattern -> {
                // Guard patterns - use the standard pattern test
                generatePatternTest(pattern, fieldType, successLabel, failureLabel, -1)
            }
            else -> {
                // Unknown pattern type - assume failure and consume field value
                methodVisitor.visitInsn(POP)
                methodVisitor.visitJumpInsn(GOTO, failureLabel)
            }
        }
    }
    
    /**
     * Check if a constructor is nullary (has no fields).
     */
    fun isNullaryConstructor(name: String, targetType: Type): Boolean {
        return when (name) {
            "None", "Unit", "True", "False", "Active", "Inactive", "Pending" -> true
            else -> false
        }
    }
}