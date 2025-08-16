package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import org.taylorlang.ast.*
import kotlinx.collections.immutable.persistentListOf

/**
 * Basic Type Checking Tests
 * 
 * Tests fundamental type checking functionality including:
 * - Literal type checking (Int, String, Boolean, Float)
 * - Simple binary operations (arithmetic, comparison, logical)
 * - Basic unary operations
 * - Variable references and undefined variable errors
 * - Tuple literals and type inference
 * - Mixed arithmetic type promotion
 */
class BasicTypeCheckingTest : TypeCheckingTestBase() {
    init {

    "should type check integer literals" {
        val expression = Literal.IntLiteral(42)
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check string literals" {
        val expression = Literal.StringLiteral("hello")
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.STRING
    }

    "should type check boolean literals" {
        val expression = Literal.BooleanLiteral(true)
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check float literals" {
        val expression = Literal.FloatLiteral(3.14)
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.DOUBLE
    }

    "should type check binary arithmetic operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(2)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check subtraction operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(10),
            operator = BinaryOperator.MINUS,
            right = Literal.IntLiteral(3)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check multiplication operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(6),
            operator = BinaryOperator.MULTIPLY,
            right = Literal.IntLiteral(7)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check division operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(15),
            operator = BinaryOperator.DIVIDE,
            right = Literal.IntLiteral(3)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check comparison operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.LESS_THAN,
            right = Literal.IntLiteral(2)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check greater than operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(5),
            operator = BinaryOperator.GREATER_THAN,
            right = Literal.IntLiteral(3)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check equality operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(42),
            operator = BinaryOperator.EQUAL,
            right = Literal.IntLiteral(42)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check logical operations" {
        val expression = BinaryOp(
            left = Literal.BooleanLiteral(true),
            operator = BinaryOperator.AND,
            right = Literal.BooleanLiteral(false)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check logical OR operations" {
        val expression = BinaryOp(
            left = Literal.BooleanLiteral(false),
            operator = BinaryOperator.OR,
            right = Literal.BooleanLiteral(true)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check unary minus operations" {
        val expression = UnaryOp(
            operator = UnaryOperator.MINUS,
            operand = Literal.IntLiteral(42)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.INT
    }

    "should type check unary not operations" {
        val expression = UnaryOp(
            operator = UnaryOperator.NOT,
            operand = Literal.BooleanLiteral(true)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check variable references" {
        val expression = Identifier("x")
        val context = TypeContext().withVariable("x", BuiltinTypes.INT)
        val result = typeCheckExpressionSuccess(expression, context)
        result.type shouldBe BuiltinTypes.INT
    }

    "should fail for undefined variables" {
        val expression = Identifier("undefined")
        val error = expectExpressionTypeCheckFailure(expression)
        error should beInstanceOf<TypeError.UnresolvedSymbol>()
    }

    "should allow string concatenation with numbers" {
        val expression = BinaryOp(
            left = Literal.StringLiteral("hello"),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(42)
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.STRING
    }

    "should allow number concatenation with strings" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(42),
            operator = BinaryOperator.PLUS,
            right = Literal.StringLiteral("hello")
        )
        val result = typeCheckExpressionSuccess(expression)
        result.type shouldBe BuiltinTypes.STRING
    }

    "should type check tuple literals" {
        val expression = Literal.TupleLiteral(
            persistentListOf(
                Literal.IntLiteral(1),
                Literal.StringLiteral("hello")
            )
        )
        val result = typeCheckExpressionSuccess(expression)
        
        result.type should beInstanceOf<Type.TupleType>()
        val tupleType = result.type as Type.TupleType
        tupleType.elementTypes.size shouldBe 2
        tupleType.elementTypes[0] shouldBe BuiltinTypes.INT
        tupleType.elementTypes[1] shouldBe BuiltinTypes.STRING
    }

    "should type check empty tuple literals" {
        val expression = Literal.TupleLiteral(persistentListOf())
        val result = typeCheckExpressionSuccess(expression)
        
        result.type should beInstanceOf<Type.TupleType>()
        val tupleType = result.type as Type.TupleType
        tupleType.elementTypes.size shouldBe 0
    }

    "should handle mixed arithmetic types" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.PLUS,
            right = Literal.FloatLiteral(2.5)
        )
        val result = typeCheckExpressionSuccess(expression)
        // Should promote to Double
        result.type shouldBe BuiltinTypes.DOUBLE
    }

    "should handle complex expressions" {
        val source = "1 + 2 * 3 < 10"
        val expression = parser.parseExpression(source).getOrThrow()
        val result = typeCheckExpressionSuccess(expression)
        // Should be Boolean due to comparison
        result.type shouldBe BuiltinTypes.BOOLEAN
    }

    "should type check variable declarations with type inference" {
        val source = "val x = 42"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.VariableDeclaration>()
        
        val varDecl = statement as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should type check string variable declarations" {
        val source = "val greeting = \"hello world\""
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.VariableDeclaration>()
        
        val varDecl = statement as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.STRING
    }

    "should type check boolean variable declarations" {
        val source = "val flag = true"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.VariableDeclaration>()
        
        val varDecl = statement as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.BOOLEAN
    }
    }
}