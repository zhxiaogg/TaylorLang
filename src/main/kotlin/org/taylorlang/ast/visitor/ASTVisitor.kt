package org.taylorlang.ast.visitor

import org.taylorlang.ast.*

/**
 * Visitor interface for AST traversal using the Visitor pattern.
 * 
 * This interface provides a double dispatch mechanism for visiting all AST node types.
 * Implementations can focus on their specific processing logic rather than traversal concerns.
 * 
 * The generic type parameter R allows visitors to return values during traversal, 
 * making it suitable for various use cases:
 * - Collection/analysis operations (return collections)
 * - Transformation operations (return modified AST nodes)
 * - Validation operations (return boolean results)
 * - Generation operations (return Unit for side effects)
 * 
 * @param R The return type of visit methods
 */
interface ASTVisitor<R> {
    
    // =============================================================================
    // Core Program Structure
    // =============================================================================
    
    fun visitProgram(node: Program): R
    
    // =============================================================================
    // Statements
    // =============================================================================
    
    fun visitStatement(node: Statement): R
    fun visitFunctionDecl(node: FunctionDecl): R
    fun visitTypeDecl(node: TypeDecl): R
    fun visitValDecl(node: ValDecl): R
    fun visitVarDecl(node: VarDecl): R
    fun visitAssignment(node: Assignment): R
    fun visitReturnStatement(node: ReturnStatement): R
    
    // =============================================================================
    // Supporting Statement Nodes
    // =============================================================================
    
    fun visitParameter(node: Parameter): R
    fun visitFunctionBody(node: FunctionBody): R
    fun visitExpressionBody(node: FunctionBody.ExpressionBody): R
    fun visitBlockBody(node: FunctionBody.BlockBody): R
    fun visitUnionType(node: UnionType): R
    fun visitProductType(node: ProductType): R
    fun visitPositionedProductType(node: ProductType.Positioned): R
    fun visitNamedProductType(node: ProductType.Named): R
    fun visitNamedField(node: NamedField): R
    fun visitField(node: Field): R
    
    // =============================================================================
    // Expressions
    // =============================================================================
    
    fun visitExpression(node: Expression): R
    fun visitIdentifier(node: Identifier): R
    fun visitPropertyAccess(node: PropertyAccess): R
    fun visitFunctionCall(node: FunctionCall): R
    fun visitIndexAccess(node: IndexAccess): R
    fun visitUnaryOp(node: UnaryOp): R
    fun visitBinaryOp(node: BinaryOp): R
    fun visitMatchExpression(node: MatchExpression): R
    fun visitMatchCase(node: MatchCase): R
    fun visitLambdaExpression(node: LambdaExpression): R
    fun visitIfExpression(node: IfExpression): R
    fun visitWhileExpression(node: WhileExpression): R
    fun visitForExpression(node: ForExpression): R
    fun visitConstructorCall(node: ConstructorCall): R
    fun visitBlockExpression(node: BlockExpression): R
    
    // =============================================================================
    // Literals
    // =============================================================================
    
    fun visitLiteral(node: Literal): R
    fun visitIntLiteral(node: Literal.IntLiteral): R
    fun visitFloatLiteral(node: Literal.FloatLiteral): R
    fun visitStringLiteral(node: Literal.StringLiteral): R
    fun visitBooleanLiteral(node: Literal.BooleanLiteral): R
    fun visitTupleLiteral(node: Literal.TupleLiteral): R
    fun visitNullLiteral(node: Literal.NullLiteral): R
    
    // =============================================================================
    // Patterns
    // =============================================================================
    
    fun visitPattern(node: Pattern): R
    fun visitWildcardPattern(node: Pattern.WildcardPattern): R
    fun visitIdentifierPattern(node: Pattern.IdentifierPattern): R
    fun visitLiteralPattern(node: Pattern.LiteralPattern): R
    fun visitConstructorPattern(node: Pattern.ConstructorPattern): R
    fun visitGuardPattern(node: Pattern.GuardPattern): R
    
    // =============================================================================
    // Types
    // =============================================================================
    
    fun visitType(node: Type): R
    fun visitPrimitiveType(node: Type.PrimitiveType): R
    fun visitNamedType(node: Type.NamedType): R
    fun visitGenericType(node: Type.GenericType): R
    fun visitNullableType(node: Type.NullableType): R
    fun visitTupleType(node: Type.TupleType): R
    fun visitFunctionType(node: Type.FunctionType): R
    fun visitUnionTypeRef(node: Type.UnionType): R
    fun visitTypeVar(node: Type.TypeVar): R
}