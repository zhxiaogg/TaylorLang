package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Debug test to investigate runtime execution failures
 */
class DebugRuntimeTest : DescribeSpec({
    
    describe("Debug runtime execution failures") {
        
        it("should debug simplest case step by step") {
            val generator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            val tempDir = Files.createTempDirectory("taylor_debug").toFile()
            
            try {
                println("\\nDEBUG: Starting simple test")
                println("Debug directory: ${tempDir.absolutePath}")
                
                // Create program with string literal: println("Hello")
                val printCall = FunctionCall(
                    target = Identifier("println"),
                    arguments = persistentListOf(Literal.StringLiteral("Hello"))
                )
                val program = Program(persistentListOf(printCall))
                
                println("DEBUG: Created program: println(\"Hello\")")
                
                // Type check
                val typedResult = typeChecker.typeCheck(program)
                if (typedResult.isSuccess) {
                    val typedProgram = typedResult.getOrThrow()
                    println("DEBUG: Type checking successful")
                    
                    // Generate bytecode
                    val codegenResult = generator.generateBytecode(typedProgram, tempDir, "SimpleTest")
                    if (codegenResult.isSuccess) {
                        val generationResult = codegenResult.getOrThrow()
                        val classFile = generationResult.bytecodeFiles.first()
                        
                        println("DEBUG: Bytecode generation successful")
                        println("  - Class file: ${classFile.absolutePath}")
                        println("  - File exists: ${classFile.exists()}")
                        println("  - File size: ${classFile.length()} bytes")
                        
                        // Inspect with javap first
                        val javapResult = runJavap(tempDir, "SimpleTest")
                        println("DEBUG: Javap inspection:")
                        println("  - Exit code: ${javapResult.exitCode}")
                        if (javapResult.output.isNotEmpty()) {
                            println("  - Bytecode structure:")
                            javapResult.output.lines().take(20).forEach { line ->
                                println("    $line")
                            }
                        }
                        if (javapResult.errorOutput.isNotEmpty()) {
                            println("  - Javap errors: ${javapResult.errorOutput}")
                        }
                        
                        // Try to execute
                        println("DEBUG: Attempting execution...")
                        val execResult = runJavaClass(tempDir, "SimpleTest")
                        println("  - Exit code: ${execResult.exitCode}")
                        println("  - Output: '${execResult.output}'")
                        println("  - Error output: '${execResult.errorOutput}'")
                        
                        // Try with verbose output
                        println("DEBUG: Attempting execution with verbose output...")
                        val verboseResult = runJavaClassVerbose(tempDir, "SimpleTest")
                        println("  - Verbose exit code: ${verboseResult.exitCode}")
                        println("  - Verbose output: '${verboseResult.output}'")
                        println("  - Verbose error output: '${verboseResult.errorOutput}'")
                                
                    } else {
                        val error = codegenResult.exceptionOrNull()
                        println("DEBUG: Bytecode generation failed: ${error?.message}")
                        error?.printStackTrace()
                    }
                } else {
                    val error = typedResult.exceptionOrNull()
                    println("DEBUG: Type checking failed: ${error?.message}")
                    error?.printStackTrace()
                }
            } finally {
                println("DEBUG: Temp directory preserved: ${tempDir.absolutePath}")
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
        
        private fun runJavaClass(classDir: File, className: String): ExecutionResult {
            val command = listOf("java", "-cp", classDir.absolutePath, className)
            return runCommand(command, classDir)
        }
        
        private fun runJavaClassVerbose(classDir: File, className: String): ExecutionResult {
            val command = listOf("java", "-verbose:class", "-cp", classDir.absolutePath, className)
            return runCommand(command, classDir)
        }
        
        private fun runJavap(classDir: File, className: String): ExecutionResult {
            val command = listOf("javap", "-v", "-cp", classDir.absolutePath, className)
            return runCommand(command, classDir)
        }
        
        private fun runCommand(command: List<String>, workingDir: File): ExecutionResult {
            return try {
                val processBuilder = ProcessBuilder(command)
                processBuilder.directory(workingDir)
                
                val process = processBuilder.start()
                val success = process.waitFor(10, TimeUnit.SECONDS)
                
                val output = process.inputStream.bufferedReader().readText().trim()
                val errorOutput = process.errorStream.bufferedReader().readText().trim()
                val exitCode = if (success) process.exitValue() else -1
                
                ExecutionResult(exitCode, output, errorOutput)
            } catch (e: Exception) {
                ExecutionResult(-1, "", "Exception: ${e.message}")
            }
        }
    }
}