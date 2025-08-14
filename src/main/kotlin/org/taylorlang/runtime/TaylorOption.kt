package org.taylorlang.runtime

/**
 * Runtime implementation of TaylorLang's Option<T> type for JVM bytecode execution.
 */
sealed class TaylorOption<out T> {
    
    /**
     * Some case containing a value of type T.
     */
    data class Some<T>(val value: T) : TaylorOption<T>() {
        override fun toString(): String = "Some($value)"
    }
    
    /**
     * None case containing no value.
     */
    object None : TaylorOption<Nothing>() {
        override fun toString(): String = "None"
        
        // Allow None() constructor calls
        operator fun invoke(): None = this
    }
}