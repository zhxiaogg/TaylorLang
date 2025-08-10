package org.taylorlang.ast.visitor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*

class VisitorTest : StringSpec({

    "BaseASTVisitor should traverse simple program correctly" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "test",
                    parameters = persistentListOf(
                        Parameter("x", Type.PrimitiveType("Int"))
                    ),
                    returnType = Type.PrimitiveType("Int"),
                    body = FunctionBody.ExpressionBody(
                        expression = Identifier("x")
                    )
                )
            )
        )
        
        // Test node counter visitor
        val counter = object : BaseASTVisitor<Int>() {
            override fun defaultResult() = 0
            override fun combine(first: Int, second: Int) = first + second
            
            override fun visitIdentifier(node: Identifier): Int {
                return if (node.name == "x") 1 else 0
            }
        }
        
        val result = program.accept(counter)
        result shouldBe 1
    }
    
    "BaseASTVisitor should collect all nodes of specific type" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "test",
                    parameters = persistentListOf(
                        Parameter("x", Type.PrimitiveType("Int")),
                        Parameter("y", Type.PrimitiveType("String"))
                    ),
                    returnType = Type.PrimitiveType("Int"),
                    body = FunctionBody.ExpressionBody(
                        expression = BinaryOp(
                            left = Identifier("x"),
                            operator = BinaryOperator.PLUS,
                            right = Literal.IntLiteral(1)
                        )
                    )
                )
            )
        )
        
        // Test identifier collector
        val identifierCollector = object : BaseASTVisitor<List<String>>() {
            override fun defaultResult() = emptyList<String>()
            override fun combine(first: List<String>, second: List<String>) = first + second
            
            override fun visitIdentifier(node: Identifier): List<String> {
                return listOf(node.name)
            }
        }
        
        val identifiers = program.accept(identifierCollector)
        identifiers shouldHaveSize 1
        identifiers shouldContain "x"
    }
    
    "BaseASTVisitor should handle nested expressions correctly" {
        val nestedExpression = BinaryOp(
            left = BinaryOp(
                left = Identifier("a"),
                operator = BinaryOperator.PLUS,
                right = Identifier("b")
            ),
            operator = BinaryOperator.MULTIPLY,
            right = BinaryOp(
                left = Identifier("c"),
                operator = BinaryOperator.MINUS,
                right = Identifier("d")
            )
        )
        
        // Count all identifiers
        val identifierCounter = object : BaseASTVisitor<Int>() {
            override fun defaultResult() = 0
            override fun combine(first: Int, second: Int) = first + second
            
            override fun visitIdentifier(node: Identifier): Int = 1
        }
        
        val count = nestedExpression.accept(identifierCounter)
        count shouldBe 4
    }
    
    "BaseASTVisitor should handle patterns correctly" {
        val matchExpr = MatchExpression(
            target = Identifier("value"),
            cases = persistentListOf(
                MatchCase(
                    pattern = Pattern.ConstructorPattern(
                        constructor = "Some",
                        patterns = persistentListOf(Pattern.IdentifierPattern("x"))
                    ),
                    expression = Identifier("x")
                ),
                MatchCase(
                    pattern = Pattern.WildcardPattern,
                    expression = Literal.IntLiteral(0)
                )
            )
        )
        
        // Count pattern identifiers
        val patternCounter = object : BaseASTVisitor<Int>() {
            override fun defaultResult() = 0
            override fun combine(first: Int, second: Int) = first + second
            
            override fun visitIdentifierPattern(node: Pattern.IdentifierPattern): Int = 1
        }
        
        val patternCount = matchExpr.accept(patternCounter)
        patternCount shouldBe 1
    }
    
    "BaseASTVisitor should handle types correctly" {
        val genericType = Type.GenericType(
            name = "List",
            arguments = persistentListOf(
                Type.GenericType(
                    name = "Option",
                    arguments = persistentListOf(Type.PrimitiveType("Int"))
                )
            )
        )
        
        // Count primitive types
        val primitiveTypeCounter = object : BaseASTVisitor<Int>() {
            override fun defaultResult() = 0
            override fun combine(first: Int, second: Int) = first + second
            
            override fun visitPrimitiveType(node: Type.PrimitiveType): Int = 1
        }
        
        val count = genericType.accept(primitiveTypeCounter)
        count shouldBe 1
    }
    
    "BaseASTVisitor should handle complex nested structures" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "complex",
                    parameters = persistentListOf(
                        Parameter("x", Type.PrimitiveType("Int")),
                        Parameter("y", Type.PrimitiveType("String"))  
                    ),
                    returnType = Type.PrimitiveType("Bool"),
                    body = FunctionBody.ExpressionBody(
                        expression = IfExpression(
                            condition = BinaryOp(
                                left = Identifier("x"),
                                operator = BinaryOperator.GREATER_THAN,
                                right = Literal.IntLiteral(0)
                            ),
                            thenExpression = Literal.BooleanLiteral(true),
                            elseExpression = Literal.BooleanLiteral(false)
                        )
                    )
                )
            )
        )
        
        // Count all boolean literals
        val boolCounter = object : BaseASTVisitor<Int>() {
            override fun defaultResult() = 0
            override fun combine(first: Int, second: Int) = first + second
            
            override fun visitBooleanLiteral(node: Literal.BooleanLiteral): Int = 1
        }
        
        val boolCount = program.accept(boolCounter)
        boolCount shouldBe 2
    }
    
    "Visitor pattern should enable double dispatch correctly" {
        val testVisitor = object : BaseASTVisitor<String>() {
            override fun defaultResult() = "OTHER"
            
            override fun visitIdentifier(node: Identifier) = "IDENTIFIER"
            override fun visitIntLiteral(node: Literal.IntLiteral) = "LITERAL"
            override fun visitBinaryOp(node: BinaryOp) = "BINARY_OP"
        }
        
        val identifier = Identifier("test")
        val literal = Literal.IntLiteral(42)
        val binaryOp = BinaryOp(
            left = identifier,
            operator = BinaryOperator.PLUS,
            right = literal
        )
        
        identifier.accept(testVisitor) shouldBe "IDENTIFIER"
        literal.accept(testVisitor) shouldBe "LITERAL"
        binaryOp.accept(testVisitor) shouldBe "BINARY_OP"
    }
})