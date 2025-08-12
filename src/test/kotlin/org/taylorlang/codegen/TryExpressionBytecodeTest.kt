package org.taylorlang.codegen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.taylorlang.ast.*
import org.taylorlang.parser.ASTBuilder
import org.taylorlang.typechecker.*
import org.taylorlang.runtime.TaylorResult
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader

/**
 * Comprehensive test suite for try expression bytecode generation.
 * 
 * Tests the complete pipeline from AST to executable bytecode for try expressions:
 * - Basic try expression compilation
 * - Result type unwrapping
 * - Error propagation patterns
 * - Catch clause handling with pattern matching
 * - Integration with existing bytecode generation infrastructure
 * - End-to-end execution validation
 */
class TryExpressionBytecodeTest {

    private lateinit var bytecodeGenerator: BytecodeGenerator
    private lateinit var typeChecker: RefactoredTypeChecker
    
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        bytecodeGenerator = BytecodeGenerator()
        typeChecker = RefactoredTypeChecker()
    }

    // =============================================================================
    // Basic Try Expression Bytecode Generation Tests
    // =============================================================================

    @Test
    fun `test simple try expression bytecode generation`() {
        // Create a simple try expression: try TaylorResult.ok(42)
        val tryExpr = TryExpression(
            expression = createOkResultExpression(42),
            catchClauses = persistentListOf()
        )
        
        val mainFunction = FunctionDecl(
            name = "main",
            parameters = persistentListOf(),
            returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE),
            body = FunctionBody.ExpressionBody(tryExpr)
        )
        
        val program = Program(persistentListOf(mainFunction))
        
        // Type check the program
        val typedProgramResult = typeChecker.typeCheck(program)
        assertTrue(typedProgramResult.isSuccess, "Program should type check successfully")
        
        val typedProgram = typedProgramResult.getOrThrow()
        
        // Generate bytecode
        val result = bytecodeGenerator.generateBytecode(typedProgram, tempDir, "TryTest")
        assertTrue(result.isSuccess, "Bytecode generation should succeed")
        
        val generationResult = result.getOrThrow()
        assertEquals(1, generationResult.bytecodeFiles.size)
        assertTrue(generationResult.bytecodeFiles[0].exists())
        assertTrue(generationResult.bytecodeFiles[0].name.endsWith(".class"))
    }

    @Test
    fun `test try expression with Result type unwrapping`() {
        // Create a try expression that unwraps a Result<Int, Throwable>
        val okResult = createOkResultExpression(42)
        val tryExpr = TryExpression(
            expression = okResult,
            catchClauses = persistentListOf()
        )
        
        val program = createProgramWithTryExpression(tryExpr, BuiltinTypes.INT)
        val result = compileAndTest(program, "UnwrapTest")
        
        assertTrue(result.isSuccess, "Try expression with Ok result should compile successfully")
    }

    @Test
    fun `test try expression error propagation`() {
        // Create a try expression that propagates an error
        val errorResult = createErrorResultExpression("Test error")
        val tryExpr = TryExpression(
            expression = errorResult,
            catchClauses = persistentListOf()
        )
        
        val program = createProgramWithTryExpression(tryExpr, BuiltinTypes.STRING)
        val result = compileAndTest(program, "ErrorPropagationTest")
        
        assertTrue(result.isSuccess, "Try expression with Error result should compile successfully")
    }

    // =============================================================================
    // Catch Clause Bytecode Generation Tests  
    // =============================================================================

    @Test
    fun `test try expression with simple catch clause`() {
        // Create a try expression with a catch clause
        val errorResult = createErrorResultExpression("Test error")
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern(
                name = "e"
            ),
            guardExpression = null,
            body = Literal.StringLiteral("caught error")
        )
        
        val tryExpr = TryExpression(
            expression = errorResult,
            catchClauses = persistentListOf(catchClause)
        )
        
        val program = createProgramWithTryExpression(tryExpr, BuiltinTypes.STRING)
        val result = compileAndTest(program, "CatchTest")
        
        assertTrue(result.isSuccess, "Try expression with catch clause should compile successfully")
    }

    @Test
    fun `test try expression with multiple catch clauses`() {
        // Create a try expression with multiple catch clauses
        val errorResult = createErrorResultExpression("Test error")
        
        val catchClause1 = CatchClause(
            pattern = Pattern.IdentifierPattern(
                name = "iae"
            ),
            guardExpression = null,
            body = Literal.StringLiteral("illegal argument")
        )
        
        val catchClause2 = CatchClause(
            pattern = Pattern.IdentifierPattern(
                name = "e"
            ),
            guardExpression = null,
            body = Literal.StringLiteral("general error")
        )
        
        val tryExpr = TryExpression(
            expression = errorResult,
            catchClauses = persistentListOf(catchClause1, catchClause2)
        )
        
        val program = createProgramWithTryExpression(tryExpr, BuiltinTypes.STRING)
        val result = compileAndTest(program, "MultipleCatchTest")
        
        assertTrue(result.isSuccess, "Try expression with multiple catch clauses should compile successfully")
    }

    // =============================================================================
    // End-to-End Execution Tests
    // =============================================================================

    @Test
    fun `test try expression execution with Ok result`() {
        // Create a function that returns Ok(42)
        val okFunction = FunctionDecl(
            name = "getOkValue",
            parameters = persistentListOf(),
            returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE),
            body = FunctionBody.ExpressionBody(createOkResultExpression(42))
        )
        
        // Create main function that uses try expression
        val tryExpr = TryExpression(
            expression = FunctionCall(
                target = Identifier("getOkValue"),
                arguments = persistentListOf()
            ),
            catchClauses = persistentListOf()
        )
        
        val mainFunction = FunctionDecl(
            name = "main",
            parameters = persistentListOf(
                Parameter("args", BuiltinTypes.STRING)
            ),
            returnType = BuiltinTypes.UNIT,
            body = FunctionBody.ExpressionBody(tryExpr)
        )
        
        val program = Program(persistentListOf(okFunction, mainFunction))
        
        // Compile and execute
        val executionResult = compileAndExecute(program, "OkExecutionTest")
        assertTrue(executionResult.isSuccess, "Try expression with Ok should execute successfully")
    }

    // =============================================================================
    // Integration and Regression Tests
    // =============================================================================

    @Test
    fun `test try expression integration with existing features`() {
        // Test try expressions work with other language features
        val complexTryExpr = TryExpression(
            expression = BinaryOp(
                left = TryExpression(
                    expression = createOkResultExpression(20),
                    catchClauses = persistentListOf()
                ),
                operator = BinaryOperator.PLUS,
                right = TryExpression(
                    expression = createOkResultExpression(22),
                    catchClauses = persistentListOf()
                )
            ),
            catchClauses = persistentListOf()
        )
        
        val program = createProgramWithTryExpression(complexTryExpr, BuiltinTypes.INT)
        val result = compileAndTest(program, "IntegrationTest")
        
        assertTrue(result.isSuccess, "Complex try expression should compile successfully")
    }

    @Test
    fun `test try expression bytecode verification`() {
        // Ensure generated bytecode passes JVM verification
        val tryExpr = TryExpression(
            expression = createOkResultExpression(100),
            catchClauses = persistentListOf()
        )
        
        val program = createProgramWithTryExpression(tryExpr, BuiltinTypes.INT)
        val result = compileAndTest(program, "VerificationTest")
        
        assertTrue(result.isSuccess, "Generated bytecode should pass JVM verification")
        
        // Load the class to trigger verification
        val classFile = result.getOrThrow().bytecodeFiles[0]
        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
        
        assertDoesNotThrow {
            classLoader.loadClass("VerificationTest")
        }
    }

    // =============================================================================
    // Helper Methods
    // =============================================================================

    /**
     * Create a Result.Ok expression with the given value.
     */
    private fun createOkResultExpression(value: Any): Expression {
        return FunctionCall(
            target = Identifier("TaylorResult.ok"),
            arguments = persistentListOf(
                when (value) {
                    is Int -> Literal.IntLiteral(value)
                    is String -> Literal.StringLiteral(value)
                    is Boolean -> Literal.BooleanLiteral(value)
                    is Double -> Literal.FloatLiteral(value)
                    else -> Literal.IntLiteral(0)
                }
            )
        )
    }

    /**
     * Create a Result.Error expression with the given error message.
     */
    private fun createErrorResultExpression(message: String): Expression {
        return FunctionCall(
            target = Identifier("TaylorResult.error"),
            arguments = persistentListOf(
                FunctionCall(
                    target = Identifier("RuntimeException"),
                    arguments = persistentListOf(Literal.StringLiteral(message))
                )
            )
        )
    }

    /**
     * Create a program with a try expression in the main function.
     */
    private fun createProgramWithTryExpression(tryExpr: TryExpression, returnType: Type): Program {
        val mainFunction = FunctionDecl(
            name = "main",
            parameters = persistentListOf(
                Parameter("args", BuiltinTypes.STRING)
            ),
            returnType = BuiltinTypes.createResultType(returnType, BuiltinTypes.THROWABLE),
            body = FunctionBody.ExpressionBody(tryExpr)
        )
        
        return Program(persistentListOf(mainFunction))
    }

    /**
     * Compile a program and return the generation result.
     */
    private fun compileAndTest(program: Program, className: String): Result<GenerationResult> {
        val typedProgramResult = typeChecker.typeCheck(program)
        if (typedProgramResult.isFailure) {
            return Result.failure(Exception("Type checking failed: ${typedProgramResult.exceptionOrNull()}"))
        }
        
        val typedProgram = typedProgramResult.getOrThrow()
        return bytecodeGenerator.generateBytecode(typedProgram, tempDir, className)
    }

    /**
     * Compile and execute a program, returning the execution result.
     */
    private fun compileAndExecute(program: Program, className: String): Result<Any?> {
        return try {
            val compileResult = compileAndTest(program, className)
            if (compileResult.isFailure) {
                return Result.failure(compileResult.exceptionOrNull()!!)
            }
            
            // Load and execute the compiled class
            val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
            val clazz = classLoader.loadClass(className)
            val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
            
            val result = mainMethod.invoke(null, arrayOf<String>())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate that a class file contains expected bytecode patterns.
     */
    private fun validateBytecode(classFile: File, expectedPatterns: List<String>): Boolean {
        // This would use ASM to read and validate the bytecode
        // For now, just check that the file exists and has reasonable size
        return classFile.exists() && classFile.length() > 100
    }
}