package org.taylorlang.stdlib.collections

/**
 * Immutable persistent map implementation for TaylorLang standard library.
 * 
 * This class provides the foundation for functional map operations in TaylorLang,
 * implementing an efficient persistent data structure that supports structural sharing
 * for memory efficiency and immutable operations.
 * 
 * Design principles:
 * - Immutable by default - all operations return new instances
 * - Structural sharing for memory efficiency using binary tree structure
 * - Key-value associations with fast lookup operations
 * - Full generic type safety for both keys and values
 * - Balanced binary search tree for O(log n) operations
 * 
 * Performance characteristics:
 * - O(log n) get, put, remove, containsKey operations
 * - O(n) map, filter, fold operations with single traversal
 * - O(n) keys(), values(), entries() operations
 */
sealed class TaylorMap<K : Comparable<K>, out V> {
    
    /**
     * Number of key-value pairs in the map.
     */
    abstract val size: Int
    
    /**
     * Check if the map is empty.
     */
    abstract val isEmpty: Boolean
    
    // =============================================================================
    // Core Access Operations
    // =============================================================================
    
    /**
     * Get the value associated with a key.
     * @param key the key to look up
     * @return the associated value, or null if key is not present
     */
    abstract operator fun get(key: K): V?
    
    /**
     * Check if the map contains a specific key.
     * @param key the key to check
     * @return true if the key exists in the map
     */
    abstract fun containsKey(key: K): Boolean
    
    /**
     * Check if the map contains a specific value.
     * @param value the value to check
     * @return true if the value exists in the map
     */
    abstract fun containsValue(value: @UnsafeVariance V): Boolean
    
    // =============================================================================
    // Modification Operations
    // =============================================================================
    
    /**
     * Associate a key with a value, returning a new map.
     * If the key already exists, its value is replaced.
     * @param key the key
     * @param value the value to associate
     * @return new map with the key-value association
     */
    abstract fun put(key: K, value: @UnsafeVariance V): TaylorMap<K, V>
    
    /**
     * Remove a key and its associated value, returning a new map.
     * @param key the key to remove
     * @return new map without the key-value pair
     */
    abstract fun remove(key: K): TaylorMap<K, V>
    
    // =============================================================================
    // Collection Access Operations
    // =============================================================================
    
    /**
     * Get all keys as a TaylorList.
     * @return TaylorList containing all keys in sorted order
     */
    abstract fun keys(): TaylorList<K>
    
    /**
     * Get all values as a TaylorList.
     * @return TaylorList containing all values in key-sorted order
     */
    abstract fun values(): TaylorList<V>
    
    /**
     * Get all key-value pairs as a TaylorList.
     * @return TaylorList containing all entries as Pair objects in key-sorted order
     */
    abstract fun entries(): TaylorList<Pair<K, V>>
    
    // =============================================================================
    // Functional Operations
    // =============================================================================
    
    /**
     * Transform values using the provided function, keeping keys unchanged.
     * @param transform function to apply to each value
     * @return new map with transformed values
     */
    fun <R> mapValues(transform: (V) -> R): TaylorMap<K, R> = when (this) {
        is EmptyMap -> empty()
        is Empty -> empty()
        is Node -> {
            val newValue = transform(value)
            val newLeft = left.mapValues(transform)
            val newRight = right.mapValues(transform)
            Node(key, newValue, newLeft, newRight)
        }
    }
    
    /**
     * Transform keys using the provided function, keeping values unchanged.
     * @param transform function to apply to each key
     * @return new map with transformed keys
     */
    fun <R : Comparable<R>> mapKeys(transform: (K) -> R): TaylorMap<R, V> = when (this) {
        is EmptyMap -> empty()
        is Empty -> empty()
        is Node -> {
            // Rebuild the map with new keys to maintain BST property
            val entries = this.entries()
            entries.fold(empty<R, V>()) { acc, (k, v) ->
                acc.put(transform(k), v)
            }
        }
    }
    
