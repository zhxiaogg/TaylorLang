package org.taylorlang.typechecker

import org.taylorlang.ast.SourceLocation
import org.taylorlang.ast.Type

/**
 * Hierarchy of type checking errors.
 * 
 * This sealed class provides a comprehensive set of error types that can occur
 * during type checking. All errors carry optional source location information
 * for better error reporting and debugging.
 * 
 * Design principles:
 * - Each error type is specific to a particular kind of type checking failure
 * - Errors are immutable and contain all necessary information for reporting
 * - Composition errors (MultipleErrors) support collecting multiple failures
 * - All errors extend Throwable to enable exception-based error handling
 */
sealed class TypeError : Throwable() {
    
    /**
     * Symbol could not be resolved in the current scope.
     * This includes variables, functions, types, and constructors.
     */
    data class UnresolvedSymbol(
        val symbol: String,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Unresolved symbol: '$symbol'"
    }
    
    /**
     * Type mismatch between expected and actual types.
     * This is the most common type error, occurring when types don't match
     * in assignments, function calls, return values, etc.
     */
    data class TypeMismatch(
        val expected: Type,
        val actual: Type,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Type mismatch: expected '$expected', but got '$actual'"
    }
    
    /**
     * A type name was used but no corresponding type definition exists.
     */
    data class UndefinedType(
        val typeName: String,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Undefined type: '$typeName'"
    }
    
    /**
     * Wrong number of arguments provided to a function or constructor.
     * This includes both too few and too many arguments.
     */
    data class ArityMismatch(
        val expected: Int,
        val actual: Int,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Arity mismatch: expected $expected arguments, but got $actual"
    }
    
    /**
     * An operation is not valid for the given operand types.
     * This includes binary operations like +, -, unary operations, and other operations.
     */
    data class InvalidOperation(
        val operation: String,
        val operandTypes: List<Type>,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = if (operandTypes.isEmpty()) {
                "Invalid operation: $operation"
            } else {
                val typesList = operandTypes.joinToString(", ")
                "Invalid operation: $operation on types [$typesList]"
            }
    }
    
    /**
     * Pattern matching is not exhaustive - some cases are missing.
     * This error includes the specific patterns that need to be covered.
     */
    data class NonExhaustiveMatch(
        val missingPatterns: List<String>,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Non-exhaustive match: missing patterns [${missingPatterns.joinToString(", ")}]"
    }
    
    /**
     * A symbol (variable, function, type) is defined multiple times in the same scope.
     */
    data class DuplicateDefinition(
        val name: String,
        val location: SourceLocation?
    ) : TypeError() {
        override val message: String
            get() = "Duplicate definition: '$name'"
    }
    
    /**
     * Try expression used in an invalid context.
     * Try expressions are only allowed in functions that return Result<T, E>.
     */
    data class InvalidTryExpressionContext(
        override val message: String,
        val location: SourceLocation?
    ) : TypeError()
    
    /**
     * A composite error containing multiple individual type errors.
     * This is useful for collecting all errors in a compilation unit
     * rather than stopping at the first error.
     */
    data class MultipleErrors(
        val errors: List<TypeError>
    ) : TypeError() {
        override val message: String
            get() = "Multiple errors:\n${errors.joinToString("\n") { "  ${it.message}" }}"
    }
}