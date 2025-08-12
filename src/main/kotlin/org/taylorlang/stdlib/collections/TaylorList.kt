package org.taylorlang.stdlib.collections

/**
 * Immutable persistent list implementation for TaylorLang standard library.
 * 
 * This class provides the foundation for functional list operations in TaylorLang,
 * implementing an efficient persistent data structure that supports structural sharing
 * for memory efficiency and immutable operations.
 * 
 * Design principles:
 * - Immutable by default - all operations return new instances
 * - Structural sharing for memory efficiency  
 * - Head/tail structure compatible with pattern matching
 * - Lazy evaluation for chained operations where beneficial
 * - Full generic type safety
 * 
 * Performance characteristics:
 * - O(1) head access and prepend operations
 * - O(n) append, get(index), and size operations
 * - O(n) map, filter, fold operations with single traversal
 */
sealed class TaylorList<out T> {
    
    /**
     * Number of elements in the list.
     * Note: This is computed on each access for cons lists. 
     * Consider caching for frequently accessed lists.
     */
    abstract val size: Int
    
    /**
     * Check if the list is empty.
     */
    abstract val isEmpty: Boolean
    
    // =============================================================================
    // Core Access Operations
    // =============================================================================
    
    /**
     * Get the first element (head) of the list.
     * @return the first element, or null if the list is empty
     */
    abstract fun head(): T?
    
    /**
     * Get the tail of the list (all elements except the first).
     * @return the tail list, or Empty if the list has 0 or 1 elements
     */
    abstract fun tail(): TaylorList<T>
    
    /**
     * Get element at the specified index.
     * @param index zero-based index
     * @return element at index, or null if index is out of bounds
     */
    abstract operator fun get(index: Int): T?
    
    // =============================================================================
    // Construction Operations
    // =============================================================================
    
    /**
     * Prepend an element to the front of the list (cons operation).
     * This is the most efficient way to add elements.
     * @param element element to prepend
     * @return new list with element at the front
     */
    abstract fun prepend(element: @UnsafeVariance T): TaylorList<T>
    
    /**
     * Append an element to the end of the list.
     * Note: This is O(n) operation, prefer prepend when possible.
     * @param element element to append
     * @return new list with element at the end
     */
    abstract fun append(element: @UnsafeVariance T): TaylorList<T>
    
    /**
     * Concatenate this list with another list.
     * @param other list to concatenate
     * @return new list containing all elements from both lists
     */
    abstract fun concat(other: TaylorList<@UnsafeVariance T>): TaylorList<T>
    
    // =============================================================================
    // Functional Operations
    // =============================================================================
    
    /**
     * Transform each element using the provided function.
     * @param transform function to apply to each element
     * @return new list with transformed elements
     */
    fun <R> map(transform: (T) -> R): TaylorList<R> = when (this) {
        is EmptyList -> empty()
        is Empty -> empty()
        is Cons -> {
            val transformedHead = transform(head)
            val transformedTail = tail.map(transform)
            Cons(transformedHead, transformedTail)
        }
    }
    
    /**
     * Filter elements based on a predicate.
     * @param predicate function returning true for elements to keep
     * @return new list containing only elements matching the predicate
     */
    fun filter(predicate: (T) -> Boolean): TaylorList<T> = when (this) {
        is EmptyList -> empty()
        is Empty -> empty()
        is Cons -> {
            val filteredTail = tail.filter(predicate)
            if (predicate(head)) {
                Cons(head, filteredTail)
            } else {
                filteredTail
            }
        }
    }
    
    /**
     * Fold (reduce) the list from left to right.
     * @param initial initial accumulator value
     * @param operation function combining accumulator and element
     * @return final accumulated value
     */
    fun <R> fold(initial: R, operation: (R, T) -> R): R = when (this) {
        is EmptyList -> initial
        is Empty -> initial
        is Cons -> tail.fold(operation(initial, head), operation)
    }
    
