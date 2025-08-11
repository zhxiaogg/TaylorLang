package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files

/**
 * Non-failing test to identify the exact pattern matching issue
 */
class NonFailingPatternTest : DescribeSpec({
    
    describe("Pattern Matching Investigation") {
        
        it("should investigate pattern matching execution without failing") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_pattern_investigate").toFile()
            
            try {
                println("=== INVESTIGATING PATTERN MATCHING ===")
                
                // Create pattern matching program
                val program = TestUtils.createProgram(listOf(
                    TestUtils.createExpressionStatement(
                        MatchExpression(
                            target = Literal.IntLiteral(42),
                            cases = persistentListOf(
                                MatchCase(
                                    pattern = Pattern.LiteralPattern(Literal.IntLiteral(42)),
                                    expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("matched 42")))
                                ),
                                MatchCase(
                                    pattern = Pattern.WildcardPattern,
                                    expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("no match")))
                                )
                            )
                        )
                    )
                ))
                
                // Type check
                val typedProgramResult = typeChecker.typeCheck(program)
                val success = typedProgramResult.fold(
                    onSuccess = { typedProgram ->
                        println("✓ Type checking SUCCESS")
                        
                        // Generate bytecode
                        val result = generator.generateBytecode(typedProgram, tempDir)
                        result.fold(
                            onSuccess = { generationResult ->
                                println("✓ Bytecode generation SUCCESS")
                                
                                try {
                                    // Load class
                                    val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                                    val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                                    val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                                    println("✓ Class loading SUCCESS")
                                    
                                    // Execute with output capture
                                    val originalOut = System.out
                                    val baos = ByteArrayOutputStream()
                                    val ps = PrintStream(baos)
                                    
                                    System.setOut(ps)
                                    mainMethod.invoke(null, arrayOf<String>())
                                    System.setOut(originalOut)
                                    
                                    val actualOutput = baos.toString().trim()
                                    println("✓ Execution SUCCESS")
                                    println("✓ Captured output: '$actualOutput'")
                                    
                                    if (actualOutput == "matched 42") {
                                        println("✓ OUTPUT MATCHES EXPECTED!")
                                        true
                                    } else {
                                        println("✗ Output mismatch: expected 'matched 42', got '$actualOutput'")
                                        false
                                    }
                                    
                                } catch (e: Exception) {
                                    println("✗ Execution FAILED: ${e.javaClass.name}: ${e.message}")
                                    if (e.cause != null) {
                                        println("  Caused by: ${e.cause!!.javaClass.name}: ${e.cause!!.message}")
                                    }
                                    e.printStackTrace()
                                    false
                                }
                            },
                            onFailure = { error ->
                                println("✗ Bytecode generation FAILED: ${error.message}")
                                false
                            }
                        )
                    },
                    onFailure = { error ->
                        println("✗ Type checking FAILED: ${error.message}")
                        false
                    }
                )
                
                if (success) {
                    println("=== PATTERN MATCHING TEST COMPLETED SUCCESSFULLY ===")
                } else {
                    println("=== PATTERN MATCHING TEST FAILED ===")
                }
                
            } catch (e: Exception) {
                println("=== UNEXPECTED ERROR ===")
                println("Exception: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
    }
})