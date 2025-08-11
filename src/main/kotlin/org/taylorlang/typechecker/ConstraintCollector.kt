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
 * Main constraint collector that coordinates specialized visitors for constraint generation.
 * 
 * This collector implements bidirectional type checking using a visitor-based architecture:
 * - Synthesis mode: infer the type of an expression
 * - Checking mode: check that an expression has a specific expected type
 * 
 * Architecture:
 * - ExpressionConstraintVisitor: handles literals, operations, function calls, property access
 * - PatternConstraintVisitor: handles pattern matching and exhaustiveness checking
 * - StatementConstraintVisitor: handles blocks, lambdas, control flow, constructors
 * - ConstraintCollector: coordinates visitors and provides shared utilities
 * 
 * The collector generates three types of constraints:
 * - Equality constraints: type1 ~ type2 (types must be equal)
 * - Subtype constraints: type1 <: type2 (type1 is a subtype of type2)  
 * - Instance constraints: typeVar ∈ scheme (type variable is instance of scheme)
 * 
 * Key features:
 * - Modular visitor-based architecture for maintainability
 * - Fresh type variable generation for unknown types
 * - Scope-aware variable lookup with let-polymorphism support
 * - Pattern matching with exhaustiveness constraint generation
 * - Integration with existing TypeChecker infrastructure
 */
class ConstraintCollector {
    
    // =============================================================================
    // Specialized Visitors
    // =============================================================================
    
    private val expressionVisitor: ExpressionConstraintVisitor by lazy {
        ExpressionConstraintVisitor(this)
    }
    
    private val patternVisitor: PatternConstraintVisitor by lazy {
        PatternConstraintVisitor(this)
    }
    
    private val statementVisitor: StatementConstraintVisitor by lazy {
        StatementConstraintVisitor(this, patternVisitor)
    }
    
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
            // Literals - delegate to expression visitor
            is Literal.IntLiteral -> expressionVisitor.handleIntLiteral(expression, expectedType, context)
            is Literal.FloatLiteral -> expressionVisitor.handleFloatLiteral(expression, expectedType, context)
            is Literal.StringLiteral -> expressionVisitor.handleStringLiteral(expression, expectedType, context)
            is Literal.BooleanLiteral -> expressionVisitor.handleBooleanLiteral(expression, expectedType, context)
            is Literal.NullLiteral -> expressionVisitor.handleNullLiteral(expression, expectedType, context)
            is Literal.TupleLiteral -> expressionVisitor.handleTupleLiteral(expression, expectedType, context)
            
            // Variables and operations - delegate to expression visitor
            is Identifier -> expressionVisitor.handleIdentifier(expression, expectedType, context)
            is BinaryOp -> expressionVisitor.handleBinaryOp(expression, expectedType, context)
            is UnaryOp -> expressionVisitor.handleUnaryOp(expression, expectedType, context)
            is FunctionCall -> expressionVisitor.handleFunctionCall(expression, expectedType, context)
            is PropertyAccess -> expressionVisitor.handlePropertyAccess(expression, expectedType, context)
            is IndexAccess -> expressionVisitor.handleIndexAccess(expression, expectedType, context)
            
            // Constructor calls and control flow - delegate to statement visitor
            is ConstructorCall -> statementVisitor.handleConstructorCall(expression, expectedType, context)
            is IfExpression -> statementVisitor.handleIfExpression(expression, expectedType, context)
            is WhileExpression -> statementVisitor.handleWhileExpression(expression, expectedType, context)
            is MatchExpression -> statementVisitor.handleMatchExpression(expression, expectedType, context)
            
            // Scoped expressions - delegate to statement visitor
            is BlockExpression -> statementVisitor.handleBlockExpression(expression, expectedType, context)
            is LambdaExpression -> statementVisitor.handleLambdaExpression(expression, expectedType, context)
            is ForExpression -> statementVisitor.handleForExpression(expression, expectedType, context)
        }
    }
    
    // =============================================================================
    // Public API Methods for Visitors
    // =============================================================================
    
    /**
     * Handle constructor calls - exposed for statement visitor.
     */
    fun handleConstructorCall(
        call: ConstructorCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        return statementVisitor.handleConstructorCall(call, expectedType, context)
    }
    
    
    
    
    
    
    
    // =============================================================================
    // Shared Helper Methods - Accessible to Visitors
    // =============================================================================
    
    
    /**
     * Check if two types are structurally equal (ignoring source locations).
     * Made public for visitor access.
     */
    fun typesAreEqual(type1: Type, type2: Type): Boolean {
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
     * Made public for visitor access.
     */
    fun instantiateScheme(scheme: TypeScheme): Pair<Type, ConstraintSet> {
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
     * Substitute type variables in a type using the given substitution map.
     * Made public for visitor access.
     */
    fun substituteTypeVariables(type: Type, substitution: Map<String, Type>): Type {
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
     * Made public for visitor access.
     */
    fun substituteTypeVarsWithMapping(type: Type, substitution: Map<TypeVar, Type>): Type {
        // Convert TypeVar -> Type mapping to String -> Type mapping
        val stringSubstitution = substitution.mapKeys { (typeVar, _) -> typeVar.id }
        return substituteTypeVariables(type, stringSubstitution)
    }
    
    // =============================================================================
    // Pattern Processing - Delegated to PatternVisitor
    // =============================================================================
    
    /**
     * Process patterns - exposed for backward compatibility and integration.
     */
    fun processPattern(
        pattern: Pattern,
        targetType: Type,
        context: InferenceContext
    ): Pair<ConstraintSet, Map<String, Type>> {
        return patternVisitor.processPattern(pattern, targetType, context)
    }
    
    /**
     * Handle match cases - exposed for integration.
     */
    fun handleMatchCase(
        case: MatchCase,
        targetType: Type,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        return patternVisitor.handleMatchCase(case, targetType, expectedType, context)
    }
}