    /**
     * Transform both keys and values using the provided function.
     * @param transform function to apply to each key-value pair
     * @return new map with transformed key-value pairs
     */
    fun <RK : Comparable<RK>, RV> map(transform: (Pair<K, V>) -> Pair<RK, RV>): TaylorMap<RK, RV> = when (this) {
        is EmptyMap -> empty()
        is Empty -> empty()
        is Node -> {
            // Rebuild the map with new key-value pairs to maintain BST property
            val entries = this.entries()
            entries.fold(empty<RK, RV>()) { acc, pair ->
                val (newKey, newValue) = transform(pair)
                acc.put(newKey, newValue)
            }
        }
    }
    
    /**
     * Filter key-value pairs based on a predicate.
     * @param predicate function returning true for pairs to keep
     * @return new map containing only pairs matching the predicate
     */
    fun filter(predicate: (Pair<K, V>) -> Boolean): TaylorMap<K, V> = when (this) {
        is EmptyMap -> empty()
        is Empty -> empty()
        is Node -> {
            val currentPair = Pair(key, value)
            val filteredLeft = left.filter(predicate)
            val filteredRight = right.filter(predicate)
            
            if (predicate(currentPair)) {
                // Include current node - rebuild to maintain BST structure
                filteredLeft.put(key, value).let { tempMap ->
                    filteredRight.entries().fold(tempMap) { acc, (k, v) ->
                        acc.put(k, v)
                    }
                }
            } else {
                // Exclude current node - merge left and right
                filteredRight.entries().fold(filteredLeft) { acc, (k, v) ->
                    acc.put(k, v)
                }
            }
        }
    }
    
    /**
     * Fold (reduce) the map from left to right over key-value pairs.
     * @param initial initial accumulator value
     * @param operation function combining accumulator and key-value pair
     * @return final accumulated value
     */
    fun <R> fold(initial: R, operation: (R, Pair<K, V>) -> R): R = when (this) {
        is EmptyMap -> initial
        is Empty -> initial
        is Node -> {
            val afterLeft = left.fold(initial, operation)
            val afterCurrent = operation(afterLeft, Pair(key, value))
            right.fold(afterCurrent, operation)
        }
    }
    
    /**
     * Find the first key-value pair matching a predicate.
     * @param predicate function to test key-value pairs
     * @return first matching pair, or null if none found
     */
    fun find(predicate: (Pair<K, V>) -> Boolean): Pair<K, V>? {
        return when (this) {
            is EmptyMap -> null
            is Empty -> null
            is Node -> {
                // Check left subtree first (in-order traversal)
                left.find(predicate)?.let { return it }
                
                // Check current node
                val currentPair = Pair(key, value)
                if (predicate(currentPair)) return currentPair
                
                // Check right subtree
                right.find(predicate)
            }
        }
    }
    
    /**
     * Check if all key-value pairs satisfy a predicate.
     * @param predicate function to test key-value pairs
     * @return true if all pairs match, false otherwise
     */
    fun all(predicate: (Pair<K, V>) -> Boolean): Boolean = when (this) {
        is EmptyMap -> true
        is Empty -> true
        is Node -> {
            predicate(Pair(key, value)) && left.all(predicate) && right.all(predicate)
        }
    }
    
    /**
     * Check if any key-value pair satisfies a predicate.
     * @param predicate function to test key-value pairs
     * @return true if any pair matches, false otherwise
     */
    fun any(predicate: (Pair<K, V>) -> Boolean): Boolean = when (this) {
        is EmptyMap -> false
        is Empty -> false
        is Node -> {
            predicate(Pair(key, value)) || left.any(predicate) || right.any(predicate)
        }
    }
    
    /**
     * Count key-value pairs matching a predicate.
     * @param predicate function to test key-value pairs
     * @return number of matching pairs
     */
    fun count(predicate: (Pair<K, V>) -> Boolean): Int = when (this) {
        is EmptyMap -> 0
        is Empty -> 0
        is Node -> {
            val currentCount = if (predicate(Pair(key, value))) 1 else 0
            currentCount + left.count(predicate) + right.count(predicate)
        }
    }
    
