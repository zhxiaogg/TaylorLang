package org.taylorlang.ast

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.visitor.ASTVisitor

/**
 * Base interface for all AST nodes
 */
sealed interface ASTNode {
    val sourceLocation: SourceLocation?
    
    /**
     * Accept method for visitor pattern.
     * Enables double dispatch for type-safe AST traversal.
     */
    fun <R> accept(visitor: ASTVisitor<R>): R
}

/**
 * Source location information for error reporting
 */
data class SourceLocation(
    val line: Int,
    val column: Int,
    val file: String? = null
)

// =============================================================================
// Statements
// =============================================================================

sealed interface Statement : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitStatement(this)
}

data class FunctionDecl(
    val name: String,
    val typeParams: PersistentList<String> = persistentListOf(),
    val parameters: PersistentList<Parameter> = persistentListOf(),
    val returnType: Type? = null,
    val body: FunctionBody,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitFunctionDecl(this)
}

data class Parameter(
    val name: String,
    val type: Type? = null,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitParameter(this)
}

sealed class FunctionBody : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitFunctionBody(this)
    
    data class ExpressionBody(
        val expression: Expression,
        override val sourceLocation: SourceLocation? = null
    ) : FunctionBody() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitExpressionBody(this)
    }
    
    data class BlockBody(
        val statements: PersistentList<Statement>,
        override val sourceLocation: SourceLocation? = null
    ) : FunctionBody() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitBlockBody(this)
    }
}

data class TypeDecl(
    val name: String,
    val typeParams: PersistentList<String> = persistentListOf(),
    val unionType: UnionType,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitTypeDecl(this)
}

data class UnionType(
    val variants: PersistentList<ProductType>,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitUnionType(this)
}

sealed class ProductType : ASTNode {
    abstract val name: String
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitProductType(this)
    
    data class Positioned(
        override val name: String,
        val types: PersistentList<Type> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : ProductType() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPositionedProductType(this)
    }
    
    data class Named(
        override val name: String,
        val fields: PersistentList<NamedField> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : ProductType() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitNamedProductType(this)
    }
}

data class NamedField(
    val name: String,
    val type: Type,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitNamedField(this)
}

data class Field(
    val name: String,
    val type: Type,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitField(this)
}

data class ValDecl(
    val name: String,
    val type: Type? = null,
    val initializer: Expression,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitValDecl(this)
}

data class VarDecl(
    val name: String,
    val type: Type? = null,
    val initializer: Expression,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitVarDecl(this)
}

data class Assignment(
    val variable: String,
    val value: Expression,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitAssignment(this)
}

data class ReturnStatement(
    val expression: Expression?,
    override val sourceLocation: SourceLocation? = null
) : Statement {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitReturnStatement(this)
}

// =============================================================================
// Expressions
// =============================================================================

sealed interface Expression : Statement {
    // Computed type after type checking
    val computedType: Type?
        get() = null
        
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitExpression(this)
}

data class Identifier(
    val name: String,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitIdentifier(this)
}

data class PropertyAccess(
    val target: Expression,
    val property: String,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPropertyAccess(this)
}

data class FunctionCall(
    val target: Expression,
    val arguments: PersistentList<Expression> = persistentListOf(),
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitFunctionCall(this)
}

data class IndexAccess(
    val target: Expression,
    val index: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitIndexAccess(this)
}

data class UnaryOp(
    val operator: UnaryOperator,
    val operand: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitUnaryOp(this)
}

enum class UnaryOperator { MINUS, NOT }

data class BinaryOp(
    val left: Expression,
    val operator: BinaryOperator,
    val right: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitBinaryOp(this)
}

enum class BinaryOperator {
    // Arithmetic
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    // Comparison
    LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL, EQUAL, NOT_EQUAL,
    // Logical
    AND, OR,
    // Null coalescing
    NULL_COALESCING
}

data class MatchExpression(
    val target: Expression,
    val cases: PersistentList<MatchCase>,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitMatchExpression(this)
}

