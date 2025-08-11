package org.taylorlang.parser

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.taylorlang.ast.*
import kotlinx.collections.immutable.persistentListOf

class ListPatternParsingTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    
    describe("List Pattern Parsing") {
        
        it("should parse empty list pattern") {
            val input = "match x { case [] => 0 }"
            val result = parser.parse(input)
            
            result.isSuccess shouldBe true
            val program = result.getOrThrow()
            val matchExpr = ((program.statements[0] as Expression) as MatchExpression)
            val pattern = matchExpr.cases[0].pattern
            
            pattern.shouldBeInstanceOf<Pattern.ListPattern>()
            pattern.elements.size shouldBe 0
            pattern.restVariable shouldBe null
        }
        
        it("should parse single element list pattern") {
            val input = "match x { case [a] => a }"
            val result = parser.parse(input)
            
            result.isSuccess shouldBe true
            val program = result.getOrThrow()
            val matchExpr = ((program.statements[0] as Expression) as MatchExpression)
            val pattern = matchExpr.cases[0].pattern
            
            pattern.shouldBeInstanceOf<Pattern.ListPattern>()
            pattern.elements.size shouldBe 1
            pattern.elements[0].shouldBeInstanceOf<Pattern.IdentifierPattern>()
            (pattern.elements[0] as Pattern.IdentifierPattern).name shouldBe "a"
            pattern.restVariable shouldBe null
        }
        
        it("should parse fixed-length list pattern") {
            val input = "match x { case [a, b] => a + b }"
            val result = parser.parse(input)
            
            result.isSuccess shouldBe true
            val program = result.getOrThrow()
            val matchExpr = ((program.statements[0] as Expression) as MatchExpression)
            val pattern = matchExpr.cases[0].pattern
            
            pattern.shouldBeInstanceOf<Pattern.ListPattern>()
            pattern.elements.size shouldBe 2
            pattern.elements[0].shouldBeInstanceOf<Pattern.IdentifierPattern>()
            pattern.elements[1].shouldBeInstanceOf<Pattern.IdentifierPattern>()
            (pattern.elements[0] as Pattern.IdentifierPattern).name shouldBe "a"
            (pattern.elements[1] as Pattern.IdentifierPattern).name shouldBe "b"
            pattern.restVariable shouldBe null
        }
        
        it("should parse head/tail list pattern") {
            val input = "match x { case [first, ...rest] => first }"
            val result = parser.parse(input)
            
            result.isSuccess shouldBe true
            val program = result.getOrThrow()
            val matchExpr = ((program.statements[0] as Expression) as MatchExpression)
            val pattern = matchExpr.cases[0].pattern
            
            pattern.shouldBeInstanceOf<Pattern.ListPattern>()
            pattern.elements.size shouldBe 1
            pattern.elements[0].shouldBeInstanceOf<Pattern.IdentifierPattern>()
            (pattern.elements[0] as Pattern.IdentifierPattern).name shouldBe "first"
            pattern.restVariable shouldBe "rest"
        }
        
        it("should parse complex head/tail pattern") {
            val input = "match x { case [a, b, ...tail] => a + b }"
            val result = parser.parse(input)
            
            result.isSuccess shouldBe true
            val program = result.getOrThrow()
            val matchExpr = ((program.statements[0] as Expression) as MatchExpression)
            val pattern = matchExpr.cases[0].pattern
            
            pattern.shouldBeInstanceOf<Pattern.ListPattern>()
            pattern.elements.size shouldBe 2
            pattern.restVariable shouldBe "tail"
        }
    }
})