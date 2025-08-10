package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentMap
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
                }
                is FunctionDecl -> {
                    val signature = createFunctionSignature(statement, ctx)
                        .fold(
                            onSuccess = { it },
                            onFailure = { errors.addAll(listOf(TypeError.InvalidOperation(it.message ?: "Unknown error", emptyList(), null))); null }
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
        
        // Second pass: type check all statements
        val typedStatements = program.statements.mapNotNull { statement ->
            typeCheckStatement(statement, contextWithDeclarations).fold(
                onSuccess = { it },
                onFailure = { 
                    errors.add(TypeError.InvalidOperation(it.message ?: "Unknown error", emptyList(), null))
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
                        onFailure = { 
                            errors.add(when (it) {
                                is TypeError -> it
                                else -> TypeError.InvalidOperation(it.message ?: "Unknown error", emptyList(), null)
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
            
            is Literal.TupleLiteral -> typeCheckTupleLiteral(expression, context)
            
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
        
        // Type check each argument against corresponding parameter type
        val typedArguments = mutableListOf<TypedExpression>()
        for (i in call.arguments.indices) {
            val argument = call.arguments[i]
            val expectedType = functionSignature.parameterTypes[i]
            
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
            
            // Check if argument type is compatible with parameter type
            if (!typesCompatible(typedArg.type, expectedType)) {
                errors.add(TypeError.TypeMismatch(
                    expected = expectedType,
                    actual = typedArg.type,
                    location = argument.sourceLocation
                ))
            }
        }
        
        return if (errors.isEmpty()) {
            // Create typed function call with typed arguments
            val typedCall = call.copy(
                arguments = typedArguments.map { it.expression }.toPersistentList()
            )
            Result.success(TypedExpression(typedCall, functionSignature.returnType))
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
                            // Type check constructor arguments
                            val expectedTypes = matchingVariant.fields
                            if (call.arguments.size != expectedTypes.size) {
                                return Result.failure(TypeError.ArityMismatch(
                                    expected = expectedTypes.size,
                                    actual = call.arguments.size,
                                    location = call.sourceLocation
                                ))
                            }
                            
                            // Check each argument type
                            for (i in call.arguments.indices) {
                                val argResult = typeCheckExpression(call.arguments[i], context)
                                if (argResult.isFailure) {
                                    return argResult
                                }
                                val argType = argResult.getOrThrow().type
                                if (!typesCompatible(argType, expectedTypes[i])) {
                                    return Result.failure(TypeError.TypeMismatch(
                                        expected = expectedTypes[i],
                                        actual = argType,
                                        location = call.arguments[i].sourceLocation
                                    ))
                                }
                            }
                            
                            // Return the union type with generic parameters if any
                            val typeArguments = if (typeDef.typeParameters.isNotEmpty()) {
                                // For now, we'll use Unit for unspecified type parameters
                                // In a full implementation, we'd perform type inference
                                typeDef.typeParameters.map { BuiltinTypes.UNIT }.toPersistentList()
                            } else {
                                persistentListOf()
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
    
    // Helper functions
    
    private fun createBuiltinContext(): TypeContext {
        return TypeContext(
            types = BuiltinTypes.primitives.mapKeys { it.key }.mapValues { 
                TypeDefinition.UnionTypeDef(emptyList(), emptyList()) 
            }.toPersistentMap()
        )
    }
    
    private fun createTypeDefinition(typeDecl: TypeDecl): TypeDefinition.UnionTypeDef {
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