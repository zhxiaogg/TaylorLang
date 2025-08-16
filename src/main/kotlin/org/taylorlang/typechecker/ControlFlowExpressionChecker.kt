package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Handles type checking for control flow expressions and complex constructs.
 * 
 * This class is responsible for:
 * - If expressions (conditional branching)
 * - Match expressions (pattern matching)
 * - Block expressions (scoped statement sequences)
 * - Function calls (including generic function type inference)
 * - Constructor calls (union type variant construction)
 * - Type inference and substitution for generics
 * 
 * Separated from the main ExpressionTypeChecker to maintain the 500-line limit.
 */
class ControlFlowExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker,
    private val arithmeticChecker: ArithmeticExpressionChecker
) {
    
    /**
     * Type check a function call.
     * 
     * @param node The function call to type check
     * @return Result containing the typed expression or error
     */
    fun visitFunctionCall(node: FunctionCall): Result<TypedExpression> {
        return when (node.target) {
            is Identifier -> {
                // Regular function call
                visitRegularFunctionCall(node, node.target.name)
            }
            is PropertyAccess -> {
                // Method call (e.g., value.toString())
                visitMethodCall(node, node.target)
            }
            else -> {
                Result.failure(TypeError.InvalidOperation(
                    "Complex function expressions not yet supported",
                    emptyList(),
                    node.target.sourceLocation
                ))
            }
        }
    }
    
    /**
     * Handle regular function calls where target is an identifier.
     */
    private fun visitRegularFunctionCall(node: FunctionCall, functionName: String): Result<TypedExpression> {
        // Look up the function signature
        val functionSignature = context.lookupFunction(functionName)
            ?: return Result.failure(TypeError.UnresolvedSymbol(
                functionName, 
                node.sourceLocation
            ))
        
        val errors = mutableListOf<TypeError>()
        
        // Check arity (parameter count)
        if (node.arguments.size != functionSignature.parameterTypes.size) {
            return Result.failure(TypeError.ArityMismatch(
                expected = functionSignature.parameterTypes.size,
                actual = node.arguments.size,
                location = node.sourceLocation
            ))
        }
        
        // Type check each argument to get their types for inference
        val typedArguments = mutableListOf<TypedExpression>()
        val argumentTypes = mutableListOf<Type>()
        for (i in node.arguments.indices) {
            val argument = node.arguments[i]
            
            val argResult = argument.accept(baseChecker)
            if (argResult.isFailure) {
                val error = argResult.exceptionOrNull()
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error?.message ?: "Unknown error", 
                        emptyList(), 
                        argument.sourceLocation
                    )
                })
                continue
            }
            
            val typedArg = argResult.getOrThrow()
            typedArguments.add(typedArg)
            argumentTypes.add(typedArg.type)
        }
        
        // If this is a generic function, infer type arguments and substitute parameter types
        val (actualParameterTypes, actualReturnType) = if (functionSignature.typeParameters.isNotEmpty()) {
            val inferredTypeArgs = inferTypeArguments(
                functionSignature.parameterTypes.toPersistentList(),
                argumentTypes,
                functionSignature.typeParameters
            )
            
            val substitutedParamTypes = functionSignature.parameterTypes.map { paramType ->
                substituteTypeParameters(paramType, functionSignature.typeParameters, inferredTypeArgs)
            }
            
            val substitutedReturnType = substituteTypeParameters(
                functionSignature.returnType, 
                functionSignature.typeParameters, 
                inferredTypeArgs
            )
            
            Pair(substitutedParamTypes, substitutedReturnType)
        } else {
            Pair(functionSignature.parameterTypes, functionSignature.returnType)
        }
        
        // Type check each argument against the (possibly substituted) parameter type
        for (i in node.arguments.indices) {
            if (i < typedArguments.size && i < actualParameterTypes.size) {
                val typedArg = typedArguments[i]
                val expectedType = actualParameterTypes[i]
                
                // Check if argument type is compatible with parameter type
                if (!typesCompatible(typedArg.type, expectedType)) {
                    errors.add(TypeError.TypeMismatch(
                        expected = expectedType,
                        actual = typedArg.type,
                        location = node.arguments[i].sourceLocation
                    ))
                }
            }
        }
        
        return if (errors.isEmpty()) {
            // Create typed function call with typed arguments
            val typedCall = node.copy(
                arguments = typedArguments.map { it.expression }.toPersistentList()
            )
            // Use canonical builtin type if the return type matches a builtin
            val canonicalReturnType = when {
                actualReturnType is Type.PrimitiveType -> {
                    BuiltinTypes.lookupPrimitive(actualReturnType.name) ?: actualReturnType
                }
                else -> actualReturnType
            }
            Result.success(TypedExpression(typedCall, canonicalReturnType))
        } else {
            val error = if (errors.size == 1) {
                errors.first()
            } else {
                TypeError.MultipleErrors(errors)
            }
            Result.failure(error)
        }
    }
    
    /**
     * Handle method calls where target is a property access (e.g., value.toString()).
     * For now, this implements basic method calls on built-in types.
     */
    private fun visitMethodCall(node: FunctionCall, propertyAccess: PropertyAccess): Result<TypedExpression> {
        // First, type check the target object
        val targetResult = propertyAccess.target.accept(baseChecker)
        if (targetResult.isFailure) {
            return targetResult // Propagate error from target
        }
        
        val targetType = targetResult.getOrThrow().type
        val methodName = propertyAccess.property
        
        // Handle built-in method calls
        return when {
            // toString() method is available on all types and returns String
            methodName == "toString" && node.arguments.isEmpty() -> {
                Result.success(TypedExpression(node, BuiltinTypes.STRING))
            }
            
            // For numeric types, implement basic methods
            targetType == BuiltinTypes.INT && methodName == "toDouble" && node.arguments.isEmpty() -> {
                Result.success(TypedExpression(node, BuiltinTypes.DOUBLE))
            }
            
            targetType == BuiltinTypes.DOUBLE && methodName == "toInt" && node.arguments.isEmpty() -> {
                Result.success(TypedExpression(node, BuiltinTypes.INT))
            }
            
            // String methods
            targetType == BuiltinTypes.STRING && methodName == "length" && node.arguments.isEmpty() -> {
                Result.success(TypedExpression(node, BuiltinTypes.INT))
            }
            
            else -> {
                Result.failure(TypeError.UnresolvedSymbol(
                    "$targetType.$methodName",
                    propertyAccess.sourceLocation
                ))
            }
        }
    }
    
    /**
     * Type check a constructor call.
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
            // Both branches present - unify their types
            arithmeticChecker.unifyTypes(thenType, elseType) ?: run {
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
    
    
    // =============================================================================
    // Private Helper Methods
    // =============================================================================
    
    /**
     * Check if two types are compatible (structural equality ignoring source locations).
     * Migrated to use centralized TypeOperations for consistent type comparison.
     */
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        return TypeOperations.areEqual(type1, type2)
    }
    
    /**
     * Helper methods for type inference - delegate to LiteralExpressionChecker.
     */
    private fun inferTypeArguments(
        parameterTypes: kotlinx.collections.immutable.PersistentList<Type>,
        argumentTypes: List<Type>,
        typeParameters: List<String>
    ): List<Type> {
        val literalChecker = LiteralExpressionChecker(context, baseChecker)
        return literalChecker.inferTypeArguments(parameterTypes, argumentTypes, typeParameters)
    }
    
    private fun substituteTypeParameters(
        type: Type,
        typeParameters: List<String>,
        typeArguments: List<Type>
    ): Type {
        val literalChecker = LiteralExpressionChecker(context, baseChecker)
        return literalChecker.substituteTypeParameters(type, typeParameters, typeArguments)
    }
}