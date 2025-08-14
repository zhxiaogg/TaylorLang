package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Test to verify that the VerifyError for integer boxing in println is fixed.
 */
class VerifyErrorFixTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker()
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("verify_error_fix_test").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    describe("VerifyError fix for primitive boxing") {
        
        it("should fix VerifyError when printing integers directly") {
            val sourceCode = "println(42)"
            
            val parseResult = parser.parse(sourceCode)
            parseResult shouldBeSuccess { program ->
                
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    
                    val codegenResult = generator.generateBytecode(
                        typedProgram, 
                        tempDir, 
                        "VerifyErrorFixTest"
                    )
                    codegenResult shouldBeSuccess { generationResult ->
                        
                        val classFile = generationResult.bytecodeFiles.first()
                        classFile.exists() shouldBe true
                        
                        // CRITICAL TEST: Execute manually to verify no VerifyError
                        val executionResult = executeJavaClass(tempDir, "VerifyErrorFixTest")
                        
                        println("=== Execution Result ===")
                        println("Exit code: ${executionResult.exitCode}")
                        println("Output: '${executionResult.output}'")
                        if (executionResult.errorOutput.isNotEmpty()) {
                            println("Error: '${executionResult.errorOutput}'")
                        }
                        println("=========================")
                        
                        // Should succeed without VerifyError
                        executionResult.exitCode shouldBe 0
                        executionResult.output.trim() shouldBe "42"
                    }
                }
            }
        }
        
        it("should fix VerifyError when printing variables containing integers") {
            val sourceCode = "println(5 + 5)"
            
            val parseResult = parser.parse(sourceCode)
            parseResult shouldBeSuccess { program ->
                
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    
                    val codegenResult = generator.generateBytecode(
                        typedProgram, 
                        tempDir, 
                        "VerifyErrorFixTestLoop"
                    )
                    codegenResult shouldBeSuccess { generationResult ->
                        
                        val classFile = generationResult.bytecodeFiles.first()
                        classFile.exists() shouldBe true
                        
                        // CRITICAL TEST: Execute manually to verify no VerifyError
                        val executionResult = executeJavaClass(tempDir, "VerifyErrorFixTestLoop")
                        
                        println("=== Loop Execution Result ===")
                        println("Exit code: ${executionResult.exitCode}")
                        println("Output: '${executionResult.output}'")
                        if (executionResult.errorOutput.isNotEmpty()) {
                            println("Error: '${executionResult.errorOutput}'")
                        }
                        println("==============================")
                        
                        // Should succeed without VerifyError
                        executionResult.exitCode shouldBe 0
                        executionResult.output.trim() shouldBe "10"
                    }
                }
            }
        }
    }
}) {
    companion object {
        
        data class ExecutionResult(
            val exitCode: Int,
            val output: String,
            val errorOutput: String
        )
        
        private fun executeJavaClass(
            classDir: java.io.File,
            className: String,
            args: List<String> = emptyList()
        ): ExecutionResult {
            val command = listOf("java", "-cp", classDir.absolutePath, className) + args
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(classDir)
            
            val process = processBuilder.start()
            val success = process.waitFor(10, TimeUnit.SECONDS)
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val errorOutput = process.errorStream.bufferedReader().readText().trim()
            val exitCode = if (success) process.exitValue() else -1
            
            return ExecutionResult(exitCode, output, errorOutput)
        }
    }
}