package org.taylorlang.ast

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Base interface for all AST nodes
 */
sealed interface ASTNode {
    val sourceLocation: SourceLocation?
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

sealed interface Statement : ASTNode

data class FunctionDecl(
    val name: String,
    val typeParams: PersistentList<String> = persistentListOf(),
    val parameters: PersistentList<Parameter> = persistentListOf(),
    val returnType: Type? = null,
    val body: FunctionBody,
    override val sourceLocation: SourceLocation? = null
) : Statement

data class Parameter(
    val name: String,
    val type: Type? = null,
    override val sourceLocation: SourceLocation? = null
) : ASTNode

sealed class FunctionBody : ASTNode {
    data class ExpressionBody(
        val expression: Expression,
        override val sourceLocation: SourceLocation? = null
    ) : FunctionBody()
    
    data class BlockBody(
        val statements: PersistentList<Statement>,
        override val sourceLocation: SourceLocation? = null
    ) : FunctionBody()
}

data class TypeDecl(
    val name: String,
    val typeParams: PersistentList<String> = persistentListOf(),
    val unionType: UnionType,
    override val sourceLocation: SourceLocation? = null
) : Statement

data class UnionType(
    val variants: PersistentList<ProductType>,
    override val sourceLocation: SourceLocation? = null
) : ASTNode

sealed class ProductType : ASTNode {
    abstract val name: String
    
    data class Positioned(
        override val name: String,
        val types: PersistentList<Type> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : ProductType()
    
    data class Named(
        override val name: String,
        val fields: PersistentList<NamedField> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : ProductType()
}

data class NamedField(
    val name: String,
    val type: Type,
    override val sourceLocation: SourceLocation? = null
) : ASTNode

data class Field(
    val name: String,
    val type: Type,
    override val sourceLocation: SourceLocation? = null
) : ASTNode

data class ValDecl(
    val name: String,
    val type: Type? = null,
    val initializer: Expression,
    override val sourceLocation: SourceLocation? = null
) : Statement

// =============================================================================
// Expressions
// =============================================================================

sealed interface Expression : Statement {
    // Computed type after type checking
    val computedType: Type?
        get() = null
}

data class Identifier(
    val name: String,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class PropertyAccess(
    val target: Expression,
    val property: String,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class FunctionCall(
    val target: Expression,
    val arguments: PersistentList<Expression> = persistentListOf(),
    override val sourceLocation: SourceLocation? = null
) : Expression

data class IndexAccess(
    val target: Expression,
    val index: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class UnaryOp(
    val operator: UnaryOperator,
    val operand: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression

enum class UnaryOperator { MINUS, NOT }

data class BinaryOp(
    val left: Expression,
    val operator: BinaryOperator,
    val right: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression

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
) : Expression

data class MatchCase(
    val pattern: Pattern,
    val expression: Expression,
    override val sourceLocation: SourceLocation? = null
) : ASTNode

data class LambdaExpression(
    val parameters: PersistentList<String>,
    val body: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class IfExpression(
    val condition: Expression,
    val thenExpression: Expression,
    val elseExpression: Expression?,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class ForExpression(
    val variable: String,
    val iterable: Expression,
    val body: Expression,
    override val sourceLocation: SourceLocation? = null
) : Expression

data class ConstructorCall(
    val constructor: String,
    val arguments: PersistentList<Expression> = persistentListOf(),
    override val sourceLocation: SourceLocation? = null
) : Expression

data class BlockExpression(
    val statements: PersistentList<Statement>,
    val expression: Expression?,
    override val sourceLocation: SourceLocation? = null
) : Expression

// =============================================================================
// Literals
// =============================================================================

sealed class Literal : Expression {
    data class IntLiteral(
        val value: Int,
        override val sourceLocation: SourceLocation? = null
    ) : Literal()
    
    data class FloatLiteral(
        val value: Double,
        override val sourceLocation: SourceLocation? = null
    ) : Literal()
    
    data class StringLiteral(
        val value: String,
        override val sourceLocation: SourceLocation? = null
    ) : Literal()
    
    data class BooleanLiteral(
        val value: Boolean,
        override val sourceLocation: SourceLocation? = null
    ) : Literal()
    
    data class TupleLiteral(
        val elements: PersistentList<Expression>,
        override val sourceLocation: SourceLocation? = null
    ) : Literal()
    
    data object NullLiteral : Literal() {
        override val sourceLocation: SourceLocation? = null
    }
}


// =============================================================================
// Patterns
// =============================================================================

sealed class Pattern : ASTNode {
    data object WildcardPattern : Pattern() {
        override val sourceLocation: SourceLocation? = null
    }
    
    data class IdentifierPattern(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern()
    
    data class LiteralPattern(
        val literal: Literal,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern()
    
    data class ConstructorPattern(
        val constructor: String,
        val patterns: PersistentList<Pattern> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : Pattern()
    
    data class GuardPattern(
        val pattern: Pattern,
        val guard: Expression,
        override val sourceLocation: SourceLocation? = null
    ) : Pattern()
}

// =============================================================================
// Types
// =============================================================================

sealed class Type : ASTNode {
    data class PrimitiveType(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class NamedType(
        val name: String,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class GenericType(
        val name: String,
        val arguments: PersistentList<Type>,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class NullableType(
        val baseType: Type,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class TupleType(
        val elementTypes: PersistentList<Type>,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class FunctionType(
        val parameterTypes: PersistentList<Type>,
        val returnType: Type,
        override val sourceLocation: SourceLocation? = null
    ) : Type()
    
    data class UnionType(
        val name: String,
        val typeArguments: PersistentList<Type> = persistentListOf(),
        override val sourceLocation: SourceLocation? = null
    ) : Type()
}

// =============================================================================
// Program
// =============================================================================

data class Program(
    val statements: PersistentList<Statement>,
    override val sourceLocation: SourceLocation? = null
) : ASTNode