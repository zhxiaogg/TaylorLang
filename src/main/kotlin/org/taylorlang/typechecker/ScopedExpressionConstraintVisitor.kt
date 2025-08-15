package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.PersistentList
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
        var allConstraints = ConstraintSet.empty()
        
        // Enhanced validation for try expression context
        val tryExpressionValidation = validateTryExpressionContext(tryExpr, functionReturnType)
        if (tryExpressionValidation.hasError) {
            // Add a constraint that will fail during constraint solving with a proper error message
            val errorConstraint = Constraint.Equality(
                BuiltinTypes.UNIT, // Invalid placeholder type
                BuiltinTypes.INT,  // Different type to ensure failure
                tryExpr.sourceLocation
            )
            allConstraints = allConstraints.add(errorConstraint)
        }
        
        // Collect constraints from the try expression
        val tryResult = collector.collectConstraints(tryExpr.expression, context)
        allConstraints = allConstraints.merge(tryResult.constraints)
        
        // CRITICAL FIX: Try expressions must unwrap Result types to their value types
        val unwrappedType = if (BuiltinTypes.isResultType(tryResult.type)) {
            BuiltinTypes.getResultValueType(tryResult.type) ?: tryResult.type
        } else {
            // Non-Result expressions return their type directly
            tryResult.type
        }
        
        // Process catch clauses with enhanced error type checking
        val catchProcessingResult = processCatchClauses(
            tryExpr.catchClauses,
            unwrappedType,
            null, // Error type will be inferred from catch clauses
            context
        )
        allConstraints = allConstraints.merge(catchProcessingResult.constraints)
        
        // Enhanced bidirectional type checking: if we have an expected type, unify with the unwrapped value type
        if (expectedType != null) {
            val unificationConstraint = Constraint.Equality(
                unwrappedType, 
                expectedType, 
                tryExpr.sourceLocation
            )
            allConstraints = allConstraints.add(unificationConstraint)
        }
        
        // CRITICAL: Try expressions return Result<T, E> types, not unwrapped value types
        val errorType = catchProcessingResult.unifiedErrorType ?: BuiltinTypes.THROWABLE
        val resultType = BuiltinTypes.createResultType(unwrappedType, errorType)
        
        // Add constraint to ensure Result type consistency with function return type
        if (functionReturnType != null && BuiltinTypes.isResultType(functionReturnType)) {
            val returnTypeValueType = BuiltinTypes.getResultValueType(functionReturnType)
            val returnTypeErrorType = BuiltinTypes.getResultErrorType(functionReturnType)
            
            // Generate constraints to unify Result components
            if (returnTypeValueType != null) {
                val valueTypeConstraint = Constraint.Equality(
                    unwrappedType,
                    returnTypeValueType,
                    tryExpr.sourceLocation
                )
                allConstraints = allConstraints.add(valueTypeConstraint)
            }
            
            if (returnTypeErrorType != null) {
                val errorTypeConstraint = Constraint.Equality(
                    errorType,
                    returnTypeErrorType,
                    tryExpr.sourceLocation
                )
                allConstraints = allConstraints.add(errorTypeConstraint)
            }
        }
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    // =============================================================================
    // Enhanced Try Expression Type Checking Support Methods
    // =============================================================================
    
    /**
     * Validation result for try expression context checking.
     */
    private data class TryExpressionValidationResult(
        val hasError: Boolean,
        val errorMessage: String? = null
    )
    
    /**
     * Type inference result for try expressions.
     */
    private data class TryTypeInferenceResult(
        val expectedValueType: Type?,
        val expectedErrorType: Type?,
        val isInferred: Boolean
    )
    
    /**
     * Result of processing catch clauses.
     */
    private data class CatchProcessingResult(
        val constraints: ConstraintSet,
        val unifiedErrorType: Type?,
        val allErrorTypes: List<Type>
    )
    
    /**
     * Validate that a try expression is used in the proper context.
     * Try expressions are only allowed in functions returning Result<T, E>.
     */
    private fun validateTryExpressionContext(
        tryExpr: TryExpression,
        functionReturnType: Type?
    ): TryExpressionValidationResult {
        if (functionReturnType == null) {
            // No function context available - this is an error in most cases
            return TryExpressionValidationResult(
                hasError = true,
                errorMessage = "Try expressions can only be used within function bodies"
            )
        }
        
        if (!BuiltinTypes.isResultType(functionReturnType)) {
            return TryExpressionValidationResult(
                hasError = true,
                errorMessage = "Try expressions can only be used in functions that return Result<T, E>, but function returns $functionReturnType"
            )
        }
        
        return TryExpressionValidationResult(hasError = false)
    }
    
    /**
     * Infer the expected value and error types for a try expression using bidirectional type checking.
     */
    private fun inferTryExpressionTypes(
        expectedType: Type?,
        functionReturnType: Type?,
        tryExpr: TryExpression
    ): Triple<Type?, Type?, TryTypeInferenceResult> {
        // Priority order for type inference:
        // 1. Explicit expected type (if it's a Result type)
        // 2. Function return type (if it's a Result type)  
        // 3. Fresh type variables
        
        when {
            expectedType != null && BuiltinTypes.isResultType(expectedType) -> {
                val valueType = BuiltinTypes.getResultValueType(expectedType)
                val errorType = BuiltinTypes.getResultErrorType(expectedType)
                return Triple(
                    valueType, 
                    errorType,
                    TryTypeInferenceResult(valueType, errorType, false)
                )
            }
            
            functionReturnType != null && BuiltinTypes.isResultType(functionReturnType) -> {
                val valueType = BuiltinTypes.getResultValueType(functionReturnType)
                val errorType = BuiltinTypes.getResultErrorType(functionReturnType)
                return Triple(
                    valueType,
                    errorType,
                    TryTypeInferenceResult(valueType, errorType, false)
                )
            }
            
            else -> {
                // Create fresh type variables for bidirectional inference
                val valueVar = TypeVar.fresh()
                val errorVar = TypeVar.fresh()
                val valueType = Type.NamedType(valueVar.id)
                val errorType = Type.NamedType(errorVar.id)
                return Triple(
                    valueType,
                    errorType,
                    TryTypeInferenceResult(valueType, errorType, true)
                )
            }
        }
    }
    
    /**
     * Process catch clauses with enhanced error type checking and unification.
     */
    private fun processCatchClauses(
        catchClauses: PersistentList<CatchClause>,
        tryValueType: Type,
        expectedErrorType: Type?,
        context: InferenceContext
    ): CatchProcessingResult {
        if (catchClauses.isEmpty()) {
            return CatchProcessingResult(
                constraints = ConstraintSet.empty(),
                unifiedErrorType = expectedErrorType,
                allErrorTypes = emptyList()
            )
        }
        
        var allConstraints = ConstraintSet.empty()
        val errorTypes = mutableListOf<Type>()
        
        for (catchClause: CatchClause in catchClauses) {
            // Process the pattern to extract error type information
            val patternResult = processCatchPattern(catchClause.pattern, expectedErrorType, context)
            allConstraints = allConstraints.merge(patternResult.constraints)
            errorTypes.add(patternResult.errorType)
            
            // Create new context with pattern bindings
            val catchContext = context.withPatternBindings(patternResult.bindings)
            
            // Process guard expression if present
            catchClause.guardExpression?.let { guard: Expression ->
                val guardResult = collector.collectConstraintsWithExpected(
                    guard, 
                    BuiltinTypes.BOOLEAN, 
                    catchContext
                )
                allConstraints = allConstraints.merge(guardResult.constraints)
                
                // Add constraint that guard expression must be Boolean
                val booleanConstraint = Constraint.Equality(
                    guardResult.type,
                    BuiltinTypes.BOOLEAN,
                    catchClause.sourceLocation // Use catchClause location instead
                )
                allConstraints = allConstraints.add(booleanConstraint)
            }
            
            // Collect constraints from catch body - must return same type as try value
            val bodyResult = collector.collectConstraintsWithExpected(
                catchClause.body, 
                tryValueType, 
                catchContext
            )
            allConstraints = allConstraints.merge(bodyResult.constraints)
            
            // Enhanced constraint: catch body type must unify with try value type
            val bodyTypeConstraint = Constraint.Equality(
                bodyResult.type,
                tryValueType,
                catchClause.body.sourceLocation
            )
            allConstraints = allConstraints.add(bodyTypeConstraint)
        }
        
        // Unify all error types from catch clauses
        val unifiedErrorType = unifyErrorTypes(errorTypes, expectedErrorType)
        
        return CatchProcessingResult(
            constraints = allConstraints,
            unifiedErrorType = unifiedErrorType,
            allErrorTypes = errorTypes
        )
    }
    
    /**
     * Unify multiple error types into a single unified type.
     * Uses a simple strategy for now - in a full implementation this would be more sophisticated.
     */
    private fun unifyErrorTypes(errorTypes: List<Type>, expectedErrorType: Type?): Type? {
        if (errorTypes.isEmpty()) return expectedErrorType
        if (errorTypes.size == 1) return errorTypes.first()
        
        // For now, find the most general Throwable subtype
        // In a full implementation, this would create union types or find common supertypes
        val throwableTypes = errorTypes.filter { BuiltinTypes.isThrowableSubtype(it) }
        
        return when {
            throwableTypes.isNotEmpty() -> {
                // Use the most general type - in practice this would be more sophisticated
                if (throwableTypes.any { it == BuiltinTypes.THROWABLE }) {
                    BuiltinTypes.THROWABLE
                } else {
                    throwableTypes.first() // Simplified - take the first valid type
                }
            }
            expectedErrorType != null -> expectedErrorType
            else -> BuiltinTypes.THROWABLE
        }
    }
    
    /**
     * Generate constraints to ensure a type is a subtype of Throwable.
     */
    private fun generateThrowableConstraints(
        errorType: Type, 
        location: SourceLocation?
    ): ConstraintSet {
        if (BuiltinTypes.isThrowableSubtype(errorType)) {
            return ConstraintSet.empty() // Already known to be valid
        }
        
        // Add subtype constraint
        val constraint = Constraint.Subtype(
            errorType,
            BuiltinTypes.THROWABLE,
            location
        )
        return ConstraintSet.of(constraint)
    }
    
    /**
     * Generate constraints for unifying a Result type with an expected type.
     */
    private fun generateResultTypeUnificationConstraints(
        resultType: Type,
        expectedType: Type,
        location: SourceLocation?
    ): ConstraintSet {
        // Direct equality constraint
        val equalityConstraint = Constraint.Equality(resultType, expectedType, location)
        
        // If the expected type is also a Result type, add more specific constraints
        if (BuiltinTypes.isResultType(expectedType)) {
            val resultValueType = BuiltinTypes.getResultValueType(resultType)
            val resultErrorType = BuiltinTypes.getResultErrorType(resultType)
            val expectedValueType = BuiltinTypes.getResultValueType(expectedType)
            val expectedErrorType = BuiltinTypes.getResultErrorType(expectedType)
            
            val constraints = mutableListOf(equalityConstraint)
            
            // Add value type constraint
            if (resultValueType != null && expectedValueType != null) {
                constraints.add(Constraint.Equality(resultValueType, expectedValueType, location))
            }
            
            // Add error type constraint (with equality for now, subtyping can be added later)
            if (resultErrorType != null && expectedErrorType != null) {
                constraints.add(Constraint.Equality(resultErrorType, expectedErrorType, location))
            }
            
            return ConstraintSet.fromCollection(constraints)
        }
        
        return ConstraintSet.of(equalityConstraint)
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