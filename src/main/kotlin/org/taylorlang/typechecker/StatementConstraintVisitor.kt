package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Specialized visitor for collecting constraints from statements and scoped expressions.
 * 
 * This visitor handles statement-level constraint collection including:
 * - Block expressions with variable declarations
 * - Lambda expressions with parameter binding
 * - For loop expressions with iteration variables
 * - Constructor calls with type resolution
 * - Control flow expressions (if, while, match)
 * 
 * Designed to work as part of a modular constraint collection system where
 * statements and scoped constructs are handled separately from simple expressions.
 */
class StatementConstraintVisitor(
    private val collector: ConstraintCollector,
    private val patternVisitor: PatternConstraintVisitor
) {
    
    // =============================================================================
    // Constructor Call Handlers
    // =============================================================================
    
    fun handleConstructorCall(
        call: ConstructorCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Find the constructor in union type definitions
        val (typeDef, unionTypeName) = findConstructorDefinition(call.constructor, context)
        
        if (typeDef != null && unionTypeName != null) {
            return handleTypedConstructorCall(call, typeDef, unionTypeName, expectedType, context)
        } else {
            // Unknown constructor - generate fresh types
            return handleUnknownConstructorCall(call, expectedType, context)
        }
    }
    
    private fun handleTypedConstructorCall(
        call: ConstructorCall,
        typeDef: TypeDefinition.UnionTypeDef,
        unionTypeName: String,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val variant = typeDef.variants.find { it.name == call.constructor }!!
        
        // Collect constraints from constructor arguments
        val argumentResults = call.arguments.map { arg ->
            collector.collectConstraints(arg, context)
        }
        
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        // Generate type arguments if the union type is generic
        val typeArguments = if (typeDef.typeParameters.isNotEmpty()) {
            generateTypeArgumentsFromArguments(
                typeDef.typeParameters,
                variant.fields,
                argumentResults.map { it.type }
            )
        } else {
            persistentListOf<Type>()
        }
        
        // Create the constructor result type
        val constructorType = Type.UnionType(unionTypeName, typeArguments)
        
        // Generate constraints for argument types
        val argumentConstraints = generateConstructorArgumentConstraints(
            variant.fields,
            argumentResults.map { it.type },
            typeDef.typeParameters,
            typeArguments,
            call.sourceLocation
        )
        
        val allConstraints = baseConstraints.merge(argumentConstraints)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(constructorType, expectedType, call.sourceLocation)
            ConstraintResult(constructorType, allConstraints.add(constraint))
        } else {
            ConstraintResult(constructorType, allConstraints)
        }
    }
    
    private fun handleUnknownConstructorCall(
        call: ConstructorCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Create fresh type variables for unknown constructor
        val argumentResults = call.arguments.map { arg ->
            collector.collectConstraints(arg, context)
        }
        
        val resultType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        return ConstraintResult(resultType, baseConstraints)
    }
    
    // =============================================================================
    // Control Flow Expression Handlers
    // =============================================================================
    
    fun handleIfExpression(
        ifExpr: IfExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from condition (must be Boolean)
        val conditionResult = collector.collectConstraintsWithExpected(
            ifExpr.condition, 
            BuiltinTypes.BOOLEAN, 
            context
        )
        
        // Collect constraints from branches
        val thenResult = collector.collectConstraintsWithExpected(ifExpr.thenExpression, expectedType, context)
        
        val elseResult = if (ifExpr.elseExpression != null) {
            collector.collectConstraintsWithExpected(ifExpr.elseExpression, expectedType, context)
        } else {
            // No else branch - result can be null
            val nullType = Type.NullableType(thenResult.type)
            ConstraintResult.withType(nullType)
        }
        
        // Merge all constraints
        val allConstraints = conditionResult.constraints
            .merge(thenResult.constraints)
            .merge(elseResult.constraints)
        
        // Determine result type
        val resultType = if (ifExpr.elseExpression != null) {
            // Both branches present - they should have compatible types
            if (expectedType != null) {
                expectedType
            } else {
                // Try to unify the branch types directly if they're the same
                if (collector.typesAreEqual(thenResult.type, elseResult.type)) {
                    thenResult.type
                } else {
                    // Create fresh type variable and add equality constraints
                    val freshVar = TypeVar.fresh()
                    val unifiedType = Type.NamedType(freshVar.id)
                    
                    val thenConstraint = Constraint.Subtype(thenResult.type, unifiedType, ifExpr.thenExpression.sourceLocation)
                    val elseConstraint = Constraint.Subtype(elseResult.type, unifiedType, ifExpr.elseExpression.sourceLocation)
                    
                    return ConstraintResult(
                        unifiedType, 
                        allConstraints.add(thenConstraint).add(elseConstraint)
                    )
                }
            }
        } else {
            // No else branch - result is nullable
            Type.NullableType(thenResult.type)
        }
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    fun handleWhileExpression(
        whileExpr: WhileExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from condition (must be Boolean)
        val conditionResult = collector.collectConstraintsWithExpected(
            whileExpr.condition,
            BuiltinTypes.BOOLEAN,
            context
        )
        
        // Collect constraints from body - while loop body executes 0 or more times
        // so the result of the body is not directly the result of the while expression
        val bodyResult = collector.collectConstraints(whileExpr.body, context)
        
        // Merge all constraints
        val allConstraints = conditionResult.constraints
            .merge(bodyResult.constraints)
        
        // While loop result type is typically Unit in most languages
        // unless specified otherwise by expected type
        val resultType = expectedType ?: BuiltinTypes.UNIT
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    fun handleMatchExpression(
        matchExpr: MatchExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target expression
        val targetResult = collector.collectConstraints(matchExpr.target, context)
        
        // Process match cases using pattern visitor
        val (resultType, caseConstraints) = patternVisitor.processMatchCases(
            matchExpr.cases,
            targetResult.type,
            expectedType,
            context,
            matchExpr.sourceLocation
        )
        
        // Merge all constraints
        val allConstraints = targetResult.constraints.merge(caseConstraints)
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    // =============================================================================
    // Scoped Expression Handlers
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
    // Variable Declaration Processing
    // =============================================================================
    
    /**
     * Process variable declarations with type inference and constraint generation.
     */
    fun processVariableDeclaration(
        name: String,
        declaredType: Type?,
        initializer: Expression,
        context: InferenceContext,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        // Collect constraints from initializer
        val initResult = if (declaredType != null) {
            // Check initializer against declared type
            collector.collectConstraintsWithExpected(initializer, declaredType, context)
        } else {
            // Synthesize type from initializer
            collector.collectConstraints(initializer, context)
        }
        
        // Determine variable type
        val variableType = declaredType ?: initResult.type
        
        // Add constraint if both declared type and inferred type exist
        val typeConstraints = if (declaredType != null) {
            val constraint = Constraint.Equality(initResult.type, declaredType, location)
            initResult.constraints.add(constraint)
        } else {
            initResult.constraints
        }
        
        return Pair(variableType, typeConstraints)
    }
    
    /**
     * Process function parameter declarations with constraint generation.
     */
    fun processFunctionParameters(
        parameters: List<Parameter>,
        context: InferenceContext
    ): Pair<List<Type>, ConstraintSet> {
        val paramTypes = mutableListOf<Type>()
        var constraints = ConstraintSet.empty()
        
        for (parameter in parameters) {
            val paramType = parameter.type ?: run {
                // Create fresh type variable for untyped parameters
                val freshVar = TypeVar.fresh()
                Type.NamedType(freshVar.id)
            }
            
            paramTypes.add(paramType)
            
            // Add any constraints from parameter type annotations
            if (parameter.type != null) {
                // Parameter has explicit type - no additional constraints needed
                // unless we want to validate the type annotation itself
            }
        }
        
        return Pair(paramTypes, constraints)
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    /**
     * Generate constraints for constructor arguments.
     */
    private fun generateConstructorArgumentConstraints(
        fieldTypes: List<Type>,
        argumentTypes: List<Type>,
        typeParameters: List<String>,
        typeArguments: PersistentList<Type>,
        location: SourceLocation?
    ): ConstraintSet {
        val substitution = if (typeParameters.isNotEmpty() && typeArguments.isNotEmpty()) {
            typeParameters.zip(typeArguments).toMap()
        } else {
            emptyMap()
        }
        
        val constraints = fieldTypes.zip(argumentTypes).map { (fieldType, argType) ->
            val expectedType = if (substitution.isNotEmpty()) {
                collector.substituteTypeVariables(fieldType, substitution)
            } else {
                fieldType
            }
            Constraint.Subtype(argType, expectedType, location)
        }
        
        return ConstraintSet.fromCollection(constraints)
    }
    
    /**
     * Generate type arguments from constructor arguments for generic types.
     */
    private fun generateTypeArgumentsFromArguments(
        typeParameters: List<String>,
        fieldTypes: List<Type>,
        argumentTypes: List<Type>
    ): PersistentList<Type> {
        // Simple type argument inference based on argument types
        // In a full implementation, this would be more sophisticated
        val substitution = mutableMapOf<String, Type>()
        
        fieldTypes.zip(argumentTypes).forEach { (fieldType, argType) ->
            inferTypeSubstitution(fieldType, argType, substitution)
        }
        
        return typeParameters.map { param ->
            substitution[param] ?: run {
                val freshVar = TypeVar.fresh()
                Type.NamedType(freshVar.id)
            }
        }.toPersistentList()
    }
    
    /**
     * Infer type parameter substitutions from field type and argument type.
     */
    private fun inferTypeSubstitution(
        fieldType: Type,
        argType: Type,
        substitution: MutableMap<String, Type>
    ) {
        when (fieldType) {
            is Type.NamedType -> {
                // If field type is a single-letter uppercase name, it's likely a type parameter
                if (fieldType.name.length == 1 && fieldType.name[0].isUpperCase()) {
                    substitution[fieldType.name] = argType
                }
            }
            // Add more sophisticated matching for complex types as needed
            else -> {
                // For now, skip complex type matching
            }
        }
    }
    
    /**
     * Find constructor definition in union types.
     */
    private fun findConstructorDefinition(
        constructorName: String, 
        context: InferenceContext
    ): Pair<TypeDefinition.UnionTypeDef?, String?> {
        // Search through all union type definitions
        for ((typeName, typeDef) in context.typeDefinitions.entries) {
            if (typeDef is TypeDefinition.UnionTypeDef) {
                val hasVariant = typeDef.variants.any { it.name == constructorName }
                if (hasVariant) {
                    return Pair(typeDef, typeName)
                }
            }
        }
        
        // Check parent scopes
        return context.parent?.let { parent ->
            findConstructorDefinition(constructorName, parent)
        } ?: Pair(null, null)
    }
    
    /**
     * Analyze scope binding requirements for complex expressions.
     * Used to determine optimal scoping strategies for nested constructs.
     */
    fun analyzeScopeRequirements(expression: Expression): ScopeAnalysis {
        val bindsVariables = when (expression) {
            is BlockExpression -> expression.statements.any { it is ValDecl || it is VarDecl }
            is LambdaExpression -> expression.parameters.isNotEmpty()
            is ForExpression -> true // Always binds loop variable
            is MatchExpression -> expression.cases.any { case ->
                patternVisitor.extractBindings(case.pattern).isNotEmpty()
            }
            else -> false
        }
        
        val requiresNewScope = bindsVariables
        
        return ScopeAnalysis(
            bindsVariables = bindsVariables,
            requiresNewScope = requiresNewScope
        )
    }
}

/**
 * Analysis result for scope requirements in expressions.
 */
data class ScopeAnalysis(
    val bindsVariables: Boolean,
    val requiresNewScope: Boolean
)