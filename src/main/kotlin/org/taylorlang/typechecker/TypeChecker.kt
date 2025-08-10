package org.taylorlang.typechecker

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Type checking errors
 */
sealed class TypeError {
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
    fun typeCheck(program: Program): Either<List<TypeError>, TypedProgram> {
        val errors = mutableListOf<TypeError>()
        val context = createBuiltinContext()
        
        // First pass: collect type definitions and function signatures
        val contextWithDeclarations = program.statements.fold(context) { ctx, statement ->
            when (statement) {
                is TypeDecl -> {
                    val typeDef = createTypeDefinition(statement)
                    ctx.withType(statement.name, typeDef)
                }
                is FunctionDecl -> {
                    val signature = createFunctionSignature(statement, ctx)
                        .fold(
                            { errors.addAll(it); null },
                            { it }
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
            when (val result = typeCheckStatement(statement, contextWithDeclarations)) {
                is Either.Left -> {
                    errors.addAll(result.value)
                    null
                }
                is Either.Right -> result.value
            }
        }
        
        return if (errors.isEmpty()) {
            TypedProgram(typedStatements).right()
        } else {
            errors.left()
        }
    }
    
    /**
     * Type check a single statement
     */
    private fun typeCheckStatement(
        statement: Statement,
        context: TypeContext
    ): Either<List<TypeError>, TypedStatement> {
        return when (statement) {
            is FunctionDecl -> typeCheckFunction(statement, context)
            is TypeDecl -> TypedStatement.TypeDeclaration(statement).right()
            is ValDecl -> typeCheckValDecl(statement, context)
            is Expression -> typeCheckExpression(statement, context).map { 
                TypedStatement.ExpressionStatement(it) 
            }
        }.mapLeft { error -> 
            when (error) {
                is List<*> -> error.filterIsInstance<TypeError>()
                is TypeError -> listOf(error)
                else -> listOf(TypeError.InvalidOperation("Unknown error", emptyList(), null))
            }
        }
    }
    
    /**
     * Type check a function declaration
     */
    private fun typeCheckFunction(
        function: FunctionDecl,
        context: TypeContext
    ): Either<List<TypeError>, TypedStatement> {
        val errors = mutableListOf<TypeError>()
        
        // Create context with function parameters
        val paramContext = function.parameters.fold(context) { ctx, param ->
            val paramType = param.type ?: run {
                errors.add(TypeError.TypeMismatch(
                    expected = Type.PrimitiveType("Any"), // Placeholder
                    actual = Type.PrimitiveType("Unknown"),
                    location = param.sourceLocation
                ))
                BuiltinTypes.UNIT
            }
            ctx.withVariable(param.name, paramType)
        }
        
        // Type check function body
        val typedBody = when (function.body) {
            is FunctionBody.ExpressionBody -> {
                typeCheckExpression(function.body.expression, paramContext)
                    .fold(
                        { errors.addAll(listOf(it)); null },
                        { TypedFunctionBody.Expression(it) }
                    )
            }
            is FunctionBody.BlockBody -> {
                // For now, treat block bodies as Unit-returning
                TypedFunctionBody.Block(emptyList())
            }
        }
        
        return if (errors.isEmpty() && typedBody != null) {
            TypedStatement.FunctionDeclaration(
                function.copy(),
                typedBody
            ).right()
        } else {
            errors.left()
        }
    }
    
    /**
     * Type check a variable declaration
     */
    private fun typeCheckValDecl(
        valDecl: ValDecl,
        context: TypeContext
    ): Either<List<TypeError>, TypedStatement> {
        return typeCheckExpression(valDecl.initializer, context)
            .fold(
                { error -> listOf(error).left() },
                { typedInitializer ->
                    val inferredType = typedInitializer.type
                    
                    // Check declared type matches inferred type if provided
                    valDecl.type?.let { declaredType ->
                        if (!typesCompatible(declaredType, inferredType)) {
                            return listOf(TypeError.TypeMismatch(
                                expected = declaredType,
                                actual = inferredType,
                                location = valDecl.sourceLocation
                            )).left()
                        }
                    }
                    
                    TypedStatement.VariableDeclaration(
                        valDecl.copy(),
                        typedInitializer,
                        inferredType
                    ).right()
                }
            )
    }
    
    /**
     * Type check an expression and return typed expression
     */
    fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        return when (expression) {
            is Identifier -> typeCheckIdentifier(expression, context)
            is Literal.IntLiteral -> TypedExpression(expression, BuiltinTypes.INT).right()
            is Literal.FloatLiteral -> TypedExpression(expression, BuiltinTypes.DOUBLE).right()
            is Literal.StringLiteral -> TypedExpression(expression, BuiltinTypes.STRING).right()
            is Literal.BooleanLiteral -> TypedExpression(expression, BuiltinTypes.BOOLEAN).right()
            is Literal.NullLiteral -> TypedExpression(expression, Type.NullableType(BuiltinTypes.UNIT)).right()
            
            is BinaryOp -> typeCheckBinaryOp(expression, context)
            is UnaryOp -> typeCheckUnaryOp(expression, context)
            is FunctionCall -> typeCheckFunctionCall(expression, context)
            is ConstructorCall -> typeCheckConstructorCall(expression, context)
            
            is Literal.TupleLiteral -> typeCheckTupleLiteral(expression, context)
            
            else -> TypeError.InvalidOperation(
                operation = "type checking ${expression::class.simpleName}",
                operandTypes = emptyList(),
                location = expression.sourceLocation
            ).left()
        }
    }
    
    private fun typeCheckIdentifier(
        identifier: Identifier,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        val type = context.lookupVariable(identifier.name)
            ?: return TypeError.UnresolvedSymbol(
                symbol = identifier.name,
                location = identifier.sourceLocation
            ).left()
        
        return TypedExpression(identifier, type).right()
    }
    
    private fun typeCheckBinaryOp(
        binaryOp: BinaryOp,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        return typeCheckExpression(binaryOp.left, context)
            .flatMap { leftTyped ->
                typeCheckExpression(binaryOp.right, context)
                    .flatMap { rightTyped ->
                        val resultType = inferBinaryOpType(
                            binaryOp.operator,
                            leftTyped.type,
                            rightTyped.type
                        ) ?: return TypeError.InvalidOperation(
                            operation = binaryOp.operator.name,
                            operandTypes = listOf(leftTyped.type, rightTyped.type),
                            location = binaryOp.sourceLocation
                        ).left()
                        
                        TypedExpression(
                            binaryOp.copy(left = leftTyped.expression, right = rightTyped.expression),
                            resultType
                        ).right()
                    }
            }
    }
    
    private fun typeCheckUnaryOp(
        unaryOp: UnaryOp,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        return typeCheckExpression(unaryOp.operand, context)
            .flatMap { operandTyped ->
                val resultType = inferUnaryOpType(unaryOp.operator, operandTyped.type)
                    ?: return TypeError.InvalidOperation(
                        operation = unaryOp.operator.name,
                        operandTypes = listOf(operandTyped.type),
                        location = unaryOp.sourceLocation
                    ).left()
                
                TypedExpression(
                    unaryOp.copy(operand = operandTyped.expression),
                    resultType
                ).right()
            }
    }
    
    private fun typeCheckFunctionCall(
        call: FunctionCall,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        // For now, assume all function calls return Unit
        // TODO: Implement proper function signature lookup and checking
        return TypedExpression(call, BuiltinTypes.UNIT).right()
    }
    
    private fun typeCheckConstructorCall(
        call: ConstructorCall,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        // TODO: Implement constructor type checking
        return TypedExpression(call, BuiltinTypes.UNIT).right()
    }
    
    private fun typeCheckTupleLiteral(
        tupleLiteral: Literal.TupleLiteral,
        context: TypeContext
    ): Either<TypeError, TypedExpression> {
        // Type check each element of the tuple
        val elementResults = tupleLiteral.elements.map { element ->
            typeCheckExpression(element, context)
        }
        
        // Check if all elements type-checked successfully
        val errors = elementResults.mapNotNull { it.leftOrNull() }
        if (errors.isNotEmpty()) {
            return errors.first().left() // Return first error
        }
        
        // Extract types from successful results
        val elementTypes = elementResults.map { 
            it.getOrNull()!!.type 
        }.toPersistentList()
        
        val tupleType = Type.TupleType(elementTypes)
        return TypedExpression(tupleLiteral, tupleType).right()
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
    ): Either<List<TypeError>, FunctionSignature> {
        val paramTypes = function.parameters.map { param ->
            param.type ?: BuiltinTypes.UNIT // Default to Unit if no type specified
        }
        
        val returnType = function.returnType ?: BuiltinTypes.UNIT
        
        return FunctionSignature(
            typeParameters = function.typeParams.toList(),
            parameterTypes = paramTypes,
            returnType = returnType
        ).right()
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
        // Simple structural equality for now
        return type1 == type2
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