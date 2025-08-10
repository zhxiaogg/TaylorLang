package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Debug test to understand while loop behavior
 */
class WhileLoopDebugTest : DescribeSpec({
    
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_while_debug").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    describe("While loop debugging") {
        
        it("test while(false) with debug output") {
            // Create: println("before"); while (false) { println("loop") }; println("after")
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
            typedResult shouldBeSuccess { typedProgram ->
                
                // Generate bytecode
                val codegenResult = generator.generateBytecode(typedProgram, tempDir, "WhileDebug")
                codegenResult shouldBeSuccess { generationResult ->
                    
                    val classFile = generationResult.bytecodeFiles.first()
                    
                    // Execute and capture output
                    val result = executeJavaClass(tempDir, "WhileDebug")
                    println("=== While(false) Debug ===")
                    println("Exit code: ${result.exitCode}")
                    println("Output: '${result.output}'")
                    println("Error: '${result.errorOutput}'")
                    println("========================")
                    
                    // Expected: "before\nafter"
                    // If bug exists: "before\nloop\nafter" (loop executes once)
                    result.exitCode shouldBe 0
                    result.output shouldBe "before\nafter"
                }
            }
        }
        
        it("test while(1 > 2) with debug output") {
            // Create: println("start"); while (1 > 2) { println("never") }; println("end")
            val condition = BinaryOp(
                Literal.IntLiteral(1),
                BinaryOperator.GREATER_THAN,
                Literal.IntLiteral(2)
            )
            val statements = persistentListOf(
                FunctionCall(
                    target = Identifier("println"),
                    arguments = persistentListOf(Literal.StringLiteral("start"))
                ),
                WhileExpression(
                    condition = condition,
                    body = FunctionCall(
                        target = Identifier("println"),
                        arguments = persistentListOf(Literal.StringLiteral("never"))
                    )
                ),
                FunctionCall(
                    target = Identifier("println"),
                    arguments = persistentListOf(Literal.StringLiteral("end"))
                )
            )
            val program = Program(statements)
            
            // Type check
            val typedResult = typeChecker.typeCheck(program)
            typedResult shouldBeSuccess { typedProgram ->
                
                // Generate bytecode
                val codegenResult = generator.generateBytecode(typedProgram, tempDir, "WhileCompareDebug")
                codegenResult shouldBeSuccess { generationResult ->
                    
                    val classFile = generationResult.bytecodeFiles.first()
                    
                    // Execute and capture output
                    val result = executeJavaClass(tempDir, "WhileCompareDebug")
                    println("=== While(1 > 2) Debug ===")
                    println("Exit code: ${result.exitCode}")
                    println("Output: '${result.output}'")
                    println("Error: '${result.errorOutput}'")
                    println("==========================")
                    
                    // Expected: "start\nend"
                    // If bug exists: "start\nnever\nend" (loop executes once)
                    result.exitCode shouldBe 0
                    result.output shouldBe "start\nend"
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
    }
}