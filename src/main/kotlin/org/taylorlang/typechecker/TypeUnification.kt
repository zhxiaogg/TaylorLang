package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Bridge between centralized type operations and the refactored Unifier system.
 * 
 * This component provides a clean integration layer between the centralized TypeOperations
 * architecture and the existing proven Unifier system. It leverages the 4-component
 * Unifier architecture (Unifier, UnificationAlgorithm, OccursChecker, SubstitutionComposer)
 * while providing optimized workflows for common type unification scenarios.
 * 
 * Integration with existing components:
 * - Unifier.unify() for direct type unification
 * - Unifier.solve() for constraint set solving
 * - TypeComparison for pre-unification optimizations
 * - TypeFactory for creating unified result types
 */
object TypeUnification {
    
    // =============================================================================
    // Core Unification Operations
    // =============================================================================
    
    /**
     * Unify two types with optimized workflow.
     * 
     * This method provides the primary unification interface, incorporating
     * pre-unification optimizations and leveraging the existing Unifier system.
     * 
     * @param type1 First type to unify
     * @param type2 Second type to unify
     * @return UnificationResult containing success/failure information
     */
    fun unify(type1: Type, type2: Type): UnificationResult {
        // Fast path: check structural equality first
        if (TypeComparison.structuralEquals(type1, type2)) {
            return UnificationResult.Success(
                substitution = Substitution.empty(),
                unifiedType = type1
            )
        }
        
        // Handle numeric type promotion before unification
        val numericUnification = handleNumericUnification(type1, type2)
        if (numericUnification != null) {
            return UnificationResult.Success(
                substitution = Substitution.empty(),
                unifiedType = numericUnification
            )
        }
        
        // Delegate to existing Unifier system
        val result = Unifier.unify(type1, type2)
        return if (result.isSuccess) {
            val substitution = result.getOrThrow()
            val unifiedType = findMostGeneralType(type1, type2, substitution)
            UnificationResult.Success(substitution, unifiedType)
        } else {
            val error = result.exceptionOrNull()
            UnificationResult.Failure(
                error = error as? UnificationError ?: 
                        UnificationError.TypeMismatch(type1, type2),
                type1 = type1,
                type2 = type2
            )
        }
    }
    
    /**
     * Unify two types with existing substitution context.
     * 
     * This method is useful for constraint solving scenarios where
     * a partial substitution is already available.
     * 
     * @param type1 First type to unify
     * @param type2 Second type to unify
     * @param substitution Existing substitution context
     * @return UnificationResult with updated substitution
     */
    fun unifyWithSubstitution(
        type1: Type, 
        type2: Type, 
        substitution: Substitution
    ): UnificationResult {
        // Apply existing substitution to both types
        val substitutedType1 = substitution.apply(type1)
        val substitutedType2 = substitution.apply(type2)
        
        // Attempt unification on substituted types
        return when (val result = unify(substitutedType1, substitutedType2)) {
            is UnificationResult.Success -> {
                // Compose substitutions
                val composer = SubstitutionComposer()
                val composedSubst = composer.composeSubstitutions(
                    substitution, 
                    result.substitution
                )
                UnificationResult.Success(
                    substitution = composedSubst,
                    unifiedType = result.unifiedType
                )
            }
            is UnificationResult.Failure -> result
        }
    }
    
    /**
     * Solve constraint set using optimized workflow.
     * 
     * Provides direct access to constraint solving with integration
     * to centralized type operations for pre-processing optimizations.
     * 
     * @param constraints Set of type constraints to solve
     * @return ConstraintSolutionResult with substitution or failure details
     */
    fun solveConstraints(constraints: ConstraintSet): ConstraintSolutionResult {
        // Pre-process constraints for optimization opportunities
        val optimizedConstraints = optimizeConstraints(constraints)
        
        val result = Unifier.solve(optimizedConstraints)
        return if (result.isSuccess) {
            val substitution = result.getOrThrow()
            ConstraintSolutionResult.Success(substitution)
        } else {
            val error = result.exceptionOrNull()
            ConstraintSolutionResult.Failure(
                error = error as? UnificationError ?: 
                        UnificationError.TypeMismatch(Type.PrimitiveType("Unknown"), Type.PrimitiveType("Unknown")),
                constraints = constraints
            )
        }
    }
    
