package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Type checking errors
 */
sealed class TypeError : Throwable() {
    data class UnresolvedSymbol(
        val symbol: String,
        val location: SourceLocation?
    ) : TypeError()
    
    data class TypeMismatch(
        val expected: Type,
        val actual: Type,
        val location: SourceLocation?
    ) : TypeError()
    
    data class UndefinedType(
        val typeName: String,
        val location: SourceLocation?
    ) : TypeError()
    
    data class ArityMismatch(
        val expected: Int,
        val actual: Int,
        val location: SourceLocation?
    ) : TypeError()
    
    data class InvalidOperation(
        val operation: String,
        val operandTypes: List<Type>,
        val location: SourceLocation?
    ) : TypeError()
    
    data class NonExhaustiveMatch(
        val missingPatterns: List<String>,
        val location: SourceLocation?
    ) : TypeError()
    
    data class DuplicateDefinition(
        val name: String,
        val location: SourceLocation?
    ) : TypeError()
    
    data class MultipleErrors(
        val errors: List<TypeError>
    ) : TypeError()
}

/**
 * Type checking context containing symbol table and type definitions
 */
data class TypeContext(
    val variables: PersistentMap<String, Type> = persistentMapOf(),
    val functions: PersistentMap<String, FunctionSignature> = persistentMapOf(),
    val types: PersistentMap<String, TypeDefinition> = persistentMapOf()
) {
    fun withVariable(name: String, type: Type): TypeContext =
        copy(variables = variables.put(name, type))
    
    fun withFunction(name: String, signature: FunctionSignature): TypeContext =
        copy(functions = functions.put(name, signature))
    
    fun withType(name: String, definition: TypeDefinition): TypeContext =
        copy(types = types.put(name, definition))
    
    fun lookupVariable(name: String): Type? = variables[name]
    fun lookupFunction(name: String): FunctionSignature? = functions[name]
    fun lookupType(name: String): TypeDefinition? = types[name]
}

/**
 * Function signature for type checking
 */
data class FunctionSignature(
    val typeParameters: List<String> = emptyList(),
    val parameterTypes: List<Type>,
    val returnType: Type
)

/**
 * Type definition for user-defined types
 */
sealed class TypeDefinition {
    data class UnionTypeDef(
        val typeParameters: List<String>,
        val variants: List<VariantDef>
    ) : TypeDefinition()
    
    data class VariantDef(
        val name: String,
        val fields: List<Type>
    )
}

/**
 * Built-in types
 */
object BuiltinTypes {
    val INT = Type.PrimitiveType("Int")
    val LONG = Type.PrimitiveType("Long")
    val FLOAT = Type.PrimitiveType("Float")
    val DOUBLE = Type.PrimitiveType("Double")
    val BOOLEAN = Type.PrimitiveType("Boolean")
    val STRING = Type.PrimitiveType("String")
    val UNIT = Type.PrimitiveType("Unit")
    
    val primitives = mapOf(
        "Int" to INT,
        "Long" to LONG,
        "Float" to FLOAT,
        "Double" to DOUBLE,
        "Boolean" to BOOLEAN,
        "String" to STRING,
        "Unit" to UNIT
    )
}

/**
 * Main type checker implementation
 */
class TypeChecker {
    