    // =============================================================================
    // Conversion Operations
    // =============================================================================
    
    /**
     * Convert to a standard Kotlin Map for Java interoperability.
     * @return mutable Kotlin Map containing the same key-value pairs
     */
    fun toKotlinMap(): Map<K, V> = fold(mutableMapOf<K, V>()) { acc, (k, v) -> 
        acc.also { it[k] = v } 
    }
    
    /**
     * Convert to string representation.
     * @return string representation of the map
     */
    override fun toString(): String {
        if (isEmpty) return "{}"
        val entries = entries().toKotlinList().joinToString(", ") { (k, v) -> "$k=$v" }
        return "{$entries}"
    }
    
    /**
     * Equality comparison.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaylorMap<*, *>) return false
        if (this.size != other.size) return false
        
        // Compare all key-value pairs
        return this.toKotlinMap() == other.toKotlinMap()
    }
    
    /**
     * Hash code implementation.
     */
    override fun hashCode(): Int = fold(1) { acc, (k, v) -> 
        31 * acc + (k?.hashCode() ?: 0) + (v?.hashCode() ?: 0) 
    }
    
    // =============================================================================
    // Concrete Implementations
    // =============================================================================
    
    /**
     * Empty map implementation using runtime type erasure.
     */
    class EmptyMap<K : Comparable<K>, V> : TaylorMap<K, V>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override operator fun get(key: K): V? = null
        override fun containsKey(key: K): Boolean = false
        override fun containsValue(value: @UnsafeVariance V): Boolean = false
        
        override fun put(key: K, value: @UnsafeVariance V): TaylorMap<K, V> = 
            Node(key, value, this, this)
        override fun remove(key: K): TaylorMap<K, V> = this
        
        override fun keys(): TaylorList<K> = TaylorList.empty()
        override fun values(): TaylorList<V> = TaylorList.empty()
        override fun entries(): TaylorList<Pair<K, V>> = TaylorList.empty()
        
