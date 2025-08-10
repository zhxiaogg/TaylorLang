package org.taylorlang.typechecker

import org.taylorlang.ast.SourceLocation
import org.taylorlang.ast.Type
import java.util.concurrent.atomic.AtomicInteger

/**
 * Type kind for higher-kinded type support.
 * Currently only STAR is supported, but this allows for future extension to support
 * type constructors with higher kinds (e.g., * -> *, * -> * -> *).
 */
enum class TypeKind {
    STAR  // Kind * - regular types like Int, String, List[T]
}

/**
 * Type variable representation for type inference.
 * Type variables represent unknown types that will be resolved during inference.
 * Each type variable has a unique identifier to distinguish it from others.
 */
data class TypeVar(
    val id: String,  // Unique identifier (e.g., "T1", "T2")
    val kind: TypeKind = TypeKind.STAR  // For higher-kinded types (future extension)
) {
    companion object {
        private val counter = AtomicInteger(0)
        
        /**
         * Generate a fresh type variable with a unique ID.
         * Thread-safe counter ensures uniqueness across concurrent inference sessions.
         */
        fun fresh(): TypeVar {
            val id = "T${counter.incrementAndGet()}"
            return TypeVar(id)
        }
        
        /**
         * Create a type variable with a specific name.
         * Useful for debugging or when specific naming is required.
         */
        fun named(name: String): TypeVar {
            return TypeVar(name)
        }
        
        /**
         * Reset the counter for testing purposes.
         * Should only be used in test environments.
         */
        @JvmStatic
        fun resetCounter() {
            counter.set(0)
        }
    }
    
    override fun toString(): String = id
}

/**
 * Type scheme for polymorphic types.
 * A type scheme represents a polymorphic type by quantifying over type variables.
 * For example, the type scheme ∀α. α → α represents the identity function type.
 */
data class TypeScheme(
    val quantifiedVars: Set<TypeVar>,  // Type variables that are quantified (bound)
    val type: Type                     // The type with potential type variable references
) {
    /**
     * Check if this is a monomorphic type (no quantified variables).
     */
    fun isMonomorphic(): Boolean = quantifiedVars.isEmpty()
    
    /**
     * Get the set of free type variables in this scheme.
     * Free variables are those that appear in the type but are not quantified.
     */
    fun freeTypeVars(): Set<TypeVar> {
        // This would require traversing the type to find TypeVar references
        // For now, return empty set as placeholder
        return emptySet()
    }
    
    companion object {
        /**
         * Create a monomorphic type scheme (no quantified variables).
         */
        fun monomorphic(type: Type): TypeScheme {
            return TypeScheme(emptySet(), type)
        }
    }
    
    override fun toString(): String {
        return if (quantifiedVars.isEmpty()) {
            type.toString()
        } else {
            "∀${quantifiedVars.joinToString(",")}. $type"
        }
    }
}

/**
 * Constraint representation for type inference.
 * Constraints express relationships between types that must be satisfied
 * for type inference to succeed.
 */
sealed class Constraint {
    abstract val location: SourceLocation?
    
    /**
     * Equality constraint: left type must be equal to right type.
     * This is the most common constraint generated during type inference.
     * Example: when we have `x = 5`, we generate T_x = Int
     */
    data class Equality(
        val left: Type,
        val right: Type,
        override val location: SourceLocation? = null
    ) : Constraint() {
        override fun toString(): String = "$left ~ $right"
    }
    
    /**
     * Subtype constraint: subtype must be a subtype of supertype.
     * Used for subtyping relationships and variance.
     * Example: Int <: Number, List[String] <: List[Any]
     */
    data class Subtype(
        val subtype: Type,
        val supertype: Type,
        override val location: SourceLocation? = null
    ) : Constraint() {
        override fun toString(): String = "$subtype <: $supertype"
    }
    
    /**
     * Instance constraint: type variable should be an instance of a type scheme.
     * Used when instantiating polymorphic types.
     * Example: when using a polymorphic function, we constrain the type variable
     * to be an instance of the function's type scheme.
     */
    data class Instance(
        val typeVar: TypeVar,
        val scheme: TypeScheme,
        override val location: SourceLocation? = null
    ) : Constraint() {
        override fun toString(): String = "$typeVar ∈ $scheme"
    }
    
