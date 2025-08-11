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
 * Test to catch and report all exceptions during pattern matching execution
 */
class ExceptionCatchingPatternTest : DescribeSpec({
    
    describe("Exception Catching Pattern Test") {
        
        it("should catch and report all exceptions during pattern matching execution") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_exception_catch").toFile()
            
            var caughtException: Throwable? = null
            var actualOutput: String? = null
            var executionSuccess = false
            
            try {
                println("=== EXCEPTION CATCHING PATTERN TEST ===")
                
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
                typedProgramResult.fold(
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
                                    
                                    // Execute with comprehensive exception handling
                                    val originalOut = System.out
                                    val baos = ByteArrayOutputStream()
                                    val ps = PrintStream(baos)
                                    
                                    try {
                                        println("Setting output capture...")
                                        System.setOut(ps)
                                        
                                        println("About to invoke main method...")
                                        mainMethod.invoke(null, arrayOf<String>())
                                        println("Main method completed without exception")
                                        
                                        System.setOut(originalOut)
                                        actualOutput = baos.toString().trim()
                                        println("Captured output: '$actualOutput'")
                                        executionSuccess = true
                                        
                                    } catch (invocationException: java.lang.reflect.InvocationTargetException) {
                                        System.setOut(originalOut)
                                        caughtException = invocationException
                                        println("✗ InvocationTargetException caught!")
                                        println("  Target exception: ${invocationException.targetException?.javaClass?.name}")
                                        println("  Target message: ${invocationException.targetException?.message}")
                                        invocationException.targetException?.printStackTrace()
                                        
                                    } catch (generalException: Exception) {
                                        System.setOut(originalOut)
                                        caughtException = generalException
                                        println("✗ General exception caught!")
                                        println("  Exception type: ${generalException.javaClass.name}")
                                        println("  Exception message: ${generalException.message}")
                                        generalException.printStackTrace()
                                        
                                    } catch (error: Error) {
                                        System.setOut(originalOut)
                                        caughtException = error
                                        println("✗ Error caught!")
                                        println("  Error type: ${error.javaClass.name}")
                                        println("  Error message: ${error.message}")
                                        error.printStackTrace()
                                        
                                    } catch (throwable: Throwable) {
                                        System.setOut(originalOut)
                                        caughtException = throwable
                                        println("✗ Throwable caught!")
                                        println("  Throwable type: ${throwable.javaClass.name}")
                                        println("  Throwable message: ${throwable.message}")
                                        throwable.printStackTrace()
                                    }
                                    
                                } catch (loadException: Exception) {
                                    println("✗ Class loading FAILED: ${loadException.message}")
                                    caughtException = loadException
                                    loadException.printStackTrace()
                                }
                            },
                            onFailure = { error ->
                                println("✗ Bytecode generation FAILED: ${error.message}")
                            }
                        )
                    },
                    onFailure = { error ->
                        println("✗ Type checking FAILED: ${error.message}")
                    }
                )
                
                // Report results
                if (executionSuccess) {
                    println("=== EXECUTION SUCCESSFUL ===")
                    println("Output: '$actualOutput'")
                    if (actualOutput == "matched 42") {
                        println("✓ Output matches expected!")
                    } else {
                        println("✗ Output doesn't match expected")
                    }
                } else {
                    println("=== EXECUTION FAILED ===")
                    if (caughtException != null) {
                        println("Exception details:")
                        println("  Type: ${caughtException!!.javaClass.name}")
                        println("  Message: ${caughtException!!.message}")
                    }
                }
                
            } catch (outerException: Exception) {
                println("=== OUTER EXCEPTION ===")
                println("Exception: ${outerException.javaClass.name}: ${outerException.message}")
                outerException.printStackTrace()
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
    }
})