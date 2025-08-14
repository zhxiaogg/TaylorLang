package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Tests for the built-in assert function implementation.
 * 
 * The assert function should:
 * - Accept a boolean condition as parameter
 * - Return Unit (void)
 * - Execute silently when condition is true
 * - Print "Assertion failed" to stderr and exit with code 1 when condition is false
 */
class AssertFunctionTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker()
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("assert_function_test").toFile()
    
    afterSpec {
        // Clean up temp directory
        tempDir.deleteRecursively()
    }
    
    fun compileAndRun(code: String): Companion.ExecutionResult {
        val parseResult = parser.parse(code)
        val program = parseResult.getOrElse { 
            return Companion.ExecutionResult(-1, "", "Parse failed: ${it.message}")
        }
        
        val typedResult = typeChecker.typeCheck(program)
        val typedProgram = typedResult.getOrElse { 
            return Companion.ExecutionResult(-1, "", "Type check failed: ${it.message}")
        }
        
        val codegenResult = generator.generateBytecode(
            typedProgram, 
            tempDir, 
            "AssertTest"
        )
        codegenResult.getOrElse { 
            return Companion.ExecutionResult(-1, "", "Codegen failed: ${it.message}")
        }
        
        return Companion.executeJavaClass(tempDir, "AssertTest")
    }
    
    describe("assert function basic functionality") {
        
        it("should pass with true condition") {
            val code = """
                assert(true)
                println("Test passed")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 0
            result.output shouldBe "Test passed"
        }
        
        it("should fail with false condition") {
            val code = """
                assert(false)
                println("This should not print")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 1
            result.errorOutput shouldBe "Assertion failed"
            result.output shouldBe ""
        }
    }
    
    describe("assert function with expressions") {
        
        it("should pass with true equality expression") {
            val code = """
                assert(5 == 5)
                println("Equality test passed")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 0
            result.output shouldBe "Equality test passed"
        }
        
        it("should pass with arithmetic expression") {
            val code = """
                assert(2 + 2 == 4)
                println("Arithmetic test passed")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 0
            result.output shouldBe "Arithmetic test passed"
        }
        
        it("should fail with false arithmetic expression") {
            val code = """
                assert(2 + 2 == 5)
                println("This should not print")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 1
            result.errorOutput shouldBe "Assertion failed"
            result.output shouldBe ""
        }
    }
    
    describe("multiple assert calls") {
        
        it("should handle multiple passing assertions") {
            val code = """
                assert(true)
                println("First assert passed")
                assert(3 * 3 == 9)
                println("Second assert passed")
                assert(10 > 5)
                println("Third assert passed")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 0
            result.output shouldBe "First assert passed\nSecond assert passed\nThird assert passed"
        }
        
        it("should stop at first failing assertion") {
            val code = """
                assert(true)
                println("First assert passed")
                assert(2 + 2 == 5)
                println("This should not print")
                assert(true)
                println("This should also not print")
            """.trimIndent()
            
            val result = compileAndRun(code)
            result.exitCode shouldBe 1
            result.errorOutput shouldBe "Assertion failed"
            result.output shouldBe "First assert passed"
        }
    }
}) {
    companion object {
        
        data class ExecutionResult(
            val exitCode: Int,
            val output: String,
            val errorOutput: String
        )
        
        fun executeJavaClass(
            classDir: File,
            className: String,
            args: List<String> = emptyList()
        ): ExecutionResult {
            val command = listOf("java", "-cp", classDir.absolutePath, className) + args
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(classDir)
            
            val process = processBuilder.start()
            val success = process.waitFor(5, TimeUnit.SECONDS)
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val errorOutput = process.errorStream.bufferedReader().readText().trim()
            val exitCode = if (success) process.exitValue() else -1
            
            return ExecutionResult(exitCode, output, errorOutput)
        }
    }
}