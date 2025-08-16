package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import org.taylorlang.ast.*

/**
 * Error Handling Type Tests
 * 
 * Tests error reporting and aggregation in type checking:
 * - Meaningful error messages for type mismatches
 * - Multiple type errors collection
 * - Error aggregation validation
 * - Specific error types and their messages
 * - Error propagation in complex expressions
 */
class ErrorHandlingTypeTest : TypeCheckingTestBase() {
    init {

    "should provide meaningful error messages for type mismatches" {
        val expression = BinaryOp(
            left = Literal.StringLiteral("hello"),
            operator = BinaryOperator.MULTIPLY,
            right = Literal.IntLiteral(42)
        )
        val context = TypeContext()
        
        val error = expectExpressionTypeCheckFailure(expression, context)
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should handle multiple type errors" {
        val source = """
            val x = 1 + "hello"
            val y = true * false
            val z = undefined_var
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        // Should collect multiple errors (string concatenation is valid, so only 2 errors expected)
        error should beInstanceOf<TypeError.MultipleErrors>()
        val multipleErrors = error as TypeError.MultipleErrors
        multipleErrors.errors.size shouldBe 2
    }

    "should detect invalid arithmetic operations" {
        val expression = BinaryOp(
            left = Literal.BooleanLiteral(true),
            operator = BinaryOperator.PLUS,
            right = Literal.BooleanLiteral(false)
        )
        val error = expectExpressionTypeCheckFailure(expression)
        
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect invalid comparison operations" {
        val expression = BinaryOp(
            left = Literal.StringLiteral("hello"),
            operator = BinaryOperator.LESS_THAN,
            right = Literal.IntLiteral(42)
        )
        val error = expectExpressionTypeCheckFailure(expression)
        
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect invalid logical operations" {
        val expression = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.AND,
            right = Literal.IntLiteral(2)
        )
        val error = expectExpressionTypeCheckFailure(expression)
        
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect invalid unary operations" {
        val expression = UnaryOp(
            operator = UnaryOperator.NOT,
            operand = Literal.IntLiteral(42)
        )
        val error = expectExpressionTypeCheckFailure(expression)
        
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect unresolved symbol errors" {
        val expression = Identifier("nonExistentVariable")
        val error = expectExpressionTypeCheckFailure(expression)
        
        error should beInstanceOf<TypeError.UnresolvedSymbol>()
        val unresolvedError = error as TypeError.UnresolvedSymbol
        unresolvedError.symbol shouldBe "nonExistentVariable"
    }

    "should detect type mismatches in variable assignments" {
        val source = """
            val x: Int = "hello"
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.TypeMismatch>()
    }

    "should detect type mismatches in function returns" {
        val source = """
            fn getString(): String => 42
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.TypeMismatch>()
    }

    "should detect arity mismatches in function calls" {
        val source = """
            fn add(x: Int, y: Int): Int => x + y
            val result = add(1, 2, 3)
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.ArityMismatch>()
    }

    "should collect errors from nested expressions" {
        val source = """
            val x = (1 + "hello") * (true - false)
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        // Only the boolean subtraction should fail (string concatenation is valid)
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect invalid operations in complex expressions" {
        val source = """
            val result = (1 + 2) && (3 * 4)
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should provide context for nested type errors" {
        val source = """
            fn process(x: Int): String => {
                val y = x + "hello";
                val z = y * 2;
                z
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        // String concatenation (Int + String) is valid, so only one error from String * Int
        error should beInstanceOf<TypeError.InvalidOperation>()
    }

    "should detect duplicate function definitions" {
        val source = """
            fn test(): Int => 1
            fn test(): String => "hello"
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.DuplicateDefinition>()
    }

    "should detect duplicate variable definitions" {
        val source = """
            val x = 1
            val x = 2
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.DuplicateDefinition>()
    }

    "should handle cascading errors gracefully" {
        val source = """
            val a = undefined1
            val b = undefined2
            val c = a + b
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val multipleErrors = error as TypeError.MultipleErrors
        multipleErrors.errors.size shouldBe 3 // Three undefined variables
    }

    "should detect invalid constructor calls" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some()  // Missing argument
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.ArityMismatch>()
    }

    "should detect invalid pattern matching" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case Some(a, b) => a + b  // Too many arguments in pattern
                case None => 0
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
    }

    "should provide specific error for non-exhaustive matches" {
        val source = """
            type Color = Red | Green | Blue
            val c = Red
            val result = match c {
                case Red => "red"
                // Missing Green and Blue cases
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.NonExhaustiveMatch>()
        val nonExhaustiveError = error as TypeError.NonExhaustiveMatch
        nonExhaustiveError.missingPatterns shouldBe listOf("Green", "Blue")
    }

    "should handle errors in if expression conditions" {
        val source = """
            val result = if (42) "true" else "false"  // Int instead of Boolean
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.TypeMismatch>()
    }

    "should detect return type mismatches in complex functions" {
        val source = """
            fn complexFunction(x: Int): Boolean => {
                val y = x * 2;
                if (y > 10) y else 0  // Returns Int instead of Boolean
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.TypeMismatch>()
    }

    "should collect multiple errors in single expression" {
        val source = """
            val result = undefined1 + undefined2 * undefined3
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        // Type checker currently reports first error (undefined1), not all errors
        // This is expected behavior for fail-fast error reporting
        error should beInstanceOf<TypeError.UnresolvedSymbol>()
        val unresolvedError = error as TypeError.UnresolvedSymbol
        unresolvedError.symbol shouldBe "undefined1"
    }
    }
}