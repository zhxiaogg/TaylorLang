package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

/**
 * Test to generate and inspect pattern matching bytecode
 */
class BytecodeInspectionTest : DescribeSpec({
    
    describe("Pattern Matching Bytecode Inspection") {
        
        it("should generate pattern matching bytecode for inspection") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_bytecode_inspect").toFile()
            
            try {
                println("=== GENERATING PATTERN MATCHING BYTECODE FOR INSPECTION ===")
                
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
                                println("Generated files:")
                                generationResult.bytecodeFiles.forEach { file ->
                                    println("  - ${file.absolutePath}")
                                }
                                
                                // Keep the temp directory for manual inspection
                                val permanentDir = File("/tmp/taylor_pattern_bytecode")
                                if (permanentDir.exists()) {
                                    permanentDir.deleteRecursively()
                                }
                                permanentDir.mkdirs()
                                
                                tempDir.listFiles()?.forEach { file ->
                                    file.copyTo(File(permanentDir, file.name), overwrite = true)
                                }
                                
                                println("✓ Bytecode copied to ${permanentDir.absolutePath} for inspection")
                                println("To inspect the bytecode, run:")
                                println("  javap -c -v ${permanentDir.absolutePath}/Program.class")
                                
                                true
                            },
                            onFailure = { error ->
                                println("✗ Bytecode generation FAILED: ${error.message}")
                                error.printStackTrace()
                                false
                            }
                        )
                    },
                    onFailure = { error ->
                        println("✗ Type checking FAILED: ${error.message}")
                        error.printStackTrace()
                        false
                    }
                )
                
            } finally {
                // Don't clean up tempDir immediately to allow inspection
                println("Temp directory: ${tempDir.absolutePath}")
            }
        }
    }
})