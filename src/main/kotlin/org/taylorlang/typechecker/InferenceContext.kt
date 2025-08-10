package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.taylorlang.ast.Type

/**
 * Immutable inference context that tracks the type environment during constraint collection.
 * 
 * This context manages:
 * - Variable bindings and their type schemes for let-polymorphism
 * - Fresh type variable generation state
 * - Scope management for proper variable binding and generalization
 * - Parent context reference for nested scopes
 * 
 * The context is designed to be immutable to support backtracking and correct
 * handling of scope boundaries during type inference.
 */
data class InferenceContext(
    /**
     * Current type environment mapping variable names to their type schemes.
     * Type schemes allow for polymorphic types with quantified type variables.
     */
    val typeEnvironment: PersistentMap<String, TypeScheme> = persistentMapOf(),
    
    /**
     * Type definitions available in the current scope.
     * Maps type names to their definitions (union types, etc.).
     */
    val typeDefinitions: PersistentMap<String, TypeDefinition> = persistentMapOf(),
    
    /**
     * Function signatures available in the current scope.
     * Maps function names to their type signatures.
     */
    val functionSignatures: PersistentMap<String, FunctionSignature> = persistentMapOf(),
    
    /**
     * Parent context for nested scopes.
     * Allows lookup in outer scopes when variables are not found locally.
     */
    val parent: InferenceContext? = null,
    
    /**
     * Current scope level for debugging and scope management.
     * Root scope is level 0, nested scopes increment this value.
     */
    val scopeLevel: Int = 0
) {
    
    // =============================================================================
    // Variable Environment Operations
    // =============================================================================
    
    /**
     * Add a variable binding with a monomorphic type to the current scope.
     * Creates a new context with the updated type environment.
     */
    fun withVariable(name: String, type: Type): InferenceContext {
        val scheme = TypeScheme.monomorphic(type)
        return copy(typeEnvironment = typeEnvironment.put(name, scheme))
    }
    
    /**
     * Add a variable binding with a polymorphic type scheme to the current scope.
     * Used for let-polymorphism where variables can have quantified type variables.
     */
    fun withVariableScheme(name: String, scheme: TypeScheme): InferenceContext {
        return copy(typeEnvironment = typeEnvironment.put(name, scheme))
    }
    
    /**
     * Look up a variable's type scheme in the current context.
     * Searches current scope first, then parent scopes recursively.
     * Returns null if the variable is not found in any scope.
     */
    fun lookupVariable(name: String): TypeScheme? {
        // First check current scope
        typeEnvironment[name]?.let { return it }
        
        // If not found, check parent scopes
        return parent?.lookupVariable(name)
    }
    
    /**
     * Check if a variable exists in any accessible scope.
     */
    fun hasVariable(name: String): Boolean = lookupVariable(name) != null
    
    // =============================================================================
    // Type Definition Operations
    // =============================================================================
    
    /**
     * Add a type definition to the current context.
     * Creates a new context with the updated type definitions.
     */
    fun withTypeDefinition(name: String, definition: TypeDefinition): InferenceContext {
        return copy(typeDefinitions = typeDefinitions.put(name, definition))
    }
    
    /**
     * Look up a type definition in the current context.
     * Searches current scope first, then parent scopes recursively.
     */
    fun lookupTypeDefinition(name: String): TypeDefinition? {
        // First check current scope
        typeDefinitions[name]?.let { return it }
        
        // If not found, check parent scopes
        return parent?.lookupTypeDefinition(name)
    }
    
    // =============================================================================
    // Function Signature Operations
    // =============================================================================
    
    /**
     * Add a function signature to the current context.
     * Creates a new context with the updated function signatures.
     */
    fun withFunctionSignature(name: String, signature: FunctionSignature): InferenceContext {
        return copy(functionSignatures = functionSignatures.put(name, signature))
    }
    
    /**
     * Look up a function signature in the current context.
     * Searches current scope first, then parent scopes recursively.
     */
    fun lookupFunctionSignature(name: String): FunctionSignature? {
        // First check current scope
        functionSignatures[name]?.let { return it }
        
        // If not found, check parent scopes
        return parent?.lookupFunctionSignature(name)
    }
    
    // =============================================================================
    // Scope Management
    // =============================================================================
    
    /**
     * Create a new nested scope with this context as the parent.
     * Used for blocks, function bodies, and other scoped constructs.
     * Variables in the nested scope shadow variables in parent scopes.
     */
    fun enterScope(): InferenceContext {
        return InferenceContext(
            typeEnvironment = persistentMapOf(),
            typeDefinitions = persistentMapOf(),
            functionSignatures = persistentMapOf(),
            parent = this,
            scopeLevel = scopeLevel + 1
        )
    }
    
    /**
     * Create a new scope with additional variable bindings.
     * Useful for pattern matching where multiple variables are bound simultaneously.
     */
    fun enterScopeWith(bindings: Map<String, Type>): InferenceContext {
        val newEnv = bindings.map { (name, type) -> 
            name to TypeScheme.monomorphic(type) 
        }.toMap().toPersistentMap()
        
        return InferenceContext(
            typeEnvironment = newEnv,
            typeDefinitions = persistentMapOf(),
            functionSignatures = persistentMapOf(),
            parent = this,
            scopeLevel = scopeLevel + 1
        )
    }
    
    /**
     * Create a new scope with additional type scheme bindings.
     * Used for advanced polymorphic binding scenarios.
     */
    fun enterScopeWithSchemes(bindings: Map<String, TypeScheme>): InferenceContext {
        return InferenceContext(
            typeEnvironment = bindings.toPersistentMap(),
            typeDefinitions = persistentMapOf(),
            functionSignatures = persistentMapOf(),
            parent = this,
            scopeLevel = scopeLevel + 1
        )
    }
    
    // =============================================================================
    // Polymorphism and Generalization Support
    // =============================================================================
    
    /**
     * Get all free type variables in the current type environment.
     * Used for let-polymorphism to determine which type variables can be generalized.
     */
    fun getFreeTypeVars(): Set<TypeVar> {
        val currentFreeVars = typeEnvironment.values.flatMap { it.freeTypeVars() }.toSet()
        val parentFreeVars = parent?.getFreeTypeVars() ?: emptySet()
        return currentFreeVars + parentFreeVars
    }
    
    /**
     * Generalize a type by quantifying over type variables that are not free in the environment.
     * This implements let-polymorphism where local bindings can be generalized.
     * 
     * @param type The type to generalize
     * @param typeVars The type variables present in the type
     * @return A type scheme with appropriate quantification
     */
    fun generalize(type: Type, typeVars: Set<TypeVar>): TypeScheme {
        val envFreeVars = getFreeTypeVars()
        val generalizableVars = typeVars - envFreeVars
        return TypeScheme(generalizableVars, type)
    }
    
    // =============================================================================
    // Built-in Context Creation
    // =============================================================================
    
    companion object {
        /**
         * Create an empty inference context with no bindings.
         * Used as the root context for type inference.
         */
        fun empty(): InferenceContext = InferenceContext()
        
        /**
         * Create a context with built-in types and functions available.
         * This includes primitive types, standard library functions, etc.
         */
        fun withBuiltins(): InferenceContext {
            var context = empty()
            
            // Add built-in type definitions
            BuiltinTypes.primitives.forEach { (name, type) ->
                // For primitive types, we create empty union type definitions
                // This is a simplification - in a full implementation, these would be proper type definitions
                val definition = TypeDefinition.UnionTypeDef(emptyList(), emptyList())
                context = context.withTypeDefinition(name, definition)
            }
            
            return context
        }
        
        /**
         * Create a context from a TypeContext (used in existing TypeChecker).
         * This allows integration with the existing type checking infrastructure.
         */
        fun fromTypeContext(typeContext: TypeContext): InferenceContext {
            var context = empty()
            
            // Convert variables to type schemes
            typeContext.variables.forEach { (name, type) ->
                context = context.withVariable(name, type)
            }
            
            // Add function signatures
            typeContext.functions.forEach { (name, signature) ->
                context = context.withFunctionSignature(name, signature)
            }
            
            // Add type definitions
            typeContext.types.forEach { (name, definition) ->
                context = context.withTypeDefinition(name, definition)
            }
            
            return context
        }
    }
    
    // =============================================================================
    // Utility Methods
    // =============================================================================
    
    /**
     * Get all variable names bound in the current scope (not including parent scopes).
     */
    fun getLocalVariables(): Set<String> = typeEnvironment.keys
    
    /**
     * Get all variable names accessible in this context (including parent scopes).
     */
    fun getAllAccessibleVariables(): Set<String> {
        val localVars = typeEnvironment.keys
        val parentVars = parent?.getAllAccessibleVariables() ?: emptySet()
        return localVars + parentVars
    }
    
    /**
     * Check if this context is at the root scope (has no parent).
     */
    fun isRootScope(): Boolean = parent == null
    
    /**
     * Get the root context by traversing up the parent chain.
     */
    fun getRootContext(): InferenceContext {
        return parent?.getRootContext() ?: this
    }
    
    override fun toString(): String {
        val indent = "  ".repeat(scopeLevel)
        val vars = typeEnvironment.entries.joinToString(", ") { (name, scheme) ->
            "$name: $scheme"
        }
        return "${indent}InferenceContext(level=$scopeLevel, vars=[$vars])"
    }
}