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
    private val typeInferenceHelper: (Expression) -> Type,
    private val currentClassName: String = "Program"
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
            is ConstructorCall -> {
                generateConstructorCall(expression, expr.type)
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
                    // Fixed string concatenation: handle mixed types correctly
                    // Stack at this point: [left_value, right_value]
                    
                    // Get actual types of left and right operands
                    val leftType = inferExpressionType(binaryOp.left)
                    val rightType = inferExpressionType(binaryOp.right)
                    
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
                        // Arithmetic operations - handle string concatenation first
                        val leftType = inferExpressionType(expr.left)
                        val rightType = inferExpressionType(expr.right)
                        when {
                            leftType == BuiltinTypes.STRING || rightType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                            isIntegerExpression(expr) -> BuiltinTypes.INT
                            else -> BuiltinTypes.DOUBLE
                        }
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
     * 
     * Simplified version without conditional jumps to avoid ASM frame computation issues
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
    
    /**
     * Append a value to StringBuilder with correct method signature based on type
     * Stack: [value, StringBuilder] -> [StringBuilder]
     */
    private fun appendToStringBuilder(valueType: Type) {
        when {
            isStringType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false
                )
            }
            isIntegerType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(I)Ljava/lang/StringBuilder;",
                    false
                )
            }
            isDoubleType(valueType) -> {
                methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/StringBuilder", 
                    "append", 
                    "(D)Ljava/lang/StringBuilder;",
                    false
                )
            }
            isBooleanType(valueType) -> {
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
     * Check if type is a double/float type
     */
    private fun isDoubleType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() in listOf("double", "float")
            is Type.NamedType -> type.name.lowercase() in listOf("double", "float")
            else -> false
        }
    }
    
    /**
     * Check if type is a boolean type
     */
    private fun isBooleanType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "boolean"
            is Type.NamedType -> type.name.lowercase() == "boolean"
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
     * CRITICAL FIX: Box primitive types to their Object wrapper classes.
     * This prevents VerifyError when passing primitives to methods expecting Objects.
     * Stack: [primitive_value] -> [Object_value]
     */
    private fun boxPrimitiveToObject(type: Type) {
        when {
            isIntegerType(type) -> {
                // int -> Integer
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                )
            }
            isDoubleType(type) -> {
                // double -> Double
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
            }
            isBooleanType(type) -> {
                // boolean (int 0/1) -> Boolean
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
            }
            // String and other Object types don't need boxing
            else -> {
                // Already an Object type - no boxing needed
            }
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
                        // System.out.println - CRITICAL FIX: Box primitive types to Objects
                        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                        
                        if (functionCall.arguments.isNotEmpty()) {
                            val argType = inferExpressionType(functionCall.arguments[0])
                            generateExpression(TypedExpression(functionCall.arguments[0], argType))
                            
                            // CRITICAL FIX: Box primitive types to Objects before calling println
                            // This prevents VerifyError: Type integer is not assignable to Object
                            boxPrimitiveToObject(argType)
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
                        // Simplified assert implementation without conditional jumps to avoid ASM frame computation issues
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
                            
                            // Use the assertHelper method to avoid conditional jumps entirely
                            // Stack: [boolean_condition]
                            methodVisitor.visitLdcInsn("Assertion failed")
                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                currentClassName,
                                "assertHelper",
                                "(ZLjava/lang/String;)V",
                                false
                            )
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
    
    /**
     * Generate constructor call for union types
     */
    private fun generateConstructorCall(constructorCall: ConstructorCall, expectedType: Type) {
        val constructorName = constructorCall.constructor
        
        // Map constructor calls to their runtime implementations
        val runtimeClassName = getConstructorRuntimeClassName(constructorName, expectedType)
        
        // Generate constructor call based on the runtime type (arguments handled per case)
        when {
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorResult") -> {
                when (constructorName) {
                    "Ok" -> {
                        // new TaylorResult.Ok(value)
                        methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorResult\$Ok")
                        methodVisitor.visitInsn(DUP)
                        
                        // Generate argument
                        val arg = constructorCall.arguments.firstOrNull()
                        if (arg != null) {
                            val argType = typeInferenceHelper(arg)
                            generateExpression(TypedExpression(arg, argType))
                            // CRITICAL FIX: Box primitive types to Object for constructor
                            boxPrimitiveToObject(argType)
                        } else {
                            methodVisitor.visitInsn(ACONST_NULL)
                        }
                        
                        methodVisitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "org/taylorlang/runtime/TaylorResult\$Ok",
                            "<init>",
                            "(Ljava/lang/Object;)V",
                            false
                        )
                    }
                    "Error" -> {
                        // new TaylorResult.Error(error)
                        methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorResult\$Error")
                        methodVisitor.visitInsn(DUP)
                        
                        // Generate argument - if String, wrap in RuntimeException
                        val arg = constructorCall.arguments.firstOrNull()
                        if (arg != null) {
                            val argType = typeInferenceHelper(arg)
                            
                            if (argType is Type.NamedType && argType.name == "String") {
                                // Create RuntimeException from string
                                methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                                methodVisitor.visitInsn(DUP)
                                generateExpression(TypedExpression(arg, argType))
                                methodVisitor.visitMethodInsn(
                                    INVOKESPECIAL,
                                    "java/lang/RuntimeException",
                                    "<init>",
                                    "(Ljava/lang/String;)V",
                                    false
                                )
                            } else {
                                // Use argument as-is (assuming it's already a Throwable)
                                generateExpression(TypedExpression(arg, argType))
                            }
                        } else {
                            // No argument - create default RuntimeException
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
                        
                        methodVisitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "org/taylorlang/runtime/TaylorResult\$Error",
                            "<init>",
                            "(Ljava/lang/Throwable;)V",
                            false
                        )
                    }
                    else -> generateFallbackConstructor(expectedType)
                }
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorOption") -> {
                when (constructorName) {
                    "Some" -> {
                        // new TaylorOption.Some(value)
                        methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorOption\$Some")
                        methodVisitor.visitInsn(DUP)
                        
                        // Generate argument
                        val arg = constructorCall.arguments.firstOrNull()
                        if (arg != null) {
                            val argType = typeInferenceHelper(arg)
                            generateExpression(TypedExpression(arg, argType))
                        } else {
                            methodVisitor.visitInsn(ACONST_NULL)
                        }
                        
                        methodVisitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "org/taylorlang/runtime/TaylorOption\$Some",
                            "<init>",
                            "(Ljava/lang/Object;)V",
                            false
                        )
                    }
                    "None" -> {
                        // TaylorOption.None.INSTANCE
                        methodVisitor.visitFieldInsn(
                            GETSTATIC,
                            "org/taylorlang/runtime/TaylorOption\$None",
                            "INSTANCE",
                            "Lorg/taylorlang/runtime/TaylorOption\$None;"
                        )
                    }
                    else -> generateFallbackConstructor(expectedType)
                }
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorStatus") -> {
                when (constructorName) {
                    "Active" -> {
                        methodVisitor.visitFieldInsn(
                            GETSTATIC,
                            "org/taylorlang/runtime/TaylorStatus\$Active",
                            "INSTANCE",
                            "Lorg/taylorlang/runtime/TaylorStatus\$Active;"
                        )
                    }
                    "Inactive" -> {
                        methodVisitor.visitFieldInsn(
                            GETSTATIC,
                            "org/taylorlang/runtime/TaylorStatus\$Inactive",
                            "INSTANCE",
                            "Lorg/taylorlang/runtime/TaylorStatus\$Inactive;"
                        )
                    }
                    "Pending" -> {
                        methodVisitor.visitFieldInsn(
                            GETSTATIC,
                            "org/taylorlang/runtime/TaylorStatus\$Pending",
                            "INSTANCE",
                            "Lorg/taylorlang/runtime/TaylorStatus\$Pending;"
                        )
                    }
                    else -> generateFallbackConstructor(expectedType)
                }
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorTuple2") -> {
                when (constructorName) {
                    "Pair" -> {
                        // new TaylorTuple2.Pair(first, second)
                        methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorTuple2\$Pair")
                        methodVisitor.visitInsn(DUP)
                        
                        // Generate first argument
                        val firstArg = constructorCall.arguments.getOrNull(0)
                        if (firstArg != null) {
                            val argType = typeInferenceHelper(firstArg)
                            generateExpression(TypedExpression(firstArg, argType))
                        } else {
                            methodVisitor.visitInsn(ACONST_NULL)
                        }
                        
                        // Generate second argument
                        val secondArg = constructorCall.arguments.getOrNull(1)
                        if (secondArg != null) {
                            val argType = typeInferenceHelper(secondArg)
                            generateExpression(TypedExpression(secondArg, argType))
                        } else {
                            methodVisitor.visitInsn(ACONST_NULL)
                        }
                        
                        methodVisitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "org/taylorlang/runtime/TaylorTuple2\$Pair",
                            "<init>",
                            "(Ljava/lang/Object;Ljava/lang/Object;)V",
                            false
                        )
                    }
                    else -> generateFallbackConstructor(expectedType)
                }
            }
            else -> generateFallbackConstructor(expectedType)
        }
    }
    
    /**
     * Get the runtime class name for a constructor
     */
    private fun getConstructorRuntimeClassName(constructorName: String, expectedType: Type): String {
        val typeName = when (expectedType) {
            is Type.NamedType -> expectedType.name
            is Type.UnionType -> expectedType.name
            is Type.GenericType -> expectedType.name
            else -> "Object"
        }
        
        return when (typeName) {
            "Result" -> "org/taylorlang/runtime/TaylorResult\$$constructorName"
            "Option" -> "org/taylorlang/runtime/TaylorOption\$$constructorName"
            "Status" -> "org/taylorlang/runtime/TaylorStatus\$$constructorName"
            "Tuple2" -> "org/taylorlang/runtime/TaylorTuple2\$$constructorName"
            else -> "$typeName\$$constructorName"
        }
    }
    
    /**
     * Generate fallback constructor for unknown types
     */
    private fun generateFallbackConstructor(expectedType: Type) {
        when (getJvmType(expectedType)) {
            "I", "Z" -> methodVisitor.visitLdcInsn(0)
            "D" -> methodVisitor.visitLdcInsn(0.0)
            else -> methodVisitor.visitInsn(ACONST_NULL)
        }
    }
}