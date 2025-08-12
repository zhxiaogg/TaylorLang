package org.taylorlang.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*

/**
 * Comprehensive test suite for try expression parsing functionality.
 * 
 * Tests all try syntax variations according to the grammar extensions:
 * - Simple try expressions: `try expression`
 * - Try with catch blocks: `try expression catch { case pattern => expr }`
 * - Try with block expressions: `try { ... } catch { ... }`
 */
class TryExpressionParsingTest : StringSpec({
    val parser = TaylorLangParser()

    "should parse simple try expression with identifier" {
        val source = "try someOperation()"
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        // Check try expression
        tryExpr.expression should beInstanceOf<FunctionCall>()
        val functionCall = tryExpr.expression as FunctionCall
        functionCall.target should beInstanceOf<Identifier>()
        (functionCall.target as Identifier).name shouldBe "someOperation"
        functionCall.arguments shouldBe persistentListOf<Expression>()
        
        // Check no catch clauses for simple try
        tryExpr.catchClauses.isEmpty() shouldBe true
    }

    "should parse simple try expression with literal" {
        val source = "try 42"
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        // Check try expression is integer literal
        tryExpr.expression should beInstanceOf<Literal.IntLiteral>()
        val literal = tryExpr.expression as Literal.IntLiteral
        literal.value shouldBe 42
        
        // Check no catch clauses
        tryExpr.catchClauses.isEmpty() shouldBe true
    }

    "should parse try expression with simple catch clause" {
        val source = """
            try database.findUser(id) catch {
                case DatabaseError(msg) => handleError(msg)
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        // Check try expression
        tryExpr.expression should beInstanceOf<FunctionCall>()
        val functionCall = tryExpr.expression as FunctionCall
        functionCall.target should beInstanceOf<PropertyAccess>()
        val propertyAccess = functionCall.target as PropertyAccess
        propertyAccess.target should beInstanceOf<Identifier>()
        (propertyAccess.target as Identifier).name shouldBe "database"
        propertyAccess.property shouldBe "findUser"
        
        // Check catch clauses
        tryExpr.catchClauses.size shouldBe 1
        val catchClause = tryExpr.catchClauses[0]
        
        // Check pattern
        catchClause.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        val pattern = catchClause.pattern as Pattern.ConstructorPattern
        pattern.constructor shouldBe "DatabaseError"
        pattern.patterns.size shouldBe 1
        pattern.patterns[0] should beInstanceOf<Pattern.IdentifierPattern>()
        (pattern.patterns[0] as Pattern.IdentifierPattern).name shouldBe "msg"
        
        // Check catch body
        catchClause.body should beInstanceOf<FunctionCall>()
        val catchBody = catchClause.body as FunctionCall
        catchBody.target should beInstanceOf<Identifier>()
        (catchBody.target as Identifier).name shouldBe "handleError"
    }

    "should parse try expression with multiple catch clauses" {
        val source = """
            try processData() catch {
                case DatabaseError(msg) => handleDatabaseError(msg)
                case NetworkError(code) => handleNetworkError(code)
                case ValidationError(field, reason) => handleValidationError(field, reason)
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        // Check we have multiple catch clauses
        tryExpr.catchClauses.size shouldBe 3
        
        // Verify first catch clause
        val firstCatch = tryExpr.catchClauses[0]
        firstCatch.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        val firstPattern = firstCatch.pattern as Pattern.ConstructorPattern
        firstPattern.constructor shouldBe "DatabaseError"
        
        // Verify second catch clause
        val secondCatch = tryExpr.catchClauses[1]
        secondCatch.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        val secondPattern = secondCatch.pattern as Pattern.ConstructorPattern
        secondPattern.constructor shouldBe "NetworkError"
        
        // Verify third catch clause
        val thirdCatch = tryExpr.catchClauses[2]
        thirdCatch.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        val thirdPattern = thirdCatch.pattern as Pattern.ConstructorPattern
        thirdPattern.constructor shouldBe "ValidationError"
        thirdPattern.patterns.size shouldBe 2 // field, reason
    }

    "should parse try with block expression" {
        val source = """
            try {
                val user = findUser(id);
                val profile = user.getProfile();
                profile.format()
            } catch {
                case UserNotFound(_) => "User not found"
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        // Check try expression is a block
        tryExpr.expression should beInstanceOf<BlockExpression>()
        val blockExpr = tryExpr.expression as BlockExpression
        
        // Check block has statements
        blockExpr.statements.size shouldBe 2 // val user, val profile
        blockExpr.expression shouldNotBe null // profile.format()
        
        // Check catch clause
        tryExpr.catchClauses.size shouldBe 1
        val catchClause = tryExpr.catchClauses[0]
        catchClause.pattern should beInstanceOf<Pattern.ConstructorPattern>()
        val pattern = catchClause.pattern as Pattern.ConstructorPattern
        pattern.constructor shouldBe "UserNotFound"
        
        // Check wildcard pattern in constructor
        pattern.patterns[0] should beInstanceOf<Pattern.WildcardPattern>()
    }

    "should parse try expression with literal pattern in catch" {
        val source = """
            try getValue() catch {
                case 0 => "zero"
                case 1 => "one"
                case _ => "other"
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        tryExpr.catchClauses.size shouldBe 3
        
        // Check first catch has literal pattern
        val firstCatch = tryExpr.catchClauses[0]
        firstCatch.pattern should beInstanceOf<Pattern.LiteralPattern>()
        val literalPattern = firstCatch.pattern as Pattern.LiteralPattern
        literalPattern.literal should beInstanceOf<Literal.IntLiteral>()
        (literalPattern.literal as Literal.IntLiteral).value shouldBe 0
        
        // Check third catch has wildcard pattern
        val thirdCatch = tryExpr.catchClauses[2]
        thirdCatch.pattern should beInstanceOf<Pattern.WildcardPattern>()
    }

    "should parse nested try expressions" {
        val source = """
            try {
                val outer = try inner();
                outer + 1
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val outerTry = result as TryExpression
        
        // Check outer try has block expression
        outerTry.expression should beInstanceOf<BlockExpression>()
        val blockExpr = outerTry.expression as BlockExpression
        
        // Check first statement is val declaration
        blockExpr.statements.size shouldBe 1
        val valDecl = blockExpr.statements[0]
        valDecl should beInstanceOf<ValDecl>()
        val valDeclTyped = valDecl as ValDecl
        
        // Check initializer is inner try expression
        valDeclTyped.initializer should beInstanceOf<TryExpression>()
        val innerTry = valDeclTyped.initializer as TryExpression
        innerTry.expression should beInstanceOf<FunctionCall>()
    }

    "should parse try expression in function call arguments" {
        val source = "processResult(try getResult(), fallback)"
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<FunctionCall>()
        val functionCall = result as FunctionCall
        
        functionCall.arguments.size shouldBe 2
        
        // First argument should be try expression
        val firstArg = functionCall.arguments[0]
        firstArg should beInstanceOf<TryExpression>()
        val tryExpr = firstArg as TryExpression
        
        tryExpr.expression should beInstanceOf<FunctionCall>()
        val innerCall = tryExpr.expression as FunctionCall
        innerCall.target should beInstanceOf<Identifier>()
        (innerCall.target as Identifier).name shouldBe "getResult"
    }

    "should parse try expression with guard patterns in catch" {
        val source = """
            try getValue() catch {
                case x if x > 0 => "positive"
                case x => "non-positive"
            }
        """.trimIndent()
        
        val result = parseExpression(parser, source)
        
        result should beInstanceOf<TryExpression>()
        val tryExpr = result as TryExpression
        
        tryExpr.catchClauses.size shouldBe 2
        
        // First catch should have guard pattern
        val firstCatch = tryExpr.catchClauses[0]
        firstCatch.pattern should beInstanceOf<Pattern.GuardPattern>()
        val guardPattern = firstCatch.pattern as Pattern.GuardPattern
        
        guardPattern.pattern should beInstanceOf<Pattern.IdentifierPattern>()
        (guardPattern.pattern as Pattern.IdentifierPattern).name shouldBe "x"
        
        // Guard expression should be binary comparison
        guardPattern.guard should beInstanceOf<BinaryOp>()
    }
})

// =============================================================================
// Helper Methods
// =============================================================================

private fun parseExpression(parser: TaylorLangParser, source: String): Expression {
    val program = parser.parse(source).getOrThrow()
    program.statements.isEmpty() shouldBe false
    val statement = program.statements[0]
    statement should beInstanceOf<Expression>()
    return statement as Expression
}