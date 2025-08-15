package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode compiler for pattern matching constructs.
 * 
 * Handles compilation of:
 * - Match expressions with multiple patterns
 * - Pattern tests and jump table generation
 * - Variable binding in patterns
 * - Guard expressions
 * - Constructor and literal pattern matching
 */
class PatternBytecodeCompiler(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val expressionGenerator: ExpressionBytecodeGenerator,
    private val generateExpressionCallback: ((TypedExpression) -> Unit)? = null
) {
    
    /**
     * Generate code for match expressions
     */
    fun generateMatchExpression(matchExpr: MatchExpression, resultType: Type) {
        if (matchExpr.cases.isEmpty()) {
            // Empty match - push default value and return
            when (getJvmType(resultType)) {
                "I", "Z" -> methodVisitor.visitLdcInsn(0)
                "D" -> methodVisitor.visitLdcInsn(0.0)
                "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                "V" -> { /* Unit/void - no value to push */ }
                else -> methodVisitor.visitInsn(ACONST_NULL)
            }
            return
        }
        
        // Generate the target expression and store it in a local variable for repeated access
        val targetType = expressionGenerator.inferExpressionType(matchExpr.target)
        generateExpression(TypedExpression(matchExpr.target, targetType))
        
        // Store target value in a temporary slot for pattern matching
        val targetSlot = variableSlotManager.allocateTemporarySlot(targetType)
        val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
        methodVisitor.visitVarInsn(storeInstruction, targetSlot)
        
        // Create labels
        val caseBodyLabels = matchExpr.cases.map { org.objectweb.asm.Label() }
        val endLabel = org.objectweb.asm.Label()
        
        // Generate pattern tests and jumps
        for (i in matchExpr.cases.indices) {
            val case = matchExpr.cases[i]
            val caseBodyLabel = caseBodyLabels[i]
            val nextCaseLabel = if (i < matchExpr.cases.size - 1) {
                org.objectweb.asm.Label() // Label for next case test
            } else {
                // For the last case, we need a separate label to handle match failure
                // This ensures we can properly set up the stack before jumping to endLabel
                org.objectweb.asm.Label()
            }
            
            // Load target value for comparison
            val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            
            // Generate pattern test
            generatePatternTest(case.pattern, targetType, caseBodyLabel, nextCaseLabel)
            
            // Handle the next case label
            if (i < matchExpr.cases.size - 1) {
                methodVisitor.visitLabel(nextCaseLabel)
            } else {
                // Last case failure - ensure consistent stack state before jumping to endLabel
                methodVisitor.visitLabel(nextCaseLabel)
                // The stack should be empty here from the failed pattern test
                // Push a default value to match the expected result type
                when (getJvmType(resultType)) {
                    "V" -> { /* No value needed for void */ }
                    "I", "Z" -> methodVisitor.visitLdcInsn(0)
                    "D" -> methodVisitor.visitLdcInsn(0.0)
                    "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                    else -> methodVisitor.visitInsn(ACONST_NULL)
                }
                methodVisitor.visitJumpInsn(GOTO, endLabel)
            }
        }
        
        // Generate case bodies
        for (i in matchExpr.cases.indices) {
            val case = matchExpr.cases[i]
            val caseBodyLabel = caseBodyLabels[i]
            
            // Case body label
            methodVisitor.visitLabel(caseBodyLabel)
            
            // Create new scope for pattern variable bindings
            val savedSlotManager = variableSlotManager.createCheckpoint()
            
            // Bind pattern variables (if any)
            bindPatternVariables(case.pattern, targetType, targetSlot)
            
            // Generate case expression
            val caseExprType = expressionGenerator.inferExpressionType(case.expression)
            generateExpression(TypedExpression(case.expression, caseExprType))
            
            // CRITICAL FIX: Ensure ALL case branches leave the SAME stack state
            // The match expression expects consistent results from all branches
            val leavesValueOnStack = when (case.expression) {
                is FunctionCall -> {
                    if ((case.expression.target as? Identifier)?.name == "println") {
                        false // println returns void
                    } else {
                        getJvmType(caseExprType) != "V"
                    }
                }
                else -> getJvmType(caseExprType) != "V"
            }
            
            // CRITICAL FIX: Always ensure we leave exactly what the match expression expects
            // This prevents stackmap frame inconsistencies at the merge point (endLabel)
            if (getJvmType(resultType) == "V") {
                // Match expression expects void - pop any values left on stack
                if (leavesValueOnStack) {
                    if (getJvmType(caseExprType) == "D") {
                        methodVisitor.visitInsn(POP2) // Pop double value (2 slots)
                    } else {
                        methodVisitor.visitInsn(POP) // Pop single value
                    }
                }
                // Stack is now empty for all branches - consistent!
            } else {
                // Match expression expects a value - ensure we have one
                if (!leavesValueOnStack) {
                    // Push default value to match expected result type
                    when (getJvmType(resultType)) {
                        "I", "Z" -> methodVisitor.visitLdcInsn(0)
                        "D" -> methodVisitor.visitLdcInsn(0.0)
                        "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                        else -> methodVisitor.visitInsn(ACONST_NULL)
                    }
                }
                // Stack now has exactly one value of the expected type for all branches - consistent!
            }
            
            // Restore variable slot state
            variableSlotManager.restoreCheckpoint(savedSlotManager)
            
            // Jump to end (don't fall through to next case)
            // All branches now have identical stack states when reaching endLabel
            methodVisitor.visitJumpInsn(GOTO, endLabel)
        }
        
        // End label
        methodVisitor.visitLabel(endLabel)
        
        // Release temporary slot
        variableSlotManager.releaseTemporarySlot(targetSlot)
    }
    
    /**
     * Generate pattern test logic that jumps to caseLabel if pattern matches,
     * otherwise falls through to nextLabel
     */
    fun generatePatternTest(
        pattern: Pattern, 
        targetType: Type, 
        caseLabel: org.objectweb.asm.Label, 
        nextLabel: org.objectweb.asm.Label
    ) {
        when (pattern) {
            is Pattern.WildcardPattern -> {
                // Wildcard always matches - remove value and jump to case
                // CRITICAL FIX: Handle double-width values properly
                if (getJvmType(targetType) == "D") {
                    methodVisitor.visitInsn(POP2) // Remove double value (2 slots)
                } else {
                    methodVisitor.visitInsn(POP) // Remove single value
                }
                // Stack is now empty - consistent with other pattern success paths
                methodVisitor.visitJumpInsn(GOTO, caseLabel)
            }
            is Pattern.LiteralPattern -> {
                generateLiteralPatternMatch(pattern.literal, targetType, caseLabel, nextLabel)
            }
            is Pattern.ConstructorPattern -> {
                generateConstructorPatternMatch(pattern, targetType, caseLabel, nextLabel)
            }
            is Pattern.IdentifierPattern -> {
                // Check if this is a nullary constructor or a variable binding
                if (isNullaryConstructor(pattern.name, targetType)) {
                    generateNullaryConstructorMatch(pattern.name, targetType, caseLabel, nextLabel)
                } else {
                    // Variable pattern - always matches, remove value and jump
                    // CRITICAL FIX: Handle double-width values properly
                    if (getJvmType(targetType) == "D") {
                        methodVisitor.visitInsn(POP2) // Remove double value (2 slots)
                    } else {
                        methodVisitor.visitInsn(POP) // Remove single value
                    }
                    // Stack is now empty - consistent with other pattern success paths
                    methodVisitor.visitJumpInsn(GOTO, caseLabel)
                }
            }
            is Pattern.GuardPattern -> {
                // First check the inner pattern, then evaluate guard
                generateGuardPatternMatch(pattern, targetType, caseLabel, nextLabel)
            }
        }
    }
    
    /**
     * Generate literal pattern matching
     */
    private fun generateLiteralPatternMatch(
        literal: Literal,
        targetType: Type,
        caseLabel: org.objectweb.asm.Label,
        nextLabel: org.objectweb.asm.Label
    ) {
        // Stack: [target_value]
        // Generate literal value for comparison
        when (literal) {
            is Literal.IntLiteral -> {
                methodVisitor.visitLdcInsn(literal.value)
                // Compare integers: IF_ICMPEQ jumps if equal, consumes both values
                methodVisitor.visitJumpInsn(IF_ICMPEQ, caseLabel)
                // If we reach here, comparison failed and stack is empty
                // No need to pop anything
            }
            is Literal.BooleanLiteral -> {
                methodVisitor.visitLdcInsn(if (literal.value) 1 else 0)
                methodVisitor.visitJumpInsn(IF_ICMPEQ, caseLabel)
                // If we reach here, comparison failed and stack is empty
            }
            is Literal.StringLiteral -> {
                methodVisitor.visitLdcInsn(literal.value)
                // Use String.equals() for string comparison
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/String",
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false
                )
                methodVisitor.visitJumpInsn(IFNE, caseLabel)
                // If we reach here, comparison failed and stack is empty
            }
            is Literal.FloatLiteral -> {
                // CRITICAL FIX: Handle double-width values properly
                // Stack starts with: [target_double] (takes 2 stack positions)
                methodVisitor.visitLdcInsn(literal.value)
                // Stack now: [target_double, literal_double] (4 stack positions total)
                
                // Use DCMPG for double comparison
                // DCMPG pops two doubles (4 stack positions) and pushes one int
                methodVisitor.visitInsn(DCMPG)
                // Stack now: [comparison_result] (1 stack position)
                // Result: 0 if equal, >0 if target > literal, <0 if target < literal
                methodVisitor.visitJumpInsn(IFEQ, caseLabel)
                // If we reach here, comparison failed and stack is empty
            }
            is Literal.NullLiteral -> {
                // Compare with null
                methodVisitor.visitJumpInsn(IFNULL, caseLabel)
                // If we reach here, comparison failed and stack is empty
            }
            else -> {
                // Unsupported literal - treat as non-matching
                // For double-width values, pop twice
                if (getJvmType(targetType) == "D") {
                    methodVisitor.visitInsn(POP2) // Remove double value (2 slots)
                } else {
                    methodVisitor.visitInsn(POP) // Remove single value
                }
            }
        }
        // If we reach here, pattern didn't match and stack is empty - ready for next pattern
    }
    
    /**
     * Generate constructor pattern matching for union types
     */
    private fun generateConstructorPatternMatch(
        pattern: Pattern.ConstructorPattern,
        targetType: Type,
        caseLabel: org.objectweb.asm.Label,
        nextLabel: org.objectweb.asm.Label
    ) {
        // Stack: [target_value]
        
        // 1. Check if target is instanceof the constructor variant class
        val constructorClassName = getConstructorClassName(pattern.constructor, targetType)
        
        // Duplicate the target value for instanceof check
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitTypeInsn(INSTANCEOF, constructorClassName)
        
        // Create a label for when instanceof check succeeds
        val instanceofSuccessLabel = org.objectweb.asm.Label()
        methodVisitor.visitJumpInsn(IFNE, instanceofSuccessLabel) // If instanceof succeeds, jump to success
        
        // CRITICAL FIX: instanceof check failed - remove target value and jump to next pattern
        // Stack: [target_value] (DUP consumed by instanceof, original value remains)
        methodVisitor.visitInsn(POP) // Remove target value
        methodVisitor.visitJumpInsn(GOTO, nextLabel) // Jump to next pattern with empty stack
        
        // instanceof check succeeded
        methodVisitor.visitLabel(instanceofSuccessLabel)
        // Stack: [target_value] (original value from DUP)
        
        // 2. Cast to constructor type for field access
        methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
        
        // Stack: [cast_target]
        
        // 3. If no nested patterns, constructor pattern matches - jump to case
        if (pattern.patterns.isEmpty()) {
            // Remove cast target from stack and jump to case with empty stack
            methodVisitor.visitInsn(POP)
            methodVisitor.visitJumpInsn(GOTO, caseLabel)
            return
        }
        
        // 4. Store cast target for multiple field accesses
        val constructorSlot = variableSlotManager.allocateTemporarySlot(targetType)
        methodVisitor.visitVarInsn(ASTORE, constructorSlot)
        
        // Stack: []
        
        // 5. Check each nested pattern against corresponding field
        // CRITICAL FIX: Use a single failure label for all field pattern failures
        // to ensure consistent stack states at merge points
        val allFieldsFailLabel = org.objectweb.asm.Label()
        
        for ((index, fieldPattern) in pattern.patterns.withIndex()) {
            // Load constructor object and access field
            methodVisitor.visitVarInsn(ALOAD, constructorSlot)
            
            // Get field value based on constructor type and field index
            val fieldName = getFieldName(pattern.constructor, index, targetType)
            val fieldType = getFieldType(pattern.constructor, index, targetType)
            val fieldDescriptor = getJvmTypeDescriptor(fieldType)
            
            // Use getter method instead of direct field access for Kotlin data classes
            val getterMethodName = "get" + fieldName.replaceFirstChar { it.uppercaseChar() }
            methodVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                constructorClassName,
                getterMethodName,
                "()" + fieldDescriptor,
                false
            )
            
            // Stack: [field_value]
            
            // Create labels for nested pattern matching
            val fieldMatchLabel = org.objectweb.asm.Label()
            
            // Generate pattern test for this field - use common failure label
            generatePatternTest(fieldPattern, fieldType, fieldMatchLabel, allFieldsFailLabel)
            
            // Field pattern succeeded, continue to next field
            methodVisitor.visitLabel(fieldMatchLabel)
            // Stack should be empty here from pattern test success
        }
        
        // 6. All nested patterns matched - clean up and jump to case with empty stack
        variableSlotManager.releaseTemporarySlot(constructorSlot)
        methodVisitor.visitJumpInsn(GOTO, caseLabel)
        
        // 7. Handle field pattern failure - ensure consistent stack state
        methodVisitor.visitLabel(allFieldsFailLabel)
        // Stack should be empty here from pattern test failure
        variableSlotManager.releaseTemporarySlot(constructorSlot)
        methodVisitor.visitJumpInsn(GOTO, nextLabel)
    }
    
    /**
     * Generate nullary constructor matching
     */
    private fun generateNullaryConstructorMatch(
        constructorName: String,
        targetType: Type,
        caseLabel: org.objectweb.asm.Label,
        nextLabel: org.objectweb.asm.Label
    ) {
        // Similar to constructor pattern but for nullary constructors
        // TODO: Implement with proper union type runtime support
        // CRITICAL FIX: Handle double-width values properly
        if (getJvmType(targetType) == "D") {
            methodVisitor.visitInsn(POP2) // Remove double value (2 slots)
        } else {
            methodVisitor.visitInsn(POP) // Remove single value
        }
        methodVisitor.visitJumpInsn(GOTO, caseLabel)
    }
    
    /**
     * Generate list pattern matching
     */
    
    /**
     * Extract element type from a list type
     */
    private fun extractElementType(listType: Type): Type {
        return when (listType) {
            is Type.GenericType -> {
                if (listType.name == "List" && listType.arguments.isNotEmpty()) {
                    listType.arguments[0]
                } else {
                    // Default to Object if type arguments are missing
                    Type.NamedType("Object")
                }
            }
            else -> Type.NamedType("Object") // Default fallback
        }
    }
    
    /**
     * Generate guard pattern matching
     */
    private fun generateGuardPatternMatch(
        pattern: Pattern.GuardPattern,
        targetType: Type,
        caseLabel: org.objectweb.asm.Label,
        nextLabel: org.objectweb.asm.Label
    ) {
        // For guard patterns, we need to store the target value first
        // since the inner pattern test will consume it
        val guardTargetSlot = variableSlotManager.allocateTemporarySlot(targetType)
        val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
        val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
        
        // Duplicate the target value and store it for guard evaluation
        // CRITICAL FIX: Handle double-width values properly for duplication
        if (getJvmType(targetType) == "D") {
            methodVisitor.visitInsn(DUP2) // Duplicate 2-slot double value
        } else {
            methodVisitor.visitInsn(DUP) // Duplicate 1-slot value
        }
        methodVisitor.visitVarInsn(storeInstruction, guardTargetSlot)
        
        // Create intermediate label for guard evaluation
        val guardLabel = org.objectweb.asm.Label()
        
        // First, match the inner pattern
        generatePatternTest(pattern.pattern, targetType, guardLabel, nextLabel)
        
        // If inner pattern matched, evaluate guard
        methodVisitor.visitLabel(guardLabel)
        
        // Bind pattern variables for guard evaluation
        val savedSlotManager = variableSlotManager.createCheckpoint()
        bindPatternVariables(pattern.pattern, targetType, guardTargetSlot)
        
        // Generate guard expression
        val guardType = expressionGenerator.inferExpressionType(pattern.guard)
        generateExpression(TypedExpression(pattern.guard, guardType))
        
        // Restore slot manager state
        variableSlotManager.restoreCheckpoint(savedSlotManager)
        
        // Release temporary slot
        variableSlotManager.releaseTemporarySlot(guardTargetSlot)
        
        // Jump to case if guard is true
        methodVisitor.visitJumpInsn(IFNE, caseLabel)
        
        // If guard is false, fall through to next pattern
    }
    
    /**
     * Bind pattern variables to local slots
     */
    fun bindPatternVariables(pattern: Pattern, targetType: Type, targetSlot: Int) {
        when (pattern) {
            is Pattern.IdentifierPattern -> {
                if (!isNullaryConstructor(pattern.name, targetType)) {
                    // Variable binding - copy target value to new slot
                    val slot = variableSlotManager.allocateSlot(pattern.name, targetType)
                    val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
                    val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
                    
                    methodVisitor.visitVarInsn(loadInstruction, targetSlot)
                    methodVisitor.visitVarInsn(storeInstruction, slot)
                }
            }
            is Pattern.GuardPattern -> {
                bindPatternVariables(pattern.pattern, targetType, targetSlot)
            }
            is Pattern.ConstructorPattern -> {
                // Bind variables from constructor pattern fields
                bindConstructorPatternVariables(pattern, targetType, targetSlot)
            }
            // Wildcard and literal patterns don't bind variables
            else -> { }
        }
    }
    
    /**
     * Bind variables for constructor patterns
     */
    private fun bindConstructorPatternVariables(pattern: Pattern.ConstructorPattern, targetType: Type, targetSlot: Int) {
        if (pattern.patterns.isEmpty()) {
            return // No nested patterns to bind
        }
        
        // Cast target to constructor type and store
        val constructorClassName = getConstructorClassName(pattern.constructor, targetType)
        methodVisitor.visitVarInsn(ALOAD, targetSlot)
        methodVisitor.visitTypeInsn(CHECKCAST, constructorClassName)
        
        val constructorSlot = variableSlotManager.allocateTemporarySlot(targetType)
        methodVisitor.visitVarInsn(ASTORE, constructorSlot)
        
        // Bind each field pattern
        for ((index, fieldPattern) in pattern.patterns.withIndex()) {
            when (fieldPattern) {
                is Pattern.IdentifierPattern -> {
                    if (!isNullaryConstructor(fieldPattern.name, getFieldType(pattern.constructor, index, targetType))) {
                        // Variable binding - extract field and store in new slot
                        val fieldType = getFieldType(pattern.constructor, index, targetType)
                        val fieldSlot = variableSlotManager.allocateSlot(fieldPattern.name, fieldType)
                        
                        // Load constructor object
                        methodVisitor.visitVarInsn(ALOAD, constructorSlot)
                        
                        // Get field using getter method
                        val fieldName = getFieldName(pattern.constructor, index, targetType)
                        val fieldDescriptor = getJvmTypeDescriptor(fieldType)
                        val getterMethodName = "get" + fieldName.replaceFirstChar { it.uppercaseChar() }
                        methodVisitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            constructorClassName,
                            getterMethodName,
                            "()" + fieldDescriptor,
                            false
                        )
                        
                        // Store in variable slot
                        val storeInstruction = variableSlotManager.getStoreInstruction(fieldType)
                        methodVisitor.visitVarInsn(storeInstruction, fieldSlot)
                    }
                }
                is Pattern.ConstructorPattern -> {
                    // Nested constructor pattern - recursively bind
                    // Get field value and create temporary slot for it
                    val fieldType = getFieldType(pattern.constructor, index, targetType)
                    val fieldSlot = variableSlotManager.allocateTemporarySlot(fieldType)
                    
                    // Load constructor and get field using getter method
                    methodVisitor.visitVarInsn(ALOAD, constructorSlot)
                    val fieldName = getFieldName(pattern.constructor, index, targetType)
                    val fieldDescriptor = getJvmTypeDescriptor(fieldType)
                    val getterMethodName = "get" + fieldName.replaceFirstChar { it.uppercaseChar() }
                    methodVisitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        constructorClassName,
                        getterMethodName,
                        "()" + fieldDescriptor,
                        false
                    )
                    
                    // Store field value
                    val storeInstruction = variableSlotManager.getStoreInstruction(fieldType)
                    methodVisitor.visitVarInsn(storeInstruction, fieldSlot)
                    
                    // Recursively bind nested pattern
                    bindPatternVariables(fieldPattern, fieldType, fieldSlot)
                    
                    // Release temporary slot
                    variableSlotManager.releaseTemporarySlot(fieldSlot)
                }
                // TODO: Handle other pattern types (lists, guards, etc.) as needed
                else -> {
                    // For other pattern types, extract field but don't bind yet
                    // This is a simplified implementation
                }
            }
        }
        
        // Release constructor slot
        variableSlotManager.releaseTemporarySlot(constructorSlot)
    }
    
    /**
     * Bind variables for list patterns
     */
    
    /**
     * Check if an identifier is a nullary constructor for the given type
     */
    private fun isNullaryConstructor(name: String, targetType: Type): Boolean {
        // TODO: Implement proper check using type definitions
        // For now, return false to treat all identifiers as variable bindings
        return false
    }
    
    /**
     * Helper method to generate expressions - uses callback if available, otherwise falls back to expressionGenerator
     */
    private fun generateExpression(expr: TypedExpression) {
        if (generateExpressionCallback != null) {
            generateExpressionCallback.invoke(expr)
        } else {
            expressionGenerator.generateExpression(expr)
        }
    }
    
    /**
     * Map TaylorLang type to JVM type descriptor
     */
    private fun getJvmType(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit", "void" -> "V"
                    "object" -> "Ljava/lang/Object;"
                    else -> "Ljava/lang/Object;"
                }
            }
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Get JVM type descriptor for a type (used for field descriptors)
     */
    private fun getJvmTypeDescriptor(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit", "void" -> "V"
                    "object" -> "Ljava/lang/Object;"
                    else -> "L${type.name};"
                }
            }
            is Type.UnionType -> "L${type.name};"
            is Type.GenericType -> "L${type.name};"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Get the JVM class name for a constructor variant
     * Maps TaylorLang union types to their runtime implementations
     */
    private fun getConstructorClassName(constructorName: String, targetType: Type): String {
        val unionTypeName = when (targetType) {
            is Type.NamedType -> targetType.name
            is Type.UnionType -> targetType.name
            is Type.GenericType -> targetType.name
            else -> "UnknownType"
        }
        
        // Map TaylorLang types to their runtime implementations
        val runtimeTypeName = when (unionTypeName) {
            "Result" -> "org/taylorlang/runtime/TaylorResult"
            "Option" -> "org/taylorlang/runtime/TaylorOption"
            "Status" -> "org/taylorlang/runtime/TaylorStatus"
            "Tuple2" -> "org/taylorlang/runtime/TaylorTuple2"
            // Add other union types as they are implemented
            else -> unionTypeName
        }
        
        return "$runtimeTypeName\$$constructorName"
    }
    
    /**
     * Get the field name for a specific field in a constructor variant
     */
    private fun getFieldName(constructorName: String, fieldIndex: Int, targetType: Type): String {
        val unionTypeName = when (targetType) {
            is Type.NamedType -> targetType.name
            is Type.UnionType -> targetType.name
            is Type.GenericType -> targetType.name
            else -> "UnknownType"
        }
        
        // Map to known runtime implementations
        return when (unionTypeName) {
            "Result" -> when (constructorName) {
                "Ok" -> "value"
                "Error" -> "error"
                else -> "field$fieldIndex"
            }
            "Option" -> when (constructorName) {
                "Some" -> "value"
                "None" -> "field$fieldIndex" // None has no fields
                else -> "field$fieldIndex"
            }
            "Status" -> "field$fieldIndex" // Active, Inactive, Pending have no fields
            "Tuple2" -> when (constructorName) {
                "Pair" -> when (fieldIndex) {
                    0 -> "first"
                    1 -> "second"
                    else -> "field$fieldIndex"
                }
                else -> "field$fieldIndex"
            }
            // Add other union types as they are implemented
            else -> "field$fieldIndex"
        }
    }
    
    /**
     * Get the type of a specific field in a constructor variant
     */
    private fun getFieldType(constructorName: String, fieldIndex: Int, targetType: Type): Type {
        val unionTypeName = when (targetType) {
            is Type.NamedType -> targetType.name
            is Type.UnionType -> targetType.name
            is Type.GenericType -> targetType.name
            else -> "UnknownType"
        }
        
        // Map to known runtime implementations
        return when (unionTypeName) {
            "Result" -> when (constructorName) {
                "Ok" -> {
                    // For TaylorResult.Ok, the value field type depends on the generic parameter
                    // For now, return Object as a safe default
                    Type.NamedType("Object")
                }
                "Error" -> {
                    // For TaylorResult.Error, use Object for better JVM compatibility
                    Type.NamedType("Object")
                }
                else -> Type.NamedType("Object")
            }
            "Option" -> when (constructorName) {
                "Some" -> Type.NamedType("Object") // Generic value
                "None" -> Type.NamedType("Object") // No fields, but safe default
                else -> Type.NamedType("Object")
            }
            "Status" -> Type.NamedType("Object") // No fields
            "Tuple2" -> when (constructorName) {
                "Pair" -> Type.NamedType("Object") // Generic fields
                else -> Type.NamedType("Object")
            }
            // Add other union types as they are implemented
            else -> Type.NamedType("Object")
        }
    }
}