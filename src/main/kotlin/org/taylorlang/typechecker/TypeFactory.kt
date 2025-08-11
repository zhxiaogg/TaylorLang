package org.taylorlang.typechecker

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized type creation with caching and interning.
 * 
 * This factory provides optimized type creation through caching frequently-used type instances,
 * reducing memory overhead and object creation costs. It follows the proven architectural patterns
 * established in the TaylorLang codebase.
 * 
 * Key optimizations:
 * - Primitive type singletons cached by name
 * - Generic type interning with composite key caching
 * - Function type caching by signature
 * - Thread-safe concurrent access
 * 
 * Performance targets:
 * - 70%+ reduction in type object creation overhead
 * - 15% memory usage reduction via interning
 * - Sub-millisecond type creation for cached instances
 */
object TypeFactory {
    
    // =============================================================================
    // Cache Data Structures
    // =============================================================================
    
    /**
     * Composite key for generic type caching.
     * Ensures proper equality and hashing for cache lookups.
     */
    private data class GenericTypeKey(
        val name: String,
        val arguments: List<Type>
    ) {
        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + arguments.hashCode()
            return result
        }
    }
    
    /**
     * Composite key for function type caching.
     * Includes parameter types and return type for comprehensive caching.
     */
    private data class FunctionTypeKey(
        val parameterTypes: List<Type>,
        val returnType: Type
    ) {
        override fun hashCode(): Int {
            var result = parameterTypes.hashCode()
            result = 31 * result + returnType.hashCode()
            return result
        }
    }
    
    /**
     * Composite key for tuple type caching.
     * Based on element type list for efficient deduplication.
     */
    private data class TupleTypeKey(
        val elementTypes: List<Type>
    ) {
        override fun hashCode(): Int = elementTypes.hashCode()
    }
    
    // =============================================================================
    // Thread-Safe Caches
    // =============================================================================
    
    /**
     * Cache for primitive types - singleton instances by name.
     * Thread-safe concurrent access for high-frequency operations.
     */
    private val primitiveCache = ConcurrentHashMap<String, Type.PrimitiveType>()
    
    /**
     * Cache for named types - commonly referenced user-defined types.
     */
    private val namedTypeCache = ConcurrentHashMap<String, Type.NamedType>()
    
    /**
     * Cache for generic types with composite key based on name and arguments.
     * Handles complex generic instantiations efficiently.
     */
    private val genericCache = ConcurrentHashMap<GenericTypeKey, Type.GenericType>()
    
    /**
     * Cache for function types by parameter and return type signature.
     * Optimizes function type creation in type checking.
     */
    private val functionCache = ConcurrentHashMap<FunctionTypeKey, Type.FunctionType>()
    
    /**
     * Cache for tuple types by element type list.
     * Reduces overhead for common tuple patterns.
     */
    private val tupleCache = ConcurrentHashMap<TupleTypeKey, Type.TupleType>()
    
    /**
     * Cache for nullable types wrapping base types.
     */
    private val nullableCache = ConcurrentHashMap<Type, Type.NullableType>()
    
    /**
     * Cache for union type references with type arguments.
     */
    private val unionCache = ConcurrentHashMap<GenericTypeKey, Type.UnionType>()
    
    // =============================================================================
    // Core Type Creation Methods
    // =============================================================================
    
    /**
     * Create or retrieve cached primitive type.
     * 
     * Primitive types are singleton instances for optimal memory usage.
     * This method is thread-safe and optimized for high-frequency access.
     * 
     * @param name The primitive type name (e.g., "Int", "String")
     * @return Cached or newly created primitive type
     */
    fun createPrimitive(name: String): Type.PrimitiveType {
        return primitiveCache.computeIfAbsent(name) { 
            Type.PrimitiveType(it)
        }
    }
    
    /**
     * Create or retrieve cached named type.
     * 
     * Named types represent user-defined types and type parameters.
     * Caching reduces object overhead for frequently referenced types.
     * 
     * @param name The named type identifier
     * @return Cached or newly created named type
     */
    fun createNamed(name: String): Type.NamedType {
        return namedTypeCache.computeIfAbsent(name) {
            Type.NamedType(it)
        }
    }
    
    /**
     * Create or retrieve cached generic type.
     * 
     * Generic types are cached by composite key including name and type arguments.
     * This enables efficient sharing of common generic instantiations.
     * 
     * @param name The generic type name (e.g., "List", "Map")
     * @param arguments Type arguments for generic instantiation
     * @return Cached or newly created generic type
     */
    fun createGeneric(name: String, arguments: List<Type>): Type.GenericType {
        val key = GenericTypeKey(name, arguments)
        return genericCache.computeIfAbsent(key) { 
            Type.GenericType(
                name = it.name,
                arguments = it.arguments.toPersistentList()
            )
        }
    }
    
    /**
     * Create or retrieve cached function type.
     * 
     * Function types are cached by complete signature (parameters + return type).
     * This optimization is critical for type checker performance.
     * 
     * @param parameterTypes List of parameter types
     * @param returnType Function return type
     * @return Cached or newly created function type
     */
    fun createFunction(parameterTypes: List<Type>, returnType: Type): Type.FunctionType {
        val key = FunctionTypeKey(parameterTypes, returnType)
        return functionCache.computeIfAbsent(key) {
            Type.FunctionType(
                parameterTypes = it.parameterTypes.toPersistentList(),
                returnType = it.returnType
            )
        }
    }
    
    /**
     * Create or retrieve cached tuple type.
     * 
     * Tuple types are cached by their element type list.
     * Common tuple patterns benefit significantly from this optimization.
     * 
     * @param elementTypes List of tuple element types
     * @return Cached or newly created tuple type
     */
    fun createTuple(elementTypes: List<Type>): Type.TupleType {
        val key = TupleTypeKey(elementTypes)
        return tupleCache.computeIfAbsent(key) {
            Type.TupleType(
                elementTypes = it.elementTypes.toPersistentList()
            )
        }
    }
    
    /**
     * Create or retrieve cached nullable type.
     * 
     * Nullable types wrap base types and are cached by base type.
     * This reduces overhead for nullable variants of common types.
     * 
     * @param baseType The type to make nullable
     * @return Cached or newly created nullable type
     */
    fun createNullable(baseType: Type): Type.NullableType {
        return nullableCache.computeIfAbsent(baseType) {
            Type.NullableType(baseType = it)
        }
    }
    
    /**
     * Create or retrieve cached union type reference.
     * 
     * Union types are cached by name and type arguments similar to generic types.
     * This enables efficient sharing of union type instances.
     * 
     * @param name The union type name
     * @param typeArguments Type arguments for generic union types
     * @return Cached or newly created union type reference
     */
    fun createUnion(name: String, typeArguments: List<Type> = emptyList()): Type.UnionType {
        val key = GenericTypeKey(name, typeArguments)
        return unionCache.computeIfAbsent(key) {
            Type.UnionType(
                name = it.name,
                typeArguments = it.arguments.toPersistentList()
            )
        }
    }
    
    /**
     * Create type variable for type inference.
     * 
     * Type variables are not cached as they represent unique inference variables.
     * Each call creates a fresh type variable instance.
     * 
     * @param id Unique identifier for the type variable
     * @return New type variable instance
     */
    fun createTypeVar(id: String): Type.TypeVar {
        return Type.TypeVar(id)
    }
    
    // =============================================================================
    // Convenience Methods for Common Patterns
    // =============================================================================
    
    /**
     * Create builtin primitive types using cached instances.
     * Provides convenient access to frequently used primitive types.
     */
    object Builtins {
        val INT: Type.PrimitiveType get() = createPrimitive("Int")
        val LONG: Type.PrimitiveType get() = createPrimitive("Long")
        val FLOAT: Type.PrimitiveType get() = createPrimitive("Float")
        val DOUBLE: Type.PrimitiveType get() = createPrimitive("Double")
        val BOOLEAN: Type.PrimitiveType get() = createPrimitive("Boolean")
        val STRING: Type.PrimitiveType get() = createPrimitive("String")
        val UNIT: Type.PrimitiveType get() = createPrimitive("Unit")
    }
    
    /**
     * Create list type with element type.
     * Common generic type pattern with optimization.
     */
    fun createList(elementType: Type): Type.GenericType {
        return createGeneric("List", listOf(elementType))
    }
    
    /**
     * Create map type with key and value types.
     * Another common generic pattern for convenience.
     */
    fun createMap(keyType: Type, valueType: Type): Type.GenericType {
        return createGeneric("Map", listOf(keyType, valueType))
    }
    
    /**
     * Create option type with element type.
     * Common pattern for nullable-like types.
     */
    fun createOption(elementType: Type): Type.GenericType {
        return createGeneric("Option", listOf(elementType))
    }
    
    // =============================================================================
    // Cache Management and Statistics
    // =============================================================================
    
    /**
     * Cache statistics for performance monitoring and optimization.
     */
    data class CacheStats(
        val primitiveHits: Int,
        val genericHits: Int,
        val functionHits: Int,
        val tupleHits: Int,
        val totalCacheSize: Int
    )
    
    /**
     * Get current cache statistics for performance analysis.
     * Useful for monitoring optimization effectiveness.
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            primitiveHits = primitiveCache.size,
            genericHits = genericCache.size,
            functionHits = functionCache.size,
            tupleHits = tupleCache.size,
            totalCacheSize = primitiveCache.size + genericCache.size + 
                           functionCache.size + tupleCache.size + 
                           nullableCache.size + unionCache.size
        )
    }
    
    /**
     * Clear all caches - primarily for testing and memory management.
     * Use cautiously as it eliminates performance optimizations.
     */
    fun clearCaches() {
        primitiveCache.clear()
        namedTypeCache.clear()
        genericCache.clear()
        functionCache.clear()
        tupleCache.clear()
        nullableCache.clear()
        unionCache.clear()
    }
    
    /**
     * Get cache memory usage estimate in bytes.
     * Rough estimate for performance monitoring.
     */
    fun estimateMemoryUsage(): Long {
        // Rough estimate: each cached type ~100 bytes + key overhead
        val totalEntries = getCacheStats().totalCacheSize
        return totalEntries * 150L // Conservative estimate
    }
}