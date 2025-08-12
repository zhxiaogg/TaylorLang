package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.taylorlang.ast.Type

/**
 * Type checking context containing symbol tables and type information.
 * 
 * The TypeContext maintains the state needed for type checking, including:
 * - Variable bindings (name -> type)
 * - Function signatures (name -> signature)  
 * - Type definitions (name -> definition)
 * 
 * This class is immutable and uses persistent data structures for efficient
 * copying when creating new scopes or adding bindings.
 * 
 * Design principles:
 * - Immutable for thread safety and predictable behavior
 * - Efficient scope management through structural sharing
 * - Clear separation of different kinds of symbols
 * - Support for nested scopes through context chaining
 */
data class TypeContext(
    val variables: PersistentMap<String, Type> = persistentMapOf(),
    val functions: PersistentMap<String, FunctionSignature> = persistentMapOf(),
    val types: PersistentMap<String, TypeDefinition> = persistentMapOf()
) {
    
    // =============================================================================
    // Context Modification (Immutable)
    // =============================================================================
    
    /**
     * Create a new context with an additional variable binding.
     * @param name The variable name
     * @param type The type of the variable
     * @return New context with the variable binding added
     */
    fun withVariable(name: String, type: Type): TypeContext {
        return copy(variables = variables.put(name, type))
    }
    
    /**
     * Create a new context with multiple variable bindings.
     * @param bindings Map of variable names to types
     * @return New context with all variable bindings added
     */
    fun withVariables(bindings: Map<String, Type>): TypeContext {
        return copy(variables = variables.putAll(bindings))
    }
    
    /**
     * Create a new context with an additional function signature.
     * @param name The function name
     * @param signature The function signature
     * @return New context with the function signature added
     */
    fun withFunction(name: String, signature: FunctionSignature): TypeContext {
        return copy(functions = functions.put(name, signature))
    }
    
    /**
     * Create a new context with multiple function signatures.
     * @param signatures Map of function names to signatures
     * @return New context with all function signatures added
     */
    fun withFunctions(signatures: Map<String, FunctionSignature>): TypeContext {
        return copy(functions = functions.putAll(signatures))
    }
    
    /**
     * Create a new context with an additional type definition.
     * @param name The type name
     * @param definition The type definition
     * @return New context with the type definition added
     */
    fun withType(name: String, definition: TypeDefinition): TypeContext {
        return copy(types = types.put(name, definition))
    }
    
    /**
     * Create a new context with multiple type definitions.
     * @param definitions Map of type names to definitions
     * @return New context with all type definitions added
     */
    fun withTypes(definitions: Map<String, TypeDefinition>): TypeContext {
        return copy(types = types.putAll(definitions))
    }
    
    // =============================================================================
    // Symbol Lookup
    // =============================================================================
    
    /**
     * Look up a variable type by name.
     * @param name The variable name
     * @return The variable type if found, null otherwise
     */
    fun lookupVariable(name: String): Type? {
        return variables[name]
    }
    
    /**
     * Look up a function signature by name.
     * @param name The function name
     * @return The function signature if found, null otherwise
     */
    fun lookupFunction(name: String): FunctionSignature? {
        return functions[name]
    }
    
    /**
     * Look up a type definition by name.
     * @param name The type name
     * @return The type definition if found, null otherwise
     */
    fun lookupType(name: String): TypeDefinition? {
        return types[name]
    }
    
    /**
     * Check if a variable is defined in this context.
     * @param name The variable name
     * @return true if the variable is defined
     */
    fun hasVariable(name: String): Boolean {
        return variables.containsKey(name)
    }
    
    /**
     * Check if a function is defined in this context.
     * @param name The function name
     * @return true if the function is defined
     */
    fun hasFunction(name: String): Boolean {
        return functions.containsKey(name)
    }
    
    /**
     * Check if a type is defined in this context.
     * @param name The type name
     * @return true if the type is defined
     */
    fun hasType(name: String): Boolean {
        return types.containsKey(name)
    }
    
    // =============================================================================
    // Context Queries and Utilities
    // =============================================================================
    
    /**
     * Get all variable names defined in this context.
     * @return Set of all variable names
     */
    fun getAllVariableNames(): Set<String> {
        return variables.keys
    }
    
    /**
     * Get all function names defined in this context.
     * @return Set of all function names
     */
    fun getAllFunctionNames(): Set<String> {
        return functions.keys
    }
    
    /**
     * Get all type names defined in this context.
     * @return Set of all type names
     */
    fun getAllTypeNames(): Set<String> {
        return types.keys
    }
    
    /**
     * Create an empty context.
     * @return Empty TypeContext with no bindings
     */
    fun empty(): TypeContext {
        return TypeContext()
    }
    
    /**
     * Check if this context is empty (has no bindings).
     * @return true if the context has no variables, functions, or types
     */
    fun isEmpty(): Boolean {
        return variables.isEmpty() && functions.isEmpty() && types.isEmpty()
    }
    
    /**
     * Get the total number of bindings in this context.
     * @return Total count of variables + functions + types
     */
    fun size(): Int {
        return variables.size + functions.size + types.size
    }
    
    // =============================================================================
    // Companion Object - Factory Methods
    // =============================================================================
    
    companion object {
        /**
         * Create a context with built-in types and functions.
         * @return TypeContext initialized with built-in definitions
         */
        fun withBuiltins(): TypeContext {
            // Create empty type definitions for primitive types
            // (primitives don't have constructors, so empty variant lists)
            val primitiveTypeDefs = BuiltinTypes.primitives.mapValues { 
                TypeDefinition.UnionTypeDef(emptyList(), emptyList()) 
            }
            
            // Add generic type definitions for built-in containers
            val containerTypeDefs = mapOf(
                "List" to TypeDefinition.UnionTypeDef(
                    typeParameters = listOf("T"),
                    variants = emptyList() // List is handled specially in the runtime
                )
            )
            
            // Add builtin functions
            // Note: For now, we'll model println as polymorphic accepting any single argument
            // In a real implementation, we'd want proper function overloading support
            val builtinFunctions = mapOf(
                "println" to FunctionSignature(
                    typeParameters = listOf("T"), // Generic type parameter
                    parameterTypes = listOf(Type.NamedType("T")),
                    returnType = BuiltinTypes.UNIT
                ),
                // List construction functions
                "emptyList" to FunctionSignature(
                    typeParameters = listOf("T"), 
                    parameterTypes = listOf(),
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                ),
                "singletonList" to FunctionSignature(
                    typeParameters = listOf("T"),
                    parameterTypes = listOf(Type.NamedType("T")),
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                ),
                // listOf with single parameter
                "listOf" to FunctionSignature(
                    typeParameters = listOf("T"),
                    parameterTypes = listOf(Type.NamedType("T")), // listOf(element)
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                ),
                // listOf2 for two parameters
                "listOf2" to FunctionSignature(
                    typeParameters = listOf("T"),
                    parameterTypes = listOf(Type.NamedType("T"), Type.NamedType("T")), // listOf(elem1, elem2)
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                ),
                "listOf3" to FunctionSignature(
                    typeParameters = listOf("T"),
                    parameterTypes = listOf(Type.NamedType("T"), Type.NamedType("T"), Type.NamedType("T")), // listOf(elem1, elem2, elem3)
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                ),
                "listOf4" to FunctionSignature(
                    typeParameters = listOf("T"),
                    parameterTypes = listOf(Type.NamedType("T"), Type.NamedType("T"), Type.NamedType("T"), Type.NamedType("T")), // listOf(elem1, elem2, elem3, elem4)
                    returnType = Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(Type.NamedType("T")))
                )
            )
            
            return TypeContext(
                types = (primitiveTypeDefs + containerTypeDefs).toPersistentMap(),
                functions = builtinFunctions.toPersistentMap()
            )
        }
        
        /**
         * Create an empty context.
         * @return Empty TypeContext with no bindings
         */
        fun empty(): TypeContext {
            return TypeContext()
        }
        
        /**
         * Create a context from maps of bindings.
         * @param variables Variable bindings
         * @param functions Function bindings
         * @param types Type bindings
         * @return TypeContext with the specified bindings
         */
        fun from(
            variables: Map<String, Type> = emptyMap(),
            functions: Map<String, FunctionSignature> = emptyMap(),
            types: Map<String, TypeDefinition> = emptyMap()
        ): TypeContext {
            return TypeContext(
                variables = variables.toPersistentMap(),
                functions = functions.toPersistentMap(),
                types = types.toPersistentMap()
            )
        }
    }
}