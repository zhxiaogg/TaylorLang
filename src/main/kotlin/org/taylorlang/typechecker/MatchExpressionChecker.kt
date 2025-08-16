package org.taylorlang.typechecker

import org.taylorlang.ast.*

/**
 * Specialized type checker for match expressions.
 * 
 * This component handles:
 * - Match expression pattern validation
 * - Exhaustiveness checking for union types
 * - Pattern binding type checking
 * - Case expression type unification
 * 
 * Part of the coordinator pattern implementation for ControlFlowExpressionChecker.
 */
class MatchExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker,
    private val arithmeticChecker: ArithmeticExpressionChecker
) {
    
    /**
     * Type check a match expression.
     * 
     * @param node The match expression to type check
     * @return Result containing the typed expression or error
     */
    fun visitMatchExpression(node: MatchExpression): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the target expression
        val targetResult = node.target.accept(baseChecker)
        val targetType = targetResult.fold(
            onSuccess = { it.type },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        node.target.sourceLocation
                    )
                })
                return Result.failure(
                    if (errors.size == 1) errors.first()
                    else TypeError.MultipleErrors(errors)
                )
            }
        )
        
        // Handle different target types
        val (unionTypeDef, allVariantNames) = when (targetType) {
            is Type.UnionType -> {
                // Find the union type definition to get available variants
                val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                    ?: return Result.failure(TypeError.UndefinedType(
                        targetType.name,
                        node.target.sourceLocation
                    ))
                Pair(unionTypeDef, unionTypeDef.getAllVariantNames())
            }
            is Type.GenericType -> {
                // For generic types, check if the base type is a union type
                val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                    ?: return Result.failure(TypeError.UndefinedType(
                        targetType.name,
                        node.target.sourceLocation
                    ))
                Pair(unionTypeDef, unionTypeDef.getAllVariantNames())
            }
            is Type.PrimitiveType, is Type.NamedType -> {
                // For primitive types, we can't enumerate all variants, so no exhaustiveness checking
                Pair(null, emptySet<String>())
            }
            else -> {
                return Result.failure(TypeError.InvalidOperation(
                    "Match expressions currently only support union types and primitive types, got ${targetType}",
                    listOf(targetType),
                    node.target.sourceLocation
                ))
            }
        }
        
        // Type check each case and collect their result types
        val caseTypes = mutableListOf<Type>()
        val coveredVariants = mutableSetOf<String>()
        val patternChecker = PatternTypeChecker(context, baseChecker)
        
        for (case in node.cases) {
            // Type check the pattern against the target type
            val patternResult = patternChecker.checkPattern(case.pattern, targetType)
            patternResult.fold(
                onSuccess = { patternInfo ->
                    coveredVariants.addAll(patternInfo.coveredVariants)
                    
                    // Create a new context with pattern bindings for the case expression
                    val caseContext = context.withVariables(patternInfo.bindings)
                    val caseChecker = ExpressionTypeChecker(caseContext)
                    
                    // Type check the case expression
                    val caseExprResult = case.expression.accept(caseChecker)
                    caseExprResult.fold(
                        onSuccess = { typedExpr ->
                            caseTypes.add(typedExpr.type)
                        },
                        onFailure = { error ->
                            when (error) {
                                is TypeError.MultipleErrors -> {
                                    // Flatten multiple errors instead of nesting them
                                    errors.addAll(error.errors)
                                }
                                is TypeError -> {
                                    errors.add(error)
                                }
                                else -> {
                                    errors.add(TypeError.InvalidOperation(
                                        error.message ?: "Unknown error",
                                        emptyList(),
                                        case.expression.sourceLocation
                                    ))
                                }
                            }
                        }
                    )
                },
                onFailure = { error ->
                    when (error) {
                        is TypeError.MultipleErrors -> {
                            // Flatten multiple errors instead of nesting them
                            errors.addAll(error.errors)
                        }
                        is TypeError -> {
                            errors.add(error)
                        }
                        else -> {
                            errors.add(TypeError.InvalidOperation(
                                error.message ?: "Unknown error",
                                emptyList(),
                                case.pattern.sourceLocation
                            ))
                        }
                    }
                }
            )
        }
        
        // Check for exhaustiveness - all variants must be covered (only for union types)
        if (allVariantNames.isNotEmpty()) {
            val missingVariants = allVariantNames - coveredVariants
            if (missingVariants.isNotEmpty()) {
                errors.add(TypeError.NonExhaustiveMatch(
                    missingVariants.toList(),
                    node.sourceLocation
                ))
            }
        }
        // For primitive types, exhaustiveness checking is not enforced
        
        // Return errors if any occurred
        if (errors.isNotEmpty()) {
            return Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        }
        
        // Determine the unified result type from all case expressions
        val unifiedType = if (caseTypes.isEmpty()) {
            BuiltinTypes.UNIT
        } else {
            caseTypes.drop(1).fold(caseTypes.first()) { acc, caseType ->
                arithmeticChecker.unifyTypes(acc, caseType) ?: return Result.failure(TypeError.TypeMismatch(
                    expected = acc,
                    actual = caseType,
                    location = node.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(node, unifiedType))
    }
}