    /**
     * Reduce the list using the first element as initial value.
     * @param operation function combining two elements
     * @return reduced value, or null if list is empty
     */
    fun reduce(operation: (T, T) -> @UnsafeVariance T): T? = when (this) {
        is EmptyList -> null
        is Empty -> null
        is Cons -> tail.fold(head, operation)
    }
    
    /**
     * Find the first element matching a predicate.
     * @param predicate function to test elements
     * @return first matching element, or null if none found
     */
    fun find(predicate: (T) -> Boolean): T? = when (this) {
        is EmptyList -> null
        is Empty -> null
        is Cons -> if (predicate(head)) head else tail.find(predicate)
    }
    
    /**
     * Check if all elements satisfy a predicate.
     * @param predicate function to test elements
     * @return true if all elements match, false otherwise
     */
    fun all(predicate: (T) -> Boolean): Boolean = when (this) {
        is EmptyList -> true
        is Empty -> true
        is Cons -> predicate(head) && tail.all(predicate)
    }
    
    /**
     * Check if any element satisfies a predicate.
     * @param predicate function to test elements  
     * @return true if any element matches, false otherwise
     */
    fun any(predicate: (T) -> Boolean): Boolean = when (this) {
        is EmptyList -> false
        is Empty -> false
        is Cons -> predicate(head) || tail.any(predicate)
    }
    
    // =============================================================================
    // Utility Operations
    // =============================================================================
    
    /**
     * Take the first n elements.
     * @param n number of elements to take
     * @return new list with first n elements
     */
    fun take(n: Int): TaylorList<T> = when {
        n <= 0 -> empty()
        this is EmptyList -> empty()
        this is Empty -> empty()
        this is Cons -> {
            if (n == 1) of(head) else Cons(head, tail.take(n - 1))
        }
        else -> empty()
    }
    
    /**
     * Drop the first n elements.
     * @param n number of elements to drop
     * @return new list without first n elements
     */
    fun drop(n: Int): TaylorList<T> = when {
        n <= 0 -> this
        this is EmptyList -> empty()
        this is Empty -> empty()
        this is Cons -> {
            if (n == 1) tail else tail.drop(n - 1)
        }
        else -> empty()
    }
    
    /**
     * Reverse the list.
     * @return new list with elements in reverse order
     */
    fun reverse(): TaylorList<T> = fold(empty<T>()) { acc, elem -> acc.prepend(elem) }
    
    /**
     * Check if the list contains a specific element.
     * @param element element to search for
     * @return true if element is found, false otherwise
     */
    fun contains(element: @UnsafeVariance T): Boolean = find { it == element } != null
    
    // =============================================================================
    // Conversion Operations
    // =============================================================================
    
    /**
     * Convert to a standard Kotlin List for Java interoperability.
     * @return mutable Kotlin List containing the same elements
     */
    fun toKotlinList(): List<T> = fold(mutableListOf<T>()) { acc, elem -> 
        acc.also { it.add(elem) } 
    }
    
    /**
     * Convert to string representation.
     * @return string representation of the list
     */
    override fun toString(): String {
        if (this is EmptyList || this is Empty) return "[]"
        val elements = toKotlinList().joinToString(", ")
        return "[$elements]"
    }
    
    /**
     * Equality comparison.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaylorList<*>) return false
        if (this.size != other.size) return false
        
        // Convert both to Kotlin lists for comparison
        return this.toKotlinList() == other.toKotlinList()
    }
    
    /**
     * Hash code implementation.
     */
    override fun hashCode(): Int = fold(1) { acc, elem -> 31 * acc + (elem?.hashCode() ?: 0) }
    
    // =============================================================================
    // Concrete Implementations
    // =============================================================================
    
    /**
     * Empty list implementation using runtime type erasure.
     */
    class EmptyList<T> : TaylorList<T>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override fun head(): T? = null
        override fun tail(): TaylorList<T> = this
        override operator fun get(index: Int): T? = null
        
        override fun prepend(element: @UnsafeVariance T): TaylorList<T> = 
            Cons(element, this)
        
        override fun append(element: @UnsafeVariance T): TaylorList<T> = 
            Cons(element, this)
            
