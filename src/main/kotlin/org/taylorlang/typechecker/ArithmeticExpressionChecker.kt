package org.taylorlang.typechecker

import org.taylorlang.ast.*

/**
 * Handles type checking for arithmetic and logical binary/unary operations.
 * 
 * This class is responsible for:
 * - Binary arithmetic operations (PLUS, MINUS, MULTIPLY, DIVIDE, MODULO)
 * - Binary comparison operations (LESS_THAN, GREATER_THAN, EQUAL, etc.)
 * - Binary logical operations (AND, OR, NULL_COALESCING)
 * - Unary operations (MINUS, NOT)
 * - Numeric type promotion and compatibility checking
 * 
 * Separated from the main ExpressionTypeChecker to maintain the 500-line limit.
 */
class ArithmeticExpressionChecker(
    private val context: TypeContext,
    private val baseChecker: ExpressionTypeChecker
) {
    
    /**
     * Type check a binary operation.
     * 
     * @param node The binary operation to type check
     * @return Result containing the typed expression or error
     */
    fun visitBinaryOp(node: BinaryOp): Result<TypedExpression> {
        return node.left.accept(baseChecker).mapCatching { leftTyped ->
            node.right.accept(baseChecker).mapCatching { rightTyped ->
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
    
    /**
     * Type check a unary operation.
     * 
     * @param node The unary operation to type check  
     * @return Result containing the typed expression or error
     */
    fun visitUnaryOp(node: UnaryOp): Result<TypedExpression> {
        return node.operand.accept(baseChecker).mapCatching { operandTyped ->
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
    
    // =============================================================================
    // Private Helper Methods
    // =============================================================================
    
    /**
     * Infer the result type of a binary operation.
     * 
     * @param operator The binary operator
     * @param leftType Type of the left operand
     * @param rightType Type of the right operand
     * @return The result type, or null if the operation is invalid
     */
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
    
    /**
     * Infer the result type of a unary operation.
     * 
     * @param operator The unary operator
     * @param operandType Type of the operand
     * @return The result type, or null if the operation is invalid
     */
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
    
    /**
     * Check if two types are compatible (structural equality ignoring source locations).
     * 
     * @param type1 First type to compare
     * @param type2 Second type to compare
     * @return true if the types are compatible
     */
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
     * Attempt to unify two types, handling numeric type promotion.
     * 
     * @param type1 First type to unify
     * @param type2 Second type to unify
     * @return The unified type, or null if unification fails
     */
    fun unifyTypes(type1: Type, type2: Type): Type? {
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