    /**
     * Type check a complete program
     */
    fun typeCheck(program: Program): Result<TypedProgram> {
        val errors = mutableListOf<TypeError>()
        val context = createBuiltinContext()
        
        // First pass: collect type definitions and function signatures
        val contextWithDeclarations = program.statements.fold(context) { ctx, statement ->
            when (statement) {
                is TypeDecl -> {
                    try {
                        val typeDef = createTypeDefinition(statement)
                        val newCtx = ctx.withType(statement.name, typeDef)
                    
                        // Also add each variant constructor as a function signature
                        typeDef.variants.fold(newCtx) { acc, variant ->
                            val signature = FunctionSignature(
                                typeParameters = statement.typeParams.toList(),
                                parameterTypes = variant.fields,
                                returnType = Type.UnionType(
                                    name = statement.name,
                                    typeArguments = statement.typeParams.map { Type.NamedType(it) }.toPersistentList()
                                )
                            )
                            acc.withFunction(variant.name, signature)
                        }
                    } catch (e: TypeError) {
                        errors.add(e)
                        ctx  // Return unchanged context on error
                    }
                }
                is FunctionDecl -> {
                    val signature = createFunctionSignature(statement, ctx)
                        .fold(
                            onSuccess = { it },
                            onFailure = { error ->
                                errors.addAll(listOf(when (error) {
                                    is TypeError -> error
                                    else -> TypeError.InvalidOperation(error.message ?: "Unknown error", emptyList(), null)
                                }))
                                null
                            }
                        )
                    if (signature != null) {
                        ctx.withFunction(statement.name, signature)
                    } else {
                        ctx
                    }
                }
                else -> ctx
            }
        }
        
        // Second pass: type check all statements with context accumulation
        var currentContext = contextWithDeclarations
        val typedStatements = program.statements.mapNotNull { statement ->
            val result = typeCheckStatement(statement, currentContext)
            result.fold(
                onSuccess = { typedStatement ->
                    // Update context with new variable bindings from val declarations
                    if (typedStatement is TypedStatement.VariableDeclaration) {
                        currentContext = currentContext.withVariable(
                            typedStatement.declaration.name,
                            typedStatement.inferredType
                        )
                    }
                    typedStatement
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(error.message ?: "Unknown error", emptyList(), null)
                    })
                    null 
                }
            )
        }
        
        return if (errors.isEmpty()) {
            Result.success(TypedProgram(typedStatements))
        } else {
            Result.failure(TypeError.MultipleErrors(errors))
        }
    }
    
    /**
     * Type check a single statement
     */
    private fun typeCheckStatement(
        statement: Statement,
        context: TypeContext
    ): Result<TypedStatement> {
        return when (statement) {
            is FunctionDecl -> typeCheckFunction(statement, context)
            is TypeDecl -> Result.success(TypedStatement.TypeDeclaration(statement))
            is ValDecl -> typeCheckValDecl(statement, context)
            is Expression -> typeCheckExpression(statement, context).map { 
                TypedStatement.ExpressionStatement(it) 
            }
        }
    }
    
    /**
     * Type check a function declaration
     */
    private fun typeCheckFunction(
        function: FunctionDecl,
        context: TypeContext
    ): Result<TypedStatement> {
        val errors = mutableListOf<TypeError>()
        
        // Resolve parameter types and create context with function parameters
        val paramContext = function.parameters.fold(context) { ctx, param ->
            val paramType = param.type ?: run {
                errors.add(TypeError.UndefinedType(
                    "Missing type annotation for parameter '${param.name}'",
                    param.sourceLocation
                ))
                BuiltinTypes.UNIT
            }
            ctx.withVariable(param.name, paramType)
        }
        
        // Get the declared return type
        val declaredReturnType = function.returnType ?: BuiltinTypes.UNIT
        
        // Type check function body
        val typedBody = when (function.body) {
            is FunctionBody.ExpressionBody -> {
                typeCheckExpression(function.body.expression, paramContext)
                    .fold(
                        onSuccess = { typedExpr ->
                            // Validate return type matches function body type
                            if (!typesCompatible(typedExpr.type, declaredReturnType)) {
                                errors.add(TypeError.TypeMismatch(
                                    expected = declaredReturnType,
                                    actual = typedExpr.type,
                                    location = function.body.expression.sourceLocation
                                ))
                            }
                            TypedFunctionBody.Expression(typedExpr)
                        },
                        onFailure = { error ->
                            errors.add(when (error) {
                                is TypeError -> error
                                else -> TypeError.InvalidOperation(error.message ?: "Unknown error", emptyList(), null)
                            })
                            null 
                        }
                    )
            }
            is FunctionBody.BlockBody -> {
                // For block bodies, validate the return type is Unit unless explicitly declared
                if (declaredReturnType != BuiltinTypes.UNIT) {
                    errors.add(TypeError.TypeMismatch(
                        expected = declaredReturnType,
                        actual = BuiltinTypes.UNIT,
                        location = function.sourceLocation
                    ))
                }
                TypedFunctionBody.Block(emptyList())
            }
        }
        
        return if (errors.isEmpty() && typedBody != null) {
            Result.success(TypedStatement.FunctionDeclaration(
                function.copy(),
                typedBody
            ))
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
     * Type check a variable declaration
     */
    private fun typeCheckValDecl(
        valDecl: ValDecl,
        context: TypeContext
    ): Result<TypedStatement> {
        return typeCheckExpression(valDecl.initializer, context)
            .mapCatching { typedInitializer ->
                val inferredType = typedInitializer.type
                
                // Check declared type matches inferred type if provided
                valDecl.type?.let { declaredType ->
                    if (!typesCompatible(declaredType, inferredType)) {
                        throw RuntimeException("Type mismatch: expected $declaredType, got $inferredType")
                    }
                }
                
                TypedStatement.VariableDeclaration(
                    valDecl.copy(),
                    typedInitializer,
                    inferredType
                )
            }
    }
    
    /**
     * Type check an expression and return typed expression
     */
    fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        return when (expression) {
            is Identifier -> typeCheckIdentifier(expression, context)
            is Literal.IntLiteral -> Result.success(TypedExpression(expression, BuiltinTypes.INT))
            is Literal.FloatLiteral -> Result.success(TypedExpression(expression, BuiltinTypes.DOUBLE))
            is Literal.StringLiteral -> Result.success(TypedExpression(expression, BuiltinTypes.STRING))
            is Literal.BooleanLiteral -> Result.success(TypedExpression(expression, BuiltinTypes.BOOLEAN))
            is Literal.NullLiteral -> Result.success(TypedExpression(expression, Type.NullableType(BuiltinTypes.UNIT)))
            
            is BinaryOp -> typeCheckBinaryOp(expression, context)
            is UnaryOp -> typeCheckUnaryOp(expression, context)
            is FunctionCall -> typeCheckFunctionCall(expression, context)
            is ConstructorCall -> typeCheckConstructorCall(expression, context)
            is MatchExpression -> typeCheckMatchExpression(expression, context)
            
            is Literal.TupleLiteral -> typeCheckTupleLiteral(expression, context)
            is BlockExpression -> typeCheckBlockExpression(expression, context)
            is IfExpression -> typeCheckIfExpression(expression, context)
            
            else -> Result.failure(RuntimeException("Type checking not implemented for ${expression::class.simpleName}"))
        }
    }
    
    private fun typeCheckIdentifier(
        identifier: Identifier,
        context: TypeContext
    ): Result<TypedExpression> {
        // First check if it's a variable
        val variableType = context.lookupVariable(identifier.name)
        if (variableType != null) {
            return Result.success(TypedExpression(identifier, variableType))
        }
        
        // Check if it's a zero-argument constructor
        val constructorSignature = context.lookupFunction(identifier.name)
        if (constructorSignature != null && constructorSignature.parameterTypes.isEmpty()) {
            // Convert the identifier to a constructor call for consistency
            val constructorCall = ConstructorCall(
                constructor = identifier.name,
                arguments = persistentListOf(),
                sourceLocation = identifier.sourceLocation
            )
            return typeCheckConstructorCall(constructorCall, context)
        }
        
        return Result.failure(TypeError.UnresolvedSymbol(identifier.name, identifier.sourceLocation))
    }
    
    private fun typeCheckBinaryOp(
        binaryOp: BinaryOp,
        context: TypeContext
    ): Result<TypedExpression> {
        return typeCheckExpression(binaryOp.left, context)
            .mapCatching { leftTyped ->
                typeCheckExpression(binaryOp.right, context)
                    .mapCatching { rightTyped ->
                        val resultType = inferBinaryOpType(
                            binaryOp.operator,
                            leftTyped.type,
                            rightTyped.type
                        ) ?: throw TypeError.InvalidOperation(
                            "${binaryOp.operator.name} on types ${leftTyped.type} and ${rightTyped.type}",
                            listOf(leftTyped.type, rightTyped.type),
                            binaryOp.sourceLocation
                        )
                        
                        TypedExpression(
                            binaryOp.copy(left = leftTyped.expression, right = rightTyped.expression),
                            resultType
                        )
                    }.getOrThrow()
            }
    }
    
    private fun typeCheckUnaryOp(
        unaryOp: UnaryOp,
        context: TypeContext
    ): Result<TypedExpression> {
        return typeCheckExpression(unaryOp.operand, context)
            .mapCatching { operandTyped ->
                val resultType = inferUnaryOpType(unaryOp.operator, operandTyped.type)
                    ?: throw TypeError.InvalidOperation(
                        "${unaryOp.operator.name} on type ${operandTyped.type}",
                        listOf(operandTyped.type),
                        unaryOp.sourceLocation
                    )
                
                TypedExpression(
                    unaryOp.copy(operand = operandTyped.expression),
                    resultType
                )
            }
    }
    
    private fun typeCheckFunctionCall(
        call: FunctionCall,
        context: TypeContext
    ): Result<TypedExpression> {
        // Extract function name from target (assuming it's an Identifier)
        val functionName = when (call.target) {
            is Identifier -> call.target.name
            else -> return Result.failure(TypeError.InvalidOperation(
                "Complex function expressions not yet supported",
                emptyList(),
                call.target.sourceLocation
            ))
        }
        
        // Look up the function signature
        val functionSignature = context.lookupFunction(functionName)
            ?: return Result.failure(TypeError.UnresolvedSymbol(
                functionName, 
                call.sourceLocation
            ))
        
        val errors = mutableListOf<TypeError>()
        
        // Check arity (parameter count)
        if (call.arguments.size != functionSignature.parameterTypes.size) {
            return Result.failure(TypeError.ArityMismatch(
                expected = functionSignature.parameterTypes.size,
                actual = call.arguments.size,
                location = call.sourceLocation
            ))
        }
        
        // Type check each argument to get their types for inference
        val typedArguments = mutableListOf<TypedExpression>()
        val argumentTypes = mutableListOf<Type>()
        for (i in call.arguments.indices) {
            val argument = call.arguments[i]
            
            val argResult = typeCheckExpression(argument, context)
            if (argResult.isFailure) {
                val error = argResult.exceptionOrNull()
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(error?.message ?: "Unknown error", emptyList(), argument.sourceLocation)
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
        for (i in call.arguments.indices) {
            if (i < typedArguments.size && i < actualParameterTypes.size) {
                val typedArg = typedArguments[i]
                val expectedType = actualParameterTypes[i]
                
                // Check if argument type is compatible with parameter type
                if (!typesCompatible(typedArg.type, expectedType)) {
                    errors.add(TypeError.TypeMismatch(
                        expected = expectedType,
                        actual = typedArg.type,
                        location = call.arguments[i].sourceLocation
                    ))
                }
            }
        }
        
        return if (errors.isEmpty()) {
            // Create typed function call with typed arguments
            val typedCall = call.copy(
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
    
    private fun typeCheckConstructorCall(
        call: ConstructorCall,
        context: TypeContext
    ): Result<TypedExpression> {
        // Look for the constructor in all union type definitions
        val matchingType = context.types.values.firstNotNullOfOrNull { typeDef ->
            when (typeDef) {
                is TypeDefinition.UnionTypeDef -> {
                    // Check if any variant matches the constructor name
                    val matchingVariant = typeDef.variants.find { variant -> 
                        variant.name == call.constructor 
                    }
                    if (matchingVariant != null) {
                        // Find the union type name from the context
                        val unionTypeName = context.types.entries.find { it.value == typeDef }?.key
                        if (unionTypeName != null) {
                            // Type check constructor arguments first to get argument types
                            val expectedTypes = matchingVariant.fields
                            if (call.arguments.size != expectedTypes.size) {
                                return Result.failure(TypeError.ArityMismatch(
                                    expected = expectedTypes.size,
                                    actual = call.arguments.size,
                                    location = call.sourceLocation
                                ))
                            }
                            
                            // Type check arguments to get their types for inference
                            val typedArguments = mutableListOf<Type>()
                            for (i in call.arguments.indices) {
                                val argResult = typeCheckExpression(call.arguments[i], context)
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
                                persistentListOf<Type>()
                            }
                            
                            // Now perform type checking with substituted parameter types
                            for (i in call.arguments.indices) {
                                val expectedType = if (typeDef.typeParameters.isNotEmpty()) {
                                    substituteTypeParameters(
                                        expectedTypes[i], 
                                        typeDef.typeParameters, 
                                        typeArguments
                                    )
                                } else {
                                    expectedTypes[i]
                                }
                                
                                val argType = typedArguments[i]
                                if (!typesCompatible(argType, expectedType)) {
                                    return Result.failure(TypeError.TypeMismatch(
                                        expected = expectedType,
                                        actual = argType,
                                        location = call.arguments[i].sourceLocation
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
            Result.success(TypedExpression(call, matchingType))
        } else {
            Result.failure(TypeError.UnresolvedSymbol(
                call.constructor, 
                call.sourceLocation
            ))
        }
    }
    
    private fun typeCheckTupleLiteral(
        tupleLiteral: Literal.TupleLiteral,
        context: TypeContext
    ): Result<TypedExpression> {
        // Type check each element of the tuple
        val elementResults = tupleLiteral.elements.map { element ->
            typeCheckExpression(element, context)
        }
        
        // Check if all elements type-checked successfully
        val failures = elementResults.mapNotNull { it.exceptionOrNull() }
        if (failures.isNotEmpty()) {
            return Result.failure(failures.first()) // Return first error
        }
        
        // Extract types from successful results
        val elementTypes = elementResults.map { 
            it.getOrThrow().type 
        }.toPersistentList()
        
        val tupleType = Type.TupleType(elementTypes)
        return Result.success(TypedExpression(tupleLiteral, tupleType))
    }
    
    private fun typeCheckBlockExpression(
        blockExpression: BlockExpression,
        context: TypeContext
    ): Result<TypedExpression> {
        // Create a new scope for the block
        var blockContext = context
        val errors = mutableListOf<TypeError>()
        val typedStatements = mutableListOf<TypedStatement>()
        
        // Type check each statement in the block and update context
        for (statement in blockExpression.statements) {
            val stmtResult = typeCheckStatement(statement, blockContext)
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
        val blockType = if (blockExpression.expression != null) {
            // Block has a final expression - type is the type of that expression
            typeCheckExpression(blockExpression.expression, blockContext)
                .fold(
                    onSuccess = { it.type },
                    onFailure = { error ->
                        return Result.failure(when (error) {
                            is TypeError -> error
                            else -> TypeError.InvalidOperation(
                                error.message ?: "Unknown error",
                                emptyList(),
                                blockExpression.expression.sourceLocation
                            )
                        })
                    }
                )
        } else {
            // Block has no final expression - type is Unit
            BuiltinTypes.UNIT
        }
        
        return Result.success(TypedExpression(blockExpression, blockType))
    }
    
    private fun typeCheckIfExpression(
        ifExpression: IfExpression,
        context: TypeContext
    ): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the condition - must be Boolean
        val conditionResult = typeCheckExpression(ifExpression.condition, context)
        val conditionType = conditionResult.fold(
            onSuccess = { typedExpr ->
                if (!typesCompatible(typedExpr.type, BuiltinTypes.BOOLEAN)) {
                    errors.add(TypeError.TypeMismatch(
                        expected = BuiltinTypes.BOOLEAN,
                        actual = typedExpr.type,
                        location = ifExpression.condition.sourceLocation
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
                        ifExpression.condition.sourceLocation
                    )
                })
                BuiltinTypes.BOOLEAN // Default to avoid further cascading errors
            }
        )
        
        // Type check the then branch
        val thenResult = typeCheckExpression(ifExpression.thenExpression, context)
        val thenType = thenResult.fold(
            onSuccess = { it.type },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        ifExpression.thenExpression.sourceLocation
                    )
                })
                BuiltinTypes.UNIT // Default to avoid further cascading errors
            }
        )
        
        // Type check the else branch if present
        val elseType = if (ifExpression.elseExpression != null) {
            val elseResult = typeCheckExpression(ifExpression.elseExpression, context)
            elseResult.fold(
                onSuccess = { it.type },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            ifExpression.elseExpression.sourceLocation
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
                    location = ifExpression.elseExpression?.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(ifExpression, unifiedType))
    }
    
    /**
     * Type check a match expression with exhaustiveness checking
     */
    private fun typeCheckMatchExpression(
        matchExpression: MatchExpression,
        context: TypeContext
    ): Result<TypedExpression> {
        val errors = mutableListOf<TypeError>()
        
        // Type check the target expression
        val targetResult = typeCheckExpression(matchExpression.target, context)
        val targetType = targetResult.fold(
            onSuccess = { it.type },
            onFailure = { error ->
                errors.add(when (error) {
                    is TypeError -> error
                    else -> TypeError.InvalidOperation(
                        error.message ?: "Unknown error",
                        emptyList(),
                        matchExpression.target.sourceLocation
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
                        matchExpression.target.sourceLocation
                    ))
                Pair(unionTypeDef, unionTypeDef.variants.map { it.name }.toSet())
            }
            is Type.GenericType -> {
                // For generic types, check if the base type is a union type
                val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                    ?: return Result.failure(TypeError.UndefinedType(
                        targetType.name,
                        matchExpression.target.sourceLocation
                    ))
                Pair(unionTypeDef, unionTypeDef.variants.map { it.name }.toSet())
            }
            is Type.PrimitiveType, is Type.NamedType -> {
                // For primitive types, we can't enumerate all variants, so no exhaustiveness checking
                Pair(null, emptySet<String>())
            }
            else -> {
                return Result.failure(TypeError.InvalidOperation(
                    "Match expressions currently only support union types and primitive types, got ${targetType}",
                    listOf(targetType),
                    matchExpression.target.sourceLocation
                ))
            }
        }
        
        // Type check each case and collect their result types
        val caseTypes = mutableListOf<Type>()
        val coveredVariants = mutableSetOf<String>()
        
        for (case in matchExpression.cases) {
            // Type check the pattern against the target type
            val patternResult = typeCheckPattern(case.pattern, targetType, context)
            patternResult.fold(
                onSuccess = { patternInfo ->
                    coveredVariants.addAll(patternInfo.coveredVariants)
                    
                    // Create a new context with pattern bindings for the case expression
                    val caseContext = patternInfo.bindings.entries.fold(context) { ctx, entry ->
                        ctx.withVariable(entry.key, entry.value)
                    }
                    
                    // Type check the case expression
                    val caseExprResult = typeCheckExpression(case.expression, caseContext)
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
                    matchExpression.sourceLocation
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
                    location = matchExpression.sourceLocation
                ))
            }
        }
        
        return Result.success(TypedExpression(matchExpression, unifiedType))
    }
    
    /**
     * Information about a pattern match including variable bindings and covered variants
     */
    private data class PatternInfo(
        val bindings: Map<String, Type>,
        val coveredVariants: Set<String>
    )
    
    /**
     * Type check a pattern against a target type
     */
    private fun typeCheckPattern(
        pattern: Pattern,
        targetType: Type,
        context: TypeContext
    ): Result<PatternInfo> {
        return when (pattern) {
            is Pattern.WildcardPattern -> {
                // Wildcard matches anything and covers all variants for the target type
                val coveredVariants = if (targetType is Type.UnionType) {
                    val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                    unionTypeDef?.variants?.map { it.name }?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }
                Result.success(PatternInfo(
                    bindings = emptyMap(),
                    coveredVariants = coveredVariants
                ))
            }
            
            is Pattern.IdentifierPattern -> {
                // Identifier pattern binds the entire value to a variable
                val coveredVariants = if (targetType is Type.UnionType) {
                    val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                    unionTypeDef?.variants?.map { it.name }?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }
                Result.success(PatternInfo(
                    bindings = mapOf(pattern.name to targetType),
                    coveredVariants = coveredVariants
                ))
            }
            
            is Pattern.LiteralPattern -> {
                // Literal patterns must match the target type exactly
                val literalResult = typeCheckExpression(pattern.literal, context)
                literalResult.fold(
                    onSuccess = { typedLiteral ->
                        if (typesCompatible(typedLiteral.type, targetType)) {
                            Result.success(PatternInfo(
                                bindings = emptyMap(),
                                coveredVariants = emptySet() // Literals don't cover union variants
                            ))
                        } else {
                            Result.failure(TypeError.TypeMismatch(
                                expected = targetType,
                                actual = typedLiteral.type,
                                location = pattern.literal.sourceLocation
                            ))
                        }
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            }
            
            is Pattern.ConstructorPattern -> {
                // Constructor patterns must match a specific variant of a union type
                val typeName = when (targetType) {
                    is Type.UnionType -> targetType.name
                    is Type.GenericType -> targetType.name
                    else -> {
                        return Result.failure(TypeError.InvalidOperation(
                            "Constructor pattern can only be used with union types",
                            listOf(targetType),
                            pattern.sourceLocation
                        ))
                    }
                }
                
                val unionTypeDef = context.lookupType(typeName) as? TypeDefinition.UnionTypeDef
                    ?: return Result.failure(TypeError.UndefinedType(
                        typeName,
                        pattern.sourceLocation
                    ))
                
                val matchingVariant = unionTypeDef.variants.find { it.name == pattern.constructor }
                    ?: return Result.failure(TypeError.UnresolvedSymbol(
                        pattern.constructor,
                        pattern.sourceLocation
                    ))
                
                // Check that pattern arity matches variant arity
                if (pattern.patterns.size != matchingVariant.fields.size) {
                    return Result.failure(TypeError.ArityMismatch(
                        expected = matchingVariant.fields.size,
                        actual = pattern.patterns.size,
                        location = pattern.sourceLocation
                    ))
                }
                
                // Type check nested patterns and collect bindings
                val allBindings = mutableMapOf<String, Type>()
                val errors = mutableListOf<TypeError>()
                
                for (i in pattern.patterns.indices) {
                    val nestedPattern = pattern.patterns[i]
                    val expectedFieldType = if (unionTypeDef.typeParameters.isNotEmpty()) {
                        // Substitute type parameters with concrete types from the target
                        val typeArguments = when (targetType) {
                            is Type.UnionType -> targetType.typeArguments.toList()
                            is Type.GenericType -> targetType.arguments.toList()
                            else -> emptyList()
                        }
                        substituteTypeParameters(
                            matchingVariant.fields[i],
                            unionTypeDef.typeParameters,
                            typeArguments
                        )
                    } else {
                        matchingVariant.fields[i]
                    }
                    
                    val nestedResult = typeCheckPattern(nestedPattern, expectedFieldType, context)
                    nestedResult.fold(
                        onSuccess = { nestedInfo ->
                            allBindings.putAll(nestedInfo.bindings)
                        },
                        onFailure = { error ->
                            errors.add(when (error) {
                                is TypeError -> error
                                else -> TypeError.InvalidOperation(
                                    error.message ?: "Unknown error",
                                    emptyList(),
                                    nestedPattern.sourceLocation
                                )
                            })
                        }
                    )
                }
                
                if (errors.isNotEmpty()) {
                    Result.failure(
                        if (errors.size == 1) errors.first()
                        else TypeError.MultipleErrors(errors)
                    )
                } else {
                    Result.success(PatternInfo(
                        bindings = allBindings,
                        coveredVariants = setOf(pattern.constructor)
                    ))
                }
            }
            
            is Pattern.GuardPattern -> {
                // Type check the inner pattern first
                val innerResult = typeCheckPattern(pattern.pattern, targetType, context)
                innerResult.fold(
                    onSuccess = { innerInfo ->
                        // Create context with pattern bindings for guard expression
                        val guardContext = innerInfo.bindings.entries.fold(context) { ctx, entry ->
                            ctx.withVariable(entry.key, entry.value)
                        }
                        
                        // Type check the guard expression - must be Boolean
                        val guardResult = typeCheckExpression(pattern.guard, guardContext)
                        guardResult.fold(
                            onSuccess = { typedGuard ->
                                if (typesCompatible(typedGuard.type, BuiltinTypes.BOOLEAN)) {
                                    Result.success(innerInfo) // Return inner pattern info
                                } else {
                                    Result.failure(TypeError.TypeMismatch(
                                        expected = BuiltinTypes.BOOLEAN,
                                        actual = typedGuard.type,
                                        location = pattern.guard.sourceLocation
                                    ))
                                }
                            },
                            onFailure = { error -> Result.failure(error) }
                        )
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            }
        }
    }
    
    // Helper functions
    
    /**
     * Infers type arguments for generic type parameters based on constructor arguments.
     * Uses basic inference by matching parameter types with argument types.
     */
    private fun inferTypeArguments(
        parameterTypes: PersistentList<Type>,
        argumentTypes: List<Type>,
        typeParameters: List<String>
    ): PersistentList<Type> {
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
    
    /**
     * Recursively infers type parameter mappings from parameter type to argument type.
     */
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
    
    /**
     * Substitutes type parameters in a type with concrete type arguments.
     */
    private fun substituteTypeParameters(
        type: Type,
        typeParameters: List<String>,
        typeArguments: List<Type>
    ): Type {
        val substitutionMap = typeParameters.zip(typeArguments).toMap()
        return substituteType(type, substitutionMap)
    }
    
    /**
     * Recursively substitutes type parameters in a type using the substitution map.
     */
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
            // For primitive types, return as-is
            is Type.PrimitiveType -> type
        }
    }
    
    private fun createBuiltinContext(): TypeContext {
        return TypeContext(
            types = BuiltinTypes.primitives.mapKeys { it.key }.mapValues { 
                TypeDefinition.UnionTypeDef(emptyList(), emptyList()) 
            }.toPersistentMap()
        )
    }
    
    private fun createTypeDefinition(typeDecl: TypeDecl): TypeDefinition.UnionTypeDef {
        // Validate that variant names are unique within the union
        val variantNames = mutableSetOf<String>()
        val duplicateVariants = mutableListOf<String>()
        
        typeDecl.unionType.variants.forEach { variant ->
            if (!variantNames.add(variant.name)) {
                duplicateVariants.add(variant.name)
            }
        }
        
        if (duplicateVariants.isNotEmpty()) {
            throw TypeError.DuplicateDefinition(
                "Duplicate variant names in union type '${typeDecl.name}': ${duplicateVariants.joinToString(", ")}",
                typeDecl.sourceLocation
            )
        }
        
        val variants = typeDecl.unionType.variants.map { variant ->
            val types = when (variant) {
                is ProductType.Positioned -> variant.types
                is ProductType.Named -> variant.fields.map { it.type }
            }
            TypeDefinition.VariantDef(variant.name, types)
        }
        return TypeDefinition.UnionTypeDef(typeDecl.typeParams.toList(), variants)
    }
    
    private fun createFunctionSignature(
        function: FunctionDecl,
        context: TypeContext
    ): Result<FunctionSignature> {
        val paramTypes = function.parameters.map { param ->
            param.type ?: BuiltinTypes.UNIT // Default to Unit if no type specified
        }
        
        val returnType = function.returnType ?: BuiltinTypes.UNIT
        
        return Result.success(FunctionSignature(
            typeParameters = function.typeParams.toList(),
            parameterTypes = paramTypes,
            returnType = returnType
        ))
    }
    
    private fun inferBinaryOpType(operator: BinaryOperator, leftType: Type, rightType: Type): Type? {
        return when (operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS, 
            BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, 
            BinaryOperator.MODULO -> {
                if (typesCompatible(leftType, BuiltinTypes.INT) && 
                    typesCompatible(rightType, BuiltinTypes.INT)) {
                    BuiltinTypes.INT
                } else if ((typesCompatible(leftType, BuiltinTypes.DOUBLE) || 
                          typesCompatible(leftType, BuiltinTypes.INT)) &&
                         (typesCompatible(rightType, BuiltinTypes.DOUBLE) || 
                          typesCompatible(rightType, BuiltinTypes.INT))) {
                    BuiltinTypes.DOUBLE
                } else null
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
                if (typesCompatible(operandType, BuiltinTypes.INT) ||
                    typesCompatible(operandType, BuiltinTypes.DOUBLE)) {
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
    
    /**
     * Attempts to unify two types for control flow expressions like if-else.
     * Returns a common type if unification is possible, null otherwise.
     * 
     * Unification rules:
     * - Identical types unify to themselves
     * - Different primitive types may create union types (for now, fail)
     * - Compatible numeric types unify to the more general type
     * - For incompatible types, we could create union types (future enhancement)
     */
    private fun unifyTypes(type1: Type, type2: Type): Type? {
        // If types are exactly the same, unification succeeds
        if (typesCompatible(type1, type2)) {
            return type1
        }
        
        // Handle numeric type promotion
        val numericUnification = attemptNumericUnification(type1, type2)
        if (numericUnification != null) {
            return numericUnification
        }
        
        // For this implementation, we'll be strict about type unification
        // In a more advanced implementation, we could create union types
        // or allow implicit conversions for compatible types
        return null
    }
    
    /**
     * Attempts to unify numeric types following common promotion rules
     */
    private fun attemptNumericUnification(type1: Type, type2: Type): Type? {
        // Extract the type names for numeric types
        val type1Name = (type1 as? Type.PrimitiveType)?.name
        val type2Name = (type2 as? Type.PrimitiveType)?.name
        
        return when {
            // Both are Int -> Int
            type1Name == "Int" && type2Name == "Int" -> BuiltinTypes.INT
            
            // Int and Double -> Double
            (type1Name == "Int" && type2Name == "Double") ||
            (type1Name == "Double" && type2Name == "Int") -> BuiltinTypes.DOUBLE
            
            // Int and Float -> Double (promote to highest precision)
            (type1Name == "Int" && type2Name == "Float") ||
            (type1Name == "Float" && type2Name == "Int") -> BuiltinTypes.DOUBLE
            
            // Float and Double -> Double
            (type1Name == "Float" && type2Name == "Double") ||
            (type1Name == "Double" && type2Name == "Float") -> BuiltinTypes.DOUBLE
            
            // Both are Double -> Double
            type1Name == "Double" && type2Name == "Double" -> BuiltinTypes.DOUBLE
            
            // Both are Float -> Float
            type1Name == "Float" && type2Name == "Float" -> BuiltinTypes.FLOAT
            
            // Int and Long -> Long
            (type1Name == "Int" && type2Name == "Long") ||
            (type1Name == "Long" && type2Name == "Int") -> BuiltinTypes.LONG
            
            // Long and Float -> Double
            (type1Name == "Long" && type2Name == "Float") ||
            (type1Name == "Float" && type2Name == "Long") -> BuiltinTypes.DOUBLE
            
            // Long and Double -> Double
            (type1Name == "Long" && type2Name == "Double") ||
            (type1Name == "Double" && type2Name == "Long") -> BuiltinTypes.DOUBLE
            
            // Both are Long -> Long
            type1Name == "Long" && type2Name == "Long" -> BuiltinTypes.LONG
            
            else -> null // No unification possible
        }
    }
}

/**
 * Typed AST nodes
 */
data class TypedProgram(val statements: List<TypedStatement>)

sealed class TypedStatement {
    data class FunctionDeclaration(
        val declaration: FunctionDecl,
        val body: TypedFunctionBody
    ) : TypedStatement()
    
    data class TypeDeclaration(val declaration: TypeDecl) : TypedStatement()
    
    data class VariableDeclaration(
        val declaration: ValDecl,
        val initializer: TypedExpression,
        val inferredType: Type
    ) : TypedStatement()
    
    data class ExpressionStatement(val expression: TypedExpression) : TypedStatement()
}

sealed class TypedFunctionBody {
    data class Expression(val expression: TypedExpression) : TypedFunctionBody()
    data class Block(val statements: List<TypedStatement>) : TypedFunctionBody()
}

data class TypedExpression(
    val expression: Expression,
    val type: Type
)