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
                endLabel // Last case, jump to end if no match
            }
            
            // Load target value for comparison
            val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            
            // Generate pattern test
            generatePatternTest(case.pattern, targetType, caseBodyLabel, nextCaseLabel)
            
            // If this isn't the last case, add the next case label
            if (i < matchExpr.cases.size - 1) {
                methodVisitor.visitLabel(nextCaseLabel)
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
            
            // For statements, check if the expression result needs to be popped or left on stack
            // If the case expression is a void-returning function call, no value is left on stack
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
            
            // If this case expression left a value on the stack but we need to match the result type
            // of the overall match expression, we may need to convert or leave it
            if (leavesValueOnStack && getJvmType(resultType) == "V") {
                // Case expression returned a value but match expression should return void - pop it
                methodVisitor.visitInsn(POP)
            } else if (!leavesValueOnStack && getJvmType(resultType) != "V") {
                // Case expression was void but match expression should return a value - push default
                when (getJvmType(resultType)) {
                    "I", "Z" -> methodVisitor.visitLdcInsn(0)
                    "D" -> methodVisitor.visitLdcInsn(0.0)
                    "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                    else -> methodVisitor.visitInsn(ACONST_NULL)
                }
            }
            
            // Restore variable slot state
            variableSlotManager.restoreCheckpoint(savedSlotManager)
            
            // Jump to end (don't fall through to next case)
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
                methodVisitor.visitInsn(POP) // Remove target value from stack
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
                    methodVisitor.visitInsn(POP) // Remove target value from stack
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
                // Stack starts with: [target_double]
                methodVisitor.visitLdcInsn(literal.value)
                // Stack now: [target_double, literal_double]
                // Use DCMPG for double comparison
                methodVisitor.visitInsn(DCMPG)
                // DCMPG consumes both doubles and pushes int result:
                // 0 if equal, >0 if target > literal, <0 if target < literal
                // Stack now: [comparison_result]
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
                methodVisitor.visitInsn(POP) // Remove target value
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
        // For now, implement basic constructor matching
        // TODO: Full union type runtime support will be needed
        
        // Placeholder: assume the constructor name matches some field or method
        // Real implementation would check union type variant tags
        methodVisitor.visitInsn(POP) // Remove target value for now
        
        // For demonstration, always jump to case (this will be properly implemented
        // when union type runtime representation is finalized)
        methodVisitor.visitJumpInsn(GOTO, caseLabel)
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
        methodVisitor.visitInsn(POP) // Remove target value
        methodVisitor.visitJumpInsn(GOTO, caseLabel)
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
        methodVisitor.visitInsn(if (getJvmType(targetType) == "D") DUP2 else DUP)
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
                // TODO: Implement field binding for constructor patterns
                // This will require union type runtime support for field extraction
            }
            // Wildcard and literal patterns don't bind variables
            else -> { }
        }
    }
    
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
                    else -> "Ljava/lang/Object;"
                }
            }
            else -> "Ljava/lang/Object;"
        }
    }
}