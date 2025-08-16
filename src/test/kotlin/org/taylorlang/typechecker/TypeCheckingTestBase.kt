package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.assertions.fail
import org.taylorlang.ast.*
import org.taylorlang.parser.TaylorLangParser

/**
 * Abstract base class for all type checking tests, providing shared utilities and common test setup.
 * 
 * This class centralizes the common infrastructure needed across all type checking test classes:
 * - Parser and TypeChecker initialization
 * - Utility methods for type checking expressions and programs
 * - Error validation helpers
 * - Type comparison utilities
 */
abstract class TypeCheckingTestBase : StringSpec() {
    protected val parser = TaylorLangParser()
    protected val typeChecker = TypeChecker()

    /**
     * Type check an expression with a fresh TypeContext and return the typed result.
     * Throws if type checking fails.
     */
    protected fun typeCheckExpressionSuccess(expression: Expression): TypedExpression {
        val context = TypeContext()
        return typeChecker.typeCheckExpression(expression, context)
            .getOrThrow()
    }

    /**
     * Type check an expression with the given context and return the typed result.
     * Throws if type checking fails.
     */
    protected fun typeCheckExpressionSuccess(expression: Expression, context: TypeContext): TypedExpression {
        return typeChecker.typeCheckExpression(expression, context)
            .getOrThrow()
    }

    /**
     * Type check a program from source code and return the typed result.
     * Throws if parsing or type checking fails.
     */
    protected fun typeCheckProgramSuccess(source: String): TypedProgram {
        val program = parser.parse(source).getOrThrow()
        return typeChecker.typeCheck(program).getOrThrow()
    }

    /**
     * Parse and type check a program, expecting it to fail.
     * Returns the error for further validation.
     */
    protected fun expectTypeCheckFailure(source: String): TypeError {
        val program = parser.parse(source).getOrThrow()
        val result = typeChecker.typeCheck(program)
        
        result.isFailure shouldBe true
        return result.exceptionOrNull() as TypeError
    }

    /**
     * Type check an expression, expecting it to fail.
     * Returns the error for further validation.
     */
    protected fun expectExpressionTypeCheckFailure(expression: Expression, context: TypeContext = TypeContext()): TypeError {
        val result = typeChecker.typeCheckExpression(expression, context)
        
        result.isFailure shouldBe true
        return result.exceptionOrNull() as TypeError
    }

    /**
     * Parse source code, expecting it to fail.
     * Returns the parse error for further validation.
     */
    protected fun expectParseFailure(source: String): RuntimeException {
        val result = parser.parse(source)
        
        result.isFailure shouldBe true
        return result.exceptionOrNull() as RuntimeException
    }
}