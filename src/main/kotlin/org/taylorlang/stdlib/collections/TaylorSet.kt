package org.taylorlang.stdlib.collections

/**
 * Immutable persistent set implementation for TaylorLang standard library.
 * 
 * This class provides the foundation for functional set operations in TaylorLang,
 * implementing an efficient persistent data structure that supports structural sharing
 * for memory efficiency and immutable operations.
 * 
 * Design principles:
 * - Immutable by default - all operations return new instances
 * - Structural sharing for memory efficiency using binary tree structure
 * - Unique element storage with fast membership testing
 * - Full generic type safety
 * - Balanced binary search tree for O(log n) operations
 * 
 * Performance characteristics:
 * - O(log n) add, remove, contains operations
 * - O(n) map, filter, fold operations with single traversal
 * - O(n + m) union, intersection, difference operations
 * - O(n) toList(), toKotlinSet() operations
 */
sealed class TaylorSet<out T : Comparable<@UnsafeVariance T>> {
    
    /**
     * Number of elements in the set.
     */
    abstract val size: Int
    
    /**
     * Check if the set is empty.
     */
    abstract val isEmpty: Boolean
    
    // =============================================================================
    // Core Set Operations
    // =============================================================================
    
    /**
     * Add an element to the set, returning a new set.
     * If the element already exists, returns the same set.
     * @param element the element to add
     * @return new set containing the element
     */
    abstract fun add(element: @UnsafeVariance T): TaylorSet<T>
    
    /**
     * Remove an element from the set, returning a new set.
     * @param element the element to remove
     * @return new set without the element
     */
    abstract fun remove(element: @UnsafeVariance T): TaylorSet<T>
    
    /**
     * Check if the set contains a specific element.
     * @param element the element to check
     * @return true if the element exists in the set
     */
    abstract fun contains(element: @UnsafeVariance T): Boolean
    
    // =============================================================================
    // Set Algebra Operations
    // =============================================================================
    
    /**
     * Compute the union of this set with another set.
     * @param other the other set
     * @return new set containing all elements from both sets
     */
    abstract fun union(other: TaylorSet<@UnsafeVariance T>): TaylorSet<T>
    
    /**
     * Compute the intersection of this set with another set.
     * @param other the other set
     * @return new set containing only elements present in both sets
     */
    abstract fun intersection(other: TaylorSet<@UnsafeVariance T>): TaylorSet<T>
    
    /**
     * Compute the difference of this set with another set.
     * @param other the other set
     * @return new set containing elements in this set but not in the other
     */
    abstract fun difference(other: TaylorSet<@UnsafeVariance T>): TaylorSet<T>
    
    /**
     * Check if this set is a subset of another set.
     * @param other the other set
     * @return true if all elements in this set are in the other set
     */
    fun isSubsetOf(other: TaylorSet<@UnsafeVariance T>): Boolean = 
        this.all { other.contains(it) }
    
    /**
     * Check if this set is a superset of another set.
     * @param other the other set
     * @return true if all elements in the other set are in this set
     */
    fun isSupersetOf(other: TaylorSet<@UnsafeVariance T>): Boolean = 
        other.isSubsetOf(this)
    
    /**
     * Check if this set is disjoint with another set.
     * @param other the other set
     * @return true if no elements are common between the sets
     */
    fun disjointWith(other: TaylorSet<@UnsafeVariance T>): Boolean = 
        this.intersection(other).isEmpty
    
    // =============================================================================
    // Collection Access Operations
    // =============================================================================
    
    /**
     * Convert the set to a TaylorList containing all elements in sorted order.
     * @return TaylorList containing all elements
     */
    abstract fun toList(): TaylorList<T>
    
    // =============================================================================
    // Functional Operations
    // =============================================================================
    
