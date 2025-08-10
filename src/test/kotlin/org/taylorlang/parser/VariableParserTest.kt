package org.taylorlang.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import org.taylorlang.ast.*

class VariableParserTest : StringSpec({
    val parser = TaylorLangParser()
    
    "should parse var declaration with explicit type" {
        val program = parser.parse("var x: Int = 42").getOrThrow()
        program.statements.size shouldBe 1
        
        val statement = program.statements[0]
        statement should beInstanceOf<VarDecl>()
        val varDecl = statement as VarDecl
        varDecl.name shouldBe "x"
        varDecl.type should beInstanceOf<Type.PrimitiveType>()
        (varDecl.type as Type.PrimitiveType).name shouldBe "Int"
        varDecl.initializer should beInstanceOf<Literal.IntLiteral>()
        (varDecl.initializer as Literal.IntLiteral).value shouldBe 42
    }
    
    "should parse var declaration with inferred type" {
        val program = parser.parse("var name = \"Hello\"").getOrThrow()
        program.statements.size shouldBe 1
        
        val statement = program.statements[0]
        statement should beInstanceOf<VarDecl>()
        val varDecl = statement as VarDecl
        varDecl.name shouldBe "name"
        varDecl.type shouldBe null
        varDecl.initializer should beInstanceOf<Literal.StringLiteral>()
        (varDecl.initializer as Literal.StringLiteral).value shouldBe "Hello"
    }
    
    "should parse assignment statement" {
        val program = parser.parse("x = 10").getOrThrow()
        program.statements.size shouldBe 1
        
        val statement = program.statements[0]
        statement should beInstanceOf<Assignment>()
        val assignment = statement as Assignment
        assignment.variable shouldBe "x"
        assignment.value should beInstanceOf<Literal.IntLiteral>()
        (assignment.value as Literal.IntLiteral).value shouldBe 10
    }
    
    "should parse complex assignment with expression" {
        val program = parser.parse("counter = counter + 1").getOrThrow()
        program.statements.size shouldBe 1
        
        val statement = program.statements[0]
        statement should beInstanceOf<Assignment>()
        val assignment = statement as Assignment
        assignment.variable shouldBe "counter"
        assignment.value should beInstanceOf<BinaryOp>()
        val binOp = assignment.value as BinaryOp
        binOp.operator shouldBe BinaryOperator.PLUS
        binOp.left should beInstanceOf<Identifier>()
        (binOp.left as Identifier).name shouldBe "counter"
        binOp.right should beInstanceOf<Literal.IntLiteral>()
        (binOp.right as Literal.IntLiteral).value shouldBe 1
    }
})