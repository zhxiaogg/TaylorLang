package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import org.taylorlang.ast.*

/**
 * Integration tests for constraint-based type checking.
 * Tests the integration between ConstraintCollector and TypeChecker.
 */
class ConstraintBasedTypeCheckerTest : StringSpec({

    beforeEach {
        TypeVar.resetCounter()
    }

    "Constraint-based type checker should handle simple expressions" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = Literal.IntLiteral(42)
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "Constraint-based type checker should handle binary operations" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = BinaryOp(
            left = Literal.IntLiteral(5),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(3)
        )
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.DOUBLE  // Arithmetic result type
    }

    "Constraint-based type checker should handle variables" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext().withVariable("x", BuiltinTypes.STRING)

        val expr = Identifier("x")
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.STRING
    }

    "Constraint-based type checker should handle function calls" {
        val typeChecker = TypeChecker.withConstraints()
        val signature = FunctionSignature(
            parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.INT),
            returnType = BuiltinTypes.INT
        )
        val context = createBuiltinContext().withFunction("add", signature)

        val expr = FunctionCall(
            target = Identifier("add"),
            arguments = persistentListOf(
                Literal.IntLiteral(2),
                Literal.IntLiteral(3)
            )
        )
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "Constraint-based type checker should handle if expressions" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = IfExpression(
            condition = Literal.BooleanLiteral(true),
            thenExpression = Literal.IntLiteral(1),
            elseExpression = Literal.IntLiteral(2)
        )
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "Constraint-based type checker should handle lambda expressions" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = LambdaExpression(
            parameters = persistentListOf("x", "y"),
            body = BinaryOp(
                left = Identifier("x"),
                operator = BinaryOperator.PLUS,
                right = Identifier("y")
            )
        )
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type.shouldBeInstanceOf<Type.FunctionType>()
        
        val functionType = typedExpr.type as Type.FunctionType
        functionType.parameterTypes.size shouldBe 2
        functionType.returnType shouldBe BuiltinTypes.DOUBLE
    }

    "Constraint-based type checker should handle block expressions" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = BlockExpression(
            statements = persistentListOf(
                ValDecl("x", null, Literal.IntLiteral(42))
            ),
            expression = Identifier("x")
        )
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "collectConstraintsOnly should return constraint information" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = BinaryOp(
            left = Literal.IntLiteral(5),
            operator = BinaryOperator.EQUAL,
            right = Literal.StringLiteral("test")
        )
        val result = typeChecker.collectConstraintsOnly(expr, context)

        result.isSuccess shouldBe true
        val constraintResult = result.getOrThrow()
        constraintResult.type shouldBe BuiltinTypes.BOOLEAN
        constraintResult.constraints.isNotEmpty() shouldBe true
        constraintResult.constraints.size() shouldBe 1  // Equality constraint between operand types
    }

    "typeCheckExpressionWithExpected should handle expected types" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = Literal.IntLiteral(42)
        val expectedType = BuiltinTypes.INT
        val result = typeChecker.typeCheckExpressionWithExpected(expr, expectedType, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "Constraint-based type checker should handle unknown identifiers gracefully" {
        val typeChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext()

        val expr = Identifier("unknown")
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type.shouldBeInstanceOf<Type.NamedType>()
        
        // Should generate a fresh type variable for unknown identifier
        val namedType = typedExpr.type as Type.NamedType
        namedType.name shouldBe "T1"
    }

    "Algorithmic type checker should still work as before" {
        val typeChecker = TypeChecker.algorithmic()  // Use algorithmic mode
        val context = createBuiltinContext()

        val expr = Literal.IntLiteral(42)
        val result = typeChecker.typeCheckExpression(expr, context)

        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.INT
    }

    "Both type checking modes should produce same results for simple cases" {
        val algorithmicChecker = TypeChecker.algorithmic()
        val constraintChecker = TypeChecker.withConstraints()
        val context = createBuiltinContext().withVariable("x", BuiltinTypes.STRING)

        val expr = BinaryOp(
            left = Literal.IntLiteral(5),
            operator = BinaryOperator.LESS_THAN,
            right = Literal.IntLiteral(10)
        )

        val algorithmicResult = algorithmicChecker.typeCheckExpression(expr, context)
        val constraintResult = constraintChecker.typeCheckExpression(expr, context)

        algorithmicResult.isSuccess shouldBe true
        constraintResult.isSuccess shouldBe true

        val algorithmicType = algorithmicResult.getOrThrow().type
        val constraintType = constraintResult.getOrThrow().type

        algorithmicType shouldBe constraintType
        algorithmicType shouldBe BuiltinTypes.BOOLEAN
    }
}) {
    companion object {
        private fun createBuiltinContext(): TypeContext {
            return TypeContext(
                types = BuiltinTypes.primitives.mapKeys { it.key }.mapValues { 
                    TypeDefinition.UnionTypeDef(emptyList(), emptyList()) 
                }.toPersistentMap()
            )
        }
    }
}