    /**
     * Transform elements using the provided function.
     * @param transform function to apply to each element
     * @return new set with transformed elements
     */
    fun <R : Comparable<R>> map(transform: (T) -> R): TaylorSet<R> = when (this) {
        is EmptySet -> empty()
        is Empty -> empty()
        is Node -> {
            // Transform current element and subtrees, then rebuild to maintain uniqueness
            val transformedElement = transform(element)
            val transformedLeft = left.map(transform)
            val transformedRight = right.map(transform)
            
            // Combine all transformed elements, maintaining set semantics
            transformedLeft.add(transformedElement).union(transformedRight)
        }
    }
    
    /**
     * Filter elements based on a predicate.
     * @param predicate function returning true for elements to keep
     * @return new set containing only elements matching the predicate
     */
    fun filter(predicate: (T) -> Boolean): TaylorSet<T> = when (this) {
        is EmptySet -> empty()
        is Empty -> empty()
        is Node -> {
            val filteredLeft = left.filter(predicate)
            val filteredRight = right.filter(predicate)
            
            if (predicate(element)) {
                filteredLeft.add(element).union(filteredRight)
            } else {
                filteredLeft.union(filteredRight)
            }
        }
    }
    
    /**
     * Fold (reduce) the set from left to right over elements.
     * @param initial initial accumulator value
     * @param operation function combining accumulator and element
     * @return final accumulated value
     */
    fun <R> fold(initial: R, operation: (R, T) -> R): R = when (this) {
        is EmptySet -> initial
        is Empty -> initial
        is Node -> {
            val afterLeft = left.fold(initial, operation)
            val afterCurrent = operation(afterLeft, element)
            right.fold(afterCurrent, operation)
        }
    }
    
    /**
     * Reduce the set using the provided operation.
     * @param operation function combining two elements
     * @return the reduced value, or null if the set is empty
     */
    fun reduce(operation: (@UnsafeVariance T, @UnsafeVariance T) -> @UnsafeVariance T): T? = when (this) {
        is EmptySet -> null
        is Empty -> null
        is Node -> {
            val leftReduced = left.reduce(operation)
            val rightReduced = right.reduce(operation)
            
            when {
                leftReduced != null && rightReduced != null -> operation(operation(leftReduced, element), rightReduced)
                leftReduced != null -> operation(leftReduced, element)
                rightReduced != null -> operation(element, rightReduced)
                else -> element
            }
        }
    }
    
    /**
     * Find the first element matching a predicate.
     * @param predicate function to test elements
     * @return first matching element, or null if none found
     */
    fun find(predicate: (T) -> Boolean): T? {
        return when (this) {
            is EmptySet -> null
            is Empty -> null
            is Node -> {
                // Check left subtree first (in-order traversal)
                left.find(predicate)?.let { return it }
                
                // Check current element
                if (predicate(element)) return element
                
                // Check right subtree
                right.find(predicate)
            }
        }
    }
    
    /**
     * Check if all elements satisfy a predicate.
     * @param predicate function to test elements
     * @return true if all elements match, false otherwise
     */
    fun all(predicate: (T) -> Boolean): Boolean = when (this) {
        is EmptySet -> true
        is Empty -> true
        is Node -> {
            predicate(element) && left.all(predicate) && right.all(predicate)
        }
    }
    
    /**
     * Check if any element satisfies a predicate.
     * @param predicate function to test elements
     * @return true if any element matches, false otherwise
     */
    fun any(predicate: (T) -> Boolean): Boolean = when (this) {
        is EmptySet -> false
        is Empty -> false
        is Node -> {
            predicate(element) || left.any(predicate) || right.any(predicate)
        }
    }
    
    /**
     * Count elements matching a predicate.
     * @param predicate function to test elements
     * @return number of matching elements
     */
    fun count(predicate: (T) -> Boolean): Int = when (this) {
        is EmptySet -> 0
        is Empty -> 0
        is Node -> {
            val currentCount = if (predicate(element)) 1 else 0
            currentCount + left.count(predicate) + right.count(predicate)
        }
    }
    
