package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.Type

/**
 * Immutable type substitution representing mappings from type variables to concrete types.
 * 
 * A substitution is a finite mapping from type variables to types that can be applied to
 * types to replace type variables with their substituted values. Substitutions are the
 * core data structure used in unification-based type inference.
 * 
 * Key properties:
 * - Immutable: applying or composing substitutions creates new instances
 * - Thread-safe: multiple threads can safely use substitutions concurrently
 * - Functional: supports composition and idempotent application
 * 
 * Example usage:
 * ```kotlin
 * val subst = Substitution.single(TypeVar("T"), BuiltinTypes.INT)
 * val type = Type.GenericType("List", persistentListOf(Type.TypeVar("T")))
 * val result = subst.apply(type) // List[Int]
 * ```
 */
class Substitution private constructor(
    private val mapping: PersistentMap<TypeVar, Type>
) {
    
    companion object {
        /**
         * Create an empty substitution with no mappings.
         * The identity substitution that leaves all types unchanged.
         */
        fun empty(): Substitution = Substitution(persistentMapOf())
        
        /**
         * Create a substitution with a single type variable mapping.
         * 
         * @param typeVar The type variable to substitute
         * @param type The type to substitute it with
         */
        fun single(typeVar: TypeVar, type: Type): Substitution {
            return Substitution(persistentMapOf(typeVar to type))
        }
        
        /**
         * Create a substitution from a map of type variable mappings.
         * 
         * @param mappings The mappings from type variables to types
         */
        fun of(mappings: Map<TypeVar, Type>): Substitution {
            return Substitution(mappings.toPersistentMap())
        }
        
        /**
         * Create a substitution from pairs of type variable mappings.
         * 
         * @param mappings Pairs of (TypeVar, Type) mappings
         */
        fun of(vararg mappings: Pair<TypeVar, Type>): Substitution {
            return Substitution(mappings.toMap().toPersistentMap())
        }
    }
    
    /**
     * Check if this substitution is empty (contains no mappings).
     */
    fun isEmpty(): Boolean = mapping.isEmpty()
    
    /**
     * Check if this substitution is not empty.
     */
    fun isNotEmpty(): Boolean = mapping.isNotEmpty()
    
    /**
     * Get the number of type variable mappings in this substitution.
     */
    fun size(): Int = mapping.size
    
    /**
     * Check if this substitution contains a mapping for the given type variable.
     */
    fun contains(typeVar: TypeVar): Boolean = mapping.containsKey(typeVar)
    
    /**
     * Get the type that a type variable is mapped to, or null if not mapped.
     */
    fun get(typeVar: TypeVar): Type? = mapping[typeVar]
    
    /**
     * Get all type variables that have mappings in this substitution.
     */
    fun domain(): Set<TypeVar> = mapping.keys.toSet()
    
    /**
     * Get all types that type variables are mapped to in this substitution.
     */
    fun range(): Collection<Type> = mapping.values
    
    /**
     * Apply this substitution to a type, replacing type variables with their mappings.
     * 
     * This operation is idempotent: applying the same substitution multiple times
     * produces the same result as applying it once.
     * 
     * @param type The type to apply the substitution to
     * @return The type with type variables replaced by their substitutions
     */
    fun apply(type: Type): Type {
        return when (type) {
            is Type.TypeVar -> {
                // Replace type variable with its mapping if present
                mapping[TypeVar(type.id)] ?: type
            }
            
            is Type.PrimitiveType -> {
                // Primitive types have no type variables, return unchanged
                type
            }
            
            is Type.NamedType -> {
                // Check if this is actually a type variable represented as NamedType
                // This handles the case where TypeVar is represented as NamedType in some contexts
                val typeVar = TypeVar(type.name)
                mapping[typeVar]?.let { substitutedType ->
                    // If we have a mapping for this name as a type variable, use it
                    // Recursively apply to handle chained substitutions
                    apply(substitutedType)
                } ?: type
            }
            
            is Type.GenericType -> {
                // Apply substitution recursively to all type arguments
                val substitutedArgs = type.arguments.map { apply(it) }
                if (substitutedArgs == type.arguments.toList()) {
                    type // No change, return original
                } else {
                    type.copy(arguments = substitutedArgs.toPersistentList())
                }
            }
            
            is Type.NullableType -> {
                // Apply substitution to the base type
                val substitutedBase = apply(type.baseType)
                if (substitutedBase == type.baseType) {
                    type // No change, return original
                } else {
                    type.copy(baseType = substitutedBase)
                }
            }
            
            is Type.TupleType -> {
                // Apply substitution to all element types
                val substitutedElements = type.elementTypes.map { apply(it) }
                if (substitutedElements == type.elementTypes.toList()) {
                    type // No change, return original
                } else {
                    type.copy(elementTypes = substitutedElements.toPersistentList())
                }
            }
            
            is Type.FunctionType -> {
                // Apply substitution to parameter types and return type
                val substitutedParams = type.parameterTypes.map { apply(it) }
                val substitutedReturn = apply(type.returnType)
                if (substitutedParams == type.parameterTypes.toList() && substitutedReturn == type.returnType) {
                    type // No change, return original
                } else {
                    type.copy(
                        parameterTypes = substitutedParams.toPersistentList(),
                        returnType = substitutedReturn
                    )
                }
            }
            
            is Type.UnionType -> {
                // Apply substitution to all type arguments
                val substitutedArgs = type.typeArguments.map { apply(it) }
                if (substitutedArgs == type.typeArguments.toList()) {
                    type // No change, return original
                } else {
                    type.copy(typeArguments = substitutedArgs.toPersistentList())
                }
            }
        }
    }
    
    /**
     * Extend this substitution with a new type variable mapping.
     * 
     * If the type variable already has a mapping, it will be replaced.
     * This creates a new substitution instance.
     * 
     * @param typeVar The type variable to add a mapping for
     * @param type The type to map it to
     * @return A new substitution with the additional mapping
     */
    fun extend(typeVar: TypeVar, type: Type): Substitution {
        return Substitution(mapping.put(typeVar, type))
    }
    
    /**
     * Compose this substitution with another substitution.
     * 
     * The composition s2 ∘ s1 means: apply s1 first, then apply s2 to the result.
     * Mathematically: (s2 ∘ s1)(t) = s2(s1(t))
     * 
     * The resulting substitution combines the effects of both substitutions:
     * - All mappings from s1, with s2 applied to their values
     * - All mappings from s2 that don't conflict with s1
     * 
     * @param other The substitution to compose with (applied second)
     * @return A new substitution representing the composition
     */
    fun compose(other: Substitution): Substitution {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        
        // Start with mappings from this substitution, with other applied to their values
        val composedMappings = mapping.entries.associate { (typeVar, type) ->
            typeVar to other.apply(type)
        }.toMutableMap()
        
        // Add mappings from other that are not already in this substitution
        other.mapping.forEach { (typeVar, type) ->
            if (!composedMappings.containsKey(typeVar)) {
                composedMappings[typeVar] = type
            }
        }
        
        return Substitution(composedMappings.toPersistentMap())
    }
    
    /**
     * Remove a type variable mapping from this substitution.
     * 
     * @param typeVar The type variable to remove
     * @return A new substitution without the mapping for the given type variable
     */
    fun remove(typeVar: TypeVar): Substitution {
        return if (mapping.containsKey(typeVar)) {
            Substitution(mapping.remove(typeVar))
        } else {
            this
        }
    }
    
    /**
     * Restrict this substitution to only the given type variables.
     * 
     * @param typeVars The type variables to keep mappings for
     * @return A new substitution containing only mappings for the given type variables
     */
    fun restrictTo(typeVars: Set<TypeVar>): Substitution {
        val restrictedMappings = mapping.entries
            .filter { (typeVar, _) -> typeVars.contains(typeVar) }
            .associate { (typeVar, type) -> typeVar to type }
        
        return Substitution(restrictedMappings.toPersistentMap())
    }
    
    /**
     * Get all type variables that appear in the range of this substitution.
     * These are the free type variables in the types that type variables are mapped to.
     */
    fun freeTypeVars(): Set<TypeVar> {
        return mapping.values.flatMap { type -> collectTypeVars(type) }.toSet()
    }
    
    /**
     * Collect all type variables that appear in a type.
     */
    private fun collectTypeVars(type: Type): Set<TypeVar> {
        return when (type) {
            is Type.TypeVar -> setOf(TypeVar(type.id))
            is Type.NamedType -> {
                // Check if this might be a type variable
                if (type.name.length == 1 && type.name[0].isUpperCase()) {
                    setOf(TypeVar(type.name))
                } else {
                    emptySet()
                }
            }
            is Type.GenericType -> type.arguments.flatMap { collectTypeVars(it) }.toSet()
            is Type.NullableType -> collectTypeVars(type.baseType)
            is Type.TupleType -> type.elementTypes.flatMap { collectTypeVars(it) }.toSet()
            is Type.FunctionType -> {
                val paramVars = type.parameterTypes.flatMap { collectTypeVars(it) }.toSet()
                val returnVars = collectTypeVars(type.returnType)
                paramVars + returnVars
            }
            is Type.UnionType -> type.typeArguments.flatMap { collectTypeVars(it) }.toSet()
            is Type.PrimitiveType -> emptySet()
        }
    }
    
    /**
     * Check if two substitutions are equivalent.
     * Two substitutions are equivalent if they produce the same result when applied to any type.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Substitution) return false
        return mapping == other.mapping
    }
    
    override fun hashCode(): Int {
        return mapping.hashCode()
    }
    
    override fun toString(): String {
        if (mapping.isEmpty()) {
            return "∅"
        }
        val mappings = mapping.entries.joinToString(", ") { (typeVar, type) ->
            "${typeVar.id} ↦ $type"
        }
        return "{$mappings}"
    }
    
    /**
     * Convert this substitution to a regular Map for compatibility with other APIs.
     */
    fun toMap(): Map<TypeVar, Type> = mapping.toMap()
    
    /**
     * Create a new substitution by filtering mappings based on a predicate.
     */
    fun filter(predicate: (TypeVar, Type) -> Boolean): Substitution {
        val filteredMappings = mapping.entries
            .filter { (typeVar, type) -> predicate(typeVar, type) }
            .associate { (typeVar, type) -> typeVar to type }
        
        return Substitution(filteredMappings.toPersistentMap())
    }
    
    /**
     * Transform all types in this substitution using the given function.
     */
    fun mapTypes(transform: (Type) -> Type): Substitution {
        val transformedMappings = mapping.entries
            .associate { (typeVar, type) -> typeVar to transform(type) }
        
        return Substitution(transformedMappings.toPersistentMap())
    }
}