package org.taylorlang.runtime

/**
 * Runtime implementation of TaylorLang's Tuple2<A, B> type for JVM bytecode execution.
 */
sealed class TaylorTuple2<out A, out B> {
    
    /**
     * Pair case containing two values.
     */
    data class Pair<A, B>(val first: A, val second: B) : TaylorTuple2<A, B>() {
        override fun toString(): String = "Pair($first, $second)"
    }
}