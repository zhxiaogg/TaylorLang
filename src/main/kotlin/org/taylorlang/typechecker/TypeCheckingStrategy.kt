package org.taylorlang.typechecker

import org.taylorlang.ast.Expression
import org.taylorlang.ast.Type

/**
 * Strategy interface for different type checking approaches.
 * 
 * This interface defines the contract for type checking strategies, allowing
 * the TypeChecker to switch between different type checking algorithms:
 * - Algorithmic type checking (direct inference)
 * - Constraint-based type checking (with unification)
 * 
 * The Strategy pattern enables:
 * - Clean separation of type checking algorithms
 * - Easy switching between modes at runtime
 * - Extensibility for future type checking approaches
 * - Testability of individual strategies
 */
interface TypeCheckingStrategy {
    
    /**
     * Type check an expression and return a typed expression result.
     * 
     * This is the core method that each strategy must implement.
     * It should perform type checking according to the strategy's algorithm
     * and return either a successfully typed expression or an error.
     * 
     * @param expression The expression to type check
     * @param context The type checking context with variable/function/type bindings
     * @return Result containing either a TypedExpression or a TypeError
     */
    fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression>
    
    /**
     * Type check an expression with an expected type.
     * 
     * This method allows for bidirectional type checking where the expected
     * type can guide the inference process. Some strategies may use this
     * information more effectively than others.
     * 
     * @param expression The expression to type check
     * @param expectedType The type expected for this expression
     * @param context The type checking context
     * @return Result containing either a TypedExpression or a TypeError
     */
    fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression>
    
    /**
     * Get a human-readable name for this strategy.
     * Used for debugging and error reporting.
     * 
     * @return The name of this type checking strategy
     */
    fun getStrategyName(): String
}

/**
 * Typed expression wrapper containing the original expression and its inferred type.
 * 
 * This data class represents the result of successful type checking,
 * pairing an AST expression with its computed type information.
 */
data class TypedExpression(
    val expression: Expression,
    val type: Type
)

/**
 * Enumeration of available type checking modes.
 * 
 * This enum provides a convenient way to specify which type checking
 * strategy should be used, with factory methods for creating the
 * corresponding strategy instances.
 */
enum class TypeCheckingMode {
    /**
     * Traditional algorithmic type checking with direct type inference.
     * Uses the existing type checking implementation with immediate
     * type computation during AST traversal.
     */
    ALGORITHMIC,
    
    /**
     * Constraint-based type checking with type inference.
     * Generates constraints during AST traversal and uses unification
     * algorithm to solve them and compute final types.
     */
    CONSTRAINT_BASED;
    
    /**
     * Create a strategy instance for this mode.
     * 
     * @return TypeCheckingStrategy instance corresponding to this mode
     */
    fun createStrategy(): TypeCheckingStrategy {
        return when (this) {
            ALGORITHMIC -> AlgorithmicTypeCheckingStrategy()
            CONSTRAINT_BASED -> ConstraintBasedTypeCheckingStrategy()
        }
    }
}