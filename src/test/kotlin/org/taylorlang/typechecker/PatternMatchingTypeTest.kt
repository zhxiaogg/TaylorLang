package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import io.kotest.matchers.collections.shouldContain
import org.taylorlang.ast.*

/**
 * Pattern Matching Type Tests
 * 
 * Tests pattern matching and exhaustiveness checking:
 * - Simple match expressions with union types
 * - Non-exhaustive match detection
 * - Wildcard and identifier patterns
 * - Nested constructor patterns
 * - Pattern type mismatches and literal patterns
 * - Constructor pattern arity validation
 * - Generic union types in match expressions
 */
class PatternMatchingTypeTest : TypeCheckingTestBase() {
    init {

    "should type check simple match expressions with union types" {
        val source = """
            type Option<T> = Some(T) | None
            fn unwrap<T>(opt: Option<T>, default: T): T => match opt {
                case Some(value) => value
                case None => default
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val functionDecl = result.statements.last()
        functionDecl should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should detect non-exhaustive match expressions" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case Some(value) => value
                // Missing None case - should fail exhaustiveness check
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val errors = (error as TypeError.MultipleErrors).errors
        
        // Should contain a NonExhaustiveMatch error
        val nonExhaustiveError = errors.find { it is TypeError.NonExhaustiveMatch }
        nonExhaustiveError shouldBe beInstanceOf<TypeError.NonExhaustiveMatch>()
        
        val exhaustivenessError = nonExhaustiveError as TypeError.NonExhaustiveMatch
        exhaustivenessError.missingPatterns shouldContain "None"
    }

    "should handle wildcard patterns correctly" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case _ => 0
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle identifier patterns with variable binding" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case opt => match opt {
                    case Some(value) => value
                    case None => 0
                }
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.INT
    }

    "should type check nested constructor patterns" {
        val source = """
            type Result<T, E> = Ok(T) | Error(E)
            type Option<T> = Some(T) | None
            val x = Ok(Some(42))
            val result = match x {
                case Ok(Some(value)) => value
                case Ok(None) => 0
                case Error(_) => -1
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.INT
    }

    "should detect pattern type mismatches" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case Some("hello") => 1  // String literal doesn't match Int type parameter
                case None => 0
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
    }

    "should handle pattern matching with literal patterns" {
        val source = """
            val x = 42
            val result = match x {
                case 42 => "found forty-two"
                case _ => "something else"
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.STRING
    }

    "should validate constructor pattern arity" {
        val source = """
            type Result<T, E> = Ok(T) | Error(E)
            val x = Ok(42)
            val result = match x {
                case Ok(value, extra) => value  // Too many arguments for Ok constructor
                case Error(_) => 0
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val errors = (error as TypeError.MultipleErrors).errors
        
        // Should contain an ArityMismatch error - check both direct and nested errors
        val allErrors = errors.flatMap { error ->
            when (error) {
                is TypeError.MultipleErrors -> error.errors
                else -> listOf(error)
            }
        }
        val arityError = allErrors.find { it is TypeError.ArityMismatch }
        arityError should beInstanceOf<TypeError.ArityMismatch>()
    }

    "should handle match expressions with different case result types" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case Some(value) => value.toString()  // Returns String
                case None => "empty"                  // Returns String
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.STRING
    }

    "should reject match expressions with incompatible case result types" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
            val result = match x {
                case Some(value) => value     // Returns Int
                case None => "empty"         // Returns String - incompatible!
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
    }

    "should handle generic union types in match expressions" {
        val source = """
            type Result<T, E> = Ok(T) | Error(E)
            val x = Ok(42)
            val result = match x {
                case Ok(value) => value + 1
                case Error(_) => 0
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle match expressions with boolean literals" {
        val source = """
            val flag = true
            val result = match flag {
                case true => "yes"
                case false => "no"
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.STRING
    }

    "should handle match expressions with string literals" {
        val source = """
            val greeting = "hello"
            val result = match greeting {
                case "hello" => "world"
                case "hi" => "there"
                case _ => "unknown"
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.STRING
    }

    "should detect incomplete pattern coverage for primitive types" {
        val source = """
            val x = 42
            val result = match x {
                case 42 => "forty-two"
                // Missing wildcard or other cases
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val errors = (error as TypeError.MultipleErrors).errors
        
        // Should contain a NonExhaustiveMatch error
        val nonExhaustiveError = errors.find { it is TypeError.NonExhaustiveMatch }
        nonExhaustiveError shouldBe beInstanceOf<TypeError.NonExhaustiveMatch>()
    }

    "should handle tuple patterns in match expressions" {
        val source = """
            val point = (1, 2)
            val result = match point {
                case (0, 0) => "origin"
                case (x, 0) => "on x-axis"
                case (0, y) => "on y-axis"
                case (x, y) => "general point"
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.STRING
    }

    "should detect tuple pattern arity mismatches" {
        val source = """
            val pair = (1, 2)
            val result = match pair {
                case (x, y, z) => x + y + z  // Too many elements for pair
                case _ => 0
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
    }

    "should handle nested tuple patterns" {
        val source = """
            val nestedTuple = ((1, 2), (3, 4))
            val result = match nestedTuple {
                case ((a, b), (c, d)) => a + b + c + d
                case _ => 0
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        matchResult.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle match expressions in function returns" {
        val source = """
            type Color = Red | Green | Blue
            fn colorName(c: Color): String => match c {
                case Red => "red"
                case Green => "green"
                case Blue => "blue"
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val functionDecl = result.statements.last()
        functionDecl should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should detect missing variants in union type matches" {
        val source = """
            type Color = Red | Green | Blue
            val c = Red
            val result = match c {
                case Red => "red"
                case Green => "green"
                // Missing Blue case
            }
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val errors = (error as TypeError.MultipleErrors).errors
        
        val nonExhaustiveError = errors.find { it is TypeError.NonExhaustiveMatch }
        nonExhaustiveError shouldBe beInstanceOf<TypeError.NonExhaustiveMatch>()
        
        val exhaustivenessError = nonExhaustiveError as TypeError.NonExhaustiveMatch
        exhaustivenessError.missingPatterns shouldContain "Blue"
    }
    }
}