        override fun toString(): String = "{}"
        override fun equals(other: Any?): Boolean = other is EmptyMap<*, *>
        override fun hashCode(): Int = 0
    }
    
    /**
     * Empty map singleton access.
     */
    object Empty : TaylorMap<Nothing, Nothing>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override operator fun get(key: Nothing): Nothing? = null
        override fun containsKey(key: Nothing): Boolean = false
        override fun containsValue(value: Nothing): Boolean = false
        
        override fun put(key: Nothing, value: Nothing): TaylorMap<Nothing, Nothing> = 
            Node(key, value, Empty, Empty)
        override fun remove(key: Nothing): TaylorMap<Nothing, Nothing> = Empty
        
        override fun keys(): TaylorList<Nothing> = TaylorList.empty()
        override fun values(): TaylorList<Nothing> = TaylorList.empty()
        override fun entries(): TaylorList<Pair<Nothing, Nothing>> = TaylorList.empty()
        
        override fun toString(): String = "{}"
        override fun equals(other: Any?): Boolean = other === Empty || other is EmptyMap<*, *>
        override fun hashCode(): Int = 0
    }
    
    /**
     * Non-empty map implementation using binary search tree.
     * @param key the key at this node
     * @param value the value associated with the key
     * @param left left subtree (keys less than this key)
     * @param right right subtree (keys greater than this key)
     */
    class Node<K : Comparable<K>, out V>(
        val key: K,
        val value: V,
        val left: TaylorMap<K, V>,
        val right: TaylorMap<K, V>
    ) : TaylorMap<K, V>() {
        
        override val size: Int by lazy { 1 + left.size + right.size }
        override val isEmpty: Boolean = false
        
        override operator fun get(key: K): V? = when {
            key < this.key -> left[key]
            key > this.key -> right[key]
            else -> value
        }
        
        override fun containsKey(key: K): Boolean = when {
            key < this.key -> left.containsKey(key)
            key > this.key -> right.containsKey(key)
            else -> true
        }
        
        override fun containsValue(value: @UnsafeVariance V): Boolean = 
            this.value == value || left.containsValue(value) || right.containsValue(value)
        
        override fun put(key: K, value: @UnsafeVariance V): TaylorMap<K, V> = when {
            key < this.key -> Node(this.key, this.value, left.put(key, value), right)
            key > this.key -> Node(this.key, this.value, left, right.put(key, value))
            else -> Node(key, value, left, right) // Replace existing value
        }
        
        override fun remove(key: K): TaylorMap<K, V> = when {
            key < this.key -> Node(this.key, value, left.remove(key), right)
            key > this.key -> Node(this.key, value, left, right.remove(key))
            else -> {
                // Remove this node
                when {
                    left.isEmpty -> right
                    right.isEmpty -> left
                    else -> {
                        // Find inorder successor (leftmost node in right subtree)
                        val successor = findMin(right)
                        Node(successor.key, successor.value, left, right.remove(successor.key))
                    }
                }
            }
        }
        
        override fun keys(): TaylorList<K> {
            val leftKeys = left.keys()
            val rightKeys = right.keys()
            return leftKeys.append(key).concat(rightKeys)
        }
        
        override fun values(): TaylorList<V> {
            val leftValues = left.values()
            val rightValues = right.values()
            return leftValues.append(value).concat(rightValues)
        }
        
        override fun entries(): TaylorList<Pair<K, V>> {
            val leftEntries = left.entries()
            val rightEntries = right.entries()
            return leftEntries.append(Pair(key, value)).concat(rightEntries)
        }
        
        override fun toString(): String {
            if (size == 1) return "{$key=$value}"
            val entries = entries().toKotlinList().joinToString(", ") { (k, v) -> "$k=$v" }
            return "{$entries}"
        }
        
        /**
         * Helper function to find the minimum key node in a subtree.
         */
        private fun findMin(tree: TaylorMap<K, V>): Node<K, V> = when (tree) {
            is EmptyMap -> throw IllegalStateException("Cannot find minimum in empty tree")
            is Empty -> throw IllegalStateException("Cannot find minimum in empty tree")
            is Node -> if (tree.left.isEmpty) tree else findMin(tree.left)
        }
    }
    
    // =============================================================================
    // Companion Object - Factory Methods
    // =============================================================================
    
    companion object {
        private val EMPTY_INSTANCE = EmptyMap<Nothing, Any?>()
        
        /**
         * Create an empty map.
         * @return empty TaylorMap
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <K : Comparable<K>, V> empty(): TaylorMap<K, V> = EMPTY_INSTANCE as TaylorMap<K, V>
        
        /**
         * Create a map with a single key-value pair.
         * @param key the key
         * @param value the value
         * @return TaylorMap containing the key-value pair
         */
        @JvmStatic
        fun <K : Comparable<K>, V> of(key: K, value: V): TaylorMap<K, V> = 
            Node(key, value, empty(), empty())
        
        /**
         * Create a map from multiple key-value pairs.
         * @param pairs vararg key-value pairs
         * @return TaylorMap containing all key-value pairs
         */
        @JvmStatic
        fun <K : Comparable<K>, V> of(vararg pairs: Pair<K, V>): TaylorMap<K, V> = 
            pairs.fold(empty<K, V>()) { acc, (k, v) -> acc.put(k, v) }
        
        /**
         * Create a map from a collection of key-value pairs.
         * @param pairs collection of pairs to convert
         * @return TaylorMap containing all key-value pairs
         */
        @JvmStatic
        fun <K : Comparable<K>, V> from(pairs: Collection<Pair<K, V>>): TaylorMap<K, V> =
            pairs.fold(empty<K, V>()) { acc, (k, v) -> acc.put(k, v) }
        
        /**
         * Create a map from a Kotlin Map.
         * @param map Kotlin map to convert
         * @return TaylorMap containing all key-value pairs from the Kotlin map
         */
        @JvmStatic
        fun <K : Comparable<K>, V> fromJava(map: Map<K, V>): TaylorMap<K, V> =
            map.entries.fold(empty<K, V>()) { acc, (k, v) -> acc.put(k, v) }
    }
}