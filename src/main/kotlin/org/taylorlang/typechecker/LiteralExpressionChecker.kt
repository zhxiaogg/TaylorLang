package org.taylorlang.typechecker

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Handles type checking for literals, identifiers, and type inference utilities.
 * 
 * This class is responsible for:
 * - All literal types (Int, Float, String, Boolean, Null, Tuple)
 * - Identifier resolution (variables and zero-argument constructors)
 * - Generic type inference and substitution
 * - Common type compatibility checking utilities
 * 
 * Separated from the main ExpressionTypeChecker to maintain the 500-line limit.
 */
class LiteralExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker
) {
    
    /**
     * Type check an identifier (variable or zero-argument constructor).
     * 
     * @param node The identifier to type check
     * @return Result containing the typed expression or error
     */
    fun visitIdentifier(node: Identifier): Result<TypedExpression> {
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
    
    /**
     * Type check all literal types.
     * 
     * @param node The literal to type check
     * @return Result containing the typed expression or error
     */
    fun visitLiteral(node: Literal): Result<TypedExpression> {
        return when (node) {
            is Literal.IntLiteral -> visitIntLiteral(node)
            is Literal.FloatLiteral -> visitFloatLiteral(node)
            is Literal.StringLiteral -> visitStringLiteral(node)
            is Literal.BooleanLiteral -> visitBooleanLiteral(node)
            is Literal.NullLiteral -> visitNullLiteral(node)
            is Literal.TupleLiteral -> visitTupleLiteral(node)
            is Literal.ListLiteral -> visitListLiteral(node)
        }
    }
    
    /**
     * Type check an integer literal.
     */
    fun visitIntLiteral(node: Literal.IntLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.INT))
    }
    
    /**
     * Type check a float literal.
     */
    fun visitFloatLiteral(node: Literal.FloatLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.DOUBLE))
    }
    
    /**
     * Type check a string literal.
     */
    fun visitStringLiteral(node: Literal.StringLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.STRING))
    }
    
    /**
     * Type check a boolean literal.
     */
    fun visitBooleanLiteral(node: Literal.BooleanLiteral): Result<TypedExpression> {
        return Result.success(TypedExpression(node, BuiltinTypes.BOOLEAN))
    }
    
    /**
     * Type check a null literal.
     */
    fun visitNullLiteral(node: Literal.NullLiteral): Result<TypedExpression> {
        // Null literals create nullable types - we'll use a generic nullable type for now
        return Result.success(TypedExpression(node, Type.NullableType(BuiltinTypes.UNIT)))
    }
    
    /**
     * Type check a tuple literal.
     */
    fun visitTupleLiteral(node: Literal.TupleLiteral): Result<TypedExpression> {
        val elementTypes = mutableListOf<Type>()
        val errors = mutableListOf<TypeError>()
        
        // Type check each element
        for (element in node.elements) {
            val elementResult = element.accept(baseChecker)
            elementResult.fold(
                onSuccess = { typedExpr ->
                    elementTypes.add(typedExpr.type)
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            element.sourceLocation
                        )
                    })
                }
            )
        }
        
        return if (errors.isEmpty()) {
            val tupleType = Type.TupleType(elementTypes.toPersistentList())
            Result.success(TypedExpression(node, tupleType))
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
     * Type check a list literal.
     */
    fun visitListLiteral(node: Literal.ListLiteral): Result<TypedExpression> {
        if (node.elements.isEmpty()) {
            // Empty list: create a List<Unit> as default, can be refined through type inference
            val listType = Type.GenericType("List", persistentListOf(BuiltinTypes.UNIT))
            return Result.success(TypedExpression(node, listType))
        }
        
        val elementTypes = mutableListOf<Type>()
        val errors = mutableListOf<TypeError>()
        
        // Type check each element
        for (element in node.elements) {
            val elementResult = element.accept(baseChecker)
            elementResult.fold(
                onSuccess = { typedExpr ->
                    elementTypes.add(typedExpr.type)
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            element.sourceLocation
                        )
                    })
                }
            )
        }
        
        if (errors.isNotEmpty()) {
            val error = if (errors.size == 1) {
                errors.first()
            } else {
                TypeError.MultipleErrors(errors)
            }
            return Result.failure(error)
        }
        
        // For now, we require all elements to have exactly the same type
        // This is a simple approach - a full implementation would use unification
        val firstElementType = elementTypes.first()
        val allSameType = elementTypes.all { it == firstElementType }
        
        if (!allSameType) {
            return Result.failure(TypeError.TypeMismatch(
                expected = firstElementType,
                actual = elementTypes.find { it != firstElementType }!!,
                node.sourceLocation
            ))
        }
        
        // Create the list type
        val listType = Type.GenericType("List", persistentListOf(firstElementType))
        return Result.success(TypedExpression(node, listType))
    }
    
    // =============================================================================
    // Type Inference and Substitution Utilities
    // =============================================================================
    
    /**
     * Infer type arguments from parameter and argument types.
     * 
     * This is a simplified type inference algorithm that attempts to unify
     * parameter types with argument types to determine type arguments.
     * 
     * @param parameterTypes The declared parameter types (may contain type variables)
     * @param argumentTypes The actual argument types
     * @param typeParameters The type parameter names to infer
     * @return List of inferred types for each type parameter
     */
    fun inferTypeArguments(
        parameterTypes: kotlinx.collections.immutable.PersistentList<Type>,
        argumentTypes: List<Type>,
        typeParameters: List<String>
    ): List<Type> {
        // Create a mapping from type parameter names to inferred types
        val inferredTypes = mutableMapOf<String, Type>()
        
        // Try to infer type parameters by unifying parameter types with argument types
        for (i in parameterTypes.indices) {
            if (i < argumentTypes.size) {
                val paramType = parameterTypes[i]
                val argType = argumentTypes[i]
                
                // Collect type parameter mappings from this parameter-argument pair
                collectTypeParameterMapping(paramType, argType, typeParameters, inferredTypes)
            }
        }
        
        // Return inferred types in the order of type parameters
        return typeParameters.map { typeParam ->
            inferredTypes[typeParam] ?: BuiltinTypes.UNIT // Default if inference failed
        }
    }
    
    /**
     * Collect type parameter mappings by unifying a parameter type with an argument type.
     */
    private fun collectTypeParameterMapping(
        paramType: Type,
        argType: Type,
        typeParameters: List<String>,
        inferredTypes: MutableMap<String, Type>
    ) {
        when (paramType) {
            is Type.NamedType -> {
                // If the parameter type is a type parameter, record the mapping
                if (paramType.name in typeParameters) {
                    inferredTypes[paramType.name] = argType
                }
            }
            is Type.GenericType -> {
                // Recursively handle generic types
                if (argType is Type.GenericType && 
                    paramType.name == argType.name && 
                    paramType.arguments.size == argType.arguments.size) {
                    
                    for (i in paramType.arguments.indices) {
                        collectTypeParameterMapping(
                            paramType.arguments[i],
                            argType.arguments[i],
                            typeParameters,
                            inferredTypes
                        )
                    }
                }
            }
            is Type.TupleType -> {
                // Handle tuple types
                if (argType is Type.TupleType && 
                    paramType.elementTypes.size == argType.elementTypes.size) {
                    
                    for (i in paramType.elementTypes.indices) {
                        collectTypeParameterMapping(
                            paramType.elementTypes[i],
                            argType.elementTypes[i],
                            typeParameters,
                            inferredTypes
                        )
                    }
                }
            }
            is Type.FunctionType -> {
                // Handle function types
                if (argType is Type.FunctionType &&
                    paramType.parameterTypes.size == argType.parameterTypes.size) {
                    
                    // Map parameter types
                    for (i in paramType.parameterTypes.indices) {
                        collectTypeParameterMapping(
                            paramType.parameterTypes[i],
                            argType.parameterTypes[i],
                            typeParameters,
                            inferredTypes
                        )
                    }
                    
                    // Map return type
                    collectTypeParameterMapping(
                        paramType.returnType,
                        argType.returnType,
                        typeParameters,
                        inferredTypes
                    )
                }
            }
            // Add other type forms as needed
            else -> {
                // For other type forms, no type parameter mapping is needed
            }
        }
    }
    
    /**
     * Substitute type parameters in a type with concrete types.
     * 
     * @param type The type potentially containing type parameters
     * @param typeParameters The type parameter names
     * @param typeArguments The concrete types to substitute
     * @return The type with type parameters substituted
     */
    fun substituteTypeParameters(
        type: Type,
        typeParameters: List<String>,
        typeArguments: List<Type>
    ): Type {
        return when (type) {
            is Type.NamedType -> {
                val index = typeParameters.indexOf(type.name)
                if (index >= 0 && index < typeArguments.size) {
                    typeArguments[index]
                } else {
                    type
                }
            }
            is Type.GenericType -> {
                val substitutedArgs = type.arguments.map { arg ->
                    substituteTypeParameters(arg, typeParameters, typeArguments)
                }
                type.copy(arguments = substitutedArgs.toPersistentList())
            }
            is Type.TupleType -> {
                val substitutedElements = type.elementTypes.map { element ->
                    substituteTypeParameters(element, typeParameters, typeArguments)
                }
                type.copy(elementTypes = substitutedElements.toPersistentList())
            }
            is Type.FunctionType -> {
                val substitutedParamTypes = type.parameterTypes.map { param ->
                    substituteTypeParameters(param, typeParameters, typeArguments)
                }
                val substitutedReturnType = substituteTypeParameters(
                    type.returnType, 
                    typeParameters, 
                    typeArguments
                )
                type.copy(
                    parameterTypes = substitutedParamTypes.toPersistentList(),
                    returnType = substitutedReturnType
                )
            }
            is Type.NullableType -> {
                val substitutedBaseType = substituteTypeParameters(
                    type.baseType,
                    typeParameters,
                    typeArguments
                )
                type.copy(baseType = substitutedBaseType)
            }
            is Type.UnionType -> {
                val substitutedArgs = type.typeArguments.map { arg ->
                    substituteTypeParameters(arg, typeParameters, typeArguments)
                }
                type.copy(typeArguments = substitutedArgs.toPersistentList())
            }
            else -> type // PrimitiveType, etc. don't need substitution
        }
    }
    
    /**
     * Substitute a single type with another in a type expression.
     */
    private fun substituteType(type: Type, substitutionMap: Map<String, Type>): Type {
        return when (type) {
            is Type.NamedType -> substitutionMap[type.name] ?: type
            is Type.GenericType -> {
                val substitutedArgs = type.arguments.map { substituteType(it, substitutionMap) }
                type.copy(arguments = substitutedArgs.toPersistentList())
            }
            else -> type
        }
    }
    
    // =============================================================================
    // Constructor Call Helper (for zero-argument constructors from identifiers)
    // =============================================================================
    
    /**
     * Type check a constructor call (used by both visitIdentifier and external callers).
     */
    fun visitConstructorCall(node: ConstructorCall): Result<TypedExpression> {
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
                                val argResult = node.arguments[i].accept(baseChecker)
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
                                ).toPersistentList()
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
    
    /**
     * Helper method for type compatibility checking.
     */
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
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