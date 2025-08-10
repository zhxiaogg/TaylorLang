package org.taylorlang.codegen

import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Debug script to manually test bytecode generation and execution
 */
fun main() {
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_debug").toFile()
    
    println("Debug directory: ${tempDir.absolutePath}")
    
    try {
        // Create simple arithmetic test: println(5 + 3)
        val expr = BinaryOp(
            Literal.IntLiteral(5),
            BinaryOperator.PLUS,
            Literal.IntLiteral(3)
        )
        val printCall = FunctionCall(
            target = Identifier("println"),
            arguments = persistentListOf(expr)
        )
        val program = Program(persistentListOf(printCall))
        
        println("Created program: println(5 + 3)")
        
        // Type check
        val typedResult = typeChecker.typeCheck(program)
        typedResult.fold(
            onSuccess = { typedProgram ->
                println("Type checking successful")
                
                // Generate bytecode
                val codegenResult = generator.generateBytecode(typedProgram, tempDir, "DebugTest")
                codegenResult.fold(
                    onSuccess = { generationResult ->
                        println("Bytecode generation successful")
                        val classFile = generationResult.bytecodeFiles.first()
                        println("Generated class file: ${classFile.absolutePath}")
                        println("File exists: ${classFile.exists()}")
                        println("File size: ${classFile.length()} bytes")
                        
                        // Try to execute
                        println("Attempting to execute...")
                        val result = executeJavaClass(tempDir, "DebugTest")
                        println("Exit code: ${result.exitCode}")
                        println("Output: '${result.output}'")
                        println("Error output: '${result.errorOutput}'")
                        
                        // Inspect bytecode with javap
                        println("\\nBytecode inspection:")
                        val javapResult = inspectBytecode(tempDir, "DebugTest")
                        println("Javap exit code: ${javapResult.exitCode}")
                        println("Javap output:\\n${javapResult.output}")
                        if (javapResult.errorOutput.isNotEmpty()) {
                            println("Javap errors:\\n${javapResult.errorOutput}")
                        }
                    },
                    onFailure = { error ->
                        println("Bytecode generation failed: $error")
                        error.printStackTrace()
                    }
                )
            },
            onFailure = { error ->
                println("Type checking failed: $error")
                error.printStackTrace()
            }
        )
        
    } finally {
        // Keep temp directory for inspection
        println("\\nTemp directory preserved for inspection: ${tempDir.absolutePath}")
    }
}

data class ExecutionResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String
)

private fun executeJavaClass(classDir: File, className: String): ExecutionResult {
    val command = listOf("java", "-cp", classDir.absolutePath, className)
    
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(classDir)
    
    val process = processBuilder.start()
    val success = process.waitFor(10, TimeUnit.SECONDS)
    
    val output = process.inputStream.bufferedReader().readText().trim()
    val errorOutput = process.errorStream.bufferedReader().readText().trim()
    val exitCode = if (success) process.exitValue() else -1
    
    return ExecutionResult(exitCode, output, errorOutput)
}

private fun inspectBytecode(classDir: File, className: String): ExecutionResult {
    val command = listOf("javap", "-v", "-cp", classDir.absolutePath, className)
    
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(classDir)
    
    val process = processBuilder.start()
    val success = process.waitFor(10, TimeUnit.SECONDS)
    
    val output = process.inputStream.bufferedReader().readText()
    val errorOutput = process.errorStream.bufferedReader().readText()
    val exitCode = if (success) process.exitValue() else -1
    
    return ExecutionResult(exitCode, output, errorOutput)
}