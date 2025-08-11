@file:JvmName("ManualPatternTest")

import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.codegen.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

fun main() {
    println("=== Manual Pattern Matching Test ===")
    
    try {
        val typeChecker = RefactoredTypeChecker()
        val generator = BytecodeGenerator()
        val tempDir = Files.createTempDirectory("manual_pattern").toFile()
        
        println("1. Creating pattern matching program...")
        
        // Create the exact same program that PatternMatchingBytecodeTest uses
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
        
        println("✅ Program created successfully")
        
        println("2. Type checking...")
        val typedProgramResult = typeChecker.typeCheck(program)
        
        typedProgramResult.fold(
            onSuccess = { typedProgram ->
                println("✅ Type checking succeeded!")
                
                println("3. Generating bytecode...")
                val result = generator.generateBytecode(typedProgram, tempDir)
                
                result.fold(
                    onSuccess = { generationResult ->
                        println("✅ Bytecode generation succeeded!")
                        println("   Generated class: ${generationResult.mainClassName}")
                        println("   Bytecode files: ${generationResult.bytecodeFiles}")
                        
                        println("4. Attempting execution...")
                        try {
                            // Load and execute the generated class
                            val classLoader = java.net.URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                            val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                            val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                            
                            // Capture output
                            val originalOut = System.out
                            val baos = java.io.ByteArrayOutputStream()
                            val ps = java.io.PrintStream(baos)
                            System.setOut(ps)
                            
                            try {
                                mainMethod.invoke(null, arrayOf<String>())
                                val actualOutput = baos.toString().trim()
                                println("✅ Execution succeeded!")
                                println("   Output: '$actualOutput'")
                                
                                if (actualOutput == "matched 42") {
                                    println("✅ Pattern matching works correctly!")
                                } else {
                                    println("❌ Unexpected output. Expected: 'matched 42', got: '$actualOutput'")
                                }
                            } finally {
                                System.setOut(originalOut)
                            }
                            
                        } catch (e: Exception) {
                            println("❌ Execution failed: ${e.message}")
                            e.printStackTrace()
                        }
                        
                    },
                    onFailure = { error ->
                        println("❌ Bytecode generation failed: ${error.message}")
                        if (error is Throwable) {
                            error.printStackTrace()
                        }
                    }
                )
                
            },
            onFailure = { error ->
                println("❌ Type checking failed: ${error.message}")
                if (error is Throwable) {
                    error.printStackTrace()
                }
            }
        )
        
        // Clean up
        tempDir.deleteRecursively()
        
    } catch (e: Exception) {
        println("❌ Exception during test: ${e.message}")
        e.printStackTrace()
    }
    
    println("\n=== Test Complete ===")
}