data class MatchCase(
    val pattern: Pattern,
    val expression: Expression,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitMatchCase(this)
}

data class LambdaExpression(
    val parameters: PersistentList<String>,
    val body: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitLambdaExpression(this)
}

data class IfExpression(
    val condition: Expression,
    val thenExpression: Expression,
    val elseExpression: Expression?,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitIfExpression(this)
}

data class ForExpression(
    val variable: String,
    val iterable: Expression,
    val body: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitForExpression(this)
}

data class WhileExpression(
    val condition: Expression,
    val body: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitWhileExpression(this)
}

data class ConstructorCall(
    val constructor: String,
    val arguments: PersistentList<Expression> = persistentListOf(),
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitConstructorCall(this)
}

data class BlockExpression(
    val statements: PersistentList<Statement>,
    val expression: Expression?,
    override val sourceLocation: SourceLocation? = null
) : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitBlockExpression(this)
}

// =============================================================================
// Literals
// =============================================================================

sealed class Literal : Expression {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitLiteral(this)
    
    data class IntLiteral(
        val value: Int,
        override val sourceLocation: SourceLocation? = null
    ) : Literal() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitIntLiteral(this)
    }
    
    data class FloatLiteral(
        val value: Double,
        override val sourceLocation: SourceLocation? = null
    ) : Literal() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitFloatLiteral(this)
    }
    
    data class StringLiteral(
        val value: String,
        override val sourceLocation: SourceLocation? = null
    ) : Literal() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitStringLiteral(this)
    }
    
    data class BooleanLiteral(
        val value: Boolean,
        override val sourceLocation: SourceLocation? = null
    ) : Literal() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitBooleanLiteral(this)
    }
    
    data class TupleLiteral(
        val elements: PersistentList<Expression>,
        override val sourceLocation: SourceLocation? = null
    ) : Literal() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitTupleLiteral(this)
    }
    
    data object NullLiteral : Literal() {
        override val sourceLocation: SourceLocation? = null
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitNullLiteral(this)
    }
}


// =============================================================================
// Patterns
// =============================================================================

sealed class Pattern : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPattern(this)
    
    data object WildcardPattern : Pattern() {
        override val sourceLocation: SourceLocation? = null
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitWildcardPattern(this)
    }
    
    data class IdentifierPattern(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitIdentifierPattern(this)
    }
    
    data class LiteralPattern(
        val literal: Literal,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitLiteralPattern(this)
    }
    
    data class ConstructorPattern(
        val constructor: String,
        val patterns: PersistentList<Pattern> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : Pattern() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitConstructorPattern(this)
    }
    
    data class GuardPattern(
        val pattern: Pattern,
        val guard: Expression,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitGuardPattern(this)
    }
}

// =============================================================================
// Types
// =============================================================================

sealed class Type : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitType(this)
    
    data class PrimitiveType(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPrimitiveType(this)
    }
    
    data class NamedType(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitNamedType(this)
    }
    
    data class GenericType(
        val name: String,
        val arguments: PersistentList<Type>,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitGenericType(this)
    }
    
    data class NullableType(
        val baseType: Type,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitNullableType(this)
    }
    
    data class TupleType(
        val elementTypes: PersistentList<Type>,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitTupleType(this)
    }
    
    data class FunctionType(
        val parameterTypes: PersistentList<Type>,
        val returnType: Type,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitFunctionType(this)
    }
    
    data class UnionType(
        val name: String,
        val typeArguments: PersistentList<Type> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitUnionTypeRef(this)
    }
    
    /**
     * Type variable for type inference.
     * Represents an unknown type that will be resolved during inference.
     */
    data class TypeVar(
        val id: String,
        override val sourceLocation: SourceLocation? = null
    ) : Type() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitTypeVar(this)
    }
}

// =============================================================================
// Program
// =============================================================================

data class Program(
    val statements: PersistentList<Statement>,
    override val sourceLocation: SourceLocation? = null
) : ASTNode {
    override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitProgram(this)
}