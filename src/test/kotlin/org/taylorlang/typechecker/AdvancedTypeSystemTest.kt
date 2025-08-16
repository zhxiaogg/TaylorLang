package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import org.taylorlang.ast.*

/**
 * Advanced Type System Tests
 * 
 * Tests advanced type system features and edge cases:
 * - Scope and variable shadowing (disabled tests)
 * - Property access and index operations (disabled tests)
 * - Collection and stdlib function tests (disabled tests)
 * - Nullable types (future implementation)
 * - Generic type instantiation (future implementation)
 * - Complex type scenarios
 */
class AdvancedTypeSystemTest : TypeCheckingTestBase() {
    init {

    "should handle variable shadowing correctly".config(enabled = false) { // TODO: Implement variable shadowing support
        val source = """
            val x = 10
            fn test(): Int => {
                val x = 20
                x
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
    }

    "should handle nested scopes".config(enabled = false) { // TODO: Implement nested scope support
        val source = """
            fn outer(): Int => {
                val x = 10
                {
                    val y = 20
                    x + y
                }
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should detect out-of-scope variable access".config(enabled = false) { // TODO: Implement scope validation
        val source = """
            {
                val x = 10
            }
            val y = x
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        // Should fail because x is not in scope
        error should beInstanceOf<TypeError>()
    }

    "should type check property access on known types".config(enabled = false) { // TODO: Implement property access type checking
        val source = """
            type Person = Student(name: String, id: Int)
            val student = Student(name: "John", id: 123)
            val name = student.name
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
    }

    "should type check index access on lists".config(enabled = false) { // TODO: Implement index access type checking
        val source = """
            val arr = List.of(1, 2, 3)
            val first = arr[0]
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
    }

    "should type check stdlib collection functions".config(enabled = false) { // TODO: Implement stdlib function type checking
        val source = "val numbers = List.of(1, 2, 3)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should type check map creation with stdlib".config(enabled = false) { // TODO: Implement stdlib Map type checking
        val source = "val config = Map.of(\"host\", \"localhost\", \"port\", 8080)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should type check tuple literals correctly".config(enabled = false) { // TODO: Fix tuple literal type checking
        val source = "val point = (10, 20)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should type check nullable types".config(enabled = false) { // TODO: Implement nullable type support
        val source = "val x: String? = null"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should type check tuple types".config(enabled = false) { // TODO: Fix tuple type checking implementation
        val source = "val x: (Int, String) = (42, \"hello\")"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    "should type check generic type instantiation".config(enabled = false) { // TODO: Implement generic type checking
        val source = "val x: List<Int> = [1, 2, 3]"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
    }

    // Enabled tests for currently working features

    "should handle complex nested expressions" {
        val source = """
            val result = (1 + 2) * (3 + 4) + (5 * 6)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val varDecl = result.statements.first() as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle mixed type arithmetic with promotion" {
        val source = """
            val intValue = 42
            val doubleValue = 3.14
            val result = intValue + doubleValue
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val resultDecl = result.statements.last() as TypedStatement.VariableDeclaration
        resultDecl.inferredType shouldBe BuiltinTypes.DOUBLE
    }

    "should handle string concatenation with various types" {
        val source = """
            val message = "Value: " + 42 + ", Flag: " + true
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val varDecl = result.statements.first() as TypedStatement.VariableDeclaration
        varDecl.inferredType shouldBe BuiltinTypes.STRING
    }

    "should handle function composition" {
        val source = """
            fn double(x: Int): Int => x * 2
            fn addTen(x: Int): Int => x + 10
            fn compose(x: Int): Int => addTen(double(x))
            val result = compose(5)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        val resultDecl = result.statements.last() as TypedStatement.VariableDeclaration
        resultDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle conditional expressions with type inference" {
        val source = """
            fn getDefaultValue(hasValue: Boolean): Int => 
                if (hasValue) 42 else 0
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        val functionDecl = result.statements.first()
        functionDecl should beInstanceOf<TypedStatement.FunctionDeclaration>()
    }

    "should handle recursive type definitions" {
        val source = """
            type List<T> = Nil | Cons(T, List<T>)
            val emptyList = Nil
            val oneElement = Cons(1, Nil)
            val twoElements = Cons(2, Cons(1, Nil))
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        
        for (i in 1..3) {
            val varDecl = result.statements[i] as TypedStatement.VariableDeclaration
            varDecl.inferredType should beInstanceOf<Type.UnionType>()
            val unionType = varDecl.inferredType as Type.UnionType
            unionType.name shouldBe "List"
        }
    }

    "should handle complex pattern matching scenarios" {
        val source = """
            type Tree<T> = Leaf(T) | Branch(Tree<T>, Tree<T>)
            val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
            fn sum(t: Tree<Int>): Int => match t {
                case Leaf(value) => value
                case Branch(left, right) => sum(left) + sum(right)
            }
            val total = sum(tree)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        val totalDecl = result.statements.last() as TypedStatement.VariableDeclaration
        totalDecl.inferredType shouldBe BuiltinTypes.INT
    }

    "should handle multiple generic type parameters" {
        val source = """
            type Pair<A, B> = MakePair(A, B)
            val stringIntPair = MakePair("hello", 42)
            val boolDoublePair = MakePair(true, 3.14)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val stringIntDecl = result.statements[1] as TypedStatement.VariableDeclaration
        stringIntDecl.inferredType should beInstanceOf<Type.UnionType>()
        val stringIntType = stringIntDecl.inferredType as Type.UnionType
        stringIntType.name shouldBe "Pair"
        stringIntType.typeArguments.size shouldBe 2
        stringIntType.typeArguments[0] shouldBe BuiltinTypes.STRING
        stringIntType.typeArguments[1] shouldBe BuiltinTypes.INT
    }
    }
}