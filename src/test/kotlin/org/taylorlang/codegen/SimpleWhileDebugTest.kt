package org.taylorlang.codegen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.typechecker.RefactoredTypeChecker
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Simple debug test to isolate while loop issue
 */
class SimpleWhileDebugTest : StringSpec({
    
    "debug while(false) execution" {
        val generator = BytecodeGenerator()
        val typeChecker = RefactoredTypeChecker()
        val tempDir = Files.createTempDirectory("simple_while_debug").toFile()
        
        try {
            // Create: while (false) { println("SHOULD NOT PRINT") }
            val program = Program(persistentListOf(
                WhileExpression(
                    condition = Literal.BooleanLiteral(false),
                    body = FunctionCall(
                        target = Identifier("println"),
                        arguments = persistentListOf(Literal.StringLiteral("SHOULD NOT PRINT"))
                    )
                )
            ))
            
            // Type check
            val typedResult = typeChecker.typeCheck(program)
            typedResult.isSuccess shouldBe true
            val typedProgram = typedResult.getOrThrow()
            
            // Generate bytecode
            val codegenResult = generator.generateBytecode(typedProgram, tempDir, "SimpleWhileDebug")
            codegenResult.isSuccess shouldBe true
            val generationResult = codegenResult.getOrThrow()
            
            // Execute and capture output
            val result = executeJavaClass(tempDir, "SimpleWhileDebug")
            println("=== Simple while(false) Debug ===")
            println("Exit code: ${result.exitCode}")
            println("Output: '${result.output}'")
            println("Error: '${result.errorOutput}'")
            println("================================")
            
            // The output should be empty - no "SHOULD NOT PRINT"
            result.exitCode shouldBe 0
            result.output shouldBe ""
        } finally {
            tempDir.deleteRecursively()
        }
    }
})

data class ExecutionResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String
)

private fun executeJavaClass(
    classDir: File,
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