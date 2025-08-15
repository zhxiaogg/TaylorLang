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
        failureLabel: Label
    ) {
        when (pattern) {
            is Pattern.LiteralPattern -> {
                generateLiteralPatternTest(pattern, targetType, successLabel, failureLabel)
            }
            is Pattern.IdentifierPattern -> {
                // Identifier patterns always match - they just bind the value
                methodVisitor.visitJumpInsn(GOTO, successLabel)
            }
            is Pattern.ConstructorPattern -> {
                generateConstructorPatternTest(pattern, targetType, successLabel, failureLabel)
            }
            is Pattern.GuardPattern -> {
                generateGuardPatternTest(pattern, targetType, successLabel, failureLabel)
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
        methodVisitor.visitJumpInsn(IFEQ, failureLabel)
        
        // Cast to constructor type
        methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
        
        // Test each field pattern
        for (i in pattern.patterns.indices) {
            val fieldPattern = pattern.patterns[i]
            val fieldType = getFieldType(constructorName, i, targetType)
            
            // Duplicate constructor instance for field access
            methodVisitor.visitInsn(DUP)
            
            // Access field
            val fieldName = getFieldName(constructorName, i, targetType)
            val fieldDescriptor = typeConverter.getJvmTypeDescriptor(fieldType)
            methodVisitor.visitFieldInsn(GETFIELD, constructorClassName, fieldName, fieldDescriptor)
            
            // Test field pattern recursively
            val fieldSuccessLabel = Label()
            generatePatternTest(fieldPattern, fieldType, fieldSuccessLabel, failureLabel)
            
            methodVisitor.visitLabel(fieldSuccessLabel)
        }
        
        // All field patterns matched
        methodVisitor.visitInsn(POP) // Remove constructor instance
        methodVisitor.visitJumpInsn(GOTO, successLabel)
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
     */
    private fun generateGuardPatternTest(
        pattern: Pattern.GuardPattern,
        targetType: Type,
        successLabel: Label,
        failureLabel: Label
    ) {
        // First test the inner pattern
        val guardSuccessLabel = Label()
        generatePatternTest(pattern.pattern, targetType, guardSuccessLabel, failureLabel)
        
        // If inner pattern matches, evaluate guard
        methodVisitor.visitLabel(guardSuccessLabel)
        
        // Generate guard expression
        generateExpressionCallback(TypedExpression(pattern.guard, BuiltinTypes.BOOLEAN))
        
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
        
        for (i in pattern.patterns.indices) {
            val fieldPattern = pattern.patterns[i]
            val fieldType = getFieldType(pattern.constructor, i, targetType)
            
            // Load constructor instance
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
            
            // Access field
            val fieldName = getFieldName(pattern.constructor, i, targetType)
            val fieldDescriptor = typeConverter.getJvmTypeDescriptor(fieldType)
            methodVisitor.visitFieldInsn(GETFIELD, constructorClassName, fieldName, fieldDescriptor)
            
            // Store field value in temporary slot
            val fieldSlot = variableSlotManager.allocateTemporarySlot(fieldType)
            val fieldStoreInstruction = variableSlotManager.getStoreInstruction(fieldType)
            methodVisitor.visitVarInsn(fieldStoreInstruction, fieldSlot)
            
            // Recursively bind variables in field pattern
            bindPatternVariables(fieldPattern, fieldType, fieldSlot)
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
            "Some" -> "org/taylorlang/runtime/Option\$Some"
            "None" -> "org/taylorlang/runtime/Option\$None"
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
            "Error" -> BuiltinTypes.THROWABLE
            else -> Type.NamedType("Object")
        }
    }
    
    /**
     * Check if a constructor is nullary (has no fields).
     */
    fun isNullaryConstructor(name: String, targetType: Type): Boolean {
        return when (name) {
            "None", "Unit", "True", "False" -> true
            else -> false
        }
    }
}