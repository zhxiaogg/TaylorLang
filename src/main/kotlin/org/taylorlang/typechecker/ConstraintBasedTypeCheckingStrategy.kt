package org.taylorlang.typechecker

import org.taylorlang.ast.Expression
import org.taylorlang.ast.Type

/**
 * Constraint-based type checking strategy implementation.
 * 
 * This strategy implements constraint-based type checking with type inference
 * through constraint generation and unification. It generates type constraints
 * during AST traversal and then uses unification to solve them.
 * 
 * Key characteristics:
 * - Two-phase approach: constraint generation + solving
 * - Powerful type inference capabilities
 * - Support for complex type relationships
 * - Better handling of polymorphic and generic code
 */
class ConstraintBasedTypeCheckingStrategy : TypeCheckingStrategy {
    
    private val constraintCollector = ConstraintCollector()
    
    override fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        return try {
            // Convert TypeContext to InferenceContext
            val inferenceContext = InferenceContext.fromTypeContext(context)
            
            // Collect constraints from the expression
            val constraintResult = constraintCollector.collectConstraints(expression, inferenceContext)
            
            // Solve constraints using unification
            val unificationResult = Unifier.solve(constraintResult.constraints)
            
            unificationResult.fold(
                onSuccess = { substitution ->
                    // Apply the solved substitution to the inferred type
                    val finalType = substitution.apply(constraintResult.type)
                    Result.success(TypedExpression(expression, finalType))
                },
                onFailure = { unificationError ->
                    // Convert unification errors to type errors
                    val typeError = convertUnificationError(unificationError as UnificationError, expression)
                    Result.failure(typeError)
                }
            )
            
        } catch (e: Exception) {
            Result.failure(TypeError.InvalidOperation(
                "Constraint-based type checking failed: ${e.message}",
                emptyList(),
                expression.sourceLocation
            ))
        }
    }
    
    override fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression> {
        return try {
            val inferenceContext = InferenceContext.fromTypeContext(context)
            val constraintResult = constraintCollector.collectConstraintsWithExpected(
                expression, 
                expectedType, 
                inferenceContext
            )
            
            // Solve constraints using unification
            val unificationResult = Unifier.solve(constraintResult.constraints)
            
            unificationResult.fold(
                onSuccess = { substitution ->
                    // Apply the solved substitution to the inferred type
                    val finalType = substitution.apply(constraintResult.type)
                    
                    // Verify that the final type is compatible with the expected type
                    val expectedTypeSubstituted = substitution.apply(expectedType)
                    if (typesCompatible(finalType, expectedTypeSubstituted)) {
                        Result.success(TypedExpression(expression, finalType))
                    } else {
                        Result.failure(TypeError.TypeMismatch(
                            expected = expectedTypeSubstituted,
                            actual = finalType,
                            location = expression.sourceLocation
                        ))
                    }
                },
                onFailure = { unificationError ->
                    // Convert unification errors to type errors
                    val typeError = convertUnificationError(unificationError as UnificationError, expression, expectedType)
                    Result.failure(typeError)
                }
            )
            
        } catch (e: Exception) {
            Result.failure(TypeError.InvalidOperation(
                "Constraint-based type checking with expected type failed: ${e.message}",
                listOf(expectedType),
                expression.sourceLocation
            ))
        }
    }
    
    override fun getStrategyName(): String = "Constraint-Based"
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    /**
     * Convert unification errors to appropriate type errors.
     */
    private fun convertUnificationError(
        unificationError: UnificationError, 
        expression: Expression,
        expectedType: Type? = null
    ): TypeError {
        return when (unificationError) {
            is UnificationError.TypeMismatch -> TypeError.TypeMismatch(
                expected = unificationError.type1,
                actual = unificationError.type2,
                location = unificationError.location ?: expression.sourceLocation
            )
            is UnificationError.InfiniteType -> TypeError.InvalidOperation(
                "Infinite type detected: ${unificationError.message}",
                expectedType?.let { listOf(it) } ?: emptyList(),
                unificationError.location ?: expression.sourceLocation
            )
            is UnificationError.ArityMismatch -> TypeError.ArityMismatch(
                expected = unificationError.expected,
                actual = unificationError.actual,
                location = unificationError.location ?: expression.sourceLocation
            )
            is UnificationError.ConstraintSolvingFailed -> TypeError.InvalidOperation(
                "Constraint solving failed: ${unificationError.message}",
                expectedType?.let { listOf(it) } ?: emptyList(),
                expression.sourceLocation
            )
            else -> TypeError.InvalidOperation(
                "Type inference failed: ${unificationError.message}",
                expectedType?.let { listOf(it) } ?: emptyList(),
                expression.sourceLocation
            )
        }
    }
    
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        // Migrated to use centralized TypeOperations for consistent type comparison
        return TypeOperations.areEqual(type1, type2)
    }
}