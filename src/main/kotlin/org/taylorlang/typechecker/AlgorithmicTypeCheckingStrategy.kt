package org.taylorlang.typechecker

import org.taylorlang.ast.Expression
import org.taylorlang.ast.Type

/**
 * Algorithmic type checking strategy implementation.
 * 
 * This strategy implements traditional algorithmic type checking with direct
 * type inference during AST traversal. It computes types immediately as it
 * visits each expression node, without generating intermediate constraints.
 * 
 * Key characteristics:
 * - Direct type computation during traversal
 * - No intermediate constraint generation
 * - Fast and straightforward for simple type systems
 * - Limited support for complex type inference scenarios
 */
class AlgorithmicTypeCheckingStrategy : TypeCheckingStrategy {
    
    override fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        val checker = ExpressionTypeChecker(context)
        return expression.accept(checker)
    }
    
    override fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression> {
        // For algorithmic type checking, we type check the expression normally
        // and then validate that it matches the expected type
        return typeCheckExpression(expression, context).mapCatching { typedExpr ->
            if (typesCompatible(typedExpr.type, expectedType)) {
                typedExpr
            } else {
                throw TypeError.TypeMismatch(
                    expected = expectedType,
                    actual = typedExpr.type,
                    location = expression.sourceLocation
                )
            }
        }
    }
    
    override fun getStrategyName(): String = "Algorithmic"
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
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