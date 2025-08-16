package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*
import org.taylorlang.ast.visitor.BaseASTVisitor

/**
 * Refactored expression type checker that delegates to specialized checkers.
 * 
 * This is the main coordinator that handles expression type checking by
 * delegating to specialized checker classes. This approach maintains the
 * visitor pattern while keeping file sizes manageable.
 * 
 * The original 881-line ExpressionTypeChecker has been split into:
 * - ArithmeticExpressionChecker: Binary/unary operations and numeric type promotion
 * - ControlFlowExpressionChecker: If/match/function/constructor calls  
 * - LiteralExpressionChecker: Literals, identifiers, and type inference utilities
 */
class ExpressionTypeChecker(
    private val context: TypeContext
) : BaseASTVisitor<Result<TypedExpression>>() {
    
    // Lazy initialization to avoid circular dependencies
    private val arithmeticChecker by lazy { ArithmeticExpressionChecker(context, this) }
    private val controlFlowChecker by lazy { ControlFlowExpressionChecker(context, this, arithmeticChecker) }
    private val literalChecker by lazy { LiteralExpressionChecker(context, this) }
    
    override fun defaultResult(): Result<TypedExpression> {
        return Result.failure(TypeError.InvalidOperation(
            "Unsupported expression type", 
            emptyList(), 
            null
        ))
    }
    
    override fun combine(first: Result<TypedExpression>, second: Result<TypedExpression>): Result<TypedExpression> {
        // For expression type checking, we typically don't combine results
        // Each expression visit should return its own result
        return second
    }
    
    // =============================================================================
    // Expression Type Checking Dispatch
    // =============================================================================
    
    override fun visitExpression(node: Expression): Result<TypedExpression> {
        return when (node) {
            is Identifier -> literalChecker.visitIdentifier(node)
            is FunctionCall -> controlFlowChecker.visitFunctionCall(node)
            is ConstructorCall -> controlFlowChecker.visitConstructorCall(node)
            is BinaryOp -> arithmeticChecker.visitBinaryOp(node)
            is UnaryOp -> arithmeticChecker.visitUnaryOp(node)
            is IfExpression -> controlFlowChecker.visitIfExpression(node)
            is WhileExpression -> controlFlowChecker.visitWhileExpression(node)
            is MatchExpression -> controlFlowChecker.visitMatchExpression(node)
            is BlockExpression -> visitBlockExpression(node)
            is LambdaExpression -> visitLambdaExpression(node)
            is Literal -> literalChecker.visitLiteral(node)
            else -> defaultResult()
        }
    }
    
    // =============================================================================  
    // Direct Expression Handlers
    // =============================================================================
    
    override fun visitIdentifier(node: Identifier): Result<TypedExpression> {
        return literalChecker.visitIdentifier(node)
    }
    
    override fun visitFunctionCall(node: FunctionCall): Result<TypedExpression> {
        return controlFlowChecker.visitFunctionCall(node)
    }
    
    override fun visitConstructorCall(node: ConstructorCall): Result<TypedExpression> {
        return controlFlowChecker.visitConstructorCall(node)
    }
    
    override fun visitBinaryOp(node: BinaryOp): Result<TypedExpression> {
        return arithmeticChecker.visitBinaryOp(node)
    }
    
    override fun visitUnaryOp(node: UnaryOp): Result<TypedExpression> {
        return arithmeticChecker.visitUnaryOp(node)
    }
    
    override fun visitIfExpression(node: IfExpression): Result<TypedExpression> {
        return controlFlowChecker.visitIfExpression(node)
    }
    
    override fun visitWhileExpression(node: WhileExpression): Result<TypedExpression> {
        return controlFlowChecker.visitWhileExpression(node)
    }
    
    override fun visitMatchExpression(node: MatchExpression): Result<TypedExpression> {
        return controlFlowChecker.visitMatchExpression(node)
    }
    
    override fun visitBlockExpression(node: BlockExpression): Result<TypedExpression> {
        // Create a new scope for the block
        var blockContext = context
        val errors = mutableListOf<TypeError>()
        val typedStatements = mutableListOf<TypedStatement>()
        
        // Type check each statement in the block and update context
        val sharedScopeManager = ScopeManager()
        
        // Populate the global scope with variables from the current context
        // Note: This is a simplified approach assuming variables in TypeContext are mutable
        // In a more complete implementation, we'd need to track mutability in TypeContext too
        for ((name, type) in blockContext.variables) {
            sharedScopeManager.declareVariable(name, type, isMutable = true)
        }
        
        // Push a new scope for the block to allow variable shadowing
        sharedScopeManager.pushScope()
        
        for (statement in node.statements) {
            val stmtChecker = StatementTypeChecker(blockContext, sharedScopeManager)
            val stmtResult = statement.accept(stmtChecker)
            stmtResult.fold(
                onSuccess = { typedStmt ->
                    typedStatements.add(typedStmt)
                    
                    // Update context with new variable bindings from variable declarations
                    when (typedStmt) {
                        is TypedStatement.VariableDeclaration -> {
                            blockContext = blockContext.withVariable(
                                typedStmt.declaration.name,
                                typedStmt.inferredType
                            )
                        }
                        is TypedStatement.MutableVariableDeclaration -> {
                            blockContext = blockContext.withVariable(
                                typedStmt.declaration.name,
                                typedStmt.inferredType
                            )
                        }
                        else -> { /* No context update needed for other statement types */ }
                    }
                },
                onFailure = { error ->
                    // Collect error but continue processing to gather all errors
                    val typeError = when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error", 
                            emptyList(), 
                            statement.sourceLocation
                        )
                    }
                    errors.add(typeError)
                    
                    // Even with errors, attempt to add placeholder context for variables
                    // This allows subsequent statements to continue type checking
                    when (statement) {
                        is ValDecl -> {
                            // Add variable with error type to allow continued processing
                            blockContext = blockContext.withVariable(
                                statement.name,
                                statement.type ?: BuiltinTypes.UNIT
                            )
                        }
                        is VarDecl -> {
                            // Add variable with error type to allow continued processing
                            blockContext = blockContext.withVariable(
                                statement.name,
                                statement.type ?: BuiltinTypes.UNIT
                            )
                        }
                        else -> { /* No context update needed for other statement types */ }
                    }
                }
            )
        }
        
        // If there are errors in statements, return them
        if (errors.isNotEmpty()) {
            // Pop the scope before returning
            sharedScopeManager.popScope()
            return Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        }
        
        // Determine the type of the block based on the final expression
        val blockType = if (node.expression != null) {
            // Block has a final expression - type is the type of that expression
            // Use a new checker with the updated block context that includes block variables
            val exprChecker = ExpressionTypeChecker(blockContext)
            val result = node.expression.accept(exprChecker).fold(
                onSuccess = { it.type },
                onFailure = { error ->
                    // Pop the scope before returning error
                    sharedScopeManager.popScope()
                    return Result.failure(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            node.expression.sourceLocation
                        )
                    })
                }
            )
            result
        } else {
            // Block has no final expression - type is Unit
            BuiltinTypes.UNIT
        }
        
        // Pop the scope when we're done with the block
        sharedScopeManager.popScope()
        
        return Result.success(TypedExpression(node, blockType))
    }
    
    override fun visitLambdaExpression(node: LambdaExpression): Result<TypedExpression> {
        // Create fresh type variables for each parameter
        val parameterTypes = node.parameters.map { TypeVar.fresh() }
        
        // Create new context with parameter bindings
        var lambdaContext = context
        node.parameters.zip(parameterTypes).forEach { (paramName, paramType) ->
            lambdaContext = lambdaContext.withVariable(paramName, Type.NamedType(paramType.id))
        }
        
        // Type check the lambda body in the new context
        val bodyChecker = ExpressionTypeChecker(lambdaContext)
        return node.body.accept(bodyChecker).fold(
            onSuccess = { typedBody ->
                // Create function type from parameter types and body type
                val functionType = Type.FunctionType(
                    parameterTypes = parameterTypes.map { Type.NamedType(it.id) }.toPersistentList(),
                    returnType = typedBody.type
                )
                Result.success(TypedExpression(node, functionType))
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
    
    // =============================================================================
    // Literal Type Checking
    // =============================================================================
    
    override fun visitLiteral(node: Literal): Result<TypedExpression> {
        return literalChecker.visitLiteral(node)
    }
    
    override fun visitIntLiteral(node: Literal.IntLiteral): Result<TypedExpression> {
        return literalChecker.visitIntLiteral(node)
    }
    
    override fun visitFloatLiteral(node: Literal.FloatLiteral): Result<TypedExpression> {
        return literalChecker.visitFloatLiteral(node)
    }
    
    override fun visitStringLiteral(node: Literal.StringLiteral): Result<TypedExpression> {
        return literalChecker.visitStringLiteral(node)
    }
    
    override fun visitBooleanLiteral(node: Literal.BooleanLiteral): Result<TypedExpression> {
        return literalChecker.visitBooleanLiteral(node)
    }
    
    override fun visitNullLiteral(node: Literal.NullLiteral): Result<TypedExpression> {
        return literalChecker.visitNullLiteral(node)
    }
    
    override fun visitTupleLiteral(node: Literal.TupleLiteral): Result<TypedExpression> {
        return literalChecker.visitTupleLiteral(node)
    }
}