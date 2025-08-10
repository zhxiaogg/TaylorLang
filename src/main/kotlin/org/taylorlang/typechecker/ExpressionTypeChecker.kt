package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*
import org.taylorlang.ast.visitor.BaseASTVisitor

/**
 * Visitor for type checking expressions using the visitor pattern.
 * 
 * This visitor handles all expression type checking logic, including:
 * - Literal type inference
 * - Variable and function resolution
 * - Binary and unary operation type checking
 * - Constructor call validation
 * - Control flow expression typing (if, match, etc.)
 * 
 * The visitor uses the algorithmic approach for type checking, computing
 * types directly during AST traversal.
 */
class ExpressionTypeChecker(
    private val context: TypeContext
) : BaseASTVisitor<Result<TypedExpression>>() {
    
    override fun defaultResult(): Result<TypedExpression> {
        return Result.failure(TypeError.InvalidOperation(
            "Unsupported expression type", 
            emptyList(), 
            null
        ))
    }
    
    override fun combine(first: Result<TypedExpression>, second: Result<TypedExpression>): Result<TypedExpression> {
        // For expression type checking, we typically don't combine results
        // Each expression visit should return its own result
        return second
    }
    
    // =============================================================================
    // Expression Type Checking
    // =============================================================================
    
    override fun visitExpression(node: Expression): Result<TypedExpression> {
        return when (node) {
            is Identifier -> visitIdentifier(node)
            is FunctionCall -> visitFunctionCall(node)
            is ConstructorCall -> visitConstructorCall(node)
            is BinaryOp -> visitBinaryOp(node)
            is UnaryOp -> visitUnaryOp(node)
            is IfExpression -> visitIfExpression(node)
            is MatchExpression -> visitMatchExpression(node)
            is BlockExpression -> visitBlockExpression(node)
            is Literal -> visitLiteral(node)
            else -> defaultResult()
        }
    }
    
    override fun visitIdentifier(node: Identifier): Result<TypedExpression> {
        // First check if it's a variable
        val variableType = context.lookupVariable(node.name)
        if (variableType != null) {
            return Result.success(TypedExpression(node, variableType))
        }
        
        // Check if it's a zero-argument constructor
        val constructorSignature = context.lookupFunction(node.name)
        if (constructorSignature != null && constructorSignature.parameterTypes.isEmpty()) {
            // Convert the identifier to a constructor call for consistency
            val constructorCall = ConstructorCall(
                constructor = node.name,
                arguments = emptyList<Expression>().toPersistentList(),
                sourceLocation = node.sourceLocation
            )
            return visitConstructorCall(constructorCall)
        }
        
        return Result.failure(TypeError.UnresolvedSymbol(node.name, node.sourceLocation))
    }
    
    override fun visitFunctionCall(node: FunctionCall): Result<TypedExpression> {
        // Extract function name from target (assuming it's an Identifier)
        val functionName = when (node.target) {
            is Identifier -> node.target.name
            else -> return Result.failure(TypeError.InvalidOperation(
                "Complex function expressions not yet supported",
                emptyList(),
                node.target.sourceLocation
            ))
        }
        
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
            
            val argResult = argument.accept(this)
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
            Result.success(TypedExpression(typedCall, actualReturnType))
        } else {
            val error = if (errors.size == 1) {
                errors.first()
            } else {
                TypeError.MultipleErrors(errors)
            }
            Result.failure(error)
        }
    }
    
    override fun visitConstructorCall(node: ConstructorCall): Result<TypedExpression> {
        // Look for the constructor in all union type definitions
        val matchingType = context.types.values.firstNotNullOfOrNull { typeDef ->
            when (typeDef) {
                is TypeDefinition.UnionTypeDef -> {
                    // Check if any variant matches the constructor name
                    val matchingVariant = typeDef.variants.find { variant -> 
                        variant.name == node.constructor 
                    }
                    if (matchingVariant != null) {
                        // Find the union type name from the context
                        val unionTypeName = context.types.entries.find { it.value == typeDef }?.key
                        if (unionTypeName != null) {
                            // Type check constructor arguments first to get argument types
                            val expectedTypes = matchingVariant.fields
                            if (node.arguments.size != expectedTypes.size) {
                                return Result.failure(TypeError.ArityMismatch(
                                    expected = expectedTypes.size,
                                    actual = node.arguments.size,
                                    location = node.sourceLocation
                                ))
                            }
                            
                            // Type check arguments to get their types for inference
                            val typedArguments = mutableListOf<Type>()
                            for (i in node.arguments.indices) {
                                val argResult = node.arguments[i].accept(this)
                                if (argResult.isFailure) {
                                    return argResult
                                }
                                typedArguments.add(argResult.getOrThrow().type)
                            }
                            
                            // Infer type arguments from constructor call if generic
                            val typeArguments = if (typeDef.typeParameters.isNotEmpty()) {
                                inferTypeArguments(
                                    matchingVariant.fields.toPersistentList(),
                                    typedArguments,
                                    typeDef.typeParameters
                                )
                            } else {
                                emptyList<Type>().toPersistentList()
                            }
                            
                            // Now perform type checking with substituted parameter types
                            for (i in node.arguments.indices) {
                                val expectedType = if (typeDef.typeParameters.isNotEmpty()) {
                                    substituteTypeParameters(
                                        expectedTypes[i], 
                                        typeDef.typeParameters, 
                                        typeArguments.toList()
                                    )
                                } else {
                                    expectedTypes[i]
                                }
                                
                                val argType = typedArguments[i]
                                if (!typesCompatible(argType, expectedType)) {
                                    return Result.failure(TypeError.TypeMismatch(
                                        expected = expectedType,
                                        actual = argType,
                                        location = node.arguments[i].sourceLocation
                                    ))
                                }
                            }
                            
                            Type.UnionType(unionTypeName, typeArguments)
                        } else null
                    } else null
                }
            }
        }
        
        return if (matchingType != null) {
            Result.success(TypedExpression(node, matchingType))
        } else {
            Result.failure(TypeError.UnresolvedSymbol(
                node.constructor, 
                node.sourceLocation
            ))
        }
    }
    
    override fun visitBinaryOp(node: BinaryOp): Result<TypedExpression> {
        return node.left.accept(this).mapCatching { leftTyped ->
            node.right.accept(this).mapCatching { rightTyped ->
                val resultType = inferBinaryOpType(
                    node.operator,
                    leftTyped.type,
                    rightTyped.type
                ) ?: throw TypeError.InvalidOperation(
                    "${node.operator.name} on types ${leftTyped.type} and ${rightTyped.type}",
                    listOf(leftTyped.type, rightTyped.type),
                    node.sourceLocation
                )
                
                TypedExpression(
                    node.copy(left = leftTyped.expression, right = rightTyped.expression),
                    resultType
                )
            }.getOrThrow()
        }
    }
    
    override fun visitUnaryOp(node: UnaryOp): Result<TypedExpression> {
        return node.operand.accept(this).mapCatching { operandTyped ->
            val resultType = inferUnaryOpType(node.operator, operandTyped.type)
                ?: throw TypeError.InvalidOperation(
                    "${node.operator.name} on type ${operandTyped.type}",
                    listOf(operandTyped.type),
                    node.sourceLocation
                )
            
            TypedExpression(
                node.copy(operand = operandTyped.expression),
                resultType
            )
        }
    }
    
    override fun visitIfExpression(node: IfExpression): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the condition - must be Boolean
        val conditionResult = node.condition.accept(this)
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
        val thenResult = node.thenExpression.accept(this)
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
            val elseResult = node.elseExpression.accept(this)
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
            unifyTypes(thenType, elseType) ?: run {
                return Result.failure(TypeError.TypeMismatch(
                    expected = thenType,
                    actual = elseType,
                    location = node.elseExpression?.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(node, unifiedType))
    }
    
    override fun visitMatchExpression(node: MatchExpression): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the target expression
        val targetResult = node.target.accept(this)
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
        val patternChecker = PatternTypeChecker(context, this)
        
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
                            errors.add(when (error) {
                                is TypeError -> error
                                else -> TypeError.InvalidOperation(
                                    error.message ?: "Unknown error",
                                    emptyList(),
                                    case.expression.sourceLocation
                                )
                            })
                        }
                    )
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            case.pattern.sourceLocation
                        )
                    })
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
                unifyTypes(acc, caseType) ?: return Result.failure(TypeError.TypeMismatch(
                    expected = acc,
                    actual = caseType,
                    location = node.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(node, unifiedType))
    }
    
    override fun visitBlockExpression(node: BlockExpression): Result<TypedExpression> {
        // Create a new scope for the block
        var blockContext = context
        val errors = mutableListOf<TypeError>()
        val typedStatements = mutableListOf<TypedStatement>()
        
        // Type check each statement in the block and update context
        for (statement in node.statements) {
            val stmtChecker = StatementTypeChecker(blockContext)
            val stmtResult = statement.accept(stmtChecker)
            stmtResult.fold(
                onSuccess = { typedStmt ->
                    typedStatements.add(typedStmt)
                    
                    // Update context with new variable bindings from val declarations
                    if (typedStmt is TypedStatement.VariableDeclaration) {
                        blockContext = blockContext.withVariable(
                            typedStmt.declaration.name,
                            typedStmt.inferredType
                        )
                    }
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error", 
                            emptyList(), 
                            statement.sourceLocation
                        )
                    })
                }
            )
        }
        
        // If there are errors in statements, return them
        if (errors.isNotEmpty()) {
            return Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        }
        
        // Determine the type of the block based on the final expression
        val blockType = if (node.expression != null) {
            // Block has a final expression - type is the type of that expression
            val exprChecker = ExpressionTypeChecker(blockContext)
            node.expression.accept(exprChecker).fold(
                onSuccess = { it.type },
                onFailure = { error ->
                    return Result.failure(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            node.expression.sourceLocation
                        )
                    })
                }
            )
        } else {
            // Block has no final expression - type is Unit
            BuiltinTypes.UNIT
        }
        
        return Result.success(TypedExpression(node, blockType))
    }
    
    // =============================================================================
    // Literal Type Checking
    // =============================================================================
    
    override fun visitLiteral(node: Literal): Result<TypedExpression> {
        return when (node) {
            is Literal.IntLiteral -> visitIntLiteral(node)
            is Literal.FloatLiteral -> visitFloatLiteral(node)
            is Literal.StringLiteral -> visitStringLiteral(node)
            is Literal.BooleanLiteral -> visitBooleanLiteral(node)
            is Literal.NullLiteral -> visitNullLiteral(node)
            is Literal.TupleLiteral -> visitTupleLiteral(node)
        }
    }
    
    override fun visitIntLiteral(node: Literal.IntLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.INT))
    }
    
    override fun visitFloatLiteral(node: Literal.FloatLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.DOUBLE))
    }
    
    override fun visitStringLiteral(node: Literal.StringLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.STRING))
    }
    
    override fun visitBooleanLiteral(node: Literal.BooleanLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.BOOLEAN))
    }
    
    override fun visitNullLiteral(node: Literal.NullLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, Type.NullableType(BuiltinTypes.UNIT)))
    }
    
    override fun visitTupleLiteral(node: Literal.TupleLiteral): Result<TypedExpression> {
        // Type check each element of the tuple
        val elementResults = node.elements.map { element ->
            element.accept(this)
        }
        
        // Check if all elements type-checked successfully
        val failures = elementResults.mapNotNull { it.exceptionOrNull() }
        if (failures.isNotEmpty()) {
            return Result.failure(failures.first() as? TypeError ?: TypeError.InvalidOperation(
                failures.first().message ?: "Unknown error",
                emptyList(),
                node.sourceLocation
            ))
        }
        
        // Extract types from successful results
        val elementTypes = elementResults.map { 
            it.getOrThrow().type 
        }.toPersistentList()
        
        val tupleType = Type.TupleType(elementTypes)
        return Result.success(TypedExpression(node, tupleType))
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    // These helper methods are copied from the original TypeChecker
    // In a full refactor, they would be moved to utility classes
    
    private fun inferTypeArguments(
        parameterTypes: kotlinx.collections.immutable.PersistentList<Type>,
        argumentTypes: List<Type>,
        typeParameters: List<String>
    ): kotlinx.collections.immutable.PersistentList<Type> {
        val typeParameterMap = mutableMapOf<String, Type>()
        
        // For each parameter-argument pair, try to infer type parameter mappings
        for (i in parameterTypes.indices) {
            if (i < argumentTypes.size) {
                val paramType = parameterTypes[i]
                val argType = argumentTypes[i]
                inferTypeParameterMapping(paramType, argType, typeParameterMap)
            }
        }
        
        // Build the type argument list in the same order as type parameters
        return typeParameters.map { typeParam: String ->
            typeParameterMap[typeParam] ?: BuiltinTypes.UNIT // Default to Unit if not inferred
        }.toPersistentList()
    }
    
    private fun inferTypeParameterMapping(
        paramType: Type,
        argType: Type,
        typeParameterMap: MutableMap<String, Type>
    ) {
        when (paramType) {
            is Type.NamedType -> {
                // If paramType is a type parameter (T, U, etc.), map it to argType
                if (paramType.name.length == 1 && paramType.name[0].isUpperCase()) {
                    typeParameterMap[paramType.name] = argType
                }
            }
            is Type.GenericType -> {
                // Handle generic types with nested type parameters
                if (argType is Type.GenericType && paramType.name == argType.name) {
                    for (i in paramType.arguments.indices) {
                        if (i < argType.arguments.size) {
                            inferTypeParameterMapping(
                                paramType.arguments[i],
                                argType.arguments[i],
                                typeParameterMap
                            )
                        }
                    }
                }
            }
            is Type.UnionType -> {
                // Handle union types with nested type parameters
                if (argType is Type.UnionType && paramType.name == argType.name) {
                    for (i in paramType.typeArguments.indices) {
                        if (i < argType.typeArguments.size) {
                            inferTypeParameterMapping(
                                paramType.typeArguments[i],
                                argType.typeArguments[i],
                                typeParameterMap
                            )
                        }
                    }
                }
            }
            // For other types, no inference needed
            else -> { /* No-op */ }
        }
    }
    
    private fun substituteTypeParameters(
        type: Type,
        typeParameters: List<String>,
        typeArguments: List<Type>
    ): Type {
        val substitutionMap = typeParameters.zip(typeArguments).toMap()
        return substituteType(type, substitutionMap)
    }
    
    private fun substituteType(type: Type, substitutionMap: Map<String, Type>): Type {
        return when (type) {
            is Type.NamedType -> {
                // Replace type parameter with concrete type if found in substitution map
                substitutionMap[type.name] ?: type
            }
            is Type.GenericType -> {
                // Recursively substitute type arguments
                Type.GenericType(
                    name = type.name,
                    arguments = type.arguments.map { substituteType(it, substitutionMap) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.UnionType -> {
                // Recursively substitute type arguments
                Type.UnionType(
                    name = type.name,
                    typeArguments = type.typeArguments.map { substituteType(it, substitutionMap) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.NullableType -> {
                Type.NullableType(
                    baseType = substituteType(type.baseType, substitutionMap),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.TupleType -> {
                Type.TupleType(
                    elementTypes = type.elementTypes.map { substituteType(it, substitutionMap) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.FunctionType -> {
                Type.FunctionType(
                    parameterTypes = type.parameterTypes.map { substituteType(it, substitutionMap) }.toPersistentList(),
                    returnType = substituteType(type.returnType, substitutionMap),
                    sourceLocation = type.sourceLocation
                )
            }
            // For type variables, return as-is (should not be substituted here)
            is Type.TypeVar -> type
            // For primitive types, return as-is
            is Type.PrimitiveType -> type
        }
    }
    
    private fun inferBinaryOpType(operator: BinaryOperator, leftType: Type, rightType: Type): Type? {
        return when (operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS, 
            BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, 
            BinaryOperator.MODULO -> {
                BuiltinTypes.getWiderNumericType(leftType, rightType)
            }
            
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> BuiltinTypes.BOOLEAN
            
            BinaryOperator.AND, BinaryOperator.OR -> {
                if (typesCompatible(leftType, BuiltinTypes.BOOLEAN) && 
                    typesCompatible(rightType, BuiltinTypes.BOOLEAN)) {
                    BuiltinTypes.BOOLEAN
                } else null
            }
            
            BinaryOperator.NULL_COALESCING -> {
                // Return the right type (non-nullable version)
                rightType
            }
        }
    }
    
    private fun inferUnaryOpType(operator: UnaryOperator, operandType: Type): Type? {
        return when (operator) {
            UnaryOperator.MINUS -> {
                if (BuiltinTypes.isNumeric(operandType)) {
                    operandType
                } else null
            }
            UnaryOperator.NOT -> {
                if (typesCompatible(operandType, BuiltinTypes.BOOLEAN)) {
                    BuiltinTypes.BOOLEAN
                } else null
            }
        }
    }
    
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        // Structural equality ignoring source locations
        return when {
            type1 is Type.PrimitiveType && type2 is Type.PrimitiveType -> 
                type1.name == type2.name
            type1 is Type.NamedType && type2 is Type.NamedType -> 
                type1.name == type2.name
            type1 is Type.GenericType && type2 is Type.GenericType -> 
                type1.name == type2.name && type1.arguments.size == type2.arguments.size &&
                type1.arguments.zip(type2.arguments).all { (a1, a2) -> typesCompatible(a1, a2) }
            type1 is Type.TupleType && type2 is Type.TupleType -> 
                type1.elementTypes.size == type2.elementTypes.size &&
                type1.elementTypes.zip(type2.elementTypes).all { (t1, t2) -> typesCompatible(t1, t2) }
            type1 is Type.NullableType && type2 is Type.NullableType ->
                typesCompatible(type1.baseType, type2.baseType)
            type1 is Type.UnionType && type2 is Type.UnionType ->
                type1.name == type2.name && type1.typeArguments.size == type2.typeArguments.size &&
                type1.typeArguments.zip(type2.typeArguments).all { (a1, a2) -> typesCompatible(a1, a2) }
            type1 is Type.FunctionType && type2 is Type.FunctionType ->
                typesCompatible(type1.returnType, type2.returnType) &&
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (p1, p2) -> typesCompatible(p1, p2) }
            else -> type1 == type2
        }
    }
    
    private fun unifyTypes(type1: Type, type2: Type): Type? {
        // If types are exactly the same, unification succeeds
        if (typesCompatible(type1, type2)) {
            return type1
        }
        
        // Handle numeric type promotion
        val numericUnification = BuiltinTypes.getWiderNumericType(type1, type2)
        if (numericUnification != null) {
            return numericUnification
        }
        
        // For this implementation, we'll be strict about type unification
        return null
    }
}