package org.taylorlang.codegen.debug

import org.junit.jupiter.api.Test
import org.taylorlang.ast.*
import org.taylorlang.codegen.BytecodeGenerator
import org.taylorlang.typechecker.*
import org.taylorlang.runtime.TaylorResult
import kotlinx.collections.immutable.persistentListOf
import java.io.File

/**
 * Debug test to understand try expression compilation issues
 */
class TryCompilationDebugTest {
    
    @Test
    fun `debug try expression compilation step by step`() {
        println("=== Try Expression Compilation Debug ===")
        
        val tempDir = File.createTempFile("try_debug", "").apply {
            delete()
            mkdir()
        }
        
        try {
            val bytecodeGenerator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            
            // Create a simple try expression: try TaylorResult.ok(42)
            val tryExpr = TryExpression(
                expression = FunctionCall(
                    target = Identifier("TaylorResult.ok"),
                    arguments = persistentListOf(Literal.IntLiteral(42))
                ),
                catchClauses = persistentListOf()
            )
            
            val mainFunction = FunctionDecl(
                name = "main",
                parameters = persistentListOf(
                    Parameter("args", BuiltinTypes.STRING)
                ),
                returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE),
                body = FunctionBody.ExpressionBody(tryExpr)
            )
            
            val program = Program(persistentListOf(mainFunction))
            
            println("1. Program AST created successfully")
            println("Try expression: ${tryExpr}")
            println("Function call target: ${(tryExpr.expression as FunctionCall).target}")
            
            // Type check the program
            println("\n2. Starting type checking...")
            val typedProgramResult = typeChecker.typeCheck(program)
            
            if (typedProgramResult.isFailure) {
                println("Type checking FAILED:")
                println(typedProgramResult.exceptionOrNull()?.message)
                typedProgramResult.exceptionOrNull()?.printStackTrace()
                return
            }
            
            println("Type checking PASSED")
            val typedProgram = typedProgramResult.getOrThrow()
            
            // Generate bytecode
            println("\n3. Starting bytecode generation...")
            val result = bytecodeGenerator.generateBytecode(typedProgram, tempDir, "TryDebug")
            
            if (result.isFailure) {
                println("Bytecode generation FAILED:")
                println(result.exceptionOrNull()?.message)
                result.exceptionOrNull()?.printStackTrace()
                return
            }
            
            println("Bytecode generation PASSED")
            val generationResult = result.getOrThrow()
            println("Generated files: ${generationResult.bytecodeFiles.map { it.name }}")
            
            // Try to load the class
            println("\n4. Testing class loading...")
            val classFile = generationResult.bytecodeFiles[0]
            println("Class file exists: ${classFile.exists()}")
            println("Class file size: ${classFile.length()} bytes")
            
            println("\n=== Debug Complete ===")
            
        } catch (e: Exception) {
            println("EXCEPTION during debug:")
            e.printStackTrace()
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `debug try expression with catch clause compilation`() {
        println("=== Try Expression with Catch Clause Debug ===")
        
        val tempDir = File.createTempFile("try_catch_debug", "").apply {
            delete()
            mkdir()
        }
        
        try {
            val bytecodeGenerator = BytecodeGenerator()
            val typeChecker = RefactoredTypeChecker()
            
            // Create a try expression with catch clause
            val okResult = FunctionCall(
                target = Identifier("TaylorResult.ok"),
                arguments = persistentListOf(Literal.StringLiteral("success value"))
            )
            
            val catchClause = CatchClause(
                pattern = Pattern.IdentifierPattern(
                    name = "e"
                ),
                guardExpression = null,
                body = Literal.StringLiteral("caught error")
            )
            
            val tryExpr = TryExpression(
                expression = okResult,
                catchClauses = persistentListOf(catchClause)
            )
            
            val mainFunction = FunctionDecl(
                name = "main",
                parameters = persistentListOf(
                    Parameter("args", BuiltinTypes.STRING)
                ),
                returnType = BuiltinTypes.createResultType(BuiltinTypes.STRING, BuiltinTypes.THROWABLE),
                body = FunctionBody.ExpressionBody(tryExpr)
            )
            
            val program = Program(persistentListOf(mainFunction))
            
            println("1. Program with catch clause AST created successfully")
            println("Try expression: ${tryExpr}")
            println("Catch clause: ${catchClause}")
            
            // Type check the program
            println("\n2. Starting type checking...")
            val typedProgramResult = typeChecker.typeCheck(program)
            
            if (typedProgramResult.isFailure) {
                println("Type checking FAILED:")
                val exception = typedProgramResult.exceptionOrNull()!!
                println("Exception type: ${exception::class.simpleName}")
                println("Exception message: ${exception.message}")
                
                // Print detailed error information if it's a type error
                if (exception.message?.contains("Multiple errors") == true) {
                    println("This is a MultipleErrors exception - there are several type errors.")
                }
                
                exception.printStackTrace()
                return
            }
            
            println("Type checking PASSED")
            val typedProgram = typedProgramResult.getOrThrow()
            
            // Generate bytecode
            println("\n3. Starting bytecode generation...")
            val result = bytecodeGenerator.generateBytecode(typedProgram, tempDir, "TryCatchDebug")
            
            if (result.isFailure) {
                println("Bytecode generation FAILED:")
                println(result.exceptionOrNull()?.message)
                result.exceptionOrNull()?.printStackTrace()
                return
            }
            
            println("Bytecode generation PASSED")
            val generationResult = result.getOrThrow()
            println("Generated files: ${generationResult.bytecodeFiles.map { it.name }}")
            
            println("\n=== Catch Clause Debug Complete ===")
            
        } catch (e: Exception) {
            println("EXCEPTION during catch clause debug:")
            e.printStackTrace()
        } finally {
            tempDir.deleteRecursively()
        }
    }
}