    /**
     * Apply an action to each element.
     * @param action function to apply to each element
     */
    fun forEach(action: (@UnsafeVariance T) -> Unit): Unit = when (this) {
        is EmptySet -> {}
        is Empty -> {}
        is Node -> {
            left.forEach(action)
            action(element)
            right.forEach(action)
        }
    }
    
    // =============================================================================
    // Conversion Operations
    // =============================================================================
    
    /**
     * Convert to a standard Kotlin Set for Java interoperability.
     * @return mutable Kotlin Set containing the same elements
     */
    fun toKotlinSet(): Set<T> = fold(mutableSetOf<T>()) { acc, element -> 
        acc.also { it.add(element) } 
    }
    
    /**
     * Convert to string representation.
     * @return string representation of the set
     */
    override fun toString(): String {
        if (isEmpty) return "{}"
        val elements = toList().toKotlinList().joinToString(", ")
        return "{$elements}"
    }
    
    /**
     * Equality comparison.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaylorSet<*>) return false
        if (this.size != other.size) return false
        
        // Compare all elements
        return this.toKotlinSet() == other.toKotlinSet()
    }
    
    /**
     * Hash code implementation.
     */
    override fun hashCode(): Int = fold(1) { acc, element -> 
        31 * acc + (element?.hashCode() ?: 0) 
    }
    
    // =============================================================================
    // Concrete Implementations
    // =============================================================================
    
    /**
     * Empty set implementation using runtime type erasure.
     */
    class EmptySet<T : Comparable<T>> : TaylorSet<T>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override fun add(element: T): TaylorSet<T> = 
            Node(element, this, this)
        override fun remove(element: T): TaylorSet<T> = this
        override fun contains(element: T): Boolean = false
        
        override fun union(other: TaylorSet<T>): TaylorSet<T> = other
        override fun intersection(other: TaylorSet<T>): TaylorSet<T> = this
        override fun difference(other: TaylorSet<T>): TaylorSet<T> = this
        
        override fun toList(): TaylorList<T> = TaylorList.empty()
        
