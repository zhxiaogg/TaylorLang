package org.taylorlang.runtime

/**
 * Runtime implementation of TaylorLang's Result<T, E> type for JVM bytecode execution.
 * 
 * This sealed class provides the foundation for functional error handling in TaylorLang,
 * enabling try expressions to work with typed error handling while maintaining
 * compatibility with JVM exception infrastructure.
 * 
 * Design principles:
 * - Sealed class for exhaustive pattern matching
 * - Throwable constraint on error types for JVM compatibility
 * - Monadic operations for functional composition
 * - Efficient JVM interoperability methods
 */
sealed class TaylorResult<out T, out E> where E : Throwable {
    
    /**
     * Success case containing a value of type T.
     */
    data class Ok<T>(val value: T) : TaylorResult<T, Nothing>() {
        override fun toString(): String = "Ok($value)"
    }
    
    /**
     * Error case containing a throwable error of type E.
     */
    data class Error<E : Throwable>(val error: E) : TaylorResult<Nothing, E>() {
        override fun toString(): String = "Error($error)"
    }
    
    // =============================================================================
    // Status Checking Methods
    // =============================================================================
    
    /**
     * Check if this Result represents a successful value.
     * @return true if this is Ok, false if Error
     */
    fun isOk(): Boolean = this is Ok
    
    /**
     * Check if this Result represents an error.
     * @return true if this is Error, false if Ok
     */
    fun isError(): Boolean = this is Error
    
    // =============================================================================
    // Value Extraction Methods
    // =============================================================================
    
    /**
     * Extract the success value or throw the error.
     * This method is used for interfacing with Java code that expects exceptions.
     * @return the success value
     * @throws E if this is an Error result
     */
    fun getOrThrow(): T = when (this) {
        is Ok -> value
        is Error -> throw error
    }
    
    /**
     * Extract the success value or return null if error.
     * @return the success value or null
     */
    fun getOrNull(): T? = when (this) {
        is Ok -> value
        is Error -> null
    }
    
    /**
     * Extract the success value or return the provided default.
     * @param default the default value to return if this is Error
     * @return the success value or default
     */
    fun getOrDefault(default: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Ok -> value
        is Error -> default
    }
    
    /**
     * Extract the success value or compute it from the error.
     * @param onError function to compute default from error
     * @return the success value or computed default
     */
    inline fun getOrElse(onError: (E) -> @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Ok -> value
        is Error -> onError(error)
    }
    
    // =============================================================================
    // Monadic Operations
    // =============================================================================
    
    /**
     * Transform the success value while preserving error.
     * @param transform function to apply to success value
     * @return new Result with transformed value or original error
     */
    inline fun <R> map(transform: (T) -> R): TaylorResult<R, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Error -> this
    }
    
    /**
     * Transform the success value to another Result, flattening nested Results.
     * @param transform function that transforms success value to Result
     * @return flattened Result
     */
    inline fun <R> flatMap(transform: (T) -> TaylorResult<R, @UnsafeVariance E>): TaylorResult<R, E> = when (this) {
        is Ok -> transform(value)
        is Error -> this
    }
    
    /**
     * Transform the error while preserving success value.
     * @param transform function to apply to error
     * @return new Result with original value or transformed error
     */
    inline fun <F : Throwable> mapError(transform: (E) -> F): TaylorResult<T, F> = when (this) {
        is Ok -> this
        is Error -> Error(transform(error))
    }
    
    /**
     * Apply a side effect to the success value without changing the Result.
     * @param action side effect to perform on success value
     * @return this Result unchanged
     */
    inline fun onSuccess(action: (T) -> Unit): TaylorResult<T, E> {
        if (this is Ok) action(value)
        return this
    }
    
    /**
     * Apply a side effect to the error without changing the Result.
     * @param action side effect to perform on error
     * @return this Result unchanged
     */
    inline fun onError(action: (E) -> Unit): TaylorResult<T, E> {
        if (this is Error) action(error)
        return this
    }
    
    // =============================================================================
    // Utility Methods for Bytecode Generation
    // =============================================================================
    
    /**
     * Static factory method for creating Ok results.
     * Used by bytecode generation for wrapping successful computations.
     */
    companion object {
        @JvmStatic
        fun <T> ok(value: T): TaylorResult<T, Nothing> = Ok(value)
        
        @JvmStatic
        fun <E : Throwable> error(error: E): TaylorResult<Nothing, E> = Error(error)
        
        /**
         * Wrap a potentially throwing computation in a Result.
         * This is used for Java interoperability and automatic exception conversion.
         */
        @JvmStatic
        inline fun <T> catching(block: () -> T): TaylorResult<T, Throwable> {
            return try {
                Ok(block())
            } catch (e: Throwable) {
                Error(e)
            }
        }
        
        /**
         * Wrap a computation that might throw specific exception types.
         * @param exceptionClass the expected exception type
         * @param block the computation to wrap
         * @return Result containing either success value or caught exception
         */
        @JvmStatic
        inline fun <T, E : Throwable> catching(
            exceptionClass: Class<E>,
            block: () -> T
        ): TaylorResult<T, E> {
            return try {
                Ok(block())
            } catch (e: Throwable) {
                if (exceptionClass.isInstance(e)) {
                    @Suppress("UNCHECKED_CAST")
                    Error(e as E)
                } else {
                    throw e // Re-throw unexpected exceptions
                }
            }
        }
    }
}

/**
 * Enhanced stacktrace information for try expression error propagation.
 * This class is used to track the location of try expressions in the call stack
 * for better debugging experience.
 */
class TryLocationTracker {
    companion object {
        private val tryLocationStack = ThreadLocal<MutableList<String>>()
        
        /**
         * Mark entry into a try expression for stacktrace enhancement.
         * @param location source location of the try expression
         */
        @JvmStatic
        fun enterTryExpression(location: String) {
            val stack = tryLocationStack.get() ?: mutableListOf()
            stack.add(location)
            tryLocationStack.set(stack)
        }
        
        /**
         * Mark exit from a try expression.
         */
        @JvmStatic
        fun exitTryExpression() {
            val stack = tryLocationStack.get()
            if (stack != null && stack.isNotEmpty()) {
                stack.removeLastOrNull()
                if (stack.isEmpty()) {
                    tryLocationStack.remove()
                }
            }
        }
        
        /**
         * Enhance an error's stacktrace with try expression location information.
         * @param error the error to enhance
         * @param currentLocation the current try expression location
         * @return the enhanced error (same instance, modified)
         */
        @JvmStatic
        fun <E : Throwable> enhanceStacktrace(error: E, currentLocation: String): E {
            val stack = tryLocationStack.get()
            if (stack != null) {
                for (location in stack.reversed()) {
                    val tryException = RuntimeException("Try expression at $location")
                    error.addSuppressed(tryException)
                }
            }
            // Add current location
            val currentException = RuntimeException("Try expression at $currentLocation")
            error.addSuppressed(currentException)
            return error
        }
        
        /**
         * Propagate an error Result with enhanced location information.
         * This method is called by generated bytecode for error propagation.
         * 
         * @param result the Result to potentially enhance
         * @param location the try expression location
         * @return the Result with enhanced error information
         */
        @JvmStatic
        fun <T, E : Throwable> propagateError(
            result: TaylorResult<T, E>,
            location: String
        ): TaylorResult<T, E> {
            return when (result) {
                is TaylorResult.Ok -> result
                is TaylorResult.Error -> {
                    enhanceStacktrace(result.error, location)
                    result
                }
            }
        }
    }
}