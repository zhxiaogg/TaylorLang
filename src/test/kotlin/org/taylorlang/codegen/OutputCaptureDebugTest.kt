package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files

/**
 * Debug test to isolate output capture issues with pattern matching
 */
class OutputCaptureDebugTest : DescribeSpec({
    
    describe("Output Capture Debug") {
        
        it("should capture pattern matching output correctly") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_output_debug").toFile()
            
            try {
                println("=== DEBUGGING OUTPUT CAPTURE ===")
                
                // Create simple pattern matching program
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
                
                // Type check and generate bytecode
                val typedProgramResult = typeChecker.typeCheck(program)
                typedProgramResult.fold(
                    onSuccess = { typedProgram ->
                        val result = generator.generateBytecode(typedProgram, tempDir)
                        result.fold(
                            onSuccess = { generationResult ->
                                // Load and execute with output capture
                                val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                                val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                                val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                                
                                // Capture stdout
                                val originalOut = System.out
                                val baos = ByteArrayOutputStream()
                                val ps = PrintStream(baos)
                                
                                try {
                                    println("Setting up output capture...")
                                    
                                    // CRITICAL FIX: Set output capture BEFORE any debug statements that should not be captured
                                    System.setOut(ps)
                                    
                                    // FIXED: Remove debug output that was contaminating captured results
                                    mainMethod.invoke(null, arrayOf<String>())
                                    
                                    // FIXED: Restore original output BEFORE any debug prints
                                    System.setOut(originalOut)
                                    val actualOutput = baos.toString().trim()
                                    
                                    println("Captured output: '$actualOutput'")
                                    println("Expected output: 'matched 42'")
                                    
                                    // Check if output matches
                                    actualOutput shouldBe "matched 42"
                                    println("Output capture test PASSED!")
                                    
                                } catch (e: Exception) {
                                    System.setOut(originalOut)
                                    println("Exception during output capture:")
                                    println("Exception type: ${e.javaClass.name}")
                                    println("Exception message: ${e.message}")
                                    e.printStackTrace()
                                    throw e
                                }
                            },
                            onFailure = { error ->
                                println("Bytecode generation failed: ${error.message}")
                                throw error
                            }
                        )
                    },
                    onFailure = { error ->
                        println("Type checking failed: ${error.message}")
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