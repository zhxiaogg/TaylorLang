package org.taylorlang.ast.visitor

import org.taylorlang.ast.*

/**
 * Base visitor implementation that provides default traversal logic for all AST nodes.
 * 
 * This class implements the ASTVisitor interface with sensible defaults that traverse
 * child nodes recursively. Subclasses can override specific visit methods to customize
 * behavior while relying on default traversal for other nodes.
 * 
 * Key features:
 * - Pre-order traversal by default (visit parent before children)
 * - Configurable traversal behavior through protected methods
 * - Consistent error handling for unimplemented visit methods
 * - Support for short-circuiting traversals
 * 
 * Usage patterns:
 * - Analysis visitors: Return collections, counts, or boolean results
 * - Transformation visitors: Return modified AST nodes
 * - Validation visitors: Return error lists or validation results
 * - Side-effect visitors: Return Unit for operations like printing or code generation
 * 
 * @param R The return type of visit methods
 */
abstract class BaseASTVisitor<R> : ASTVisitor<R> {
    
    /**
     * Default result to return when no specific processing is needed.
     * Subclasses should override this to provide an appropriate default.
     */
    protected abstract fun defaultResult(): R
    
    /**
     * Combine multiple results from child nodes into a single result.
     * Override to customize how results are aggregated.
     */
    protected open fun combine(first: R, second: R): R = second
    
    /**
     * Combine multiple results from a list of child nodes.
     * By default, reduces using the combine method.
     */
    protected open fun combineResults(results: List<R>): R {
        return results.fold(defaultResult()) { acc, result -> combine(acc, result) }
    }
    
    // =============================================================================
    // Core Program Structure
    // =============================================================================
    
    override fun visitProgram(node: Program): R {
        val results = node.statements.map { it.accept(this) }
        return combineResults(results)
    }
    
    // =============================================================================
    // Statements
    // =============================================================================
    
    override fun visitStatement(node: Statement): R {
        return when (node) {
            is FunctionDecl -> visitFunctionDecl(node)
            is TypeDecl -> visitTypeDecl(node)
            is ValDecl -> visitValDecl(node)
            is VarDecl -> visitVarDecl(node)
            is Assignment -> visitAssignment(node)
            is ReturnStatement -> visitReturnStatement(node)
            is Expression -> visitExpression(node)
        }
    }
    
    override fun visitFunctionDecl(node: FunctionDecl): R {
        val paramResults = node.parameters.map { it.accept(this) }
        val returnTypeResult = node.returnType?.accept(this) ?: defaultResult()
        val bodyResult = node.body.accept(this)
        
        return combineResults(paramResults + returnTypeResult + bodyResult)
    }
    
    override fun visitTypeDecl(node: TypeDecl): R {
        return node.unionType.accept(this)
    }
    
    override fun visitValDecl(node: ValDecl): R {
        val typeResult = node.type?.accept(this) ?: defaultResult()
        val initResult = node.initializer.accept(this)
        return combine(typeResult, initResult)
    }
    
    override fun visitVarDecl(node: VarDecl): R {
        val typeResult = node.type?.accept(this) ?: defaultResult()
        val initResult = node.initializer.accept(this)
        return combine(typeResult, initResult)
    }
    
    override fun visitAssignment(node: Assignment): R {
        return node.value.accept(this)
    }
    
    override fun visitReturnStatement(node: ReturnStatement): R {
        return node.expression?.accept(this) ?: defaultResult()
    }
    
    // =============================================================================
    // Supporting Statement Nodes
    // =============================================================================
    
    override fun visitParameter(node: Parameter): R {
        return node.type?.accept(this) ?: defaultResult()
    }
    
    override fun visitFunctionBody(node: FunctionBody): R {
        return when (node) {
            is FunctionBody.ExpressionBody -> visitExpressionBody(node)
            is FunctionBody.BlockBody -> visitBlockBody(node)
        }
    }
    
    override fun visitExpressionBody(node: FunctionBody.ExpressionBody): R {
        return node.expression.accept(this)
    }
    
