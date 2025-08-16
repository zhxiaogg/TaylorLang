package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.should
import io.kotest.assertions.fail
import org.taylorlang.ast.*

/**
 * Generic Type Inference Tests
 * 
 * Tests generic type parameter inference and type instantiation:
 * - Generic type parameter inference from constructor arguments
 * - Union type declarations and constructor calls
 * - Multi-parameter generic types
 * - Type instantiation scenarios
 * - Zero-argument constructor handling
 */
class GenericTypeInferenceTest : TypeCheckingTestBase() {
    init {

    "should infer generic type parameters from constructor arguments" {
        // Test cases that demonstrate the generic type inference bug
        val testCases = listOf(
            // Basic generic constructor with Int argument
            Triple(
                """
                type Option<T> = Some(T) | None
                val x = Some(42)
                """.trimIndent(),
                "x",
                "Option<Int>"
            ),
            // Generic constructor with String argument  
            Triple(
                """
                type Option<T> = Some(T) | None
                val y = Some("hello")
                """.trimIndent(),
                "y", 
                "Option<String>"
            ),
            // Zero-argument constructor (should work without inference)
            Triple(
                """
                type Option<T> = Some(T) | None
                val z = None
                """.trimIndent(),
                "z",
                "Option<T>" // This might remain generic without inference context
            ),
            // Multi-parameter generic type
            Triple(
                """
                type Result<T,E> = Ok(T) | Error(E)
                val w = Ok(42)
                """.trimIndent(),
                "w",
                "Result<Int, ?>" // E should remain unresolved without inference context
            )
        )
        
        testCases.forEach { (source, variableName, expectedTypeString) ->
            println("=== Testing: $source ===")
            
            // Parse the source code
            val program = parser.parse(source)
                .getOrElse { error ->
                    println("Parse error: $error")
                    fail("Failed to parse source: $source")
                }
            
            println("Parsed program: $program")
            
            // Type check the program
            val result = typeChecker.typeCheck(program)
            
            result.fold(
                onSuccess = { typedProgram ->
                    println("Type checking succeeded")
                    println("Typed statements: ${typedProgram.statements}")
                    
                    // Find the variable declaration we're testing
                    val varDecl = typedProgram.statements
                        .filterIsInstance<TypedStatement.VariableDeclaration>()
                        .find { it.declaration.name == variableName }
                    
                    if (varDecl != null) {
                        val actualType = varDecl.inferredType
                        println("Variable '$variableName' has inferred type: $actualType")
                        
                        // For this test, we'll verify the type structure rather than exact string match
                        // since type representation might vary
                        when (expectedTypeString) {
                            "Option<Int>" -> {
                                actualType should beInstanceOf<Type.UnionType>()
                                val unionType = actualType as Type.UnionType
                                unionType.name shouldBe "Option"
                                unionType.typeArguments.size shouldBe 1
                                unionType.typeArguments[0] shouldBe BuiltinTypes.INT
                            }
                            "Option<String>" -> {
                                actualType should beInstanceOf<Type.UnionType>()
                                val unionType = actualType as Type.UnionType
                                unionType.name shouldBe "Option"
                                unionType.typeArguments.size shouldBe 1
                                unionType.typeArguments[0] shouldBe BuiltinTypes.STRING
                            }
                            else -> {
                                // For other cases, just verify it's a union type for now
                                actualType should beInstanceOf<Type.UnionType>()
                                println("Type verification for '$expectedTypeString' not fully implemented yet")
                            }
                        }
                    } else {
                        fail("Could not find variable declaration for '$variableName' in typed program")
                    }
                },
                onFailure = { error ->
                    println("Type checking failed with error: $error")
                    println("Error details: ${error.stackTraceToString()}")
                    
                    // Print detailed debugging information
                    when (error) {
                        is TypeError.MultipleErrors -> {
                            println("Multiple errors encountered:")
                            error.errors.forEachIndexed { index, err ->
                                println("  Error $index: $err")
                            }
                        }
                        is TypeError.UnresolvedSymbol -> {
                            println("Unresolved symbol: ${error.symbol}")
                        }
                        is TypeError.TypeMismatch -> {
                            println("Type mismatch - Expected: ${error.expected}, Actual: ${error.actual}")
                        }
                        is TypeError.ArityMismatch -> {
                            println("Arity mismatch - Expected: ${error.expected}, Actual: ${error.actual}")
                        }
                        else -> {
                            println("Other error type: ${error::class.simpleName}")
                        }
                    }
                    
                    fail("Type checking should have succeeded for: $source")
                }
            )
            
            println("=== End test case ===\n")
        }
    }

    "should infer Int type from generic constructor with integer argument" {
        val source = """
            type Option<T> = Some(T) | None
            val intOption = Some(42)
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Option"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments[0] shouldBe BuiltinTypes.INT
    }

    "should infer String type from generic constructor with string argument" {
        val source = """
            type Option<T> = Some(T) | None
            val stringOption = Some("hello")
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Option"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments[0] shouldBe BuiltinTypes.STRING
    }

    "should infer Boolean type from generic constructor with boolean argument" {
        val source = """
            type Option<T> = Some(T) | None
            val boolOption = Some(true)
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Option"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments[0] shouldBe BuiltinTypes.BOOLEAN
    }

    "should handle zero-argument constructors" {
        val source = """
            type Option<T> = Some(T) | None
            val noneOption = None
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Option"
        // Type parameters might remain unresolved for zero-argument constructors
    }

    "should handle multi-parameter generic types with partial inference" {
        val source = """
            type Result<T,E> = Ok(T) | Error(E)
            val okResult = Ok(42)
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Result"
        unionType.typeArguments.size shouldBe 2
        unionType.typeArguments[0] shouldBe BuiltinTypes.INT
        // E parameter might remain unresolved without inference context
    }

    "should handle multi-parameter generic types with error variant" {
        val source = """
            type Result<T,E> = Ok(T) | Error(E)
            val errorResult = Error("failure")
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Result"
        unionType.typeArguments.size shouldBe 2
        // T parameter might remain unresolved
        unionType.typeArguments[1] shouldBe BuiltinTypes.STRING
    }

    "should infer nested generic types" {
        val source = """
            type Option<T> = Some(T) | None
            type Result<T,E> = Ok(T) | Error(E)
            val nestedResult = Ok(Some(42))
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 3
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val resultType = varDecl.inferredType as Type.UnionType
        resultType.name shouldBe "Result"
        resultType.typeArguments.size shouldBe 2
        
        // First type argument should be Option<Int>
        val optionType = resultType.typeArguments[0]
        optionType should beInstanceOf<Type.UnionType>()
        val innerOption = optionType as Type.UnionType
        innerOption.name shouldBe "Option"
        innerOption.typeArguments[0] shouldBe BuiltinTypes.INT
    }

    "should handle generic type instantiation with complex expressions" {
        val source = """
            type Option<T> = Some(T) | None
            val computedOption = Some(1 + 2 * 3)
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Option"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments[0] shouldBe BuiltinTypes.INT
    }

    "should handle tuple types in generic constructors" {
        val source = """
            type Container<T> = Hold(T)
            val tupleContainer = Hold((1, "hello"))
        """.trimIndent()
        
        val result = typeCheckProgramSuccess(source)
        result.statements.size shouldBe 2
        
        val varDecl = result.statements.last() as TypedStatement.VariableDeclaration
        varDecl.inferredType should beInstanceOf<Type.UnionType>()
        
        val unionType = varDecl.inferredType as Type.UnionType
        unionType.name shouldBe "Container"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments[0] should beInstanceOf<Type.TupleType>()
        
        val tupleType = unionType.typeArguments[0] as Type.TupleType
        tupleType.elementTypes.size shouldBe 2
        tupleType.elementTypes[0] shouldBe BuiltinTypes.INT
        tupleType.elementTypes[1] shouldBe BuiltinTypes.STRING
    }
    }
}