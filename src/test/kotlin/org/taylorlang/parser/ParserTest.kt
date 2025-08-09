package org.taylorlang.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import arrow.core.getOrElse
import org.taylorlang.ast.*

class ParserTest : StringSpec({
    val parser = TaylorLangParser()

    "should parse simple function declaration" {
        val source = "fn add(x: Int, y: Int): Int => x + y"
        
        val result = parser.parse(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result.statements.size shouldBe 1
        val function = result.statements[0] should beInstanceOf<FunctionDecl>()
        function as FunctionDecl
        
        function.name shouldBe "add"
        function.parameters.size shouldBe 2
        function.parameters[0].name shouldBe "x"
        function.parameters[1].name shouldBe "y"
        function.returnType should beInstanceOf<Type.PrimitiveType>()
        function.body should beInstanceOf<FunctionBody.ExpressionBody>()
    }

    "should parse union type declaration" {
        val source = "type Result<T, E> = Ok(T) | Error(E)"
        
        val result = parser.parse(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result.statements.size shouldBe 1
        val typeDecl = result.statements[0] should beInstanceOf<TypeDecl>()
        typeDecl as TypeDecl
        
        typeDecl.name shouldBe "Result"
        typeDecl.typeParams.size shouldBe 2
        typeDecl.typeParams[0] shouldBe "T"
        typeDecl.typeParams[1] shouldBe "E"
        
        typeDecl.unionType.variants.size shouldBe 2
        typeDecl.unionType.variants[0].name shouldBe "Ok"
        typeDecl.unionType.variants[1].name shouldBe "Error"
    }

    "should parse variable declaration with type inference" {
        val source = "val x = 42"
        
        val result = parser.parse(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result.statements.size shouldBe 1
        val valDecl = result.statements[0] should beInstanceOf<ValDecl>()
        valDecl as ValDecl
        
        valDecl.name shouldBe "x"
        valDecl.type shouldBe null // Type inference
        valDecl.initializer should beInstanceOf<Literal.IntLiteral>()
    }

    "should parse literals correctly" {
        val testCases = listOf(
            "42" to Literal.IntLiteral::class,
            "3.14" to Literal.FloatLiteral::class,
            "\"hello\"" to Literal.StringLiteral::class,
            "true" to Literal.BooleanLiteral::class,
            "[1, 2, 3]" to Literal.ListLiteral::class,
            "{\"key\": \"value\"}" to Literal.MapLiteral::class,
            "(1, 2)" to Literal.TupleLiteral::class,
            "null" to Literal.NullLiteral::class
        )
        
        testCases.forEach { (source, expectedType) ->
            val result = parser.parseExpression(source).getOrElse { 
                throw AssertionError("Parse failed for '$source': ${it.message}") 
            }
            
            result should beInstanceOf(expectedType)
        }
    }

    "should parse binary operations with correct precedence" {
        val source = "1 + 2 * 3"
        
        val result = parser.parseExpression(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result should beInstanceOf<BinaryOp>()
        result as BinaryOp
        
        result.operator shouldBe BinaryOperator.PLUS
        result.left should beInstanceOf<Literal.IntLiteral>()
        result.right should beInstanceOf<BinaryOp>()
        
        val rightOp = result.right as BinaryOp
        rightOp.operator shouldBe BinaryOperator.MULTIPLY
    }

    "should parse match expressions" {
        val source = """
            match result {
                case Ok(value) => value
                case Error(msg) => "failed"
            }
        """.trimIndent()
        
        val result = parser.parseExpression(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result should beInstanceOf<MatchExpression>()
        result as MatchExpression
        
        result.target should beInstanceOf<Identifier>()
        result.cases.size shouldBe 2
        
        val okCase = result.cases[0]
        okCase.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        
        val errorCase = result.cases[1]
        errorCase.pattern should beInstanceOf<Pattern.ConstructorPattern>()
    }

    "should parse lambda expressions" {
        val testCases = listOf(
            "x => x * 2",
            "(x, y) => x + y",
            "() => 42"
        )
        
        testCases.forEach { source ->
            val result = parser.parseExpression(source).getOrElse { 
                throw AssertionError("Parse failed for '$source': ${it.message}") 
            }
            
            result should beInstanceOf<LambdaExpression>()
        }
    }

    "should handle parse errors gracefully" {
        val invalidSources = listOf(
            "fn (x: Int) => x",  // Missing function name
            "type = Ok | Error", // Missing type name
            "val = 42",          // Missing variable name
            "1 + + 2",           // Invalid expression
            "match { case => }"  // Invalid match syntax
        )
        
        invalidSources.forEach { source ->
            val result = parser.parse(source)
            result.isLeft() shouldBe true
        }
    }

    "should parse complex example program" {
        val source = """
            type Result<T, E> = Ok(T) | Error(E)
            
            fn divide(x: Int, y: Int): Result<Int, String> => {
                if (y == 0) {
                    Error("Division by zero")
                } else {
                    Ok(x / y)
                }
            }
            
            fn handleResult(result: Result<Int, String>): String => match result {
                case Ok(value) => "Result: " + value
                case Error(msg) => "Error: " + msg
            }
        """.trimIndent()
        
        val result = parser.parse(source).getOrElse { 
            throw AssertionError("Parse failed: ${it.message}") 
        }
        
        result.statements.size shouldBe 3
        result.statements[0] should beInstanceOf<TypeDecl>()
        result.statements[1] should beInstanceOf<FunctionDecl>()
        result.statements[2] should beInstanceOf<FunctionDecl>()
    }
})