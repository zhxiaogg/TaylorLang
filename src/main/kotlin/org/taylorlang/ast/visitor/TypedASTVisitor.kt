package org.taylorlang.ast.visitor

import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Visitor interface for typed AST nodes, extending the base ASTVisitor pattern.
 * 
 * This interface provides visitor methods that work with TypedExpression wrappers,
 * making it ideal for code generation phases that need both AST structure and type information.
 * 
 * Key features:
 * - Works with TypedExpression wrappers from type checker
 * - Maintains type information during traversal
 * - Supports all AST node types through delegation to underlying AST
 * - Enables clean visitor-based code generation
 * 
 * @param R The return type of visit methods
 */
interface TypedASTVisitor<R> : ASTVisitor<R> {
    
    /**
     * Visit a typed expression, providing both the expression and its inferred type.
     * 
     * This is the primary entry point for visiting typed expressions. The default
     * implementation delegates to the underlying AST node's accept method, but
     * subclasses can override to access type information.
     */
    fun visitTypedExpression(typedExpr: TypedExpression): R {
        return typedExpr.expression.accept(this)
    }
    
    /**
     * Visit a typed statement, providing both the statement and any associated type information.
     * 
     * This method handles typed statements from the type checker output. The default
     * implementation extracts the underlying AST node and delegates to the appropriate
     * visitor method.
     */
    fun visitTypedStatement(typedStmt: TypedStatement): R {
        return when (typedStmt) {
            is TypedStatement.FunctionDeclaration -> typedStmt.declaration.accept(this)
            is TypedStatement.TypeDeclaration -> typedStmt.declaration.accept(this)
            is TypedStatement.VariableDeclaration -> typedStmt.declaration.accept(this)
            is TypedStatement.MutableVariableDeclaration -> typedStmt.declaration.accept(this)
            is TypedStatement.Assignment -> typedStmt.assignment.accept(this)
            is TypedStatement.ExpressionStatement -> visitTypedExpression(typedStmt.expression)
            is TypedStatement.ReturnStatement -> typedStmt.returnStatement.accept(this)
        }
    }
    
    /**
     * Visit a typed program containing typed statements.
     */
    fun visitTypedProgram(typedProgram: TypedProgram): R {
        // Convert to list of results and combine them
        val results = typedProgram.statements.map { visitTypedStatement(it) }
        return if (results.isEmpty()) {
            defaultResult() 
        } else {
            results.reduce { acc, result -> combineTyped(acc, result) }
        }
    }
    
    /**
     * Default result when no processing is needed.
     * Must be implemented by concrete visitors.
     */
    fun defaultResult(): R
    
    /**
     * Combine two results. Default implementation returns the second result.
     * Override to customize result aggregation.
     */
    fun combineTyped(first: R, second: R): R = second
}

/**
 * Base implementation of TypedASTVisitor that provides default traversal behavior.
 * 
 * This abstract class extends BaseASTVisitor and adds typed visitor capabilities.
 * It provides sensible defaults for handling typed expressions while maintaining
 * compatibility with the existing visitor infrastructure.
 * 
 * Usage:
 * - Extend this class for visitors that need type information
 * - Override specific visitTyped* methods for custom behavior
 * - Use visitTypedExpression to access both expression and type
 * 
 * @param R The return type of visit methods
 */
abstract class BaseTypedASTVisitor<R> : BaseASTVisitor<R>(), TypedASTVisitor<R> {
    
    /**
     * Override to access type information during expression visiting.
     * 
     * This method is called for each typed expression and provides access to both
     * the AST node and its inferred type. Subclasses can override this to perform
     * type-specific processing while still benefiting from visitor dispatch.
     * 
     * @param typedExpr The typed expression containing both AST and type info
     * @return Result of processing the typed expression
     */
    override fun visitTypedExpression(typedExpr: TypedExpression): R {
        // Default implementation: just visit the underlying expression
        // Subclasses can override to access type information
        return typedExpr.expression.accept(this)
    }
    
    override fun combineTyped(first: R, second: R): R = combine(first, second)
}