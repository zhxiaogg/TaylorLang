package org.taylorlang

import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf

/**
 * Test utilities for safer type checking and casting
 */

/**
 * Safely cast an object after verifying its type with a kotest assertion
 * This combines the type check and cast in a single operation for better test safety
 */
inline fun <reified T : Any> Any?.safeCast(errorMessage: String = "Type cast failed"): T {
    this should beInstanceOf<T>()
    return this as T
}

/**
 * Safely get and cast an element from a list after verifying the list size and element type
 */
inline fun <reified T : Any> List<*>.safeGetAs(index: Int, errorMessage: String = "Element type mismatch"): T {
    require(index >= 0 && index < this.size) { "Index $index out of bounds for list of size ${this.size}" }
    return this[index].safeCast<T>(errorMessage)
}

/**
 * Extension function for more readable test assertions
 */
inline fun <reified T : Any> Any?.shouldBeA(): T {
    this should beInstanceOf<T>()
    return this as T
}