    /**
     * Get all type variables mentioned in this constraint.
     * Used for dependency analysis during constraint solving.
     */
    fun mentionedTypeVars(): Set<TypeVar> {
        return when (this) {
            is Equality -> getTypeVarsFromType(left) + getTypeVarsFromType(right)
            is Subtype -> getTypeVarsFromType(subtype) + getTypeVarsFromType(supertype)
            is Instance -> setOf(typeVar) + scheme.freeTypeVars()
        }
    }
    
    private fun getTypeVarsFromType(type: Type): Set<TypeVar> {
        // This would require traversing the type tree to find TypeVar references
        // For now, return empty set as placeholder since TypeVar is not part of Type hierarchy yet
        return emptySet()
    }
}

/**
 * Immutable set of constraints for type inference.
 * Provides operations to build and manipulate constraint sets while maintaining immutability.
 * This is crucial for backtracking during constraint solving.
 */
class ConstraintSet private constructor(
    private val constraints: List<Constraint>
) {
    companion object {
        /**
         * Create an empty constraint set.
         */
        fun empty(): ConstraintSet {
            return ConstraintSet(emptyList())
        }
        
        /**
         * Create a constraint set from a collection of constraints.
         */
        fun of(vararg constraints: Constraint): ConstraintSet {
            return ConstraintSet(constraints.toList())
        }
        
        /**
         * Create a constraint set from a collection of constraints.
         */
        fun fromCollection(constraints: Collection<Constraint>): ConstraintSet {
            return ConstraintSet(constraints.toList())
        }
    }
    
    /**
     * Add a single constraint to this set, returning a new constraint set.
     * Maintains immutability by creating a new instance.
     */
    fun add(constraint: Constraint): ConstraintSet {
        return ConstraintSet(constraints + constraint)
    }
    
    /**
     * Add multiple constraints to this set, returning a new constraint set.
     */
    fun addAll(newConstraints: Collection<Constraint>): ConstraintSet {
        return ConstraintSet(constraints + newConstraints)
    }
    
    /**
     * Merge this constraint set with another, returning a new constraint set.
     * The result contains all constraints from both sets.
     */
    fun merge(other: ConstraintSet): ConstraintSet {
        return ConstraintSet(constraints + other.constraints)
    }
    
    /**
     * Get an immutable view of the constraints as a list.
     */
    fun toList(): List<Constraint> {
        return constraints.toList() // Defensive copy
    }
    
    /**
     * Get the number of constraints in this set.
     */
    fun size(): Int = constraints.size
    
    /**
     * Check if this constraint set is empty.
     */
    fun isEmpty(): Boolean = constraints.isEmpty()
    
    /**
     * Check if this constraint set is not empty.
     */
    fun isNotEmpty(): Boolean = constraints.isNotEmpty()
    
    /**
     * Filter constraints based on a predicate, returning a new constraint set.
     */
    fun filter(predicate: (Constraint) -> Boolean): ConstraintSet {
        return ConstraintSet(constraints.filter(predicate))
    }
    
    /**
     * Partition constraints based on a predicate.
     * Returns a pair where the first element contains constraints matching the predicate,
     * and the second contains the rest.
     */
    fun partition(predicate: (Constraint) -> Boolean): Pair<ConstraintSet, ConstraintSet> {
        val (matching, nonMatching) = constraints.partition(predicate)
        return ConstraintSet(matching) to ConstraintSet(nonMatching)
    }
    
    /**
     * Get all type variables mentioned in this constraint set.
     * Useful for dependency analysis and garbage collection of unused variables.
     */
    fun mentionedTypeVars(): Set<TypeVar> {
        return constraints.flatMap { it.mentionedTypeVars() }.toSet()
    }
    
    /**
     * Check if this constraint set contains any constraints involving the given type variable.
     */
    fun involvesTypeVar(typeVar: TypeVar): Boolean {
        return constraints.any { it.mentionedTypeVars().contains(typeVar) }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConstraintSet) return false
        return constraints == other.constraints
    }
    
    override fun hashCode(): Int {
        return constraints.hashCode()
    }
    
    override fun toString(): String {
        if (constraints.isEmpty()) {
            return "∅"
        }
        return constraints.joinToString(", ", "{", "}") { it.toString() }
    }
}