package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Result of constraint collection containing the inferred type and generated constraints.
 * This represents the output of processing an expression during type inference.
 */
data class ConstraintResult(
    /**
     * The inferred type for the expression.
     * This may be a concrete type or a fresh type variable.
     */
    val type: Type,
    
    /**
     * The set of constraints generated while processing the expression.
     * These constraints must be satisfied for type inference to succeed.
     */
    val constraints: ConstraintSet
) {
    companion object {
        /**
         * Create a constraint result with no constraints (just a type).
         * Used for expressions that don't generate additional constraints.
         */
        fun withType(type: Type): ConstraintResult {
            return ConstraintResult(type, ConstraintSet.empty())
        }
        
        /**
         * Create a constraint result with a type and single constraint.
         */
        fun withConstraint(type: Type, constraint: Constraint): ConstraintResult {
            return ConstraintResult(type, ConstraintSet.of(constraint))
        }
    }
    
    /**
     * Add additional constraints to this result, returning a new result.
     */
    fun addConstraints(newConstraints: ConstraintSet): ConstraintResult {
        return copy(constraints = constraints.merge(newConstraints))
    }
    
    /**
     * Add a single constraint to this result, returning a new result.
     */
    fun addConstraint(constraint: Constraint): ConstraintResult {
        return copy(constraints = constraints.add(constraint))
    }
}

/**
 * Constraint collector that traverses AST expressions and generates type constraints.
 * 
 * This collector implements bidirectional type checking:
 * - Synthesis mode: infer the type of an expression
 * - Checking mode: check that an expression has a specific expected type
 * 
 * The collector generates three types of constraints:
 * - Equality constraints: type1 ~ type2 (types must be equal)
 * - Subtype constraints: type1 <: type2 (type1 is a subtype of type2)  
 * - Instance constraints: typeVar ∈ scheme (type variable is instance of scheme)
 * 
 * Key features:
 * - Fresh type variable generation for unknown types
 * - Scope-aware variable lookup with let-polymorphism support
 * - Pattern matching with exhaustiveness constraint generation
 * - Integration with existing TypeChecker infrastructure
 */
class ConstraintCollector {
    
    /**
     * Collect constraints from an expression in synthesis mode.
     * Infers the type of the expression and generates necessary constraints.
     * 
     * @param expression The expression to analyze
     * @param context The inference context containing variable bindings and type information
     * @return A constraint result with the inferred type and generated constraints
     */
    fun collectConstraints(expression: Expression, context: InferenceContext): ConstraintResult {
        return collectConstraintsWithExpected(expression, null, context)
    }
    
    /**
     * Collect constraints from an expression with an expected type (checking mode).
     * Checks that the expression can have the expected type and generates constraints.
     * 
     * @param expression The expression to analyze
     * @param expectedType The expected type for the expression (null for synthesis mode)
     * @param context The inference context containing variable bindings and type information
     * @return A constraint result with the inferred type and generated constraints
     */
    fun collectConstraintsWithExpected(
        expression: Expression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        return when (expression) {
            // Literals have known concrete types
            is Literal.IntLiteral -> handleIntLiteral(expression, expectedType, context)
            is Literal.FloatLiteral -> handleFloatLiteral(expression, expectedType, context)
            is Literal.StringLiteral -> handleStringLiteral(expression, expectedType, context)
            is Literal.BooleanLiteral -> handleBooleanLiteral(expression, expectedType, context)
            is Literal.NullLiteral -> handleNullLiteral(expression, expectedType, context)
            is Literal.TupleLiteral -> handleTupleLiteral(expression, expectedType, context)
            
            // Variables require lookup in context
            is Identifier -> handleIdentifier(expression, expectedType, context)
            
            // Operations generate constraints between operand types
            is BinaryOp -> handleBinaryOp(expression, expectedType, context)
            is UnaryOp -> handleUnaryOp(expression, expectedType, context)
            
            // Function and constructor calls need argument type checking
            is FunctionCall -> handleFunctionCall(expression, expectedType, context)
            is ConstructorCall -> handleConstructorCall(expression, expectedType, context)
            
            // Property and index access
            is PropertyAccess -> handlePropertyAccess(expression, expectedType, context)
            is IndexAccess -> handleIndexAccess(expression, expectedType, context)
            
            // Control flow expressions
            is IfExpression -> handleIfExpression(expression, expectedType, context)
            is WhileExpression -> handleWhileExpression(expression, expectedType, context)
            is MatchExpression -> handleMatchExpression(expression, expectedType, context)
            
            // Scoped expressions
            is BlockExpression -> handleBlockExpression(expression, expectedType, context)
            is LambdaExpression -> handleLambdaExpression(expression, expectedType, context)
            
            // Collection and iteration
            is ForExpression -> handleForExpression(expression, expectedType, context)
        }
    }
    
