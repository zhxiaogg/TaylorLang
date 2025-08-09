package org.taylorlang.parser

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*
import org.taylorlang.grammar.TaylorLangParser
import org.taylorlang.grammar.TaylorLangBaseVisitor

/**
 * Builds AST from ANTLR parse tree using the visitor pattern
 */
class ASTBuilder : TaylorLangBaseVisitor<ASTNode>() {

    override fun visitProgram(ctx: TaylorLangParser.ProgramContext): Program {
        val statements = ctx.statement()
            .map { visit(it) as Statement }
            .toPersistentList()
        
        return Program(
            statements = statements,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Statements
    // =============================================================================

    override fun visitFunctionDecl(ctx: TaylorLangParser.FunctionDeclContext): FunctionDecl {
        val name = ctx.IDENTIFIER().text
        val typeParams = ctx.typeParams()?.typeParam()
            ?.map { it.IDENTIFIER().text }
            ?.toPersistentList()
            ?: persistentListOf()
        
        val parameters = ctx.paramList()?.param()
            ?.map { paramCtx ->
                Parameter(
                    name = paramCtx.IDENTIFIER().text,
                    type = paramCtx.type()?.let { visit(it) as Type },
                    sourceLocation = paramCtx.toSourceLocation()
                )
            }
            ?.toPersistentList()
            ?: persistentListOf()
        
        val returnType = ctx.type()?.let { visit(it) as Type }
        val body = visit(ctx.functionBody()) as FunctionBody
        
        return FunctionDecl(
            name = name,
            typeParams = typeParams,
            parameters = parameters,
            returnType = returnType,
            body = body,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitFunctionBody(ctx: TaylorLangParser.FunctionBodyContext): FunctionBody {
        return when {
            ctx.expression() != null -> {
                FunctionBody.ExpressionBody(
                    expression = visit(ctx.expression()) as Expression,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            else -> {
                val statements = ctx.statement()
                    .map { visit(it) as Statement }
                    .toPersistentList()
                
                FunctionBody.BlockBody(
                    statements = statements,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
        }
    }

    override fun visitTypeDecl(ctx: TaylorLangParser.TypeDeclContext): TypeDecl {
        val name = ctx.IDENTIFIER().text
        val typeParams = ctx.typeParams()?.typeParam()
            ?.map { it.IDENTIFIER().text }
            ?.toPersistentList()
            ?: persistentListOf()
        
        val unionType = visit(ctx.unionType()) as UnionType
        
        return TypeDecl(
            name = name,
            typeParams = typeParams,
            unionType = unionType,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitUnionType(ctx: TaylorLangParser.UnionTypeContext): UnionType {
        val variants = ctx.productType()
            .map { visit(it) as ProductType }
            .toPersistentList()
        
        return UnionType(
            variants = variants,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitProductType(ctx: TaylorLangParser.ProductTypeContext): ProductType {
        val name = ctx.IDENTIFIER().text
        val fields = ctx.fieldList()?.field()
            ?.map { fieldCtx ->
                Field(
                    name = fieldCtx.IDENTIFIER().text,
                    type = visit(fieldCtx.type()) as Type,
                    sourceLocation = fieldCtx.toSourceLocation()
                )
            }
            ?.toPersistentList()
            ?: persistentListOf()
        
        return ProductType(
            name = name,
            fields = fields,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitValDecl(ctx: TaylorLangParser.ValDeclContext): ValDecl {
        val name = ctx.IDENTIFIER().text
        val type = ctx.type()?.let { visit(it) as Type }
        val initializer = visit(ctx.expression()) as Expression
        
        return ValDecl(
            name = name,
            type = type,
            initializer = initializer,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Expressions
    // =============================================================================

    override fun visitExpression(ctx: TaylorLangParser.ExpressionContext): Expression {
        return when {
            ctx.primary() != null -> visit(ctx.primary()) as Expression
            
            ctx.getChild(1)?.text == "." -> {
                PropertyAccess(
                    target = visit(ctx.expression(0)) as Expression,
                    property = ctx.IDENTIFIER().text,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.getChild(1)?.text == "(" -> {
                val target = visit(ctx.expression(0)) as Expression
                val arguments = ctx.argList()?.expression()
                    ?.map { visit(it) as Expression }
                    ?.toPersistentList()
                    ?: persistentListOf()
                
                FunctionCall(
                    target = target,
                    arguments = arguments,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.getChild(1)?.text == "[" -> {
                IndexAccess(
                    target = visit(ctx.expression(0)) as Expression,
                    index = visit(ctx.expression(1)) as Expression,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.getChild(0)?.text in listOf("-", "!") -> {
                val operator = when (ctx.getChild(0)?.text) {
                    "-" -> UnaryOperator.MINUS
                    "!" -> UnaryOperator.NOT
                    else -> throw IllegalStateException("Unknown unary operator")
                }
                
                UnaryOp(
                    operator = operator,
                    operand = visit(ctx.expression(0)) as Expression,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.expression().size == 2 -> {
                val operatorText = ctx.children
                    .drop(1)
                    .dropLast(1)
                    .joinToString("") { it.text }
                
                val operator = when (operatorText) {
                    "+" -> BinaryOperator.PLUS
                    "-" -> BinaryOperator.MINUS
                    "*" -> BinaryOperator.MULTIPLY
                    "/" -> BinaryOperator.DIVIDE
                    "%" -> BinaryOperator.MODULO
                    "<" -> BinaryOperator.LESS_THAN
                    "<=" -> BinaryOperator.LESS_EQUAL
                    ">" -> BinaryOperator.GREATER_THAN
                    ">=" -> BinaryOperator.GREATER_EQUAL
                    "==" -> BinaryOperator.EQUAL
                    "!=" -> BinaryOperator.NOT_EQUAL
                    "&&" -> BinaryOperator.AND
                    "||" -> BinaryOperator.OR
                    "?:" -> BinaryOperator.NULL_COALESCING
                    else -> throw IllegalStateException("Unknown binary operator: $operatorText")
                }
                
                BinaryOp(
                    left = visit(ctx.expression(0)) as Expression,
                    operator = operator,
                    right = visit(ctx.expression(1)) as Expression,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.matchExpr() != null -> visit(ctx.matchExpr()) as Expression
            ctx.lambdaExpr() != null -> visit(ctx.lambdaExpr()) as Expression
            
            else -> throw IllegalStateException("Unknown expression type: ${ctx.text}")
        }
    }

    override fun visitPrimary(ctx: TaylorLangParser.PrimaryContext): Expression {
        return when {
            ctx.IDENTIFIER() != null -> {
                Identifier(
                    name = ctx.IDENTIFIER().text,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.literal() != null -> visit(ctx.literal()) as Literal
            ctx.expression() != null -> visit(ctx.expression()) as Expression
            ctx.constructorCall() != null -> visit(ctx.constructorCall()) as Expression
            else -> throw IllegalStateException("Unknown primary expression")
        }
    }

    override fun visitConstructorCall(ctx: TaylorLangParser.ConstructorCallContext): ConstructorCall {
        val constructor = ctx.IDENTIFIER().text
        val arguments = ctx.argList()?.expression()
            ?.map { visit(it) as Expression }
            ?.toPersistentList()
            ?: persistentListOf()
        
        return ConstructorCall(
            constructor = constructor,
            arguments = arguments,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Literals
    // =============================================================================

    override fun visitLiteral(ctx: TaylorLangParser.LiteralContext): Literal {
        return when {
            ctx.IntLiteral() != null -> {
                Literal.IntLiteral(
                    value = ctx.IntLiteral().text.toInt(),
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.FloatLiteral() != null -> {
                Literal.FloatLiteral(
                    value = ctx.FloatLiteral().text.toDouble(),
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.StringLiteral() != null -> {
                // Remove quotes and handle escape sequences
                val text = ctx.StringLiteral().text
                val unquoted = text.substring(1, text.length - 1)
                val unescaped = unquoted.replace("\\\"", "\"").replace("\\\\", "\\")
                
                Literal.StringLiteral(
                    value = unescaped,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.BooleanLiteral() != null -> {
                Literal.BooleanLiteral(
                    value = ctx.BooleanLiteral().text.toBoolean(),
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.listLiteral() != null -> visit(ctx.listLiteral()) as Literal.ListLiteral
            ctx.mapLiteral() != null -> visit(ctx.mapLiteral()) as Literal.MapLiteral
            ctx.tupleLiteral() != null -> visit(ctx.tupleLiteral()) as Literal.TupleLiteral
            ctx.text == "null" -> Literal.NullLiteral
            else -> throw IllegalStateException("Unknown literal type")
        }
    }

    override fun visitListLiteral(ctx: TaylorLangParser.ListLiteralContext): Literal.ListLiteral {
        val elements = ctx.expression()
            ?.map { visit(it) as Expression }
            ?.toPersistentList()
            ?: persistentListOf()
        
        return Literal.ListLiteral(
            elements = elements,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitMapLiteral(ctx: TaylorLangParser.MapLiteralContext): Literal.MapLiteral {
        val entries = ctx.mapEntry()
            ?.map { entryCtx ->
                MapEntry(
                    key = visit(entryCtx.expression(0)) as Expression,
                    value = visit(entryCtx.expression(1)) as Expression,
                    sourceLocation = entryCtx.toSourceLocation()
                )
            }
            ?.toPersistentList()
            ?: persistentListOf()
        
        return Literal.MapLiteral(
            entries = entries,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitTupleLiteral(ctx: TaylorLangParser.TupleLiteralContext): Literal.TupleLiteral {
        val elements = ctx.expression()
            .map { visit(it) as Expression }
            .toPersistentList()
        
        return Literal.TupleLiteral(
            elements = elements,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Pattern Matching
    // =============================================================================

    override fun visitMatchExpr(ctx: TaylorLangParser.MatchExprContext): MatchExpression {
        val target = visit(ctx.expression()) as Expression
        val cases = ctx.matchCase()
            .map { caseCtx ->
                MatchCase(
                    pattern = visit(caseCtx.pattern()) as Pattern,
                    expression = visit(caseCtx.expression()) as Expression,
                    sourceLocation = caseCtx.toSourceLocation()
                )
            }
            .toPersistentList()
        
        return MatchExpression(
            target = target,
            cases = cases,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    override fun visitPattern(ctx: TaylorLangParser.PatternContext): Pattern {
        return when {
            ctx.text == "_" -> Pattern.WildcardPattern
            ctx.IDENTIFIER() != null -> {
                Pattern.IdentifierPattern(
                    name = ctx.IDENTIFIER().text,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.literal() != null -> {
                Pattern.LiteralPattern(
                    literal = visit(ctx.literal()) as Literal,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            ctx.constructorPattern() != null -> visit(ctx.constructorPattern()) as Pattern
            ctx.expression() != null -> {
                Pattern.GuardPattern(
                    pattern = visit(ctx.pattern()) as Pattern,
                    guard = visit(ctx.expression()) as Expression,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            else -> throw IllegalStateException("Unknown pattern type")
        }
    }

    override fun visitConstructorPattern(ctx: TaylorLangParser.ConstructorPatternContext): Pattern.ConstructorPattern {
        val constructor = ctx.IDENTIFIER().text
        val patterns = ctx.pattern()
            ?.map { visit(it) as Pattern }
            ?.toPersistentList()
            ?: persistentListOf()
        
        return Pattern.ConstructorPattern(
            constructor = constructor,
            patterns = patterns,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Lambda Expressions
    // =============================================================================

    override fun visitLambdaExpr(ctx: TaylorLangParser.LambdaExprContext): LambdaExpression {
        val parameters = when {
            ctx.IDENTIFIER().size == 1 -> persistentListOf(ctx.IDENTIFIER(0).text)
            else -> ctx.IDENTIFIER().map { it.text }.toPersistentList()
        }
        
        val body = visit(ctx.expression()) as Expression
        
        return LambdaExpression(
            parameters = parameters,
            body = body,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Types
    // =============================================================================

    override fun visitType(ctx: TaylorLangParser.TypeContext): Type {
        return when {
            ctx.primitiveType() != null -> visit(ctx.primitiveType()) as Type
            
            ctx.IDENTIFIER() != null && ctx.typeArgs() != null -> {
                val arguments = ctx.typeArgs().type()
                    .map { visit(it) as Type }
                    .toPersistentList()
                
                Type.GenericType(
                    name = ctx.IDENTIFIER().text,
                    arguments = arguments,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.IDENTIFIER() != null -> {
                Type.NamedType(
                    name = ctx.IDENTIFIER().text,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.getChild(ctx.childCount - 1)?.text == "?" -> {
                Type.NullableType(
                    baseType = visit(ctx.type(0)) as Type,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            ctx.getChild(0)?.text == "(" -> {
                val elementTypes = ctx.type()
                    .map { visit(it) as Type }
                    .toPersistentList()
                
                Type.TupleType(
                    elementTypes = elementTypes,
                    sourceLocation = ctx.toSourceLocation()
                )
            }
            
            else -> throw IllegalStateException("Unknown type")
        }
    }

    override fun visitPrimitiveType(ctx: TaylorLangParser.PrimitiveTypeContext): Type.PrimitiveType {
        return Type.PrimitiveType(
            name = ctx.text,
            sourceLocation = ctx.toSourceLocation()
        )
    }

    // =============================================================================
    // Utility functions
    // =============================================================================

    private fun org.antlr.v4.runtime.ParserRuleContext.toSourceLocation(): SourceLocation {
        return SourceLocation(
            line = this.start.line,
            column = this.start.charPositionInLine
        )
    }
}