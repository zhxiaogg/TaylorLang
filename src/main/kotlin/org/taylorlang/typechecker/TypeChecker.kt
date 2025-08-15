package org.taylorlang.typechecker

import org.taylorlang.ast.*

/**
 * Main type checker implementation supporting both algorithmic and constraint-based type checking.
 * 
 * This class now serves as a facade over the refactored visitor-based architecture while
 * maintaining backward compatibility with existing code. It delegates to the RefactoredTypeChecker
 * for actual implementation.
 */
class TypeChecker(
    private val mode: TypeCheckingMode = TypeCheckingMode.ALGORITHMIC
) {
    
    // Delegate to the refactored implementation
    private val refactoredChecker = RefactoredTypeChecker(mode)
    private val constraintCollector = ConstraintCollector()
    
    /**
     * Type check a complete program
     */
    fun typeCheck(program: Program): Result<TypedProgram> {
        return refactoredChecker.typeCheck(program)
    }
    
    /**
     * Type check an expression using the configured strategy.
     */
    fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        return refactoredChecker.typeCheckExpression(expression, context)
    }
    
    /**
     * Type check an expression with an expected type using the configured strategy.
     */
    fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression> {
        return refactoredChecker.typeCheckExpressionWithExpected(expression, expectedType, context)
    }
    
    /**
     * Collect constraints from an expression without solving them.
     * This is useful for debugging and understanding the constraint generation process.
     */
    fun collectConstraintsOnly(
        expression: Expression,
        context: TypeContext
    ): Result<ConstraintResult> {
        return refactoredChecker.collectConstraintsOnly(expression, context)
    }
    
    /**
     * Companion object with factory methods.
     */
    companion object {
        /**
         * Create a TypeChecker instance configured for constraint-based type checking.
         */
        fun withConstraints(): TypeChecker {
            return TypeChecker(TypeCheckingMode.CONSTRAINT_BASED)
        }
        
        /**
         * Create a TypeChecker instance configured for algorithmic type checking.
         */
        fun algorithmic(): TypeChecker {
            return TypeChecker(TypeCheckingMode.ALGORITHMIC)
        }
    }
}
