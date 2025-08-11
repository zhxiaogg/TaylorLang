import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.codegen.BytecodeGenerator
import org.taylorlang.typechecker.RefactoredTypeChecker
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Manual test to debug while(false) issue
 */
fun main() {
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_while_debug").toFile()
    
    try {
        // Test while(false) 
        println("=== Testing while(false) ===")
        
        val statements = persistentListOf(
            FunctionCall(
                target = Identifier("println"),
                arguments = persistentListOf(Literal.StringLiteral("before"))
            ),
            WhileExpression(
                condition = Literal.BooleanLiteral(false),
                body = FunctionCall(
                    target = Identifier("println"),
                    arguments = persistentListOf(Literal.StringLiteral("loop"))
                )
            ),
            FunctionCall(
                target = Identifier("println"),
                arguments = persistentListOf(Literal.StringLiteral("after"))
            )
        )
        val program = Program(statements)
        
        // Type check
        val typedResult = typeChecker.typeCheck(program)
        if (typedResult.isSuccess) {
            val typedProgram = typedResult.getOrThrow()
            
            // Generate bytecode
            val codegenResult = generator.generateBytecode(typedProgram, tempDir, "WhileFalseDebug")
            if (codegenResult.isSuccess) {
                val generationResult = codegenResult.getOrThrow()
                
                // Execute and capture output
                val result = executeJavaClass(tempDir, "WhileFalseDebug")
                println("Exit code: ${result.exitCode}")
                println("Output: '${result.output}'")
                println("Error: '${result.errorOutput}'")
                
                // Check if we have the bug
                if (result.output.contains("loop")) {
                    println("❌ BUG FOUND: while(false) executed the loop body!")
                } else if (result.output == "before\nafter") {
                    println("✅ CORRECT: while(false) skipped the loop body")
                } else {
                    println("⚠️ UNEXPECTED: Got output: '${result.output}'")
                }
            } else {
                println("Bytecode generation failed: ${codegenResult.exceptionOrNull()}")
            }
        } else {
            println("Type checking failed: ${typedResult.exceptionOrNull()}")
        }
    } finally {
        tempDir.deleteRecursively()
    }
}

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