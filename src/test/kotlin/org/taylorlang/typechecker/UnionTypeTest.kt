package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import org.taylorlang.ast.*

/**
 * Union Type Tests
 * 
 * Tests union type declarations and constructor type checking:
 * - Union type declarations
 * - Named product types
 * - Multi-argument variant constructors
 * - Duplicate variant detection
 * - Constructor calls and type inference
 * - Generic union types
 */
class UnionTypeTest : TypeCheckingTestBase() {
    init {

    "should type check union type declarations" {
        val source = "type Option<T> = Some(T) | None"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        result.statements.first() should beInstanceOf<TypedStatement.TypeDeclaration>()
    }

    "should type check named product types" {
        val source = "type Person = Student(name: String, id: Int) | Teacher(name: String, subject: String)"
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 1
        result.statements.first() should beInstanceOf<TypedStatement.TypeDeclaration>()
    }

    "should type check constructor calls" {
        val source = """
            type Option<T> = Some(T) | None
            val x = Some(42)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
    }

    "should detect duplicate variant names in union declarations" {
        val source = "type Bad = A | B | A"  // Duplicate variant A
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError.MultipleErrors>()
        val errors = (error as TypeError.MultipleErrors).errors
        
        // Should contain a DuplicateDefinition error
        val duplicateError = errors.find { it is TypeError.DuplicateDefinition }
        duplicateError shouldBe beInstanceOf<TypeError.DuplicateDefinition>()
    }

    "should type check multi-argument variant constructors" {
        val source = """
            type Point = Point2D(Int, Int) | Point3D(Int, Int, Int)
            val p2 = Point2D(1, 2)
            val p3 = Point3D(1, 2, 3)
            val result = match p2 {
                case Point2D(x, y) => x + y
                case Point3D(x, y, z) => x + y + z
            }
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        val matchResult = result.statements.last() as TypedStatement.VariableDeclaration
        // Compare types using structural equality (ignoring source locations)
        TypeOperations.areEqual(matchResult.inferredType, BuiltinTypes.INT) shouldBe true
    }

    "should type check simple union types without type parameters" {
        val source = """
            type Color = Red | Green | Blue
            val primaryColor = Red
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 2
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Color"
    }

    "should type check union types with single type parameter" {
        val source = """
            type Box<T> = Empty | Full(T)
            val emptyBox = Empty
            val fullBox = Full(42)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val emptyDecl = result.statements[1] as TypedStatement.VariableDeclaration
        emptyDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val fullDecl = result.statements[2] as TypedStatement.VariableDeclaration
        fullDecl.inferredType should beInstanceOf<Type.UnionType>()
        val fullType = fullDecl.inferredType as Type.UnionType
        fullType.name shouldBe "Box"
        fullType.typeArguments.size shouldBe 1
        fullType.typeArguments[0] shouldBe BuiltinTypes.INT
    }

    "should type check union types with multiple type parameters" {
        val source = """
            type Either<L, R> = Left(L) | Right(R)
            val leftValue = Left("error")
            val rightValue = Right(42)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val leftDecl = result.statements[1] as TypedStatement.VariableDeclaration
        leftDecl.inferredType should beInstanceOf<Type.UnionType>()
        val leftType = leftDecl.inferredType as Type.UnionType
        leftType.name shouldBe "Either"
        leftType.typeArguments.size shouldBe 2
        leftType.typeArguments[0] shouldBe BuiltinTypes.STRING
        
        val rightDecl = result.statements[2] as TypedStatement.VariableDeclaration
        rightDecl.inferredType should beInstanceOf<Type.UnionType>()
        val rightType = rightDecl.inferredType as Type.UnionType
        rightType.name shouldBe "Either"
        rightType.typeArguments.size shouldBe 2
        rightType.typeArguments[1] shouldBe BuiltinTypes.INT
    }

    "should type check nested union type constructors" {
        val source = """
            type Option<T> = Some(T) | None
            type Result<T, E> = Ok(T) | Error(E)
            val nested = Ok(Some(42))
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val resultType = varDecl.inferredType as Type.UnionType
        resultType.name shouldBe "Result"
        
        val optionType = resultType.typeArguments[0]
        optionType should beInstanceOf<Type.UnionType>()
        val innerOption = optionType as Type.UnionType
        innerOption.name shouldBe "Option"
        innerOption.typeArguments[0] shouldBe BuiltinTypes.INT
    }

    "should handle union types with tuple constructors" {
        val source = """
            type Shape = Point(Int, Int) | Rectangle(Int, Int, Int, Int)
            val point = Point(5, 10)
            val rect = Rectangle(0, 0, 10, 20)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val pointDecl = result.statements[1] as TypedStatement.VariableDeclaration
        pointDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val rectDecl = result.statements[2] as TypedStatement.VariableDeclaration
        rectDecl.inferredType should beInstanceOf<Type.UnionType>()
    }

    "should detect constructor calls with wrong arity" {
        val source = """
            type Point = Point2D(Int, Int) | Point3D(Int, Int, Int)
            val badPoint = Point2D(1)  // Missing second argument
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should detect constructor calls with wrong argument types" {
        val source = """
            type Person = Student(String, Int) | Teacher(String, String)
            val badStudent = Student(42, "name")  // Arguments in wrong order
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should type check union types with boolean constructors" {
        val source = """
            type Status = Active(Boolean) | Inactive
            val activeStatus = Active(true)
            val inactiveStatus = Inactive
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val activeDecl = result.statements[1] as TypedStatement.VariableDeclaration
        activeDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val inactiveDecl = result.statements[2] as TypedStatement.VariableDeclaration
        inactiveDecl.inferredType should beInstanceOf<Type.UnionType>()
    }

    "should handle union types with mixed constructor arities" {
        val source = """
            type Value = Empty | Single(Int) | Pair(Int, Int) | Triple(Int, Int, Int)
            val empty = Empty
            val single = Single(1)
            val pair = Pair(1, 2)
            val triple = Triple(1, 2, 3)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 5
        
        // All should be instances of the same union type
        for (i in 1..4) {
            val varDecl = result.statements[i] as TypedStatement.VariableDeclaration
            varDecl.inferredType should beInstanceOf<Type.UnionType>()
            val unionType = varDecl.inferredType as Type.UnionType
            unionType.name shouldBe "Value"
        }
    }

    "should type check recursive union types" {
        val source = """
            type List<T> = Nil | Cons(T, List<T>)
            val emptyList = Nil
            val singleList = Cons(1, Nil)
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 3
        
        val emptyDecl = result.statements[1] as TypedStatement.VariableDeclaration
        emptyDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val singleDecl = result.statements[2] as TypedStatement.VariableDeclaration
        singleDecl.inferredType should beInstanceOf<Type.UnionType>()
        val singleType = singleDecl.inferredType as Type.UnionType
        singleType.name shouldBe "List"
    }

    "should detect invalid constructor names" {
        val source = """
            type Option<T> = Some(T) | None
            val invalid = NotSome(42)  // NotSome is not a valid constructor
        """.trimIndent()
        val error = expectTypeCheckFailure(source)
        
        error should beInstanceOf<TypeError>()
    }

    "should type check union types with string constructors" {
        val source = """
            type Message = Info(String) | Warning(String) | Error(String)
            val info = Info("System started")
            val warning = Warning("Low memory")
            val error = Error("Connection failed")
        """.trimIndent()
        val result = typeCheckProgramSuccess(source)
        
        result.statements.size shouldBe 4
        
        for (i in 1..3) {
            val varDecl = result.statements[i] as TypedStatement.VariableDeclaration
            varDecl.inferredType should beInstanceOf<Type.UnionType>()
            val unionType = varDecl.inferredType as Type.UnionType
            unionType.name shouldBe "Message"
        }
    }
    }
}