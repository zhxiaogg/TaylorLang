package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Specialized visitor for collecting constraints from scoped expressions.
 * 
 * This visitor handles expressions that introduce new scopes or variable bindings:
 * - Block expressions with variable declarations
 * - Lambda expressions with parameter binding
 * - For loop expressions with iteration variables
 * 
 * Designed to work as part of a modular constraint collection system where
 * scoped constructs are handled separately from simple expressions and statements.
 */
class ScopedExpressionConstraintVisitor(
    private val collector: ConstraintCollector
) {
    
    // =============================================================================
    // Block Expression Handler
    // =============================================================================
    
    fun handleBlockExpression(
        blockExpr: BlockExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Create new scope for the block
        var blockContext = context.enterScope()
        var allConstraints = ConstraintSet.empty()
        
        // Process each statement in the block
        for (statement in blockExpr.statements) {
            when (statement) {
                is ValDecl -> {
                    // Process variable declaration
                    val initResult = collector.collectConstraints(statement.initializer, blockContext)
                    allConstraints = allConstraints.merge(initResult.constraints)
                    
                    // Add variable to scope
                    val varType = statement.type ?: initResult.type
                    if (statement.type != null) {
                        // Add equality constraint for explicit type annotation
                        val constraint = Constraint.Equality(
                            initResult.type, 
                            statement.type, 
                            statement.sourceLocation
                        )
                        allConstraints = allConstraints.add(constraint)
                    }
                    
                    blockContext = blockContext.withVariable(statement.name, varType)
                }
                
                is Expression -> {
                    // Process expression statement
                    val exprResult = collector.collectConstraints(statement, blockContext)
                    allConstraints = allConstraints.merge(exprResult.constraints)
                }
                
                else -> {
                    // Other statement types - skip for now
                }
            }
        }
        
        // Process final expression if present
        val resultType = if (blockExpr.expression != null) {
            val exprResult = collector.collectConstraintsWithExpected(blockExpr.expression, expectedType, blockContext)
            allConstraints = allConstraints.merge(exprResult.constraints)
            exprResult.type
        } else {
            BuiltinTypes.UNIT
        }
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    // =============================================================================
    // Lambda Expression Handler
    // =============================================================================
    
    fun handleLambdaExpression(
        lambda: LambdaExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Create fresh type variables for parameters
        val paramTypes = lambda.parameters.map { 
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Create new scope with parameter bindings
        val paramBindings = lambda.parameters.zip(paramTypes).toMap()
        val lambdaContext = context.enterScopeWith(paramBindings)
        
        // Collect constraints from lambda body
        val bodyResult = collector.collectConstraints(lambda.body, lambdaContext)
        
        // Create function type
        val functionType = Type.FunctionType(
            parameterTypes = paramTypes.toPersistentList(),
            returnType = bodyResult.type
        )
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(functionType, expectedType, lambda.sourceLocation)
            ConstraintResult(functionType, bodyResult.constraints.add(constraint))
        } else {
            ConstraintResult(functionType, bodyResult.constraints)
        }
    }
    
    // =============================================================================
    // For Expression Handler
    // =============================================================================
    
    fun handleForExpression(
        forExpr: ForExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from iterable expression
        val iterableResult = collector.collectConstraints(forExpr.iterable, context)
        
        // Create fresh type variables for element type and result type
        val elementVar = TypeVar.fresh()
        val elementType = Type.NamedType(elementVar.id)
        
        // Generate constraint that iterable contains elements of element type
        // This would be more sophisticated in a full implementation with proper collection types
        
        // Create new scope with loop variable binding
        val loopContext = context.enterScopeWith(mapOf(forExpr.variable to elementType))
        
        // Collect constraints from loop body
        val bodyResult = collector.collectConstraints(forExpr.body, loopContext)
        
        // For expression result type is typically a collection of body results
        val resultType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        val allConstraints = iterableResult.constraints.merge(bodyResult.constraints)
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    // =============================================================================
    // Try Expression Handler
    // =============================================================================
    
    fun handleTryExpression(
        tryExpr: TryExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from the try expression
        val tryResult = collector.collectConstraintsWithExpected(tryExpr.expression, expectedType, context)
        var allConstraints = tryResult.constraints
        
        // Process catch clauses if present
        if (tryExpr.catchClauses.isNotEmpty()) {
            // For now, treat catch clauses similarly to match cases
            // In a full implementation, this would handle Result types and error propagation
            for (catchClause in tryExpr.catchClauses) {
                // Collect constraints from catch body
                val bodyResult = collector.collectConstraintsWithExpected(catchClause.body, expectedType, context)
                allConstraints = allConstraints.merge(bodyResult.constraints)
                
                // Process guard expression if present
                catchClause.guardExpression?.let { guard ->
                    val guardResult = collector.collectConstraints(guard, context)
                    allConstraints = allConstraints.merge(guardResult.constraints)
                }
            }
        }
        
        // The result type is the same as the try expression type for now
        // In the full implementation, this would involve Result type handling
        val resultType = expectedType ?: tryResult.type
        
        return ConstraintResult(resultType, allConstraints)
    }
}