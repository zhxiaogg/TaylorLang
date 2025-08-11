package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

/**
 * Unification errors that can occur during constraint solving.
 */
sealed class UnificationError : Exception() {
    /**
     * Error when two types cannot be unified.
     */
    data class TypeMismatch(
        val type1: Type,
        val type2: Type,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Cannot unify types: $type1 and $type2${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when occurs check fails (infinite type detected).
     */
    data class InfiniteType(
        val typeVar: TypeVar,
        val type: Type,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Infinite type: ${typeVar.id} occurs in $type${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when type constructor arity doesn't match.
     */
    data class ArityMismatch(
        val constructor: String,
        val expected: Int,
        val actual: Int,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Arity mismatch for $constructor: expected $expected arguments, got $actual${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when constraint solving fails.
     */
    data class ConstraintSolvingFailed(
        val constraint: Constraint,
        override val cause: UnificationError
    ) : UnificationError() {
        override val message: String
            get() = "Failed to solve constraint $constraint: ${cause.message}"
    }
}

/**
 * Unification coordinator implementing Robinson's algorithm through specialized components.
 * 
 * This class serves as the main facade for constraint-based type inference, coordinating
 * between specialized components that handle different aspects of the unification process:
 * 
 * - **UnificationAlgorithm**: Core Robinson's unification algorithm
 * - **OccursChecker**: Cycle detection and infinite type prevention  
 * - **SubstitutionComposer**: Complex substitution and constraint operations
 * 
 * This architecture provides:
 * 1. **Separation of Concerns**: Each component has a single, focused responsibility
 * 2. **Maintainability**: Complex logic is isolated in specialized classes
 * 3. **Testability**: Components can be tested independently
 * 4. **Performance**: Optimized algorithms for specific operations
 * 
 * The unification process works by:
 * 1. Taking a set of type constraints
 * 2. Iteratively solving each constraint using the algorithm component
 * 3. Applying substitutions to remaining constraints using the composer
 * 4. Returning the final substitution or an error
 * 
 * Example usage:
 * ```kotlin
 * val constraints = ConstraintSet.of(
 *     Constraint.Equality(Type.TypeVar("T"), BuiltinTypes.INT),
 *     Constraint.Equality(Type.GenericType("List", persistentListOf(Type.TypeVar("T"))), someListType)
 * )
 * val result = Unifier.solve(constraints)
 * ```
 */
class Unifier {
    
    // Specialized components for different aspects of unification
    private val algorithm = UnificationAlgorithm()
    private val occursChecker = OccursChecker()
    private val substitutionComposer = SubstitutionComposer()
    
    companion object {
        /**
         * Solve a set of type constraints to produce a type substitution.
         * 
         * This is the main entry point for constraint-based type inference.
         * It processes all constraints in the set and returns either a
         * successful substitution or an error.
         * 
         * @param constraints The set of constraints to solve
         * @return Result containing the substitution or error
         */
        fun solve(constraints: ConstraintSet): Result<Substitution> {
            return try {
                val unifier = Unifier()
                val substitution = unifier.solveConstraints(constraints)
                Result.success(substitution)
            } catch (e: UnificationError) {
                Result.failure(e)
            }
        }
        
        /**
         * Unify two types directly without constraint solving.
         * 
         * This is useful for type checking scenarios where you need
         * to check if two specific types can be unified.
         * 
         * @param type1 The first type to unify
         * @param type2 The second type to unify
         * @return Result containing the unifying substitution or error
         */
        fun unify(type1: Type, type2: Type): Result<Substitution> {
            return try {
                val unifier = Unifier()
                val substitution = unifier.algorithm.unifyTypes(type1, type2, Substitution.empty(), unifier.occursChecker)
                Result.success(substitution)
            } catch (e: UnificationError) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Solve all constraints in the constraint set.
     * 
     * This method coordinates the constraint solving process by delegating
     * to specialized components while maintaining the overall control flow.
     * 
     * @param constraints The constraints to solve
     * @return The resulting substitution
     * @throws UnificationError if any constraint cannot be solved
     */
    private fun solveConstraints(constraints: ConstraintSet): Substitution {
        var currentSubst = Substitution.empty()
        var remainingConstraints = constraints.toList()
        
        // Process constraints until none remain
        while (remainingConstraints.isNotEmpty()) {
            val constraint = remainingConstraints.first()
            remainingConstraints = remainingConstraints.drop(1)
            
            try {
                // Solve the current constraint using appropriate component
                val newSubst = solveConstraint(constraint, currentSubst)
                
                // Compose substitutions using the substitution composer
                currentSubst = substitutionComposer.composeSubstitutions(newSubst, currentSubst)
                
                // Apply the new substitution to remaining constraints
                remainingConstraints = remainingConstraints.map { c ->
                    substitutionComposer.applySubstitutionToConstraint(c, newSubst)
                }
                
            } catch (e: UnificationError) {
                throw UnificationError.ConstraintSolvingFailed(constraint, e)
            }
        }
        
        return currentSubst
    }
    
    /**
     * Solve a single constraint by delegating to the appropriate component.
     * 
     * @param constraint The constraint to solve
     * @param currentSubst The current substitution context
     * @return A substitution that satisfies this constraint
     * @throws UnificationError if the constraint cannot be solved
     */
    private fun solveConstraint(constraint: Constraint, currentSubst: Substitution): Substitution {
        return when (constraint) {
            is Constraint.Equality -> {
                // Delegate equality constraints to the unification algorithm
                algorithm.unifyTypes(constraint.left, constraint.right, currentSubst, occursChecker)
            }
            
            is Constraint.Subtype -> {
                // Delegate subtype constraints to the substitution composer
                substitutionComposer.handleSubtypeConstraint(constraint.subtype, constraint.supertype, currentSubst)
            }
            
            is Constraint.Instance -> {
                // Handle instance constraints by instantiating the scheme and unifying
                val instantiatedType = substitutionComposer.instantiateTypeScheme(constraint.scheme)
                algorithm.unifyTypes(
                    Type.NamedType(constraint.typeVar.id), // Convert TypeVar to Type
                    instantiatedType,
                    currentSubst,
                    occursChecker
                )
            }
        }
    }
}