package org.taylorlang.typechecker

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.taylorlang.ast.*
import kotlinx.collections.immutable.persistentListOf

/**
 * Comprehensive test suite for Result<T, E> type system integration.
 * 
 * This test class validates:
 * - Result type definition and validation
 * - Throwable constraint checking
 * - Try expression type checking 
 * - Function return type validation for try expressions
 * - Type inference with Result types
 * - Error handling and validation
 */
class ResultTypeSystemTest {

    private lateinit var collector: ConstraintCollector
    private lateinit var context: InferenceContext

    @BeforeEach
    fun setUp() {
        collector = ConstraintCollector()
        context = InferenceContext.withBuiltins()
    }

    // =============================================================================
    // Result Type Definition Tests
    // =============================================================================

    @Test
    fun `test Result type creation with valid types`() {
        val resultType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE)
        
        assertTrue(BuiltinTypes.isResultType(resultType))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(resultType))
        assertEquals(BuiltinTypes.THROWABLE, BuiltinTypes.getResultErrorType(resultType))
    }

    @Test
    fun `test Result type creation with default error type`() {
        val resultType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        
        assertTrue(BuiltinTypes.isResultType(resultType))
        assertEquals(BuiltinTypes.STRING, BuiltinTypes.getResultValueType(resultType))
        assertEquals(BuiltinTypes.THROWABLE, BuiltinTypes.getResultErrorType(resultType))
    }

    @Test
    fun `test isResultType with non-Result types`() {
        assertFalse(BuiltinTypes.isResultType(BuiltinTypes.INT))
        assertFalse(BuiltinTypes.isResultType(BuiltinTypes.STRING))
        
        val genericType = Type.GenericType("List", persistentListOf(BuiltinTypes.INT))
        assertFalse(BuiltinTypes.isResultType(genericType))
    }

    // =============================================================================
    // Throwable Constraint Tests
    // =============================================================================

    @Test
    fun `test Throwable subtype validation with valid types`() {
        assertTrue(BuiltinTypes.isThrowableSubtype(BuiltinTypes.THROWABLE))
        assertTrue(BuiltinTypes.isThrowableSubtype(Type.PrimitiveType("Exception")))
        assertTrue(BuiltinTypes.isThrowableSubtype(Type.PrimitiveType("RuntimeException")))
        assertTrue(BuiltinTypes.isThrowableSubtype(Type.PrimitiveType("IllegalArgumentException")))
    }

    @Test
    fun `test Throwable subtype validation with invalid types`() {
        assertFalse(BuiltinTypes.isThrowableSubtype(BuiltinTypes.INT))
        assertFalse(BuiltinTypes.isThrowableSubtype(BuiltinTypes.STRING))
        assertFalse(BuiltinTypes.isThrowableSubtype(BuiltinTypes.BOOLEAN))
    }

    @Test
    fun `test user-defined exception types`() {
        val customException = Type.NamedType("CustomException")
        assertTrue(BuiltinTypes.isThrowableSubtype(customException))
        
        val customError = Type.NamedType("ValidationError")
        assertTrue(BuiltinTypes.isThrowableSubtype(customError))
        
        val nonException = Type.NamedType("CustomType")
        assertFalse(BuiltinTypes.isThrowableSubtype(nonException))
    }

    @Test
    fun `test Result type error validation`() {
        val validResult = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE)
        assertTrue(BuiltinTypes.validateResultErrorType(validResult))
        
        val invalidResult = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.STRING)
        assertFalse(BuiltinTypes.validateResultErrorType(invalidResult))
    }

    // =============================================================================
    // Type Validation Tests
    // =============================================================================

    @Test
    fun `test Result type validation in TypeValidation`() {
        val validResult = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE)
        val validationResult = TypeValidation.validate(validResult)
        assertTrue(validationResult.isValid)
        assertTrue(validationResult.errors.isEmpty())
    }

    @Test
    fun `test Result type validation with invalid error type`() {
        val invalidResult = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.STRING)
        val validationResult = TypeValidation.validate(invalidResult)
        assertFalse(validationResult.isValid)
        assertTrue(validationResult.errors.any { it is TypeValidation.ValidationError.ResultErrorTypeViolation })
    }

    // =============================================================================
    // Try Expression Type Checking Tests
    // =============================================================================

    @Test
    fun `test try expression in Result-returning function`() {
        // Create a simple try expression: try 42
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf()
        )
        
        // Create function context with Result<Int, Throwable> return type
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        // Collect constraints
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        // Should create a Result type
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
    }

    @Test
    fun `test try expression with catch clause`() {
        // Create try expression with catch clause: try 42 catch (e: Exception) 0
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            body = Literal.IntLiteral(0)
        )
        
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        // Should create a Result type with proper constraints
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertFalse(result.constraints.isEmpty())
    }

    @Test
    fun `test try expression with specific exception catch`() {
        // Create try expression with specific exception catch
        val catchClause = CatchClause(
            pattern = Pattern.ConstructorPattern(
                constructor = "IllegalArgumentException",
                patterns = persistentListOf(Pattern.IdentifierPattern("msg"))
            ),
            body = Literal.StringLiteral("Error occurred")
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("success"),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        // Should have Throwable subtype constraints
        assertTrue(result.constraints.toList().any { it is Constraint.Subtype })
    }

    // =============================================================================
    // Function Declaration Tests
    // =============================================================================

    @Test
    fun `test function declaration with Result return type`() {
        val functionDecl = FunctionDecl(
            name = "testFunction",
            parameters = persistentListOf(),
            returnType = BuiltinTypes.createResultType(BuiltinTypes.INT),
            body = FunctionBody.ExpressionBody(
                TryExpression(
                    expression = Literal.IntLiteral(42),
                    catchClauses = persistentListOf()
                )
            )
        )
        
        val result = collector.typeCheckFunctionDeclaration(functionDecl, context)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
    }

    @Test
    fun `test function declaration with inferred Result return type`() {
        val functionDecl = FunctionDecl(
            name = "inferredFunction",
            parameters = persistentListOf(),
            returnType = null, // No explicit return type
            body = FunctionBody.ExpressionBody(
                TryExpression(
                    expression = Literal.StringLiteral("test"),
                    catchClauses = persistentListOf()
                )
            )
        )
        
        val result = collector.typeCheckFunctionDeclaration(functionDecl, context)
        
        // Should infer a type variable initially
        // In a full implementation, this would be unified with the try expression result
        assertNotNull(result.type)
        assertFalse(result.constraints.isEmpty())
    }

    // =============================================================================
    // Complex Scenarios Tests
    // =============================================================================

    @Test
    fun `test nested try expressions`() {
        // Create nested try expression: try (try 42) catch (e) 0
        val innerTry = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf()
        )
        
        val outerTry = TryExpression(
            expression = innerTry,
            catchClauses = persistentListOf(
                CatchClause(
                    pattern = Pattern.IdentifierPattern("e"),
                    body = Literal.IntLiteral(0)
                )
            )
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(outerTry, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        // Should handle nested constraints properly
        assertFalse(result.constraints.isEmpty())
    }

    @Test
    fun `test try expression with guard clause`() {
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            guardExpression = Literal.BooleanLiteral(true),
            body = Literal.StringLiteral("handled")
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("success"),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        // Should have constraints for guard expression (Boolean type)
        assertTrue(result.constraints.toList().any { 
            it is Constraint.Equality && it.right == BuiltinTypes.BOOLEAN 
        })
    }

    // =============================================================================
    // Error Handling Tests
    // =============================================================================

    @Test
    fun `test type validation error reporting`() {
        val invalidResult = Type.GenericType(
            "Result",
            persistentListOf(BuiltinTypes.INT, BuiltinTypes.STRING) // Invalid error type
        )
        
        val validationResult = TypeValidation.validate(invalidResult)
        
        assertFalse(validationResult.isValid)
        val error = validationResult.errors.firstOrNull()
        assertNotNull(error)
        assertTrue(error is TypeValidation.ValidationError.ResultErrorTypeViolation)
        
        if (error is TypeValidation.ValidationError.ResultErrorTypeViolation) {
            assertEquals(BuiltinTypes.STRING, error.errorType)
            assertTrue(error.message.contains("Throwable"))
        }
    }

    @Test
    fun `test Result type utility functions edge cases`() {
        // Test with non-Result type
        assertNull(BuiltinTypes.getResultValueType(BuiltinTypes.INT))
        assertNull(BuiltinTypes.getResultErrorType(BuiltinTypes.STRING))
        
        // Test with invalid Result type (wrong arity)
        val invalidResult = Type.GenericType("Result", persistentListOf(BuiltinTypes.INT))
        assertFalse(BuiltinTypes.isResultType(invalidResult))
        assertNull(BuiltinTypes.getResultValueType(invalidResult))
        assertNull(BuiltinTypes.getResultErrorType(invalidResult))
    }

    // =============================================================================
    // Integration Tests
    // =============================================================================

    @Test
    fun `test end-to-end Result type workflow`() {
        // Create a complete function with Result return type and try expression
        val functionDecl = FunctionDecl(
            name = "processValue",
            parameters = persistentListOf(
                Parameter("input", BuiltinTypes.STRING)
            ),
            returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, Type.PrimitiveType("ParseException")),
            body = FunctionBody.ExpressionBody(
                TryExpression(
                    expression = FunctionCall(
                        target = Identifier("parseInt"),
                        arguments = persistentListOf(Identifier("input"))
                    ),
                    catchClauses = persistentListOf(
                        CatchClause(
                            pattern = Pattern.ConstructorPattern("ParseException", persistentListOf()),
                            body = Literal.IntLiteral(-1)
                        )
                    )
                )
            )
        )
        
        val result = collector.typeCheckFunctionDeclaration(functionDecl, context)
        
        // Verify the Result type is properly constructed
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
        
        // Verify constraints are generated
        assertFalse(result.constraints.isEmpty())
        
        // Verify Throwable constraints exist
        assertTrue(result.constraints.toList().any { it is Constraint.Subtype })
    }
}