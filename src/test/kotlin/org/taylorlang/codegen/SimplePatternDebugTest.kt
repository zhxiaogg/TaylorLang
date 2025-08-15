package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

/**
 * Simple test to isolate pattern matching runtime issues
 */
class SimplePatternDebugTest : DescribeSpec({
    
    describe("Pattern Matching Debug") {
        
        it("should debug simple pattern matching execution") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_pattern_debug").toFile()
            
            try {
                println("=== DEBUGGING PATTERN MATCHING ===")
                
                // Create the simplest possible pattern matching program
                val program = TestUtils.createProgram(listOf(
                    TestUtils.createExpressionStatement(
                        MatchExpression(
                            target = Literal.IntLiteral(42),
                            cases = persistentListOf(
                                MatchCase(
                                    pattern = Pattern.LiteralPattern(Literal.IntLiteral(42)),
                                    expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("matched 42")))
                                )
                            )
                        )
                    )
                ))
                
                println("Program created: $program")
                
                // Type check
                println("Starting type checking...")
                val typedProgramResult = typeChecker.typeCheck(program)
                typedProgramResult.fold(
                    onSuccess = { typedProgram ->
                        println("Type checking SUCCESS")
                        
                        // Generate bytecode
                        println("Starting bytecode generation...")
                        val result = generator.generateBytecode(typedProgram, tempDir)
                        result.fold(
                            onSuccess = { generationResult ->
                                println("Bytecode generation SUCCESS")
                                println("Main class name: ${generationResult.mainClassName}")
                                println("Bytecode files: ${generationResult.bytecodeFiles}")
                                
                                // List generated files
                                tempDir.listFiles()?.forEach { file ->
                                    println("Generated file: ${file.name}")
                                }
                                
                                // Try to load and execute
                                println("Loading class...")
                                val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                                val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                                val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                                
                                println("About to invoke main method...")
                                try {
                                    mainMethod.invoke(null, arrayOf<String>())
                                    println("Main method executed successfully!")
                                } catch (e: Exception) {
                                    println("Exception during main method execution:")
                                    println("Exception type: ${e.javaClass.name}")
                                    println("Exception message: ${e.message}")
                                    if (e.cause != null) {
                                        println("Caused by: ${e.cause!!.javaClass.name}: ${e.cause!!.message}")
                                    }
                                    e.printStackTrace()
                                    throw e
                                }
                            },
                            onFailure = { error ->
                                println("Bytecode generation FAILED: ${error.message}")
                                error.printStackTrace()
                                throw error
                            }
                        )
                    },
                    onFailure = { error ->
                        println("Type checking FAILED: ${error.message}")
                        error.printStackTrace()
                        throw error
                    }
                )
                
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
        
        it("should debug wildcard pattern matching execution") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_wildcard_debug").toFile()
            
            try {
                println("=== DEBUGGING WILDCARD PATTERN MATCHING ===")
                
                // Create the simplest possible wildcard pattern matching program
                val program = TestUtils.createProgram(listOf(
                    TestUtils.createExpressionStatement(
                        MatchExpression(
                            target = Literal.IntLiteral(42),
                            cases = persistentListOf(
                                MatchCase(
                                    pattern = Pattern.WildcardPattern,
                                    expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("wildcard works")))
                                )
                            )
                        )
                    )
                ))
                
                println("Program created: $program")
                
                // Type check
                println("Starting type checking...")
                val typedProgramResult = typeChecker.typeCheck(program)
                typedProgramResult.fold(
                    onSuccess = { typedProgram ->
                        println("Type checking SUCCESS")
                        
                        // Generate bytecode
                        println("Starting bytecode generation...")
                        val result = generator.generateBytecode(typedProgram, tempDir)
                        result.fold(
                            onSuccess = { generationResult ->
                                println("Bytecode generation SUCCESS")
                                println("Main class name: ${generationResult.mainClassName}")
                                println("Bytecode files: ${generationResult.bytecodeFiles}")
                                
                                // List generated files
                                tempDir.listFiles()?.forEach { file ->
                                    println("Generated file: ${file.name}")
                                }
                                
                                // Try to load and execute
                                println("Loading class...")
                                val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                                val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                                val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                                
                                println("About to invoke main method...")
                                try {
                                    mainMethod.invoke(null, arrayOf<String>())
                                    println("Main method executed successfully!")
                                } catch (e: Exception) {
                                    println("Exception during main method execution:")
                                    println("Exception type: ${e.javaClass.name}")
                                    println("Exception message: ${e.message}")
                                    if (e.cause != null) {
                                        println("Caused by: ${e.cause!!.javaClass.name}: ${e.cause!!.message}")
                                    }
                                    e.printStackTrace()
                                    throw e
                                }
                            },
                            onFailure = { error ->
                                println("Bytecode generation FAILED: ${error.message}")
                                error.printStackTrace()
                                throw error
                            }
                        )
                    },
                    onFailure = { error ->
                        println("Type checking FAILED: ${error.message}")
                        error.printStackTrace()
                        throw error
                    }
                )
                
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
        
        it("should debug simple println without patterns") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_println_debug").toFile()
            
            try {
                println("=== DEBUGGING SIMPLE PRINTLN ===")
                
                // Create a simple println program without any pattern matching
                val program = TestUtils.createProgram(listOf(
                    TestUtils.createExpressionStatement(
                        TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("Hello World!")))
                    )
                ))
                
                println("Program created: $program")
                
                // Type check
                println("Starting type checking...")
                val typedProgramResult = typeChecker.typeCheck(program)
                typedProgramResult.fold(
                    onSuccess = { typedProgram ->
                        println("Type checking SUCCESS")
                        
                        // Generate bytecode
                        println("Starting bytecode generation...")
                        val result = generator.generateBytecode(typedProgram, tempDir)
                        result.fold(
                            onSuccess = { generationResult ->
                                println("Bytecode generation SUCCESS")
                                println("Main class name: ${generationResult.mainClassName}")
                                
                                // Try to load and execute
                                println("Loading class...")
                                val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                                val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                                val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                                
                                println("About to invoke main method...")
                                try {
                                    mainMethod.invoke(null, arrayOf<String>())
                                    println("Main method executed successfully!")
                                } catch (e: Exception) {
                                    println("Exception during main method execution:")
                                    println("Exception type: ${e.javaClass.name}")
                                    println("Exception message: ${e.message}")
                                    if (e.cause != null) {
                                        println("Caused by: ${e.cause!!.javaClass.name}: ${e.cause!!.message}")
                                    }
                                    e.printStackTrace()
                                    throw e
                                }
                            },
                            onFailure = { error ->
                                println("Bytecode generation FAILED: ${error.message}")
                                error.printStackTrace()
                                throw error
                            }
                        )
                    },
                    onFailure = { error ->
                        println("Type checking FAILED: ${error.message}")
                        error.printStackTrace()
                        throw error
                    }
                )
                
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
    }
})