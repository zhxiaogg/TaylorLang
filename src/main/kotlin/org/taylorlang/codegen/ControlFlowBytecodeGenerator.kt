package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for control flow structures.
 * 
 * Handles compilation of:
 * - If/else expressions with proper branching
 * - While loops with correct condition evaluation
 * - Boolean operations (short-circuit evaluation planned for future)
 * - Jump and label management
 */
class ControlFlowBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val expressionGenerator: ExpressionBytecodeGenerator,
    private val generateExpressionCallback: ((TypedExpression) -> Unit)? = null
) {
    
    /**
     * Generate code for if expressions
     */
    fun generateIfExpression(ifExpr: IfExpression, resultType: Type) {
        val elseLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
        // Generate condition
        val conditionType = expressionGenerator.inferExpressionType(ifExpr.condition)
        generateExpression(TypedExpression(ifExpr.condition, conditionType))
        
        // Jump to else if condition is false (0)
        methodVisitor.visitJumpInsn(IFEQ, elseLabel)
        
        // Generate then branch
        val thenType = expressionGenerator.inferExpressionType(ifExpr.thenExpression)
        generateExpression(TypedExpression(ifExpr.thenExpression, thenType))
        
        // Skip else branch
        methodVisitor.visitJumpInsn(GOTO, endLabel)
        
        // Generate else branch
        methodVisitor.visitLabel(elseLabel)
        if (ifExpr.elseExpression != null) {
            val elseType = expressionGenerator.inferExpressionType(ifExpr.elseExpression)
            generateExpression(TypedExpression(ifExpr.elseExpression, elseType))
        } else {
            // No else branch - push default value based on result type
            when (getJvmType(resultType)) {
                "I", "Z" -> methodVisitor.visitLdcInsn(0)
                "D" -> methodVisitor.visitLdcInsn(0.0)
                "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                "V" -> {
                    // Unit/void - no value to push
                }
                else -> methodVisitor.visitInsn(ACONST_NULL)
            }
        }
        
        // End label
        methodVisitor.visitLabel(endLabel)
    }
    
    /**
     * Generate code for while expressions
     * 
     * Standard while loop pattern:
     * 1. Jump to condition check first
     * 2. Loop body (only executed if condition is true)
     * 3. Condition check
     * 4. If condition true, jump back to body
     * 5. Continue after loop
     */
    fun generateWhileExpression(whileExpr: WhileExpression, resultType: Type) {
        val loopStartLabel = org.objectweb.asm.Label()
        val conditionCheckLabel = org.objectweb.asm.Label()
        val loopEndLabel = org.objectweb.asm.Label()
        
        // CRITICAL: Jump to condition check FIRST to implement proper while loop semantics
        // This ensures while(false) never executes the body
        methodVisitor.visitJumpInsn(GOTO, conditionCheckLabel)
        
        // === LOOP BODY SECTION ===
        methodVisitor.visitLabel(loopStartLabel)
        
        // Generate the loop body
        val bodyType = expressionGenerator.inferExpressionType(whileExpr.body)
        generateExpression(TypedExpression(whileExpr.body, bodyType))
        
        // Pop body result - while loops return Unit, not the body result
        if (getJvmType(bodyType) != "V") {
            methodVisitor.visitInsn(POP)
        }
        
        // === CONDITION CHECK SECTION ===
        methodVisitor.visitLabel(conditionCheckLabel)
        
        // Generate condition evaluation  
        val conditionType = expressionGenerator.inferExpressionType(whileExpr.condition)
        generateExpression(TypedExpression(whileExpr.condition, conditionType))
        
        // CRITICAL: IFNE jumps if stack value is NOT zero (i.e., true)
        // - while(true): condition puts 1 on stack, IFNE jumps to body -> correct
        // - while(false): condition puts 0 on stack, IFNE does NOT jump -> correct  
        methodVisitor.visitJumpInsn(IFNE, loopStartLabel)
        
        // === LOOP EXIT SECTION ===
        methodVisitor.visitLabel(loopEndLabel)
        
        // While expressions return Unit - no value to push
        // The stack should already be clean after condition evaluation
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
     * Generate short-circuit AND operation (&&)
     * Stack: [left_operand, right_operand] -> [boolean_result]
     */
    fun generateBooleanAnd() {
        val falseLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
        // For now, implement as a simple bitwise AND operation
        // This is not truly short-circuit, but it's much simpler and reliable
        methodVisitor.visitInsn(IAND)
        
        // Note: For true short-circuit behavior, we'd need to avoid evaluating
        // the right operand when the left is false, but that requires
        // restructuring how binary operations are generated
    }
    
    /**
     * Generate short-circuit OR operation (||)
     * Stack: [left_operand, right_operand] -> [boolean_result]
     */
    fun generateBooleanOr() {
        val trueLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
        // For now, implement as a simple bitwise OR operation
        // This is not truly short-circuit, but it's much simpler and reliable
        methodVisitor.visitInsn(IOR)
        
        // Note: For true short-circuit behavior, we'd need to avoid evaluating
        // the right operand when the left is true, but that requires
        // restructuring how binary operations are generated
    }
    
    /**
     * Convert a boolean value on the stack to its string representation ("true" or "false")
     */
    fun convertBooleanToString() {
        // The stack has a boolean (0 or 1) on top
        // We'll use a simple if-else to convert to string
        
        val trueLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
        // If the boolean value is not 0 (i.e., true), jump to trueLabel
        methodVisitor.visitJumpInsn(IFNE, trueLabel)
        
        // False case: push "false"
        methodVisitor.visitLdcInsn("false")
        methodVisitor.visitJumpInsn(GOTO, endLabel)
        
        // True case: push "true"
        methodVisitor.visitLabel(trueLabel)
        methodVisitor.visitLdcInsn("true")
        
        // End
        methodVisitor.visitLabel(endLabel)
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