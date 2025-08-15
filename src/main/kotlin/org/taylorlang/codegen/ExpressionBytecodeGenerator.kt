package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Coordinating bytecode generator for expressions.
 * 
 * This refactored implementation serves as a coordinator that delegates expression
 * generation to specialized components, each responsible for a specific type of
 * bytecode generation:
 * 
 * - LiteralBytecodeGenerator: Literal values (int, double, boolean, string)
 * - ArithmeticBytecodeGenerator: Binary and unary operations, string concatenation
 * - ComparisonBytecodeGenerator: Comparison operations with proper boolean results
 * - FunctionCallBytecodeGenerator: Function calls with primitive boxing
 * - ConstructorCallBytecodeGenerator: Union type constructors
 * - VariableAccessBytecodeGenerator: Variable loading and storing
 * - TypeInferenceBytecodeHelper: Type inference and utility operations
 * 
 * Benefits of this architecture:
 * - Each component under 500 lines (architectural compliance)
 * - Clear separation of concerns for focused debugging
 * - Isolated testing of individual bytecode generation features
 * - Easy extension for new expression types
 * - Reduced complexity per component
 */
class ExpressionBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val typeInferenceHelper: (Expression) -> Type,
    private val currentClassName: String = "Program"
) {
    
    // Specialized generators (initialized lazily)
    private val typeHelper = TypeInferenceBytecodeHelper(methodVisitor, variableSlotManager)
    private val literalGenerator = LiteralBytecodeGenerator(methodVisitor)
    private val arithmeticGenerator = ArithmeticBytecodeGenerator(
        methodVisitor, 
        variableSlotManager, 
        typeHelper,
        this::generateExpression
    )
    private val comparisonGenerator = ComparisonBytecodeGenerator(
        methodVisitor,
        typeHelper,
        this::generateExpression
    )
    private val functionCallGenerator = FunctionCallBytecodeGenerator(
        methodVisitor,
        typeHelper,
        currentClassName,
        this::generateExpression
    )
    private val constructorCallGenerator = ConstructorCallBytecodeGenerator(
        methodVisitor,
        typeHelper,
        typeInferenceHelper,
        this::generateExpression
    )
    private val variableAccessGenerator = VariableAccessBytecodeGenerator(
        methodVisitor,
        variableSlotManager,
        typeHelper
    )
    
    // Lazy initialization of try expression generator to avoid circular dependencies
    private var tryExpressionGenerator: TryExpressionBytecodeGenerator? = null
    
    /**
     * Generate bytecode for an expression by delegating to specialized generators
     */
    fun generateExpression(expr: TypedExpression) {
        when (val expression = expr.expression) {
            is Literal.IntLiteral,
            is Literal.FloatLiteral,
            is Literal.BooleanLiteral,
            is Literal.StringLiteral -> {
                literalGenerator.generateLiteral(expression as Literal)
            }
            
            is BinaryOp -> {
                when (expression.operator) {
                    BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
                    BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
                    BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                        // Determine operand type for comparison
                        val operandType = determineOperandType(expression, expr.type)
                        comparisonGenerator.generateComparison(expression, operandType)
                    }
                    else -> {
                        // Arithmetic and boolean operations
                        arithmeticGenerator.generateBinaryOperation(expression, expr.type)
                    }
                }
            }
            
            is UnaryOp -> {
                arithmeticGenerator.generateUnaryOperation(expression, expr.type)
            }
            
            is Identifier -> {
                variableAccessGenerator.generateVariableLoad(expression)
            }
            
            is TryExpression -> {
                // Generate try expression with Result type unwrapping
                getTryExpressionGenerator().generateTryExpression(expression, expr.type)
            }
            
            is FunctionCall -> {
                functionCallGenerator.generateFunctionCall(expression, expr.type)
            }
            
            is ConstructorCall -> {
                constructorCallGenerator.generateConstructorCall(expression, expr.type)
            }
            
            else -> {
                // Unsupported expression - push default value
                generateDefaultValue(expr.type)
            }
        }
    }
    
    /**
     * Generate default value for unsupported expressions
     */
    private fun generateDefaultValue(type: Type) {
        when (typeHelper.getJvmType(type)) {
            "I", "Z" -> methodVisitor.visitLdcInsn(0)
            "D" -> methodVisitor.visitLdcInsn(0.0)
            else -> methodVisitor.visitLdcInsn("")
        }
    }
    
    /**
     * Determine the operand type for binary operations (delegated to type helper)
     */
    private fun determineOperandType(binaryOp: BinaryOp, resultType: Type): Type {
        val leftType = typeHelper.inferExpressionType(binaryOp.left)
        val rightType = typeHelper.inferExpressionType(binaryOp.right)
        
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
            else -> leftType // For other operations, use left type
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
     * Expose type inference functionality for external use
     */
    fun inferExpressionType(expr: Expression): Type {
        return typeHelper.inferExpressionType(expr)
    }
    
    /**
     * Expose variable access functionality for external use
     */
    fun generateVariableStore(variableName: String, variableType: Type) {
        variableAccessGenerator.generateVariableStore(variableName, variableType)
    }
}