    override fun visitBlockBody(node: FunctionBody.BlockBody): R {
        val results = node.statements.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitUnionType(node: UnionType): R {
        val results = node.variants.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitProductType(node: ProductType): R {
        return when (node) {
            is ProductType.Positioned -> visitPositionedProductType(node)
            is ProductType.Named -> visitNamedProductType(node)
        }
    }
    
    override fun visitPositionedProductType(node: ProductType.Positioned): R {
        val results = node.types.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitNamedProductType(node: ProductType.Named): R {
        val results = node.fields.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitNamedField(node: NamedField): R {
        return node.type.accept(this)
    }
    
    override fun visitField(node: Field): R {
        return node.type.accept(this)
    }
    
    // =============================================================================
    // Expressions
    // =============================================================================
    
    override fun visitExpression(node: Expression): R {
        return when (node) {
            is Identifier -> visitIdentifier(node)
            is PropertyAccess -> visitPropertyAccess(node)
            is FunctionCall -> visitFunctionCall(node)
            is IndexAccess -> visitIndexAccess(node)
            is UnaryOp -> visitUnaryOp(node)
            is BinaryOp -> visitBinaryOp(node)
            is MatchExpression -> visitMatchExpression(node)
            is LambdaExpression -> visitLambdaExpression(node)
            is IfExpression -> visitIfExpression(node)
            is WhileExpression -> visitWhileExpression(node)
            is ForExpression -> visitForExpression(node)
            is ConstructorCall -> visitConstructorCall(node)
            is BlockExpression -> visitBlockExpression(node)
            is TryExpression -> visitTryExpression(node)
            is Literal -> visitLiteral(node)
        }
    }
    
    override fun visitIdentifier(node: Identifier): R {
        return defaultResult()
    }
    
    override fun visitPropertyAccess(node: PropertyAccess): R {
        return node.target.accept(this)
    }
    
    override fun visitFunctionCall(node: FunctionCall): R {
        val targetResult = node.target.accept(this)
        val argResults = node.arguments.map { it.accept(this) }
        return combineResults(listOf(targetResult) + argResults)
    }
    
    override fun visitIndexAccess(node: IndexAccess): R {
        val targetResult = node.target.accept(this)
        val indexResult = node.index.accept(this)
        return combine(targetResult, indexResult)
    }
    
    override fun visitUnaryOp(node: UnaryOp): R {
        return node.operand.accept(this)
    }
    
    override fun visitBinaryOp(node: BinaryOp): R {
        val leftResult = node.left.accept(this)
        val rightResult = node.right.accept(this)
        return combine(leftResult, rightResult)
    }
    
    override fun visitMatchExpression(node: MatchExpression): R {
        val targetResult = node.target.accept(this)
        val caseResults = node.cases.map { it.accept(this) }
        return combineResults(listOf(targetResult) + caseResults)
    }
    
    override fun visitMatchCase(node: MatchCase): R {
        val patternResult = node.pattern.accept(this)
        val expressionResult = node.expression.accept(this)
        return combine(patternResult, expressionResult)
    }
    
    override fun visitLambdaExpression(node: LambdaExpression): R {
        return node.body.accept(this)
    }
    
    override fun visitIfExpression(node: IfExpression): R {
        val conditionResult = node.condition.accept(this)
        val thenResult = node.thenExpression.accept(this)
        val elseResult = node.elseExpression?.accept(this) ?: defaultResult()
        return combineResults(listOf(conditionResult, thenResult, elseResult))
    }
    
    override fun visitWhileExpression(node: WhileExpression): R {
        val conditionResult = node.condition.accept(this)
        val bodyResult = node.body.accept(this)
        return combine(conditionResult, bodyResult)
    }
    
    override fun visitForExpression(node: ForExpression): R {
        val iterableResult = node.iterable.accept(this)
        val bodyResult = node.body.accept(this)
        return combine(iterableResult, bodyResult)
    }
    
    override fun visitConstructorCall(node: ConstructorCall): R {
        val results = node.arguments.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitBlockExpression(node: BlockExpression): R {
        val stmtResults = node.statements.map { it.accept(this) }
        val exprResult = node.expression?.accept(this) ?: defaultResult()
        return combineResults(stmtResults + exprResult)
    }
    
    override fun visitTryExpression(node: TryExpression): R {
        val exprResult = node.expression.accept(this)
        val catchResults = node.catchClauses.map { it.accept(this) }
        return combineResults(listOf(exprResult) + catchResults)
    }
    
    override fun visitCatchClause(node: CatchClause): R {
        val patternResult = node.pattern.accept(this)
        val guardResult = node.guardExpression?.accept(this) ?: defaultResult()
        val bodyResult = node.body.accept(this)
        return combineResults(listOf(patternResult, guardResult, bodyResult))
    }
    
    // =============================================================================
    // Literals
    // =============================================================================
    
    override fun visitLiteral(node: Literal): R {
        return when (node) {
            is Literal.IntLiteral -> visitIntLiteral(node)
            is Literal.FloatLiteral -> visitFloatLiteral(node)
            is Literal.StringLiteral -> visitStringLiteral(node)
            is Literal.BooleanLiteral -> visitBooleanLiteral(node)
            is Literal.TupleLiteral -> visitTupleLiteral(node)
            is Literal.ListLiteral -> visitListLiteral(node)
            is Literal.NullLiteral -> visitNullLiteral(node)
        }
    }
    
    override fun visitIntLiteral(node: Literal.IntLiteral): R {
        return defaultResult()
    }
    
    override fun visitFloatLiteral(node: Literal.FloatLiteral): R {
        return defaultResult()
    }
    
    override fun visitStringLiteral(node: Literal.StringLiteral): R {
        return defaultResult()
    }
    
    override fun visitBooleanLiteral(node: Literal.BooleanLiteral): R {
        return defaultResult()
    }
    
    override fun visitTupleLiteral(node: Literal.TupleLiteral): R {
        val results = node.elements.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitListLiteral(node: Literal.ListLiteral): R {
        val results = node.elements.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitNullLiteral(node: Literal.NullLiteral): R {
        return defaultResult()
    }
    
    // =============================================================================
    // Patterns
    // =============================================================================
    
    override fun visitPattern(node: Pattern): R {
        return when (node) {
            is Pattern.WildcardPattern -> visitWildcardPattern(node)
            is Pattern.IdentifierPattern -> visitIdentifierPattern(node)
            is Pattern.LiteralPattern -> visitLiteralPattern(node)
            is Pattern.ConstructorPattern -> visitConstructorPattern(node)
            is Pattern.GuardPattern -> visitGuardPattern(node)
        }
    }
    
    override fun visitWildcardPattern(node: Pattern.WildcardPattern): R {
        return defaultResult()
    }
    
    override fun visitIdentifierPattern(node: Pattern.IdentifierPattern): R {
        return defaultResult()
    }
    
    override fun visitLiteralPattern(node: Pattern.LiteralPattern): R {
        return node.literal.accept(this)
    }
    
    override fun visitConstructorPattern(node: Pattern.ConstructorPattern): R {
        val results = node.patterns.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitGuardPattern(node: Pattern.GuardPattern): R {
        val patternResult = node.pattern.accept(this)
        val guardResult = node.guard.accept(this)
        return combine(patternResult, guardResult)
    }
    
    // =============================================================================
    // Types
    // =============================================================================
    
    override fun visitType(node: Type): R {
        return when (node) {
            is Type.PrimitiveType -> visitPrimitiveType(node)
            is Type.NamedType -> visitNamedType(node)
            is Type.GenericType -> visitGenericType(node)
            is Type.NullableType -> visitNullableType(node)
            is Type.TupleType -> visitTupleType(node)
            is Type.FunctionType -> visitFunctionType(node)
            is Type.UnionType -> visitUnionTypeRef(node)
            is Type.TypeVar -> visitTypeVar(node)
        }
    }
    
    override fun visitPrimitiveType(node: Type.PrimitiveType): R {
        return defaultResult()
    }
    
    override fun visitNamedType(node: Type.NamedType): R {
        return defaultResult()
    }
    
    override fun visitGenericType(node: Type.GenericType): R {
        val results = node.arguments.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitNullableType(node: Type.NullableType): R {
        return node.baseType.accept(this)
    }
    
    override fun visitTupleType(node: Type.TupleType): R {
        val results = node.elementTypes.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitFunctionType(node: Type.FunctionType): R {
        val paramResults = node.parameterTypes.map { it.accept(this) }
        val returnResult = node.returnType.accept(this)
        return combineResults(paramResults + returnResult)
    }
    
    override fun visitUnionTypeRef(node: Type.UnionType): R {
        val results = node.typeArguments.map { it.accept(this) }
        return combineResults(results)
    }
    
    override fun visitTypeVar(node: Type.TypeVar): R {
        return defaultResult()
    }
}