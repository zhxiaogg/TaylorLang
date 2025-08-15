package org.taylorlang.codegen

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Refactored pattern bytecode compiler with separated concerns.
 * 
 * This class now delegates to specialized components:
 * - TypeConverter: Handles type conversions and boxing/unboxing
 * - PatternMatcher: Handles pattern matching logic
 * - BytecodeGeneratorUtils: Handles low-level bytecode operations
 * 
 * This architecture fixes the VerifyError issues and improves maintainability.
 */
class PatternBytecodeCompiler(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val expressionGenerator: ExpressionBytecodeGenerator,
    private val generateExpressionCallback: ((TypedExpression) -> Unit)? = null
) {
    
    // Delegate components for separated concerns
    private val typeConverter = TypeConverter(methodVisitor)
    private val bytecodeUtils = BytecodeGeneratorUtils(methodVisitor, typeConverter)
    private val patternMatcher = PatternMatcher(
        methodVisitor, 
        variableSlotManager, 
        typeConverter, 
        this::generateExpression
    )
    
    /**
     * Generate code for match expressions.
     * 
     * This method now has much cleaner logic thanks to the separated concerns.
     */
    fun generateMatchExpression(matchExpr: MatchExpression, resultType: Type) {
        if (matchExpr.cases.isEmpty()) {
            // Empty match - generate default value
            typeConverter.generateDefaultValue(resultType)
            return
        }
        
        // Generate the target expression and store it for repeated access
        val targetType = expressionGenerator.inferExpressionType(matchExpr.target)
        generateExpression(TypedExpression(matchExpr.target, targetType))
        
        // Store target value in a temporary slot for pattern matching
        val targetSlot = variableSlotManager.allocateTemporarySlot(targetType)
        val storeInstruction = variableSlotManager.getStoreInstruction(targetType)
        methodVisitor.visitVarInsn(storeInstruction, targetSlot)
        
        // Generate labels for cases
        val (caseBodyLabels, endLabel) = bytecodeUtils.generateCaseLabels(matchExpr.cases.size)
        
        // Generate pattern tests and jumps
        generatePatternTests(matchExpr.cases, targetType, targetSlot, caseBodyLabels, endLabel)
        
        // Generate case bodies
        generateCaseBodies(matchExpr.cases, targetType, targetSlot, caseBodyLabels, endLabel, resultType)
        
        // End label
        methodVisitor.visitLabel(endLabel)
        
        // Release temporary slot
        variableSlotManager.releaseTemporarySlot(targetSlot)
    }
    
    /**
     * Generate pattern tests for all cases.
     */
    private fun generatePatternTests(
        cases: List<MatchCase>,
        targetType: Type,
        targetSlot: Int,
        caseBodyLabels: List<Label>,
        endLabel: Label
    ) {
        for (i in cases.indices) {
            val case = cases[i]
            val caseBodyLabel = caseBodyLabels[i]
            val nextCaseLabel = if (i < cases.size - 1) {
                Label() // Label for next case test
            } else {
                // For the last case, jump to end with default value if no match
                val failureLabel = Label()
                methodVisitor.visitLabel(failureLabel)
                typeConverter.generateDefaultValue(targetType)
                methodVisitor.visitJumpInsn(GOTO, endLabel)
                failureLabel
            }
            
            // Load target value for comparison
            val loadInstruction = variableSlotManager.getLoadInstruction(targetType)
            methodVisitor.visitVarInsn(loadInstruction, targetSlot)
            
            // Generate pattern test using the pattern matcher
            patternMatcher.generatePatternTest(case.pattern, targetType, caseBodyLabel, nextCaseLabel)
            
            // Handle the next case label
            if (i < cases.size - 1) {
                methodVisitor.visitLabel(nextCaseLabel)
            }
        }
    }
    
    /**
     * Generate bodies for all match cases.
     */
    private fun generateCaseBodies(
        cases: List<MatchCase>,
        targetType: Type,
        targetSlot: Int,
        caseBodyLabels: List<Label>,
        endLabel: Label,
        resultType: Type
    ) {
        for (i in cases.indices) {
            val case = cases[i]
            val caseBodyLabel = caseBodyLabels[i]
            
            // Case body label
            methodVisitor.visitLabel(caseBodyLabel)
            
            // Create new scope for pattern variable bindings
            val savedSlotManager = variableSlotManager.createCheckpoint()
            
            // Bind pattern variables (if any)
            patternMatcher.bindPatternVariables(case.pattern, targetType, targetSlot)
            
            // Generate case expression
            val caseExprType = expressionGenerator.inferExpressionType(case.expression)
            generateExpression(TypedExpression(case.expression, caseExprType))
            
            // Ensure consistent stack state (fixes VerifyError)
            val leavesValueOnStack = bytecodeUtils.expressionLeavesValueOnStack(case.expression, caseExprType)
            bytecodeUtils.ensureConsistentStackState(resultType, caseExprType, leavesValueOnStack)
            
            // Restore variable slot state
            variableSlotManager.restoreCheckpoint(savedSlotManager)
            
            // Jump to end
            methodVisitor.visitJumpInsn(GOTO, endLabel)
        }
    }
    
    /**
     * Generate pattern test logic that jumps to caseLabel if pattern matches.
     */
    fun generatePatternTest(
        pattern: Pattern, 
        targetType: Type, 
        caseLabel: Label, 
        nextLabel: Label
    ) {
        // Delegate to the pattern matcher
        patternMatcher.generatePatternTest(pattern, targetType, caseLabel, nextLabel)
    }
    
    /**
     * Bind pattern variables to their values.
     */
    fun bindPatternVariables(pattern: Pattern, targetType: Type, targetSlot: Int) {
        // Delegate to the pattern matcher
        patternMatcher.bindPatternVariables(pattern, targetType, targetSlot)
    }
    
    /**
     * Generate expression using the callback or expression generator.
     */
    private fun generateExpression(expr: TypedExpression) {
        if (generateExpressionCallback != null) {
            generateExpressionCallback.invoke(expr)
        } else {
            expressionGenerator.generateExpression(expr)
        }
    }
    
    /**
     * Get JVM type descriptor for a TaylorLang type.
     */
    private fun getJvmType(type: Type): String {
        return typeConverter.getJvmType(type)
    }
    
    /**
     * Get full JVM type descriptor including generic parameters.
     */
    private fun getJvmTypeDescriptor(type: Type): String {
        return typeConverter.getJvmTypeDescriptor(type)
    }
}