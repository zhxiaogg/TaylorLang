package org.taylorlang.codegen

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for comparison operations.
 * 
 * This component handles the generation of JVM bytecode for:
 * - Numeric comparisons (LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL)
 * - Equality comparisons (EQUAL, NOT_EQUAL) for all types
 * - String comparisons using String.equals() method
 * - Proper boolean result generation with conditional jumps
 * 
 * Key features:
 * - Efficient integer comparisons using IF_ICMP* instructions
 * - Double comparisons using DCMPG followed by conditional jumps
 * - String equality using String.equals() without complex control flow
 * - Consistent boolean result generation (1 for true, 0 for false)
 */
class ComparisonBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val typeHelper: TypeInferenceBytecodeHelper,
    private val generateExpression: (TypedExpression) -> Unit
) {
    
    /**
     * Generate comparison bytecode for binary comparison operations
     */
    fun generateComparison(binaryOp: BinaryOp, operandType: Type) {
        // Generate operands first
        generateComparisonOperands(binaryOp, operandType)
        
        // Generate comparison based on operator and operand type
        when (binaryOp.operator) {
            BinaryOperator.LESS_THAN -> {
                generateNumericComparison(typeHelper.isIntegerType(operandType), IFLT)
            }
            BinaryOperator.LESS_EQUAL -> {
                generateNumericComparison(typeHelper.isIntegerType(operandType), IFLE)
            }
            BinaryOperator.GREATER_THAN -> {
                generateNumericComparison(typeHelper.isIntegerType(operandType), IFGT)
            }
            BinaryOperator.GREATER_EQUAL -> {
                generateNumericComparison(typeHelper.isIntegerType(operandType), IFGE)
            }
            BinaryOperator.EQUAL -> {
                if (typeHelper.isStringType(operandType)) {
                    generateStringComparison(true) // true for equality
                } else {
                    generateNumericComparison(typeHelper.isIntegerType(operandType), IFEQ)
                }
            }
            BinaryOperator.NOT_EQUAL -> {
                if (typeHelper.isStringType(operandType)) {
                    generateStringComparison(false) // false for inequality
                } else {
                    generateNumericComparison(typeHelper.isIntegerType(operandType), IFNE)
                }
            }
            else -> {
                // Should not reach here for comparison operations
                throw IllegalArgumentException("Unsupported comparison operator: ${binaryOp.operator}")
            }
        }
    }
    
    /**
     * Generate operands for comparison operations with proper type handling
     */
    private fun generateComparisonOperands(binaryOp: BinaryOp, operandType: Type) {
        val isDoubleOp = !typeHelper.isIntegerType(operandType) && !typeHelper.isStringType(operandType)
        
        // Generate left operand
        when (val left = binaryOp.left) {
            is Literal.IntLiteral -> {
                if (isDoubleOp) {
                    // Convert int to double for double operations
                    methodVisitor.visitLdcInsn(left.value.toDouble())
                } else {
                    methodVisitor.visitLdcInsn(left.value)
                }
            }
            is Literal.FloatLiteral -> methodVisitor.visitLdcInsn(left.value)
            else -> generateExpression(TypedExpression(left, operandType))
        }
        
        // Generate right operand
        when (val right = binaryOp.right) {
            is Literal.IntLiteral -> {
                if (isDoubleOp) {
                    // Convert int to double for double operations
                    methodVisitor.visitLdcInsn(right.value.toDouble())
                } else {
                    methodVisitor.visitLdcInsn(right.value)
                }
            }
            is Literal.FloatLiteral -> methodVisitor.visitLdcInsn(right.value)
            else -> generateExpression(TypedExpression(right, operandType))
        }
    }
    
    /**
     * Generate numeric comparison operation that returns a boolean result.
     * Uses conditional jumps to create proper boolean values on the stack.
     */
    private fun generateNumericComparison(isIntegerComparison: Boolean, comparisonOp: Int) {
        val trueLabel = Label()
        val endLabel = Label()
        
        if (isIntegerComparison) {
            // Integer comparison: use IF_ICMP* instructions
            val icmpOp = when (comparisonOp) {
                IFLT -> IF_ICMPLT
                IFLE -> IF_ICMPLE  
                IFGT -> IF_ICMPGT
                IFGE -> IF_ICMPGE
                IFEQ -> IF_ICMPEQ
                IFNE -> IF_ICMPNE
                else -> IF_ICMPEQ
            }
            methodVisitor.visitJumpInsn(icmpOp, trueLabel)
        } else {
            // Double comparison: use DCMPG + IF* instructions
            methodVisitor.visitInsn(DCMPG)
            methodVisitor.visitJumpInsn(comparisonOp, trueLabel)
        }
        
        // False case: push 0 (false)
        methodVisitor.visitLdcInsn(0)
        methodVisitor.visitJumpInsn(GOTO, endLabel)
        
        // True case: push 1 (true)
        methodVisitor.visitLabel(trueLabel)
        methodVisitor.visitLdcInsn(1)
        
        // End
        methodVisitor.visitLabel(endLabel)
    }
    
    /**
     * Generate string comparison operation that returns a boolean result.
     * Stack: [string1, string2] -> [boolean_result]
     * 
     * Simplified version without conditional jumps to avoid ASM frame computation issues.
     */
    private fun generateStringComparison(isEquality: Boolean) {
        // Use String.equals() for comparison
        // Stack: [string1, string2]
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/String",
            "equals",
            "(Ljava/lang/Object;)Z",
            false
        )
        // Stack: [boolean_result] (1 for equal, 0 for not equal)
        
        if (!isEquality) {
            // For inequality (!= operator), invert the boolean result
            // XOR with 1 to flip: 0 XOR 1 = 1, 1 XOR 1 = 0
            methodVisitor.visitLdcInsn(1)
            methodVisitor.visitInsn(IXOR)
        }
        
        // Result is already on stack (1 for true, 0 for false)
    }
}