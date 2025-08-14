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
            is FunctionCall -> {
                generateFunctionCall(expression, expr.type)
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
                if (isStringType(operandType)) {
                    // String concatenation using static String.valueOf and concat
                    // Stack: [left_operand, right_operand] -> [result_string]
                    
                    // Convert left operand to string if it isn't already
                    methodVisitor.visitMethodInsn(
                        INVOKESTATIC,
                        "java/lang/String",
                        "valueOf",
                        "(Ljava/lang/Object;)Ljava/lang/String;",
                        false
                    )
                    // Stack: [left_string, right_operand]
                    
                    // Convert right operand to string
                    methodVisitor.visitInsn(SWAP)
                    // Stack: [right_operand, left_string]
                    methodVisitor.visitMethodInsn(
                        INVOKESTATIC,
                        "java/lang/String",
                        "valueOf",
                        "(Ljava/lang/Object;)Ljava/lang/String;",
                        false
                    )
                    // Stack: [right_string, left_string]
                    
                    // Swap back to correct order and concatenate
                    methodVisitor.visitInsn(SWAP)
                    // Stack: [left_string, right_string]
                    methodVisitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/lang/String",
                        "concat",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false
                    )
                    // Stack: [result_string]
                } else if (isIntegerType(operandType)) {
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
                if (isStringType(operandType)) {
                    generateStringComparison(true) // true for equality
                } else {
                    generateComparison(isIntegerType(operandType), IFEQ)
                }
            }
            BinaryOperator.NOT_EQUAL -> {
                if (isStringType(operandType)) {
                    generateStringComparison(false) // false for inequality
                } else {
                    generateComparison(isIntegerType(operandType), IFNE)
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
                    "emptyList" -> Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(BuiltinTypes.INT)) // Default to List<Int>
                    "singletonList" -> {
                        // Get the element type from the argument
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT // Default
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf" -> {
                        // Single element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf2" -> {
                        // Two element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf3" -> {
                        // Three element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf4" -> {
                        // Four element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
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
     * Generate string comparison operation that returns a boolean result
     * Stack: [string1, string2] -> [boolean_result]
     */
    private fun generateStringComparison(isEquality: Boolean) {
        val trueLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
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
        
        if (isEquality) {
            // For equality: if equals() returned true (1), jump to true label
            methodVisitor.visitJumpInsn(IFNE, trueLabel)
        } else {
            // For inequality: if equals() returned false (0), jump to true label
            methodVisitor.visitJumpInsn(IFEQ, trueLabel)
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
     * Check if type is a string type
     */
    private fun isStringType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "string"
            is Type.NamedType -> type.name.lowercase() == "string"
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
    
    /**
     * Generate bytecode for function calls.
     * Handles static method calls including TaylorResult.ok/error and constructor calls.
     */
    private fun generateFunctionCall(functionCall: FunctionCall, expectedType: Type) {
        val target = functionCall.target
        
        when (target) {
            is Identifier -> {
                val functionName = target.name
                
                // Handle special static method calls
                when {
                    functionName == "TaylorResult.ok" -> {
                        // Generate arguments first
                        if (functionCall.arguments.isNotEmpty()) {
                            generateExpression(TypedExpression(functionCall.arguments[0], inferExpressionType(functionCall.arguments[0])))
                        } else {
                            methodVisitor.visitInsn(ACONST_NULL)
                        }
                        
                        // Call TaylorResult.ok(Object)
                        methodVisitor.visitMethodInsn(
                            INVOKESTATIC,
                            "org/taylorlang/runtime/TaylorResult",
                            "ok",
                            "(Ljava/lang/Object;)Lorg/taylorlang/runtime/TaylorResult;",
                            false
                        )
                    }
                    
                    functionName == "TaylorResult.error" -> {
                        // Generate arguments first  
                        if (functionCall.arguments.isNotEmpty()) {
                            generateExpression(TypedExpression(functionCall.arguments[0], BuiltinTypes.THROWABLE))
                        } else {
                            // Create a default RuntimeException
                            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                            methodVisitor.visitInsn(DUP)
                            methodVisitor.visitLdcInsn("Unknown error")
                            methodVisitor.visitMethodInsn(
                                INVOKESPECIAL,
                                "java/lang/RuntimeException",
                                "<init>",
                                "(Ljava/lang/String;)V",
                                false
                            )
                        }
                        
                        // Call TaylorResult.error(Throwable)
                        methodVisitor.visitMethodInsn(
                            INVOKESTATIC,
                            "org/taylorlang/runtime/TaylorResult",
                            "error",
                            "(Ljava/lang/Throwable;)Lorg/taylorlang/runtime/TaylorResult;",
                            false
                        )
                    }
                    
                    functionName == "RuntimeException" -> {
                        // Constructor call for RuntimeException
                        methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                        methodVisitor.visitInsn(DUP)
                        
                        // Generate constructor arguments
                        if (functionCall.arguments.isNotEmpty()) {
                            generateExpression(TypedExpression(functionCall.arguments[0], BuiltinTypes.STRING))
                            methodVisitor.visitMethodInsn(
                                INVOKESPECIAL,
                                "java/lang/RuntimeException",
                                "<init>",
                                "(Ljava/lang/String;)V",
                                false
                            )
                        } else {
                            methodVisitor.visitMethodInsn(
                                INVOKESPECIAL,
                                "java/lang/RuntimeException",
                                "<init>",
                                "()V",
                                false
                            )
                        }
                    }
                    
                    functionName == "println" -> {
                        // System.out.println
                        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                        
                        if (functionCall.arguments.isNotEmpty()) {
                            generateExpression(TypedExpression(functionCall.arguments[0], inferExpressionType(functionCall.arguments[0])))
                        } else {
                            methodVisitor.visitLdcInsn("")
                        }
                        
                        methodVisitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/io/PrintStream",
                            "println",
                            "(Ljava/lang/Object;)V",
                            false
                        )
                    }
                    
                    functionName == "assert" -> {
                        // Assert function implementation
                        if (functionCall.arguments.isEmpty()) {
                            // Invalid assert call - should have been caught by type checker
                            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                            methodVisitor.visitInsn(DUP)
                            methodVisitor.visitLdcInsn("assert() called without condition")
                            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false)
                            methodVisitor.visitInsn(ATHROW)
                        } else {
                            // Generate the condition expression
                            val conditionArg = functionCall.arguments[0]
                            generateExpression(TypedExpression(conditionArg, inferExpressionType(conditionArg)))
                            
                            // Create a label for when assertion passes
                            val assertPassLabel = org.objectweb.asm.Label()
                            
                            // If condition is true (1), jump to pass label
                            methodVisitor.visitJumpInsn(IFNE, assertPassLabel)
                            
                            // Assertion failed - print error message to stderr and exit
                            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;")
                            methodVisitor.visitLdcInsn("Assertion failed")
                            methodVisitor.visitMethodInsn(
                                INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false
                            )
                            
                            // Exit with code 1
                            methodVisitor.visitLdcInsn(1)
                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                "java/lang/System",
                                "exit",
                                "(I)V",
                                false
                            )
                            
                            // Label for when assertion passes - continue execution
                            methodVisitor.visitLabel(assertPassLabel)
                        }
                    }
                    
                    else -> {
                        // Unknown function call - generate placeholder based on expected type
                        when (getJvmType(expectedType)) {
                            "I", "Z" -> methodVisitor.visitLdcInsn(0)
                            "D" -> methodVisitor.visitLdcInsn(0.0)
                            else -> methodVisitor.visitInsn(ACONST_NULL)
                        }
                    }
                }
            }
            
            else -> {
                // Complex target - generate placeholder
                when (getJvmType(expectedType)) {
                    "I", "Z" -> methodVisitor.visitLdcInsn(0)
                    "D" -> methodVisitor.visitLdcInsn(0.0)
                    else -> methodVisitor.visitInsn(ACONST_NULL)
                }
            }
        }
    }
}