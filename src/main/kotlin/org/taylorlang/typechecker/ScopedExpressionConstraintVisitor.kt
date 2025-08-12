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
        // First, validate that we're in a Result-returning function context
        val functionReturnType = context.getCurrentFunctionReturnType()
        if (functionReturnType != null && !BuiltinTypes.isResultType(functionReturnType)) {
            // Try expressions are only allowed in Result-returning functions
            val error = TypeError.InvalidTryExpressionContext(
                "Try expressions can only be used in functions that return Result<T, E>",
                tryExpr.sourceLocation
            )
            // For now, we'll continue processing but this should be an error
            // In a full implementation, this would add a constraint violation
        }
        
        // Determine the expected Result type
        val expectedResultType = expectedType ?: functionReturnType
        val (expectedValueType, expectedErrorType) = if (expectedResultType != null && BuiltinTypes.isResultType(expectedResultType)) {
            Pair(
                BuiltinTypes.getResultValueType(expectedResultType),
                BuiltinTypes.getResultErrorType(expectedResultType)
            )
        } else {
            // Create fresh type variables for Result<T, E>
            val valueVar = TypeVar.fresh()
            val errorVar = TypeVar.fresh()
            Pair(
                Type.NamedType(valueVar.id),
                Type.NamedType(errorVar.id)
            )
        }
        
        // Collect constraints from the try expression
        val tryResult = collector.collectConstraintsWithExpected(tryExpr.expression, expectedValueType, context)
        var allConstraints = tryResult.constraints
        
        // The try expression should evaluate to the value type
        val tryValueType = tryResult.type
        
        // Process catch clauses if present
        var catchErrorTypes = mutableListOf<Type>()
        if (tryExpr.catchClauses.isNotEmpty()) {
            for (catchClause in tryExpr.catchClauses) {
                // Process the pattern to extract error type information
                val patternResult = processCatchPattern(catchClause.pattern, expectedErrorType, context)
                allConstraints = allConstraints.merge(patternResult.constraints)
                catchErrorTypes.add(patternResult.errorType)
                
                // Create new context with pattern bindings
                val catchContext = context.withPatternBindings(patternResult.bindings)
                
                // Process guard expression if present
                catchClause.guardExpression?.let { guard ->
                    val guardResult = collector.collectConstraintsWithExpected(guard, BuiltinTypes.BOOLEAN, catchContext)
                    allConstraints = allConstraints.merge(guardResult.constraints)
                }
                
                // Collect constraints from catch body - should return same type as try
                val bodyResult = collector.collectConstraintsWithExpected(catchClause.body, expectedValueType, catchContext)
                allConstraints = allConstraints.merge(bodyResult.constraints)
                
                // Add constraint that catch body type matches try body type
                val equalityConstraint = Constraint.Equality(
                    bodyResult.type,
                    tryValueType,
                    catchClause.sourceLocation
                )
                allConstraints = allConstraints.add(equalityConstraint)
            }
        }
        
        // Create the Result type for the overall try expression
        val inferredErrorType = expectedErrorType ?: run {
            // If we have catch clauses, unify their error types
            if (catchErrorTypes.isNotEmpty()) {
                // For simplicity, take the first error type
                // A full implementation would perform proper unification
                catchErrorTypes.first()
            } else {
                // Default to Throwable
                BuiltinTypes.THROWABLE
            }
        }
        
        val resultType = BuiltinTypes.createResultType(tryValueType, inferredErrorType)
        
        // Add constraint that the inferred error type is a Throwable subtype
        if (inferredErrorType != null && !BuiltinTypes.isThrowableSubtype(inferredErrorType)) {
            val throwableConstraint = Constraint.Subtype(
                inferredErrorType,
                BuiltinTypes.THROWABLE,
                tryExpr.sourceLocation
            )
            allConstraints = allConstraints.add(throwableConstraint)
        }
        
        // If we have an expected type, add equality constraint
        if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, tryExpr.sourceLocation)
            allConstraints = allConstraints.add(constraint)
        }
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    /**
     * Process a catch clause pattern to extract error type and variable bindings.
     */
    private fun processCatchPattern(
        pattern: Pattern,
        expectedErrorType: Type?,
        context: InferenceContext
    ): CatchPatternResult {
        // For now, handle simple patterns
        return when (pattern) {
            is Pattern.IdentifierPattern -> {
                val errorType = expectedErrorType ?: BuiltinTypes.THROWABLE
                CatchPatternResult(
                    errorType = errorType,
                    bindings = mapOf(pattern.name to errorType),
                    constraints = ConstraintSet.empty()
                )
            }
            is Pattern.ConstructorPattern -> {
                // Constructor pattern for specific exception types
                val errorType = Type.NamedType(pattern.constructor)
                val bindings = mutableMapOf<String, Type>()
                var constraints = ConstraintSet.empty()
                
                // Process constructor arguments
                pattern.patterns.forEachIndexed { _, argPattern ->
                    when (argPattern) {
                        is Pattern.IdentifierPattern -> {
                            val argVar = TypeVar.fresh()
                            val argType = Type.NamedType(argVar.id)
                            bindings[argPattern.name] = argType
                        }
                        else -> {
                            // Handle other pattern types as needed
                        }
                    }
                }
                
                // Add constraint that the error type is a Throwable subtype
                if (expectedErrorType != null) {
                    val subtypeConstraint = Constraint.Subtype(errorType, expectedErrorType, pattern.sourceLocation)
                    constraints = constraints.add(subtypeConstraint)
                } else {
                    val throwableConstraint = Constraint.Subtype(errorType, BuiltinTypes.THROWABLE, pattern.sourceLocation)
                    constraints = constraints.add(throwableConstraint)
                }
                
                CatchPatternResult(
                    errorType = errorType,
                    bindings = bindings,
                    constraints = constraints
                )
            }
            else -> {
                // Fallback for other pattern types
                val errorType = expectedErrorType ?: BuiltinTypes.THROWABLE
                CatchPatternResult(
                    errorType = errorType,
                    bindings = emptyMap(),
                    constraints = ConstraintSet.empty()
                )
            }
        }
    }
    
    /**
     * Result of processing a catch pattern.
     */
    private data class CatchPatternResult(
        val errorType: Type,
        val bindings: Map<String, Type>,
        val constraints: ConstraintSet
    )
}