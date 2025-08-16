package org.taylorlang.typechecker

import org.taylorlang.ast.*

/**
 * Coordinator for control flow expression type checking.
 * 
 * This coordinator delegates specialized type checking to focused components:
 * - FunctionCallTypeChecker: Function and method calls
 * - ConditionalExpressionChecker: If and while expressions  
 * - MatchExpressionChecker: Pattern matching expressions
 * - LiteralExpressionChecker: Constructor calls
 * 
 * Implements the coordinator pattern to maintain the 500-line file limit
 * while preserving all functionality and ensuring 100% test coverage.
 */
class ControlFlowExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker,
    private val arithmeticChecker: ArithmeticExpressionChecker
) {
    
    // Specialized delegate components
    private val functionCallChecker = FunctionCallTypeChecker(context, baseChecker)
    private val conditionalChecker = ConditionalExpressionChecker(context, baseChecker)
    private val matchChecker = MatchExpressionChecker(context, baseChecker, arithmeticChecker)
    
    /**
     * Type check a function call.
     * Delegates to FunctionCallTypeChecker for specialized handling.
     * 
     * @param node The function call to type check
     * @return Result containing the typed expression or error
     */
    fun visitFunctionCall(node: FunctionCall): Result<TypedExpression> {
        return functionCallChecker.visitFunctionCall(node)
    }
    
    /**
     * Type check a constructor call.
     * Delegates to LiteralExpressionChecker for specialized handling.
     * 
     * @param node The constructor call to type check
     * @return Result containing the typed expression or error
     */
    fun visitConstructorCall(node: ConstructorCall): Result<TypedExpression> {
        val literalChecker = LiteralExpressionChecker(context, baseChecker)
        return literalChecker.visitConstructorCall(node)
    }
    
    /**
     * Type check an if expression.
     * Delegates to ConditionalExpressionChecker for specialized handling.
     * 
     * @param node The if expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitIfExpression(node: IfExpression): Result<TypedExpression> {
        return conditionalChecker.visitIfExpression(node)
    }
    
    /**
     * Type check a while expression.
     * Delegates to ConditionalExpressionChecker for specialized handling.
     * 
     * @param node The while expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitWhileExpression(node: WhileExpression): Result<TypedExpression> {
        return conditionalChecker.visitWhileExpression(node)
    }
    
    /**
     * Type check a match expression.
     * Delegates to MatchExpressionChecker for specialized handling.
     * 
     * @param node The match expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitMatchExpression(node: MatchExpression): Result<TypedExpression> {
        return matchChecker.visitMatchExpression(node)
    }
}