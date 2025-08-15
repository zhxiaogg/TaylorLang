package org.taylorlang.typechecker

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Specialized visitor for collecting constraints from patterns.
 * 
 * This visitor handles all pattern-related constraint collection including:
 * - Pattern matching constraint generation
 * - Variable binding constraints in patterns
 * - Pattern guard condition handling
 * - Constructor pattern matching with nested patterns
 * - Pattern exhaustiveness checking support
 * 
 * Designed to work as part of a modular constraint collection system where
 * patterns are processed independently but integrated with the main constraint system.
 */
class PatternConstraintVisitor(
    private val collector: ConstraintCollector
) {
    
    // =============================================================================
    // Main Pattern Processing
    // =============================================================================
    
    /**
     * Process a pattern and return constraints and variable bindings.
     */
    fun processPattern(
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
                val literalResult = collector.collectConstraints(pattern.literal, context)
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
                val guardResult = collector.collectConstraintsWithExpected(
                    pattern.guard, 
                    BuiltinTypes.BOOLEAN, 
                    guardContext
                )
                
                val allConstraints = innerConstraints.merge(guardResult.constraints)
                Pair(allConstraints, innerBindings)
            }
        }
    }
    
    // =============================================================================
    // Constructor Pattern Processing
    // =============================================================================
    
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
        // Generate type arguments if the union type is generic
        val typeArguments = if (typeDef.typeParameters.isNotEmpty()) {
            // Create fresh type variables for each type parameter
            typeDef.typeParameters.map {
                val freshVar = TypeVar.fresh()
                Type.NamedType(freshVar.id)
            }.toPersistentList()
        } else {
            persistentListOf<Type>()
        }
        
        val constructorType = Type.UnionType(unionTypeName, typeArguments)
        val typeConstraint = Constraint.Equality(targetType, constructorType, pattern.sourceLocation)
        
        return Pair(allConstraints.add(typeConstraint), allBindings)
    }
    
    // =============================================================================
    // List Pattern Processing
    // =============================================================================
    
    /**
     * Process list patterns with element matching and rest variable binding.
     */
    
    // =============================================================================
    // Match Expression Processing
    // =============================================================================
    
    /**
     * Handle a single match case with pattern and expression.
     */
    fun handleMatchCase(
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
        val exprResult = collector.collectConstraintsWithExpected(case.expression, expectedType, caseContext)
        
        return ConstraintResult(
            exprResult.type,
            patternConstraints.merge(exprResult.constraints)
        )
    }
    
    /**
     * Process all cases in a match expression and unify their result types.
     */
    fun processMatchCases(
        cases: List<MatchCase>,
        targetType: Type,
        expectedType: Type?,
        context: InferenceContext,
        matchLocation: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        // Process each match case
        val caseResults = cases.map { case ->
            handleMatchCase(case, targetType, expectedType, context)
        }
        
        // Merge all constraints
        val allConstraints = caseResults.fold(ConstraintSet.empty()) { acc, caseResult ->
            acc.merge(caseResult.constraints)
        }
        
        // Determine unified result type
        val resultType = if (expectedType != null) {
            expectedType
        } else if (caseResults.isNotEmpty()) {
            // Check if all cases have the same type
            val firstCaseType = caseResults.first().type
            val allCasesSameType = caseResults.all { collector.typesAreEqual(it.type, firstCaseType) }
            
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
                        cases[index].expression.sourceLocation
                    )
                }
                
                val finalConstraints = caseConstraints.fold(allConstraints) { acc, constraint ->
                    acc.add(constraint)
                }
                
                return Pair(unifiedType, finalConstraints)
            }
        } else {
            BuiltinTypes.UNIT
        }
        
        // TODO: Add exhaustiveness checking constraints
        // This would verify that all possible constructor variants are covered
        
        return Pair(resultType, allConstraints)
    }
    
    // =============================================================================
    // Pattern Analysis and Validation
    // =============================================================================
    
    /**
     * Analyze pattern completeness for exhaustiveness checking.
     * Returns information about which constructor variants are covered.
     */
    fun analyzePatternCompleteness(
        patterns: List<Pattern>,
        targetType: Type,
        context: InferenceContext
    ): PatternAnalysis {
        val coveredConstructors = mutableSetOf<String>()
        val wildcardPresent = patterns.any { it is Pattern.WildcardPattern }
        
        patterns.forEach { pattern ->
            when (pattern) {
                is Pattern.ConstructorPattern -> {
                    coveredConstructors.add(pattern.constructor)
                }
                is Pattern.WildcardPattern -> {
                    // Wildcard covers everything
                }
                is Pattern.GuardPattern -> {
                    // Analyze inner pattern
                    if (pattern.pattern is Pattern.ConstructorPattern) {
                        coveredConstructors.add(pattern.pattern.constructor)
                    }
                }
                else -> {
                    // Literal and identifier patterns don't contribute to constructor coverage
                }
            }
        }
        
        return PatternAnalysis(
            coveredConstructors = coveredConstructors,
            hasWildcard = wildcardPresent,
            isExhaustive = wildcardPresent || isExhaustiveForType(coveredConstructors, targetType, context)
        )
    }
    
    /**
     * Check if the given set of constructors provides exhaustive coverage for a type.
     */
    private fun isExhaustiveForType(
        coveredConstructors: Set<String>,
        targetType: Type,
        context: InferenceContext
    ): Boolean {
        // For union types, check if all variants are covered
        if (targetType is Type.UnionType || targetType is Type.NamedType) {
            val typeName = when (targetType) {
                is Type.UnionType -> targetType.name
                is Type.NamedType -> targetType.name
                else -> return false
            }
            
            val typeDef = context.lookupTypeDefinition(typeName)
            if (typeDef is TypeDefinition.UnionTypeDef) {
                val allVariants = typeDef.variants.map { it.name }.toSet()
                return coveredConstructors.containsAll(allVariants)
            }
        }
        
        // For other types, exhaustiveness depends on the specific pattern types
        // This is a simplified check - a full implementation would be more sophisticated
        return false
    }
    
    /**
     * Generate constraints for pattern exhaustiveness.
     * These constraints can be used to warn about non-exhaustive matches.
     */
    fun generateExhaustivenessConstraints(
        analysis: PatternAnalysis,
        targetType: Type,
        location: SourceLocation?,
        context: InferenceContext
    ): ConstraintSet {
        if (!analysis.isExhaustive) {
            // In a full implementation, we would generate specific exhaustiveness constraints
            // For now, we'll return empty constraints but this could be extended
            // to generate warning constraints or error constraints
        }
        
        return ConstraintSet.empty()
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
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
     * Extract all variable bindings from a pattern.
     * Used for scope analysis and variable binding validation.
     */
    fun extractBindings(pattern: Pattern): Set<String> {
        return when (pattern) {
            is Pattern.WildcardPattern -> emptySet()
            is Pattern.IdentifierPattern -> setOf(pattern.name)
            is Pattern.LiteralPattern -> emptySet()
            is Pattern.ConstructorPattern -> {
                pattern.patterns.flatMap { extractBindings(it) }.toSet()
            }
            is Pattern.GuardPattern -> extractBindings(pattern.pattern)
        }
    }
    
    /**
     * Check for duplicate bindings in a pattern.
     * Patterns should not bind the same variable multiple times.
     */
    fun validateNoDuplicateBindings(pattern: Pattern): List<String> {
        val bindings = mutableListOf<String>()
        collectBindingsRecursively(pattern, bindings)
        
        val duplicates = bindings.groupBy { it }.filter { it.value.size > 1 }.keys
        return duplicates.toList()
    }
    
    private fun collectBindingsRecursively(pattern: Pattern, bindings: MutableList<String>) {
        when (pattern) {
            is Pattern.IdentifierPattern -> bindings.add(pattern.name)
            is Pattern.ConstructorPattern -> {
                pattern.patterns.forEach { collectBindingsRecursively(it, bindings) }
            }
            is Pattern.GuardPattern -> collectBindingsRecursively(pattern.pattern, bindings)
            else -> { /* No bindings for wildcard and literal patterns */ }
        }
    }
}

/**
 * Analysis result for pattern completeness checking.
 */
data class PatternAnalysis(
    val coveredConstructors: Set<String>,
    val hasWildcard: Boolean,
    val isExhaustive: Boolean
)