        override fun concat(other: TaylorList<@UnsafeVariance T>): TaylorList<T> = other
        
        override fun toString(): String = "[]"
        
        override fun equals(other: Any?): Boolean = other is EmptyList<*>
        override fun hashCode(): Int = 0
    }
    
    // Empty instance management will be handled in the main companion object
    
    /**
     * Empty list singleton access.
     */
    object Empty : TaylorList<Nothing>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override fun head(): Nothing? = null
        override fun tail(): TaylorList<Nothing> = Empty
        override operator fun get(index: Int): Nothing? = null
        
        override fun prepend(element: Nothing): TaylorList<Nothing> = Cons(element, Empty)
        override fun append(element: Nothing): TaylorList<Nothing> = Cons(element, Empty)            
        override fun concat(other: TaylorList<Nothing>): TaylorList<Nothing> = other
        
        override fun toString(): String = "[]"
        
        override fun equals(other: Any?): Boolean = other === Empty || other is EmptyList<*>
        override fun hashCode(): Int = 0
    }
    
    /**
     * Non-empty list implementation (cons cell).
     * @param head first element
     * @param tail remaining elements
     */
    data class Cons<out T>(
        val head: T,
        val tail: TaylorList<T>
    ) : TaylorList<T>() {
        
        override val size: Int by lazy { 1 + tail.size }
        override val isEmpty: Boolean = false
        
        override fun head(): T = head
        override fun tail(): TaylorList<T> = tail
        
        override operator fun get(index: Int): T? = when {
            index < 0 -> null
            index == 0 -> head
            else -> tail.get(index - 1)
        }
        
        override fun prepend(element: @UnsafeVariance T): TaylorList<T> = Cons(element, this)
        
        override fun append(element: @UnsafeVariance T): TaylorList<T> = 
            Cons(head, tail.append(element))
        
        override fun concat(other: TaylorList<@UnsafeVariance T>): TaylorList<T> = 
            Cons(head, tail.concat(other))
            
        override fun toString(): String {
            val elements = toKotlinList().joinToString(", ")
            return "[$elements]"
        }
    }
    
    // =============================================================================
    // Companion Object - Factory Methods
    // =============================================================================
    
    companion object {
        private val EMPTY_INSTANCE = EmptyList<Any?>()
        
        /**
         * Create an empty list.
         * @return empty TaylorList
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): TaylorList<T> = EMPTY_INSTANCE as TaylorList<T>
        
        /**
         * Create a list with a single element.
         * @param element the element
         * @return TaylorList containing the element
         */
        @JvmStatic
        fun <T> of(element: T): TaylorList<T> = Cons(element, empty())
        
        /**
         * Create a list from multiple elements.
         * @param elements vararg elements
         * @return TaylorList containing all elements in order
         */
        @JvmStatic
        fun <T> of(vararg elements: T): TaylorList<T> = 
            elements.reversed().fold(empty<T>()) { acc: TaylorList<T>, elem: T -> Cons(elem, acc) }
        
        /**
         * Create a list from a Kotlin collection.
         * @param elements collection to convert
         * @return TaylorList containing all elements
         */
        @JvmStatic
        fun <T> from(elements: Collection<T>): TaylorList<T> =
            elements.reversed().fold(empty<T>()) { acc: TaylorList<T>, elem: T -> Cons(elem, acc) }
        
        /**
         * Create a list with repeated elements.
         * @param element element to repeat
         * @param count number of repetitions
         * @return TaylorList with element repeated count times
         */
        @JvmStatic
        fun <T> repeat(element: T, count: Int): TaylorList<T> = when {
            count <= 0 -> empty()
            count == 1 -> of(element)
            else -> Cons(element, repeat(element, count - 1))
        }
        
        /**
         * Create a list containing integer range.
         * @param start start of range (inclusive)
         * @param end end of range (inclusive)
         * @return TaylorList containing integers from start to end
         */
        @JvmStatic
        fun range(start: Int, end: Int): TaylorList<Int> = when {
            start > end -> empty()
            start == end -> of(start)
            else -> Cons(start, range(start + 1, end))
        }
    }
}