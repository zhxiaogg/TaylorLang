package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for expressions.
 * 
 * Handles compilation of:
 * - Literals (int, double, boolean, string)
 * - Binary and unary operations
 * - Arithmetic operations with type promotion
 * - Identifier resolution
 * - Try expressions with Result type unwrapping
 * - Type inference integration
 */
class ExpressionBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val typeInferenceHelper: (Expression) -> Type
) {
    
    // Lazy initialization of try expression generator to avoid circular dependencies
    private var tryExpressionGenerator: TryExpressionBytecodeGenerator? = null
    
    /**
     * Generate code for an expression
     */
    fun generateExpression(expr: TypedExpression) {
        when (val expression = expr.expression) {
            is Literal.IntLiteral -> {
                methodVisitor.visitLdcInsn(expression.value)
            }
            is Literal.FloatLiteral -> {
                methodVisitor.visitLdcInsn(expression.value)
            }
            is Literal.BooleanLiteral -> {
                methodVisitor.visitLdcInsn(if (expression.value) 1 else 0)
            }
            is Literal.StringLiteral -> {
                methodVisitor.visitLdcInsn(expression.value)
            }
            is BinaryOp -> {
                generateBinaryOperation(expression, expr.type)
            }
            is UnaryOp -> {
                generateUnaryOperation(expression, expr.type)
            }
            is Identifier -> {
                // Load variable from local slot
                if (variableSlotManager.hasSlot(expression.name)) {
                    val slot = variableSlotManager.getSlot(expression.name)!!
                    // CRITICAL FIX: Use the actual variable type stored in the slot, not the inferred type
                    // This prevents JVM verification errors when types don't match
                    val variableType = variableSlotManager.getType(expression.name)!!
                    val loadInstruction = variableSlotManager.getLoadInstruction(variableType)
                    methodVisitor.visitVarInsn(loadInstruction, slot)
                } else {
                    // For now, load 0 as placeholder for unknown identifiers (e.g., functions)
                    methodVisitor.visitLdcInsn(0)
                }
            }
            is TryExpression -> {
                // Generate try expression with Result type unwrapping
                getTryExpressionGenerator().generateTryExpression(expression, expr.type)
            }
            else -> {
                // Unsupported expression - push default value
                when (getJvmType(expr.type)) {
                    "I", "Z" -> methodVisitor.visitLdcInsn(0)
                    "D" -> methodVisitor.visitLdcInsn(0.0)
                    else -> methodVisitor.visitLdcInsn("")
                }
            }
        }
    }
    
    /**
     * Generate code for binary operations
     */
    fun generateBinaryOperation(binaryOp: BinaryOp, resultType: Type) {
        // Determine the operand type by inspecting the operands
        val operandType = determineOperandType(binaryOp, resultType)
        val isDoubleOp = !isIntegerType(operandType)
        
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
                if (isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IADD)
                } else {
                    methodVisitor.visitInsn(DADD)
                }
            }
            BinaryOperator.MINUS -> {
                if (isIntegerType(operandType)) {
                    methodVisitor.visitInsn(ISUB)
                } else {
                    methodVisitor.visitInsn(DSUB)
                }
            }
            BinaryOperator.MULTIPLY -> {
                if (isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IMUL)
                } else {
                    methodVisitor.visitInsn(DMUL)
                }
            }
            BinaryOperator.DIVIDE -> {
                if (isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IDIV)
                } else {
                    methodVisitor.visitInsn(DDIV)
                }
            }
            // Comparison operators
            BinaryOperator.LESS_THAN -> {
                generateComparison(isIntegerType(operandType), IFLT)
            }
            BinaryOperator.LESS_EQUAL -> {
                generateComparison(isIntegerType(operandType), IFLE)
            }
            BinaryOperator.GREATER_THAN -> {
                generateComparison(isIntegerType(operandType), IFGT)
            }
            BinaryOperator.GREATER_EQUAL -> {
                generateComparison(isIntegerType(operandType), IFGE)
            }
            BinaryOperator.EQUAL -> {
                generateComparison(isIntegerType(operandType), IFEQ)
            }
            BinaryOperator.NOT_EQUAL -> {
                generateComparison(isIntegerType(operandType), IFNE)
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
                if (isIntegerType(operandType)) {
                    methodVisitor.visitInsn(IADD)
                } else {
                    methodVisitor.visitInsn(DADD)
                }
            }
        }
    }
    
    /**
     * Generate code for unary operations
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
                if (isIntegerType(resultType)) {
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
     * Determine the operand type for binary operations
     */
    private fun determineOperandType(binaryOp: BinaryOp, resultType: Type): Type {
        // Use the consolidated type inference logic
        val leftType = inferExpressionType(binaryOp.left)
        val rightType = inferExpressionType(binaryOp.right)
        
        // For comparison and boolean operations, we need to determine the operand type
        // (not the result type, which is boolean)
        return when (binaryOp.operator) {
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                // For comparisons, promote to the wider type for comparison
                when {
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
                // For arithmetic operations, if either operand is double, the result is double
                when {
                    leftType == BuiltinTypes.DOUBLE || rightType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
                    leftType == BuiltinTypes.INT && rightType == BuiltinTypes.INT -> BuiltinTypes.INT
                    else -> BuiltinTypes.INT // Default to int
                }
            }
        }
    }
    
    /**
     * Infer the type of an expression based on its structure
     */
    fun inferExpressionType(expr: Expression): Type {
        return when (expr) {
            is Literal.IntLiteral -> BuiltinTypes.INT
            is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
            is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
            is Literal.StringLiteral -> BuiltinTypes.STRING
            is BinaryOp -> {
                // For binary operations, determine the result type
                when (expr.operator) {
                    BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
                    BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
                    BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL,
                    BinaryOperator.AND, BinaryOperator.OR -> BuiltinTypes.BOOLEAN
                    else -> {
                        // Arithmetic operations
                        if (isIntegerExpression(expr)) BuiltinTypes.INT else BuiltinTypes.DOUBLE
                    }
                }
            }
            is UnaryOp -> {
                when (expr.operator) {
                    UnaryOperator.NOT -> BuiltinTypes.BOOLEAN
                    UnaryOperator.MINUS -> inferExpressionType(expr.operand)
                }
            }
            is IfExpression -> {
                // For if expressions, determine the result type based on branches
                val thenType = inferExpressionType(expr.thenExpression)
                if (expr.elseExpression != null) {
                    val elseType = inferExpressionType(expr.elseExpression)
                    // Promote to the wider type if different
                    when {
                        thenType == BuiltinTypes.DOUBLE || elseType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
                        thenType == BuiltinTypes.STRING || elseType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                        thenType == BuiltinTypes.BOOLEAN || elseType == BuiltinTypes.BOOLEAN -> BuiltinTypes.BOOLEAN
                        else -> thenType
                    }
                } else {
                    // No else branch - type is nullable or Unit
                    thenType
                }
            }
            is WhileExpression -> {
                // While loops return Unit
                BuiltinTypes.UNIT
            }
            is FunctionCall -> {
                // Infer function call return type
                when ((expr.target as? Identifier)?.name) {
                    "println" -> BuiltinTypes.UNIT // println returns void/unit
                    else -> BuiltinTypes.INT // Default for unknown functions
                }
            }
            is Identifier -> {
                // CRITICAL FIX: Look up the actual variable type from the slot manager
                // This prevents type mismatches in function calls and other expressions
                variableSlotManager.getType(expr.name) ?: BuiltinTypes.INT // Default to INT if not found
            }
            else -> BuiltinTypes.INT // Default fallback
        }
    }
    
    /**
     * Check if an expression evaluates to an integer
     */
    private fun isIntegerExpression(expr: Expression): Boolean {
        return when (expr) {
            is Literal.IntLiteral -> true
            is BinaryOp -> isIntegerExpression(expr.left) && isIntegerExpression(expr.right)
            else -> false
        }
    }
    
    /**
     * Generate comparison operation that returns a boolean result
     */
    private fun generateComparison(isIntegerComparison: Boolean, comparisonOp: Int) {
        val trueLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
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
    
    /**
     * Check if type is an integer type
     */
    private fun isIntegerType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "int"
            is Type.NamedType -> type.name.lowercase() == "int"
            else -> false
        }
    }
    
    /**
     * Get or create the try expression generator.
     * Lazy initialization to avoid circular dependencies.
     */
    private fun getTryExpressionGenerator(): TryExpressionBytecodeGenerator {
        if (tryExpressionGenerator == null) {
            tryExpressionGenerator = TryExpressionBytecodeGenerator(
                methodVisitor = methodVisitor,
                variableSlotManager = variableSlotManager,
                expressionGenerator = this,
                patternCompiler = null, // Will be set by BytecodeGenerator when available
                generateExpression = { expr -> generateExpression(expr) }
            )
        }
        return tryExpressionGenerator!!
    }
    
    /**
     * Set the pattern compiler for try expression catch clause handling.
     * This is called by BytecodeGenerator during initialization.
     */
    fun setPatternCompiler(patternCompiler: PatternBytecodeCompiler) {
        // Update existing try expression generator if it exists
        tryExpressionGenerator?.let { generator ->
            // Create a new generator with the pattern compiler
            tryExpressionGenerator = TryExpressionBytecodeGenerator(
                methodVisitor = methodVisitor,
                variableSlotManager = variableSlotManager,
                expressionGenerator = this,
                patternCompiler = patternCompiler,
                generateExpression = { expr -> generateExpression(expr) }
            )
        }
    }
}