package org.taylorlang.typechecker

import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

/**
 * Represents a variable binding in a scope.
 * 
 * @param name The variable name
 * @param type The variable's type
 * @param isMutable Whether the variable can be reassigned (true for var, false for val)
 * @param location Source location where the variable was declared
 */
data class VariableBinding(
    val name: String,
    val type: Type,
    val isMutable: Boolean,
    val location: SourceLocation? = null
)

/**
 * Represents a single scope containing variable bindings.
 * Scopes are organized in a stack to handle nested block scopes.
 */
data class Scope(
    val variables: MutableMap<String, VariableBinding> = mutableMapOf()
) {
    /**
     * Add a variable binding to this scope.
     * Returns true if successful, false if variable already exists in this scope.
     */
    fun addVariable(binding: VariableBinding): Boolean {
        if (variables.containsKey(binding.name)) {
            return false // Variable already exists in this scope
        }
        variables[binding.name] = binding
        return true
    }
    
    /**
     * Look up a variable in this scope only.
     */
    fun lookupLocal(name: String): VariableBinding? {
        return variables[name]
    }
    
    /**
     * Get all variable names in this scope.
     */
    fun getVariableNames(): Set<String> {
        return variables.keys
    }
}

/**
 * Manages variable scopes for type checking.
 * 
 * This class maintains a stack of scopes to handle:
 * - Global scope (outermost)
 * - Function parameter scopes
 * - Block scopes (for nested blocks, if expressions, while loops, etc.)
 * 
 * Key responsibilities:
 * - Track variable declarations and their types
 * - Enforce scoping rules (inner scopes can shadow outer scopes)
 * - Distinguish between mutable (var) and immutable (val) variables
 * - Provide variable lookup with proper scope resolution
 */
class ScopeManager {
    // Stack of scopes - innermost scope is at the top
    private val scopeStack = mutableListOf<Scope>()
    
    init {
        // Always start with a global scope
        pushScope()
    }
    
    /**
     * Push a new scope onto the stack (entering a new block/function).
     */
    fun pushScope() {
        scopeStack.add(Scope())
    }
    
    /**
     * Pop the current scope from the stack (leaving a block/function).
     * @throws IllegalStateException if trying to pop the global scope
     */
    fun popScope() {
        if (scopeStack.size <= 1) {
            throw IllegalStateException("Cannot pop global scope")
        }
        scopeStack.removeAt(scopeStack.size - 1)
    }
    
    /**
     * Get the current (innermost) scope.
     */
    private fun currentScope(): Scope {
        return scopeStack.last()
    }
    
    /**
     * Declare a new variable in the current scope.
     * 
     * @param name Variable name
     * @param type Variable type
     * @param isMutable Whether the variable is mutable (var vs val)
     * @param location Source location of the declaration
     * @return Result indicating success or failure with error details
     */
    fun declareVariable(
        name: String, 
        type: Type, 
        isMutable: Boolean, 
        location: SourceLocation? = null
    ): Result<Unit> {
        val binding = VariableBinding(name, type, isMutable, location)
        
        return if (currentScope().addVariable(binding)) {
            Result.success(Unit)
        } else {
            Result.failure(TypeError.DuplicateDefinition(
                "Variable '$name' is already declared in this scope",
                location
            ))
        }
    }
    
    /**
     * Look up a variable by name, searching from innermost to outermost scope.
     * 
     * @param name Variable name to look up
     * @return The variable binding if found, null otherwise
     */
    fun lookupVariable(name: String): VariableBinding? {
        // Search from innermost to outermost scope
        for (i in scopeStack.size - 1 downTo 0) {
            val binding = scopeStack[i].lookupLocal(name)
            if (binding != null) {
                return binding
            }
        }
        return null
    }
    
    /**
     * Check if a variable exists in any scope.
     */
    fun isVariableDefined(name: String): Boolean {
        return lookupVariable(name) != null
    }
    
    /**
     * Check if a variable is mutable (can be reassigned).
     * 
     * @param name Variable name to check
     * @return Result with boolean if variable exists, error if not found
     */
    fun isVariableMutable(name: String, location: SourceLocation? = null): Result<Boolean> {
        val binding = lookupVariable(name)
        return if (binding != null) {
            Result.success(binding.isMutable)
        } else {
            Result.failure(TypeError.UnresolvedSymbol(name, location))
        }
    }
    
    /**
     * Get the type of a variable.
     * 
     * @param name Variable name to look up
     * @return Result with type if variable exists, error if not found
     */
    fun getVariableType(name: String, location: SourceLocation? = null): Result<Type> {
        val binding = lookupVariable(name)
        return if (binding != null) {
            Result.success(binding.type)
        } else {
            Result.failure(TypeError.UnresolvedSymbol(name, location))
        }
    }
    
    /**
     * Get all variables visible in the current scope (including outer scopes).
     */
    fun getAllVisibleVariables(): Map<String, VariableBinding> {
        val allVariables = mutableMapOf<String, VariableBinding>()
        
        // Add from outermost to innermost so inner variables shadow outer ones
        for (scope in scopeStack) {
            for ((name, binding) in scope.variables) {
                allVariables[name] = binding
            }
        }
        
        return allVariables
    }
    
    /**
     * Get the current scope depth (0 for global scope).
     */
    fun getScopeDepth(): Int {
        return scopeStack.size - 1
    }
    
    /**
     * For debugging: get a string representation of the scope stack.
     */
    fun debugScopeStack(): String {
        return buildString {
            appendLine("Scope Stack (${scopeStack.size} scopes):")
            for (i in scopeStack.size - 1 downTo 0) {
                val scope = scopeStack[i]
                val level = if (i == 0) "Global" else "Level $i"
                appendLine("  $level: ${scope.variables.keys}")
            }
        }
    }
}

