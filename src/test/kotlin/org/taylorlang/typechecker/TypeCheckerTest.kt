package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import arrow.core.getOrElse
import org.taylorlang.ast.*
import org.taylorlang.parser.TaylorLangParser
import kotlinx.collections.immutable.persistentListOf

class TypeCheckerTest : StringSpec({
    val parser = TaylorLangParser()
    val typeChecker = TypeChecker()

    "should type check integer literals" {
        val expression = Literal.IntLiteral(42)
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check string literals" {
        val expression = Literal.StringLiteral("hello")
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.STRING
    }

    "should type check boolean literals" {
        val expression = Literal.BooleanLiteral(true)
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check binary arithmetic operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(2)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check comparison operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.LESS_THAN,
            right = Literal.IntLiteral(2)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check logical operations" {
        val expression = BinaryOp(
            left = Literal.BooleanLiteral(true),
            operator = BinaryOperator.AND,
            right = Literal.BooleanLiteral(false)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check unary operations" {
        val expression = UnaryOp(
            operator = UnaryOperator.MINUS,
            operand = Literal.IntLiteral(42)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check variable references" {
        val expression = Identifier("x")
        val context = TypeContext().withVariable("x", BuiltinTypes.INT)
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type shouldBe BuiltinTypes.INT
    }

    "should fail for undefined variables" {
        val expression = Identifier("undefined")
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
        
        result.isLeft() shouldBe true
        result.leftOrNull() should beInstanceOf<TypeError.UnresolvedSymbol>()
    }

    "should fail for type mismatches in binary operations" {
        val expression = BinaryOp(
            left = Literal.StringLiteral("hello"),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(42)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
        
        result.isLeft() shouldBe true
        result.leftOrNull() should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should type check list literals" {
        val expression = Literal.ListLiteral(
            persistentListOf(
                Literal.IntLiteral(1),
                Literal.IntLiteral(2),
                Literal.IntLiteral(3)
            )
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.type should beInstanceOf<Type.GenericType>()
        val listType = result.type as Type.GenericType
        listType.name shouldBe "List"
        listType.arguments.first() shouldBe BuiltinTypes.INT
    }

    "should type check variable declarations with type inference" {
        val source = "val x = 42"
        val program = parser.parse(source)
            .getOrElse { throw AssertionError("Parse failed: $it") }
        
        val result = typeChecker.typeCheck(program)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.VariableDeclaration>()
        
        val varDecl = statement as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should type check simple function declarations" {
        val source = "fn identity(x: Int): Int => x"
        val program = parser.parse(source)
            .getOrElse { throw AssertionError("Parse failed: $it") }
        
        val result = typeChecker.typeCheck(program)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should handle mixed arithmetic types" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.PLUS,
            right = Literal.FloatLiteral(2.5)
        )
        val context = TypeContext()
        
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        // Should promote to Double
        result.type shouldBe BuiltinTypes.DOUBLE
    }

    "should type check complex expressions" {
        val source = "1 + 2 * 3 < 10"
        val expression = parser.parseExpression(source)
            .getOrElse { throw AssertionError("Parse failed: $it") }
        
        val context = TypeContext()
        val result = typeChecker.typeCheckExpression(expression, context)
            .getOrElse { throw AssertionError("Type check failed: $it") }
        
        // Should be Boolean due to comparison
        result.type shouldBe BuiltinTypes.BOOLEAN
    }
})