    // =============================================================================
    // Literal Expression Handlers
    // =============================================================================
    
    private fun handleIntLiteral(
        literal: Literal.IntLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.INT
        
        return if (expectedType != null) {
            // Checking mode: generate equality constraint
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            // Synthesis mode: just return the literal type
            ConstraintResult.withType(literalType)
        }
    }
    
    private fun handleFloatLiteral(
        literal: Literal.FloatLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.DOUBLE
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    private fun handleStringLiteral(
        literal: Literal.StringLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.STRING
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    private fun handleBooleanLiteral(
        literal: Literal.BooleanLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.BOOLEAN
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    private fun handleNullLiteral(
        literal: Literal.NullLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        return if (expectedType != null) {
            // Null can inhabit any nullable type, so we need to ensure expected type is nullable
            val nullableConstraintType = when (expectedType) {
                is Type.NullableType -> expectedType.baseType
                else -> {
                    // Create a fresh type variable for the base type
                    val freshVar = TypeVar.fresh()
                    val baseType = Type.NamedType(freshVar.id)  // Convert TypeVar to Type representation
                    
                    // Generate constraint that expected type equals nullable version of base type
                    val nullableExpected = Type.NullableType(baseType)
                    val constraint = Constraint.Equality(expectedType, nullableExpected, literal.sourceLocation)
                    
                    return ConstraintResult.withConstraint(nullableExpected, constraint)
                }
            }
            val nullableType = Type.NullableType(nullableConstraintType)
            val constraint = Constraint.Equality(nullableType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(nullableType, constraint)
        } else {
            // In synthesis mode, create a nullable type with fresh type variable
            val freshVar = TypeVar.fresh()
            val baseType = Type.NamedType(freshVar.id)
            val nullableType = Type.NullableType(baseType)
            ConstraintResult.withType(nullableType)
        }
    }
    
    private fun handleTupleLiteral(
        literal: Literal.TupleLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from all tuple elements
        val elementResults = literal.elements.map { element ->
            collectConstraints(element, context)
        }
        
        // Merge all constraints from elements
        val allConstraints = elementResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        // Extract element types
        val elementTypes = elementResults.map { it.type }.toPersistentList()
        val tupleType = Type.TupleType(elementTypes)
        
        return if (expectedType != null) {
            // Generate equality constraint with expected type
            val constraint = Constraint.Equality(tupleType, expectedType, literal.sourceLocation)
            ConstraintResult(tupleType, allConstraints.add(constraint))
        } else {
            ConstraintResult(tupleType, allConstraints)
        }
    }
    
    // =============================================================================
    // Variable and Identifier Handlers
    // =============================================================================
    
    private fun handleIdentifier(
        identifier: Identifier,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Look up the variable in the context
        val scheme = context.lookupVariable(identifier.name)
        
        if (scheme != null) {
            // Variable found - instantiate its type scheme
            val (instanceType, instanceConstraints) = instantiateScheme(scheme)
            
            return if (expectedType != null) {
                // Checking mode: add equality constraint
                val constraint = Constraint.Equality(instanceType, expectedType, identifier.sourceLocation)
                ConstraintResult(instanceType, instanceConstraints.add(constraint))
            } else {
                // Synthesis mode: return instantiated type
                ConstraintResult(instanceType, instanceConstraints)
            }
        } else {
            // Variable not found - check if it's a zero-argument constructor
            val functionSig = context.lookupFunctionSignature(identifier.name)
            
            if (functionSig != null && functionSig.parameterTypes.isEmpty()) {
                // Treat as constructor call with no arguments
                val constructorCall = ConstructorCall(
                    constructor = identifier.name,
                    arguments = persistentListOf(),
                    sourceLocation = identifier.sourceLocation
                )
                return handleConstructorCall(constructorCall, expectedType, context)
            } else {
                // Unresolved symbol - create a fresh type variable and add error constraint
                val freshVar = TypeVar.fresh()
                val unknownType = Type.NamedType(freshVar.id)
                
                // TODO: In a full implementation, we'd collect type errors separately
                // For now, we'll continue with type inference using the fresh variable
                
                return if (expectedType != null) {
                    val constraint = Constraint.Equality(unknownType, expectedType, identifier.sourceLocation)
                    ConstraintResult.withConstraint(unknownType, constraint)
                } else {
                    ConstraintResult.withType(unknownType)
                }
            }
        }
    }
    
    // =============================================================================
    // Binary and Unary Operation Handlers
    // =============================================================================
    
    private fun handleBinaryOp(
        binaryOp: BinaryOp,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from left and right operands
        val leftResult = collectConstraints(binaryOp.left, context)
        val rightResult = collectConstraints(binaryOp.right, context)
        
        // Merge operand constraints
        val baseConstraints = leftResult.constraints.merge(rightResult.constraints)
        
        // Generate additional constraints based on operator type
        val (resultType, operatorConstraints) = generateBinaryOpConstraints(
            binaryOp.operator,
            leftResult.type,
            rightResult.type,
            binaryOp.sourceLocation
        )
        
        val allConstraints = baseConstraints.merge(operatorConstraints)
        
        return if (expectedType != null) {
            // Add equality constraint with expected type
            val constraint = Constraint.Equality(resultType, expectedType, binaryOp.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    private fun handleUnaryOp(
        unaryOp: UnaryOp,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from operand
        val operandResult = collectConstraints(unaryOp.operand, context)
        
        // Generate additional constraints based on operator type
        val (resultType, operatorConstraints) = generateUnaryOpConstraints(
            unaryOp.operator,
            operandResult.type,
            unaryOp.sourceLocation
        )
        
        val allConstraints = operandResult.constraints.merge(operatorConstraints)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, unaryOp.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    // =============================================================================
    // Function and Constructor Call Handlers
    // =============================================================================
    
    private fun handleFunctionCall(
        call: FunctionCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Extract function name from target (assuming it's an Identifier for now)
        val functionName = when (call.target) {
            is Identifier -> call.target.name
            else -> {
                // For complex expressions, we'd need to handle them differently
                // For now, create a fresh type and continue
                val freshVar = TypeVar.fresh()
                return ConstraintResult.withType(Type.NamedType(freshVar.id))
            }
        }
        
        // Look up function signature
        val functionSignature = context.lookupFunctionSignature(functionName)
        
        if (functionSignature != null) {
            return handleTypedFunctionCall(call, functionSignature, expectedType, context)
        } else {
            // Unknown function - create fresh type variables and generate constraints
            return handleUnknownFunctionCall(call, expectedType, context)
        }
    }
    
    private fun handleTypedFunctionCall(
        call: FunctionCall,
        signature: FunctionSignature,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from arguments
        val argumentResults = call.arguments.map { arg ->
            collectConstraints(arg, context)
        }
        
        // Merge argument constraints
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        // Generate type constraints for function application
        val (resultType, callConstraints) = generateFunctionCallConstraints(
            signature,
            argumentResults.map { it.type },
            call.sourceLocation
        )
        
        val allConstraints = baseConstraints.merge(callConstraints)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, call.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    private fun handleUnknownFunctionCall(
        call: FunctionCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from arguments
        val argumentResults = call.arguments.map { arg ->
            collectConstraints(arg, context)
        }
        
        // Create fresh type variables for function signature
        val paramTypes = argumentResults.map { 
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        val returnType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Generate equality constraints between argument types and parameter types
        val argumentConstraints = argumentResults.zip(paramTypes).map { (argResult, paramType) ->
            Constraint.Equality(argResult.type, paramType, call.sourceLocation)
        }
        
        // Merge all constraints
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        val allConstraints = argumentConstraints.fold(baseConstraints) { acc, constraint ->
            acc.add(constraint)
        }
        
        return ConstraintResult(returnType, allConstraints)
    }
    
    private fun handleConstructorCall(
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
            collectConstraints(arg, context)
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
            collectConstraints(arg, context)
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
    // Property and Index Access Handlers
    // =============================================================================
    
    private fun handlePropertyAccess(
        access: PropertyAccess,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target expression
        val targetResult = collectConstraints(access.target, context)
        
        // For now, create fresh type variable for property access result
        // In a full implementation, this would look up field types from type definitions
        val freshVar = TypeVar.fresh()
        val resultType = Type.NamedType(freshVar.id)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, access.sourceLocation)
            ConstraintResult(resultType, targetResult.constraints.add(constraint))
        } else {
            ConstraintResult(resultType, targetResult.constraints)
        }
    }
    
    private fun handleIndexAccess(
        access: IndexAccess,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target and index expressions
        val targetResult = collectConstraints(access.target, context)
        val indexResult = collectConstraints(access.index, context)
        
        val baseConstraints = targetResult.constraints.merge(indexResult.constraints)
        
        // Create fresh type variables for indexing operation
        val elementType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Generate constraint that target is indexable with the index type
        // For now, we'll assume integer indexing into collections
        val indexConstraint = Constraint.Equality(indexResult.type, BuiltinTypes.INT, access.sourceLocation)
        
        val allConstraints = baseConstraints.add(indexConstraint)
        
        return ConstraintResult(elementType, allConstraints)
    }
    
    // =============================================================================
    // Control Flow Expression Handlers
    // =============================================================================
    
    private fun handleIfExpression(
        ifExpr: IfExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from condition (must be Boolean)
        val conditionResult = collectConstraintsWithExpected(
            ifExpr.condition, 
            BuiltinTypes.BOOLEAN, 
            context
        )
        
        // Collect constraints from branches
        val thenResult = collectConstraintsWithExpected(ifExpr.thenExpression, expectedType, context)
        
        val elseResult = if (ifExpr.elseExpression != null) {
            collectConstraintsWithExpected(ifExpr.elseExpression, expectedType, context)
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
                if (typesAreEqual(thenResult.type, elseResult.type)) {
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
    
    private fun handleWhileExpression(
        whileExpr: WhileExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from condition (must be Boolean)
        val conditionResult = collectConstraintsWithExpected(
            whileExpr.condition,
            BuiltinTypes.BOOLEAN,
            context
        )
        
        // Collect constraints from body - while loop body executes 0 or more times
        // so the result of the body is not directly the result of the while expression
        val bodyResult = collectConstraints(whileExpr.body, context)
        
        // Merge all constraints
        val allConstraints = conditionResult.constraints
            .merge(bodyResult.constraints)
        
        // While loop result type is typically Unit in most languages
        // unless specified otherwise by expected type
        val resultType = expectedType ?: BuiltinTypes.UNIT
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    private fun handleMatchExpression(
        matchExpr: MatchExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target expression
        val targetResult = collectConstraints(matchExpr.target, context)
        
        // Process each match case
        val caseResults = matchExpr.cases.map { case ->
            handleMatchCase(case, targetResult.type, expectedType, context)
        }
        
        // Merge all constraints
        val allConstraints = caseResults.fold(targetResult.constraints) { acc, caseResult ->
            acc.merge(caseResult.constraints)
        }
        
        // Determine unified result type
        val resultType = if (expectedType != null) {
            expectedType
        } else if (caseResults.isNotEmpty()) {
            // Check if all cases have the same type
            val firstCaseType = caseResults.first().type
            val allCasesSameType = caseResults.all { typesAreEqual(it.type, firstCaseType) }
            
            if (allCasesSameType) {
                firstCaseType
            } else {
                // Create fresh type variable and add equality constraints for all cases
                val freshVar = TypeVar.fresh()
                val unifiedType = Type.NamedType(freshVar.id)
                
                val caseConstraints = caseResults.mapIndexed { index, caseResult ->
                    Constraint.Equality(
                        caseResult.type, 
                        unifiedType, 
                        matchExpr.cases[index].expression.sourceLocation
                    )
                }
                
                val finalConstraints = caseConstraints.fold(allConstraints) { acc, constraint ->
                    acc.add(constraint)
                }
                
                return ConstraintResult(unifiedType, finalConstraints)
            }
        } else {
            BuiltinTypes.UNIT
        }
        
        // TODO: Add exhaustiveness checking constraints
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    private fun handleMatchCase(
        case: MatchCase,
        targetType: Type,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Process pattern and get variable bindings
        val (patternConstraints, bindings) = processPattern(case.pattern, targetType, context)
        
        // Create new scope with pattern bindings
        val caseContext = context.enterScopeWith(bindings)
        
        // Collect constraints from case expression
        val exprResult = collectConstraintsWithExpected(case.expression, expectedType, caseContext)
        
        return ConstraintResult(
            exprResult.type,
            patternConstraints.merge(exprResult.constraints)
        )
    }
    
    // =============================================================================
    // Scoped Expression Handlers
    // =============================================================================
    
    private fun handleBlockExpression(
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
                    val initResult = collectConstraints(statement.initializer, blockContext)
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
                    val exprResult = collectConstraints(statement, blockContext)
                    allConstraints = allConstraints.merge(exprResult.constraints)
                }
                
                else -> {
                    // Other statement types - skip for now
                }
            }
        }
        
        // Process final expression if present
        val resultType = if (blockExpr.expression != null) {
            val exprResult = collectConstraintsWithExpected(blockExpr.expression, expectedType, blockContext)
            allConstraints = allConstraints.merge(exprResult.constraints)
            exprResult.type
        } else {
            BuiltinTypes.UNIT
        }
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    private fun handleLambdaExpression(
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
        val bodyResult = collectConstraints(lambda.body, lambdaContext)
        
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
    
    private fun handleForExpression(
        forExpr: ForExpression,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from iterable expression
        val iterableResult = collectConstraints(forExpr.iterable, context)
        
        // Create fresh type variables for element type and result type
        val elementVar = TypeVar.fresh()
        val elementType = Type.NamedType(elementVar.id)
        
        // Generate constraint that iterable contains elements of element type
        // This would be more sophisticated in a full implementation with proper collection types
        
        // Create new scope with loop variable binding
        val loopContext = context.enterScopeWith(mapOf(forExpr.variable to elementType))
        
        // Collect constraints from loop body
        val bodyResult = collectConstraints(forExpr.body, loopContext)
        
        // For expression result type is typically a collection of body results
        val resultType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        val allConstraints = iterableResult.constraints.merge(bodyResult.constraints)
        
        return ConstraintResult(resultType, allConstraints)
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    /**
     * Check if a type is a numeric type (INT or DOUBLE).
     */
    private fun isNumericType(type: Type): Boolean {
        return type == BuiltinTypes.INT || type == BuiltinTypes.DOUBLE
    }
    
    /**
     * Promote a type to appropriate numeric type for arithmetic operations.
     * INT -> DOUBLE for arithmetic promotion
     * DOUBLE -> DOUBLE (already promoted)
     * Other types -> Original type (will generate constraints that may fail)
     */
    private fun promoteToNumericType(type: Type): Type {
        return when (type) {
            BuiltinTypes.INT -> BuiltinTypes.DOUBLE
            BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
            else -> type // Keep original type, constraint solving will handle compatibility
        }
    }
    
    /**
     * Check if two types are structurally equal (ignoring source locations).
     */
    private fun typesAreEqual(type1: Type, type2: Type): Boolean {
        return when {
            type1 is Type.PrimitiveType && type2 is Type.PrimitiveType -> 
                type1.name == type2.name
            type1 is Type.NamedType && type2 is Type.NamedType -> 
                type1.name == type2.name
            type1 is Type.GenericType && type2 is Type.GenericType -> 
                type1.name == type2.name && type1.arguments.size == type2.arguments.size &&
                type1.arguments.zip(type2.arguments).all { (a1, a2) -> typesAreEqual(a1, a2) }
            type1 is Type.TupleType && type2 is Type.TupleType -> 
                type1.elementTypes.size == type2.elementTypes.size &&
                type1.elementTypes.zip(type2.elementTypes).all { (t1, t2) -> typesAreEqual(t1, t2) }
            type1 is Type.NullableType && type2 is Type.NullableType ->
                typesAreEqual(type1.baseType, type2.baseType)
            type1 is Type.UnionType && type2 is Type.UnionType ->
                type1.name == type2.name && type1.typeArguments.size == type2.typeArguments.size &&
                type1.typeArguments.zip(type2.typeArguments).all { (a1, a2) -> typesAreEqual(a1, a2) }
            type1 is Type.FunctionType && type2 is Type.FunctionType ->
                typesAreEqual(type1.returnType, type2.returnType) &&
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (p1, p2) -> typesAreEqual(p1, p2) }
            else -> type1 == type2
        }
    }
    
    /**
     * Instantiate a type scheme by creating fresh type variables for quantified variables.
     * Returns the instantiated type and any instance constraints generated.
     */
    private fun instantiateScheme(scheme: TypeScheme): Pair<Type, ConstraintSet> {
        if (scheme.isMonomorphic()) {
            // No quantified variables - return type as-is
            return Pair(scheme.type, ConstraintSet.empty())
        }
        
        // Create fresh type variables for each quantified variable
        val substitution = scheme.quantifiedVars.associateWith { 
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Substitute quantified variables in the type
        val instantiatedType = substituteTypeVarsWithMapping(scheme.type, substitution)
        
        // Generate instance constraints
        val constraints = scheme.quantifiedVars.map { quantVar ->
            val freshType = substitution[quantVar]!!
            // For now, we'll create a trivial instance constraint
            // In a full implementation, this would be more sophisticated
            Constraint.Instance(quantVar, scheme, null)
        }
        
        return Pair(instantiatedType, ConstraintSet.fromCollection(constraints))
    }
    
    /**
     * Generate constraints for binary operations based on operator type.
     */
    private fun generateBinaryOpConstraints(
        operator: BinaryOperator,
        leftType: Type,
        rightType: Type,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        return when (operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS, 
            BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, BinaryOperator.MODULO -> {
                // Arithmetic operations: require numeric types and return DOUBLE
                // We generate explicit numeric type constraints for operands
                val constraints = mutableListOf<Constraint>()
                
                // Always generate subtype constraints for consistent behavior
                constraints.add(Constraint.Subtype(leftType, BuiltinTypes.DOUBLE, location))
                constraints.add(Constraint.Subtype(rightType, BuiltinTypes.DOUBLE, location))
                
                val resultType = BuiltinTypes.DOUBLE // All arithmetic results are DOUBLE
                Pair(resultType, ConstraintSet.fromCollection(constraints))
            }
            
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL -> {
                // Comparison operations: require numeric types, result is Boolean
                val constraints = mutableListOf<Constraint>()
                
                // Always generate subtype constraints for consistent behavior
                constraints.add(Constraint.Subtype(leftType, BuiltinTypes.DOUBLE, location))
                constraints.add(Constraint.Subtype(rightType, BuiltinTypes.DOUBLE, location))
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.fromCollection(constraints))
            }
            
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                // Equality operations: operands should be same type, result is Boolean
                val equalityConstraint = Constraint.Equality(leftType, rightType, location)
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(equalityConstraint))
            }
            
            BinaryOperator.AND, BinaryOperator.OR -> {
                // Logical operations: operands should be Boolean, result is Boolean
                val boolConstraint1 = Constraint.Equality(leftType, BuiltinTypes.BOOLEAN, location)
                val boolConstraint2 = Constraint.Equality(rightType, BuiltinTypes.BOOLEAN, location)
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(boolConstraint1, boolConstraint2))
            }
            
            BinaryOperator.NULL_COALESCING -> {
                // Null coalescing: left should be nullable, result is right type
                val freshVar = TypeVar.fresh()
                val baseType = Type.NamedType(freshVar.id)
                val nullableType = Type.NullableType(baseType)
                val nullableConstraint = Constraint.Equality(leftType, nullableType, location)
                
                Pair(rightType, ConstraintSet.of(nullableConstraint))
            }
        }
    }
    
    /**
     * Generate constraints for unary operations based on operator type.
     */
    private fun generateUnaryOpConstraints(
        operator: UnaryOperator,
        operandType: Type,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        return when (operator) {
            UnaryOperator.MINUS -> {
                // Numeric negation: operand should be numeric, result is same type
                val numericConstraint = Constraint.Subtype(operandType, BuiltinTypes.DOUBLE, location)
                Pair(operandType, ConstraintSet.of(numericConstraint))
            }
            
            UnaryOperator.NOT -> {
                // Logical negation: operand should be Boolean, result is Boolean
                val boolConstraint = Constraint.Equality(operandType, BuiltinTypes.BOOLEAN, location)
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(boolConstraint))
            }
        }
    }
    
    /**
     * Generate constraints for function calls with known signatures.
     */
    private fun generateFunctionCallConstraints(
        signature: FunctionSignature,
        argumentTypes: List<Type>,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        // Generate constraints for each argument
        val constraints = signature.parameterTypes.zip(argumentTypes).map { (paramType, argType) ->
            Constraint.Subtype(argType, paramType, location)
        }
        
        // Handle generic functions by instantiating type parameters
        val resultType = if (signature.typeParameters.isNotEmpty()) {
            // Create fresh type variables for type parameters
            val typeVarSubstitution = signature.typeParameters.associateWith {
                val freshVar = TypeVar.fresh()
                Type.NamedType(freshVar.id)
            }
            
            // Substitute type parameters in return type
            substituteTypeVariables(signature.returnType, typeVarSubstitution)
        } else {
            signature.returnType
        }
        
        return Pair(resultType, ConstraintSet.fromCollection(constraints))
    }
    
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
                substituteTypeVariables(fieldType, substitution)
            } else {
                fieldType
            }
            Constraint.Subtype(argType, expectedType, location)
        }
        
        return ConstraintSet.fromCollection(constraints)
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
     * Process a pattern and return constraints and variable bindings.
     */
    private fun processPattern(
        pattern: Pattern,
        targetType: Type,
        context: InferenceContext
    ): Pair<ConstraintSet, Map<String, Type>> {
        return when (pattern) {
            is Pattern.WildcardPattern -> {
                // Wildcard matches anything
                Pair(ConstraintSet.empty(), emptyMap())
            }
            
            is Pattern.IdentifierPattern -> {
                // Identifier pattern binds the target to a variable
                Pair(ConstraintSet.empty(), mapOf(pattern.name to targetType))
            }
            
            is Pattern.LiteralPattern -> {
                // Literal pattern must match the target type
                val literalResult = collectConstraints(pattern.literal, context)
                val constraint = Constraint.Equality(literalResult.type, targetType, pattern.sourceLocation)
                Pair(ConstraintSet.of(constraint), emptyMap())
            }
            
            is Pattern.ConstructorPattern -> {
                // Constructor pattern matches a specific variant
                val (constraints, bindings) = processConstructorPattern(pattern, targetType, context)
                Pair(constraints, bindings)
            }
            
            is Pattern.GuardPattern -> {
                // Process inner pattern first, then add guard constraint
                val (innerConstraints, innerBindings) = processPattern(pattern.pattern, targetType, context)
                
                // Guard must be Boolean
                val guardContext = context.enterScopeWith(innerBindings)
                val guardResult = collectConstraintsWithExpected(
                    pattern.guard, 
                    BuiltinTypes.BOOLEAN, 
                    guardContext
                )
                
                val allConstraints = innerConstraints.merge(guardResult.constraints)
                Pair(allConstraints, innerBindings)
            }
        }
    }
    
    /**
     * Process constructor patterns with nested patterns.
     */
    private fun processConstructorPattern(
        pattern: Pattern.ConstructorPattern,
        targetType: Type,
        context: InferenceContext
    ): Pair<ConstraintSet, Map<String, Type>> {
        // Find constructor definition
        val (typeDef, unionTypeName) = findConstructorDefinition(pattern.constructor, context)
        
        if (typeDef == null || unionTypeName == null) {
            // Unknown constructor - create minimal constraints
            return Pair(ConstraintSet.empty(), emptyMap())
        }
        
        val variant = typeDef.variants.find { it.name == pattern.constructor }!!
        
        // Process nested patterns
        val nestedResults = pattern.patterns.zip(variant.fields).map { (nestedPattern, fieldType) ->
            processPattern(nestedPattern, fieldType, context)
        }
        
        // Merge constraints and bindings from nested patterns
        val allConstraints = nestedResults.fold(ConstraintSet.empty()) { acc, (constraints, _) ->
            acc.merge(constraints)
        }
        
        val allBindings = nestedResults.fold(emptyMap<String, Type>()) { acc, (_, bindings) ->
            acc + bindings
        }
        
        // Add constraint that target type matches constructor type
        val constructorType = Type.UnionType(unionTypeName, persistentListOf())
        val typeConstraint = Constraint.Equality(targetType, constructorType, pattern.sourceLocation)
        
        return Pair(allConstraints.add(typeConstraint), allBindings)
    }
    
    /**
     * Substitute type variables in a type using the given substitution map.
     */
    private fun substituteTypeVariables(type: Type, substitution: Map<String, Type>): Type {
        return when (type) {
            is Type.NamedType -> {
                substitution[type.name] ?: type
            }
            is Type.TypeVar -> {
                substitution[type.id] ?: type
            }
            is Type.GenericType -> {
                Type.GenericType(
                    name = type.name,
                    arguments = type.arguments.map { substituteTypeVariables(it, substitution) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.UnionType -> {
                Type.UnionType(
                    name = type.name,
                    typeArguments = type.typeArguments.map { substituteTypeVariables(it, substitution) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.NullableType -> {
                Type.NullableType(
                    baseType = substituteTypeVariables(type.baseType, substitution),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.TupleType -> {
                Type.TupleType(
                    elementTypes = type.elementTypes.map { substituteTypeVariables(it, substitution) }.toPersistentList(),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.FunctionType -> {
                Type.FunctionType(
                    parameterTypes = type.parameterTypes.map { substituteTypeVariables(it, substitution) }.toPersistentList(),
                    returnType = substituteTypeVariables(type.returnType, substitution),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.PrimitiveType -> type
        }
    }
    
    /**
     * Substitute type variables using TypeVar to Type mapping.
     */
    private fun substituteTypeVarsWithMapping(type: Type, substitution: Map<TypeVar, Type>): Type {
        // Convert TypeVar -> Type mapping to String -> Type mapping
        val stringSubstitution = substitution.mapKeys { (typeVar, _) -> typeVar.id }
        return substituteTypeVariables(type, stringSubstitution)
    }
}