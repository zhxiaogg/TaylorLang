package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for arithmetic and binary operations.
 * 
 * This component handles the generation of JVM bytecode for:
 * - Binary arithmetic operations (PLUS, MINUS, MULTIPLY, DIVIDE)
 * - Unary operations (MINUS, NOT)
 * - Type promotion and conversion logic
 * - Complex string concatenation with StringBuilder
 * - Boolean operations (AND, OR)
 * 
 * Key features:
 * - Automatic type promotion for mixed-type arithmetic
 * - Efficient string concatenation using StringBuilder
 * - Proper handling of integer vs double operations
 * - Integration with type inference for operand types
 */
class ArithmeticBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val typeHelper: TypeInferenceBytecodeHelper,
    private val generateExpression: (TypedExpression) -> Unit
) {
    
    /**
     * Generate bytecode for binary operations including arithmetic and boolean operations
     */
    fun generateBinaryOperation(binaryOp: BinaryOp, resultType: Type) {
        // Determine the operand type by inspecting the operands
        val operandType = determineOperandType(binaryOp, resultType)
        val isDoubleOp = !typeHelper.isIntegerType(operandType)
        
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
        
        // Generate operation based on operand type
        when (binaryOp.operator) {
            BinaryOperator.PLUS -> {
                if (typeHelper.isStringType(operandType)) {
                    generateStringConcatenation(binaryOp)
                } else if (typeHelper.isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IADD)
                } else {
                    methodVisitor.visitInsn(DADD)
                }
            }
            BinaryOperator.MINUS -> {
                if (typeHelper.isIntegerType(operandType)) {
                    methodVisitor.visitInsn(ISUB)
                } else {
                    methodVisitor.visitInsn(DSUB)
                }
            }
            BinaryOperator.MULTIPLY -> {
                if (typeHelper.isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IMUL)
                } else {
                    methodVisitor.visitInsn(DMUL)
                }
            }
            BinaryOperator.DIVIDE -> {
                if (typeHelper.isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IDIV)
                } else {
                    methodVisitor.visitInsn(DDIV)
                }
            }
            // Boolean operators - note: these are not truly short-circuit in current implementation
            BinaryOperator.AND -> {
                methodVisitor.visitInsn(IAND)
            }
            BinaryOperator.OR -> {
                methodVisitor.visitInsn(IOR)
            }
            else -> {
                // Unsupported operation - use appropriate add instruction as fallback
                if (typeHelper.isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IADD)
                } else {
                    methodVisitor.visitInsn(DADD)
                }
            }
        }
    }
    
    /**
     * Generate bytecode for unary operations
     */
    fun generateUnaryOperation(unaryOp: UnaryOp, resultType: Type) {
        // Generate operand
        when (val operand = unaryOp.operand) {
            is Literal.IntLiteral -> methodVisitor.visitLdcInsn(operand.value)
            is Literal.FloatLiteral -> methodVisitor.visitLdcInsn(operand.value)
            else -> generateExpression(TypedExpression(operand, resultType))
        }
        
        when (unaryOp.operator) {
            UnaryOperator.MINUS -> {
                if (typeHelper.isIntegerType(resultType)) {
                    methodVisitor.visitInsn(INEG)
                } else {
                    methodVisitor.visitInsn(DNEG)
                }
            }
            UnaryOperator.NOT -> {
                // For boolean NOT: if value is 0, push 1, else push 0
                methodVisitor.visitLdcInsn(1)
                methodVisitor.visitInsn(IXOR)
            }
        }
    }
    
    /**
     * Generate efficient string concatenation using StringBuilder.
     * Fixed string concatenation: handle mixed types correctly.
     */
    private fun generateStringConcatenation(binaryOp: BinaryOp) {
        // Stack at this point: [left_value, right_value]
        
        // Get actual types of left and right operands
        val leftType = typeHelper.inferExpressionType(binaryOp.left)
        val rightType = typeHelper.inferExpressionType(binaryOp.right)
        
        // Store right operand in temporary slot with correct type
        val rightSlot = variableSlotManager.allocateTemporarySlot(rightType)
        val rightStoreOpcode = variableSlotManager.getStoreInstruction(rightType)
        methodVisitor.visitVarInsn(rightStoreOpcode, rightSlot)
        
        // Store left operand in temporary slot with correct type  
        val leftSlot = variableSlotManager.allocateTemporarySlot(leftType)
        val leftStoreOpcode = variableSlotManager.getStoreInstruction(leftType)
        methodVisitor.visitVarInsn(leftStoreOpcode, leftSlot)
        
        // Create StringBuilder
        methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder")
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        
        // Load and append left operand with correct load instruction
        val leftLoadOpcode = variableSlotManager.getLoadInstruction(leftType)
        methodVisitor.visitVarInsn(leftLoadOpcode, leftSlot)
        appendToStringBuilder(leftType)
        
        // Load and append right operand with correct load instruction
        val rightLoadOpcode = variableSlotManager.getLoadInstruction(rightType)
        methodVisitor.visitVarInsn(rightLoadOpcode, rightSlot)
        appendToStringBuilder(rightType)
        
        // Convert to String
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        
        // Release temporary slots
        variableSlotManager.releaseTemporarySlot(rightSlot)
        variableSlotManager.releaseTemporarySlot(leftSlot)
    }
    
    /**
     * Append a value to StringBuilder with correct method signature based on type.
     * Stack: [value, StringBuilder] -> [StringBuilder]
     */
    private fun appendToStringBuilder(valueType: Type) {
        when {
            typeHelper.isStringType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false
                )
            }
            typeHelper.isIntegerType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(I)Ljava/lang/StringBuilder;",
                    false
                )
            }
            typeHelper.isDoubleType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(D)Ljava/lang/StringBuilder;",
                    false
                )
            }
            typeHelper.isBooleanType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(Z)Ljava/lang/StringBuilder;",
                    false
                )
            }
            else -> {
                // Fallback to Object version for other types
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                    false
                )
            }
        }
    }
    
    /**
     * Determine the operand type for binary operations
     */
    private fun determineOperandType(binaryOp: BinaryOp, resultType: Type): Type {
        // Use the consolidated type inference logic
        val leftType = typeHelper.inferExpressionType(binaryOp.left)
        val rightType = typeHelper.inferExpressionType(binaryOp.right)
        
        // For comparison and boolean operations, we need to determine the operand type
        // (not the result type, which is boolean)
        return when (binaryOp.operator) {
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                // For comparisons, promote to the wider type for comparison
                when {
                    leftType == BuiltinTypes.STRING || rightType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                    leftType == BuiltinTypes.DOUBLE || rightType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
                    leftType == BuiltinTypes.INT && rightType == BuiltinTypes.INT -> BuiltinTypes.INT
                    else -> BuiltinTypes.INT // Default to int
                }
            }
            BinaryOperator.AND, BinaryOperator.OR -> {
                // Boolean operations work on boolean operands
                BuiltinTypes.BOOLEAN
            }
            else -> {
                // For arithmetic operations, handle string concatenation first
                when {
                    leftType == BuiltinTypes.STRING || rightType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                    leftType == BuiltinTypes.DOUBLE || rightType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
                    leftType == BuiltinTypes.INT && rightType == BuiltinTypes.INT -> BuiltinTypes.INT
                    else -> BuiltinTypes.INT // Default to int
                }
            }
        }
    }
}