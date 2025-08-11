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
     */
    fun generateWhileExpression(whileExpr: WhileExpression, resultType: Type) {
        val conditionLabel = org.objectweb.asm.Label()
        val loopBodyLabel = org.objectweb.asm.Label() 
        val loopEndLabel = org.objectweb.asm.Label()
        
        // Use the same pattern as successful if-expressions but adapted for while loops
        // Jump to condition evaluation first (skip body initially)
        methodVisitor.visitJumpInsn(GOTO, conditionLabel)
        
        // Loop body - only executed when condition is true
        methodVisitor.visitLabel(loopBodyLabel)
        val bodyType = expressionGenerator.inferExpressionType(whileExpr.body)
        generateExpression(TypedExpression(whileExpr.body, bodyType))
        
        // Pop the body result since while loops don't return body values
        if (getJvmType(bodyType) != "V") {
            methodVisitor.visitInsn(POP)
        }
        
        // Condition evaluation point
        methodVisitor.visitLabel(conditionLabel)
        val conditionType = expressionGenerator.inferExpressionType(whileExpr.condition)
        generateExpression(TypedExpression(whileExpr.condition, conditionType))
        
        // If condition is true (non-zero), jump back to body
        methodVisitor.visitJumpInsn(IFNE, loopBodyLabel)
        
        // Condition is false - fall through to end
        methodVisitor.visitLabel(loopEndLabel)
        
        // While expressions typically return Unit, but we may need to push a default value
        // to satisfy the stack expectation
        if (getJvmType(resultType) != "V") {
            when (getJvmType(resultType)) {
                "I", "Z" -> methodVisitor.visitLdcInsn(0)
                "D" -> methodVisitor.visitLdcInsn(0.0)
                "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                else -> methodVisitor.visitInsn(ACONST_NULL)
            }
        }
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