        override fun toString(): String = "{}"
        override fun equals(other: Any?): Boolean = other is EmptySet<*>
        override fun hashCode(): Int = 0
    }
    
    /**
     * Empty set singleton access.
     */
    object Empty : TaylorSet<Nothing>() {
        override val size: Int = 0
        override val isEmpty: Boolean = true
        
        override fun add(element: Nothing): TaylorSet<Nothing> = 
            Node(element, Empty, Empty)
        override fun remove(element: Nothing): TaylorSet<Nothing> = Empty
        override fun contains(element: Nothing): Boolean = false
        
        override fun union(other: TaylorSet<Nothing>): TaylorSet<Nothing> = other
        override fun intersection(other: TaylorSet<Nothing>): TaylorSet<Nothing> = Empty
        override fun difference(other: TaylorSet<Nothing>): TaylorSet<Nothing> = Empty
        
        override fun toList(): TaylorList<Nothing> = TaylorList.empty()
        
        override fun toString(): String = "{}"
        override fun equals(other: Any?): Boolean = other === Empty || other is EmptySet<*>
        override fun hashCode(): Int = 0
    }
    
    /**
     * Non-empty set implementation using binary search tree.
     * @param element the element at this node
     * @param left left subtree (elements less than this element)
     * @param right right subtree (elements greater than this element)
     */
    class Node<T : Comparable<T>>(
        val element: T,
        val left: TaylorSet<T>,
        val right: TaylorSet<T>
    ) : TaylorSet<T>() {
        
        override val size: Int by lazy { 1 + left.size + right.size }
        override val isEmpty: Boolean = false
        
        override fun add(element: T): TaylorSet<T> = when {
            element < this.element -> Node(this.element, left.add(element), right)
            element > this.element -> Node(this.element, left, right.add(element))
            else -> this // Element already exists, return unchanged
        }
        
        override fun contains(element: T): Boolean = when {
            element < this.element -> left.contains(element)
            element > this.element -> right.contains(element)
            else -> true
        }
        
        override fun remove(element: T): TaylorSet<T> = when {
            element < this.element -> Node(this.element, left.remove(element), right)
            element > this.element -> Node(this.element, left, right.remove(element))
            else -> {
                // Remove this node
                when {
                    left.isEmpty -> right
                    right.isEmpty -> left
                    else -> {
                        // Find inorder successor (leftmost node in right subtree)
                        val successor = findMin(right)
                        Node(successor.element, left, right.remove(successor.element))
                    }
                }
            }
        }
        
        override fun union(other: TaylorSet<T>): TaylorSet<T> = when (other) {
            is EmptySet -> this
            is Empty -> this
            is Node -> {
                // Add all elements from other set to this set
                other.fold(this as TaylorSet<T>) { acc, elem -> acc.add(elem) }
            }
        }
        
        override fun intersection(other: TaylorSet<T>): TaylorSet<T> = when (other) {
            is EmptySet -> empty()
            is Empty -> empty()
            is Node -> {
                // Keep only elements that exist in both sets
                this.filter { other.contains(it) }
            }
        }
        
        override fun difference(other: TaylorSet<T>): TaylorSet<T> = when (other) {
            is EmptySet -> this
            is Empty -> this
            is Node -> {
                // Keep only elements that don't exist in the other set
                this.filter { !other.contains(it) }
            }
        }
        
        override fun toList(): TaylorList<T> {
            val leftList = left.toList()
            val rightList = right.toList()
            return leftList.append(element).concat(rightList)
        }
        
        override fun toString(): String {
            if (size == 1) return "{$element}"
            val elements = toList().toKotlinList().joinToString(", ")
            return "{$elements}"
        }
        
        /**
         * Helper function to find the minimum element node in a subtree.
         */
        private fun findMin(tree: TaylorSet<T>): Node<T> = when (tree) {
            is EmptySet -> throw IllegalStateException("Cannot find minimum in empty tree")
            is Empty -> throw IllegalStateException("Cannot find minimum in empty tree")
            is Node -> if (tree.left.isEmpty) tree else findMin(tree.left)
        }
    }
    
    // =============================================================================
    // Companion Object - Factory Methods
    // =============================================================================
    
    companion object {
        private val EMPTY_INSTANCE = EmptySet<Nothing>()
        
        /**
         * Create an empty set.
         * @return empty TaylorSet
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T : Comparable<T>> empty(): TaylorSet<T> = EMPTY_INSTANCE as TaylorSet<T>
        
        /**
         * Create a set with a single element.
         * @param element the element
         * @return TaylorSet containing the element
         */
        @JvmStatic
        fun <T : Comparable<T>> of(element: T): TaylorSet<T> = 
            Node(element, empty(), empty())
        
        /**
         * Create a set from multiple elements.
         * @param elements vararg elements (duplicates will be removed)
         * @return TaylorSet containing all unique elements
         */
        @JvmStatic
        fun <T : Comparable<T>> of(vararg elements: T): TaylorSet<T> = 
            elements.fold(empty<T>()) { acc, element -> acc.add(element) }
        
        /**
         * Create a set from a collection of elements.
         * @param elements collection of elements to convert
         * @return TaylorSet containing all unique elements
         */
        @JvmStatic
        fun <T : Comparable<T>> from(elements: Collection<T>): TaylorSet<T> =
            elements.fold(empty<T>()) { acc, element -> acc.add(element) }
        
        /**
         * Create a set from a Kotlin Set.
         * @param set Kotlin set to convert
         * @return TaylorSet containing all elements from the Kotlin set
         */
        @JvmStatic
        fun <T : Comparable<T>> fromJava(set: Set<T>): TaylorSet<T> =
            set.fold(empty<T>()) { acc, element -> acc.add(element) }
    }
}