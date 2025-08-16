package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import org.taylorlang.ast.*

/**
 * Function Type Checking Tests
 * 
 * Tests function-related type checking functionality:
 * - Multi-parameter function type checking
 * - Generic functions and return type validation
 * - Function calls with arguments
 * - Parameter type mismatches
 * - Simple function declarations
 * - Return type validation
 */
class FunctionTypeCheckingTest : TypeCheckingTestBase() {
    init {

    "should type check simple function declarations" {
        val source = "fn identity(x: Int): Int => x"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function with multiple parameters" {
        // TODO: Implement multi-parameter function type checking
        // Current issue: TypeChecker doesn't handle complex binary operations with multiple operands
        val source = "fn add(x: Int, y: Int, z: Int): Int => x + y + z"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function with two parameters" {
        val source = "fn add(x: Int, y: Int): Int => x + y"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function with string parameters" {
        val source = "fn concat(a: String, b: String): String => a + b"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function with boolean return type" {
        val source = "fn isPositive(x: Int): Boolean => x > 0"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check generic functions" { // TODO: Implement generic function support
        val source = "fn identity<T>(x: T): T => x"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function with mixed parameter types" {
        val source = "fn describe(name: String, age: Int, active: Boolean): String => name + \" is \" + age.toString()"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should detect parameter type mismatches" { // TODO: Implement return type validation
        val source = "fn test(x: Int): String => x"  // Returns Int but declares String
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should detect incorrect return type in binary operations" {
        val source = "fn badAdd(x: Int, y: Int): String => x + y"  // Returns Int but declares String
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should detect incorrect return type in comparison" {
        val source = "fn badCompare(x: Int, y: Int): Int => x > y"  // Returns Boolean but declares Int
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should type check function calls with arguments" { // TODO: Implement function call type checking
        val source = """
            fn add(x: Int, y: Int): Int => x + y
            val result = add(1, 2)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should type check nested function calls" {
        val source = """
            fn double(x: Int): Int => x * 2
            fn quadruple(x: Int): Int => double(double(x))
            val result = quadruple(5)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should type check function with zero parameters" {
        val source = "fn getAnswer(): Int => 42"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check function call with zero arguments" {
        val source = """
            fn getAnswer(): Int => 42
            val answer = getAnswer()
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should detect function call with wrong number of arguments" {
        val source = """
            fn add(x: Int, y: Int): Int => x + y
            val result = add(1)  // Missing second argument
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should detect function call with too many arguments" {
        val source = """
            fn add(x: Int, y: Int): Int => x + y
            val result = add(1, 2, 3)  // Too many arguments
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should detect function call with wrong argument types" {
        val source = """
            fn add(x: Int, y: Int): Int => x + y
            val result = add("hello", "world")  // Wrong argument types
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should type check function with complex expression body" {
        val source = "fn compute(x: Int, y: Int): Boolean => (x + y) * 2 > x - y"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should type check recursive function declarations" {
        val source = "fn factorial(n: Int): Int => if (n <= 1) 1 else n * factorial(n - 1)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should detect parsing error for tuple access syntax" {
        val source = "fn getFirst(pair: (Int, String)): Int => pair.0"
        val error = expectParseFailure(source)
        
        // Tuple access syntax (pair.0) is not yet implemented 
        error should beInstanceOf<RuntimeException>()
    }

    "should type check function returning tuple" {
        val source = "fn makePair(x: Int, y: String): (Int, String) => (x, y)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val statement = result.statements.first()
        statement should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should detect parsing error for function type syntax" {
        val source = """
            fn apply(f: (Int) => Int, x: Int): Int => f(x)
            fn double(x: Int): Int => x * 2
            val result = apply(double, 5)
        """.trimIndent()
        val error = expectParseFailure(source)
        
        // Function type syntax (Int) => Int is not yet implemented
        error should beInstanceOf<RuntimeException>()
    }

    "should detect mismatched function parameter types in calls" {
        val source = """
            fn greet(name: String): String => "Hello " + name
            val greeting = greet(42)  // Int instead of String
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }
    }
}