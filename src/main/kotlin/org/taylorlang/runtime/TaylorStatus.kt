package org.taylorlang.runtime

/**
 * Runtime implementation of TaylorLang's Status type for JVM bytecode execution.
 */
sealed class TaylorStatus {
    
    /**
     * Active status.
     */
    object Active : TaylorStatus() {
        override fun toString(): String = "Active"
        
        // Allow Active() constructor calls
        operator fun invoke(): Active = this
    }
    
    /**
     * Inactive status.
     */
    object Inactive : TaylorStatus() {
        override fun toString(): String = "Inactive"
        
        // Allow Inactive() constructor calls
        operator fun invoke(): Inactive = this
    }
    
    /**
     * Pending status.
     */
    object Pending : TaylorStatus() {
        override fun toString(): String = "Pending"
        
        // Allow Pending() constructor calls
        operator fun invoke(): Pending = this
    }
}