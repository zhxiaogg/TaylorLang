package org.taylorlang.typechecker

import org.taylorlang.ast.Expression
import org.taylorlang.ast.Type

/**
 * Algorithmic type checking strategy implementation.
 * 
 * This strategy implements traditional algorithmic type checking with direct
 * type inference during AST traversal. It computes types immediately as it
 * visits each expression node, without generating intermediate constraints.
 * 
 * Key characteristics:
 * - Direct type computation during traversal
 * - No intermediate constraint generation
 * - Fast and straightforward for simple type systems
 * - Limited support for complex type inference scenarios
 */
class AlgorithmicTypeCheckingStrategy : TypeCheckingStrategy {
    
    override fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        val checker = ExpressionTypeChecker(context)
        return expression.accept(checker)
    }
    
    override fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression> {
        // For algorithmic type checking, we type check the expression normally
        // and then validate that it matches the expected type
        return typeCheckExpression(expression, context).mapCatching { typedExpr ->
            if (typesCompatible(typedExpr.type, expectedType)) {
                typedExpr
            } else {
                throw TypeError.TypeMismatch(
                    expected = expectedType,
                    actual = typedExpr.type,
                    location = expression.sourceLocation
                )
            }
        }
    }
    
    override fun getStrategyName(): String = "Algorithmic"
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        // Migrated to use centralized TypeOperations for consistent type comparison
        return TypeOperations.areEqual(type1, type2)
    }
}