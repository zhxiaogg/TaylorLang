package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Specialized type checker for function calls and method calls.
 * 
 * This component handles:
 * - Regular function calls with type inference
 * - Method calls on objects (e.g., value.toString())
 * - Generic function type inference and substitution
 * - Parameter type checking and validation
 * 
 * Part of the coordinator pattern implementation for ControlFlowExpressionChecker.
 */
class FunctionCallTypeChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker
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
     * Check if two types are compatible (structural equality ignoring source locations).
     * Migrated to use centralized TypeOperations for consistent type comparison.
     */
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        return TypeOperations.areCompatible(type1, type2)
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