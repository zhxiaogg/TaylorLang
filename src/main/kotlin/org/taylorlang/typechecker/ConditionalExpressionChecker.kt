package org.taylorlang.typechecker

import org.taylorlang.ast.*

/**
 * Specialized type checker for conditional expressions.
 * 
 * This component handles:
 * - If expressions with type unification
 * - While expressions 
 * - Condition validation (Boolean type enforcement)
 * - Branch type checking and unification
 * 
 * Part of the coordinator pattern implementation for ControlFlowExpressionChecker.
 */
class ConditionalExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker
) {
    
    /**
     * Type check an if expression.
     * 
     * @param node The if expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitIfExpression(node: IfExpression): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the condition - must be Boolean
        val conditionResult = node.condition.accept(baseChecker)
        val conditionType = conditionResult.fold(
            onSuccess = { typedExpr ->
                if (!typesCompatible(typedExpr.type, BuiltinTypes.BOOLEAN)) {
                    errors.add(TypeError.TypeMismatch(
                        expected = BuiltinTypes.BOOLEAN,
                        actual = typedExpr.type,
                        location = node.condition.sourceLocation
                    ))
                }
                typedExpr.type
            },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        node.condition.sourceLocation
                    )
                })
                BuiltinTypes.BOOLEAN // Default to avoid further cascading errors
            }
        )
        
        // Type check the then branch
        val thenResult = node.thenExpression.accept(baseChecker)
        val thenType = thenResult.fold(
            onSuccess = { it.type },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        node.thenExpression.sourceLocation
                    )
                })
                BuiltinTypes.UNIT // Default to avoid further cascading errors
            }
        )
        
        // Type check the else branch if present
        val elseType = if (node.elseExpression != null) {
            val elseResult = node.elseExpression.accept(baseChecker)
            elseResult.fold(
                onSuccess = { it.type },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            node.elseExpression.sourceLocation
                        )
                    })
                    BuiltinTypes.UNIT // Default to avoid further cascading errors
                }
            )
        } else {
            null // No else branch
        }
        
        // Return errors if any occurred during type checking
        if (errors.isNotEmpty()) {
            return Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        }
        
        // Determine the unified type of the if expression
        val unifiedType = if (elseType == null) {
            // No else branch - result is nullable version of then type
            Type.NullableType(thenType)
        } else {
            // Both branches present - require strict type equality for if expressions
            // This is stricter than arithmetic unification to prevent mixed types like Int/Double
            if (TypeOperations.areEqual(thenType, elseType)) {
                thenType  // Types are the same, return either one
            } else {
                return Result.failure(TypeError.TypeMismatch(
                    expected = thenType,
                    actual = elseType,
                    location = node.elseExpression?.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(node, unifiedType))
    }
    
    /**
     * Type check a while expression.
     * 
     * @param node The while expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitWhileExpression(node: WhileExpression): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the condition - must be Boolean
        val conditionResult = node.condition.accept(baseChecker)
        conditionResult.fold(
            onSuccess = { typedExpr ->
                if (!typesCompatible(typedExpr.type, BuiltinTypes.BOOLEAN)) {
                    errors.add(TypeError.TypeMismatch(
                        expected = BuiltinTypes.BOOLEAN,
                        actual = typedExpr.type,
                        location = node.condition.sourceLocation
                    ))
                }
            },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        node.condition.sourceLocation
                    )
                })
            }
        )
        
        // Type check the body
        val bodyResult = node.body.accept(baseChecker)
        bodyResult.fold(
            onSuccess = { /* Body type is not used for while result */ },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        node.body.sourceLocation
                    )
                })
            }
        )
        
        // Return errors if any occurred during type checking
        if (errors.isNotEmpty()) {
            return Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        }
        
        // While loops typically return Unit
        return Result.success(TypedExpression(node, BuiltinTypes.UNIT))
    }
    
    /**
     * Check if two types are compatible (structural equality ignoring source locations).
     * Migrated to use centralized TypeOperations for consistent type comparison.
     */
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        return TypeOperations.areCompatible(type1, type2)
    }
}