    // =============================================================================
    // Optimization Helpers
    // =============================================================================
    
    /**
     * Handle numeric type unification with promotion rules.
     */
    private fun handleNumericUnification(type1: Type, type2: Type): Type? {
        return if (type1 is Type.PrimitiveType && type2 is Type.PrimitiveType) {
            BuiltinTypes.getWiderNumericType(type1, type2)
        } else null
    }
    
    /**
     * Find the most general unified type from unification results.
     */
    private fun findMostGeneralType(
        type1: Type, 
        type2: Type, 
        substitution: Substitution
    ): Type {
        val appliedType1 = substitution.apply(type1)
        val appliedType2 = substitution.apply(type2)
        
        return if (TypeComparison.structuralEquals(appliedType1, appliedType2)) {
            appliedType1
        } else {
            // Return the more general type based on type hierarchy
            when {
                appliedType1 is Type.TypeVar -> appliedType2
                appliedType2 is Type.TypeVar -> appliedType1
                else -> appliedType1 // Default to first type
            }
        }
    }
    
    /**
     * Pre-process constraint set for optimization opportunities.
     */
    private fun optimizeConstraints(constraints: ConstraintSet): ConstraintSet {
        // For now, return constraints as-is since we need access to internal structure
        // This can be enhanced when constraint filtering is needed
        return constraints
    }
    
    // =============================================================================
    // Result Types for Type-Safe Unification
    // =============================================================================
    
    /**
     * Result of a type unification operation.
     */
    sealed class UnificationResult {
        /**
         * Successful unification with resulting substitution and unified type.
         */
        data class Success(
            val substitution: Substitution,
            val unifiedType: Type
        ) : UnificationResult()
        
        /**
         * Failed unification with error details.
         */
        data class Failure(
            val error: UnificationError,
            val type1: Type,
            val type2: Type
        ) : UnificationResult()
    }
    
    /**
     * Result of constraint set solving.
     */
    sealed class ConstraintSolutionResult {
        /**
         * Successful constraint solving with resulting substitution.
         */
        data class Success(
            val substitution: Substitution
        ) : ConstraintSolutionResult()
        
        /**
         * Failed constraint solving with error details.
         */
        data class Failure(
            val error: UnificationError,
            val constraints: ConstraintSet
        ) : ConstraintSolutionResult()
    }
    
    // =============================================================================
    // Convenience Methods for Common Patterns
    // =============================================================================
    
    /**
     * Check if two types can be unified (without producing substitution).
     */
    fun canUnify(type1: Type, type2: Type): Boolean {
        return when (unify(type1, type2)) {
            is UnificationResult.Success -> true
            is UnificationResult.Failure -> false
        }
    }
    
    /**
     * Get the most general unifier for two types.
     */
    fun getMostGeneralUnifier(type1: Type, type2: Type): Substitution? {
        return when (val result = unify(type1, type2)) {
            is UnificationResult.Success -> result.substitution
            is UnificationResult.Failure -> null
        }
    }
    
    /**
     * Unify a list of types to find common supertype.
     */
    fun unifyTypes(types: List<Type>): UnificationResult? {
        if (types.isEmpty()) return null
        if (types.size == 1) return UnificationResult.Success(
            substitution = Substitution.empty(),
            unifiedType = types.first()
        )
        
        return types.reduce { acc, type ->
            when (val result = unify(acc, type)) {
                is UnificationResult.Success -> result.unifiedType
                is UnificationResult.Failure -> return result
            }
        }.let { unifiedType ->
            UnificationResult.Success(
                substitution = Substitution.empty(),
                unifiedType = unifiedType
            )
        }
    }
}