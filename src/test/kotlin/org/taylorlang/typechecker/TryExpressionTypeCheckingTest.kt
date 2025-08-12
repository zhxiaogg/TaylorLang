package org.taylorlang.typechecker

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.taylorlang.ast.*
import kotlinx.collections.immutable.persistentListOf

/**
 * Comprehensive test suite for Phase 5.3 Try Expression Type Checking.
 * 
 * This test class validates the enhanced try expression type checking implementation:
 * - Bidirectional type checking for try expressions
 * - Enhanced error type compatibility validation
 * - Function context validation improvements
 * - Comprehensive constraint generation
 * - Type inference with fresh type variables
 */
class TryExpressionTypeCheckingTest {

    private lateinit var collector: ConstraintCollector
    private lateinit var context: InferenceContext

    @BeforeEach
    fun setUp() {
        collector = ConstraintCollector()
        context = InferenceContext.withBuiltins()
    }

    // =============================================================================
    // Enhanced Try Expression Context Validation Tests
    // =============================================================================

    @Test
    fun `test try expression context validation with Result return type`() {
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf()
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertNotNull(BuiltinTypes.getResultValueType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
        assertTrue(result.constraints.isNotEmpty())
    }

    @Test
    fun `test try expression context validation without function context`() {
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf()
        )
        
        // No function return type context - should still process but with constraints
        val result = collector.collectConstraints(tryExpr, context)
        
        // Should still create a result type but with constraint violations
        assertNotNull(result.type)
        assertTrue(result.constraints.isNotEmpty())
    }

    @Test
    fun `test try expression with non-Result function return type`() {
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("test"),
            catchClauses = persistentListOf()
        )
        
        // Function returns String instead of Result<T, E>
        val functionContext = context.withFunctionReturnType(BuiltinTypes.STRING)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        // Should still process but generate constraint violations
        assertNotNull(result.type)
        assertTrue(result.constraints.isNotEmpty())
        
        // Should contain constraint violation indicating invalid context
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { it is Constraint.Equality })
    }

    // =============================================================================
    // Bidirectional Type Checking Tests
    // =============================================================================

    @Test
    fun `test bidirectional type checking with explicit expected type`() {
        val tryExpr = TryExpression(
            expression = Literal.FloatLiteral(3.14),
            catchClauses = persistentListOf()
        )
        
        val expectedResultType = BuiltinTypes.createResultType(BuiltinTypes.DOUBLE, BuiltinTypes.THROWABLE)
        val functionContext = context.withFunctionReturnType(expectedResultType)
        
        val result = collector.collectConstraintsWithExpected(tryExpr, expectedResultType, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertNotNull(BuiltinTypes.getResultValueType(result.type))
        
        // Should have constraints that unify the types
        assertTrue(result.constraints.isNotEmpty())
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { it is Constraint.Equality })
    }

    @Test
    fun `test bidirectional type checking with function return type inference`() {
        val tryExpr = TryExpression(
            expression = Literal.BooleanLiteral(true),
            catchClauses = persistentListOf()
        )
        
        val functionReturnType = BuiltinTypes.createResultType(BuiltinTypes.BOOLEAN, Type.PrimitiveType("CustomException"))
        val functionContext = context.withFunctionReturnType(functionReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.BOOLEAN, BuiltinTypes.getResultValueType(result.type))
        
        // Should infer error type from function context
        val errorType = BuiltinTypes.getResultErrorType(result.type)
        assertNotNull(errorType)
    }

    @Test
    fun `test type inference with fresh type variables`() {
        val tryExpr = TryExpression(
            expression = FunctionCall(
                target = Identifier("someFunction"),
                arguments = persistentListOf()
            ),
            catchClauses = persistentListOf()
        )
        
        // No explicit types - should generate fresh type variables
        val result = collector.collectConstraints(tryExpr, context)
        
        assertNotNull(result.type)
        assertTrue(result.constraints.isNotEmpty())
        
        // Should have generated type variables and constraints
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.isNotEmpty())
    }

    // =============================================================================
    // Enhanced Error Type Validation Tests
    // =============================================================================

    @Test
    fun `test error type validation with valid Throwable subtypes`() {
        val catchClause = CatchClause(
            pattern = Pattern.ConstructorPattern(
                constructor = "IllegalArgumentException",
                patterns = persistentListOf()
            ),
            body = Literal.StringLiteral("error handled")
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("success"),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING, BuiltinTypes.THROWABLE)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertTrue(result.constraints.isNotEmpty())
        
        // Should have Throwable subtype constraints
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { constraint ->
            when (constraint) {
                is Constraint.Subtype -> BuiltinTypes.isThrowableSubtype(constraint.subtype)
                else -> false
            }
        })
    }

    @Test
    fun `test error type validation with multiple catch clauses`() {
        val catchClause1 = CatchClause(
            pattern = Pattern.ConstructorPattern("IllegalArgumentException", persistentListOf()),
            body = Literal.IntLiteral(1)
        )
        
        val catchClause2 = CatchClause(
            pattern = Pattern.ConstructorPattern("IllegalStateException", persistentListOf()),
            body = Literal.IntLiteral(2)
        )
        
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf(catchClause1, catchClause2)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should have processed both catch clauses
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.size >= 2) // At least one constraint per catch clause
        
        // Should have unified error types appropriately
        val errorType = BuiltinTypes.getResultErrorType(result.type)
        assertNotNull(errorType)
        assertTrue(BuiltinTypes.isThrowableSubtype(errorType!!))
    }

    @Test
    fun `test error type unification with incompatible types`() {
        // Create catch clauses with different but compatible exception types
        val catchClause1 = CatchClause(
            pattern = Pattern.ConstructorPattern("RuntimeException", persistentListOf()),
            body = Literal.StringLiteral("runtime error")
        )
        
        val catchClause2 = CatchClause(
            pattern = Pattern.ConstructorPattern("IllegalArgumentException", persistentListOf()),
            body = Literal.StringLiteral("arg error")
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("test"),
            catchClauses = persistentListOf(catchClause1, catchClause2)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should unify to most general Throwable type
        val errorType = BuiltinTypes.getResultErrorType(result.type)
        assertNotNull(errorType)
        assertTrue(BuiltinTypes.isThrowableSubtype(errorType!!))
    }

    // =============================================================================
    // Guard Expression Type Checking Tests
    // =============================================================================

    @Test
    fun `test guard expression type checking with boolean constraint`() {
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            guardExpression = BinaryOp(
                left = Literal.IntLiteral(1),
                operator = BinaryOperator.EQUAL,
                right = Literal.IntLiteral(1)
            ),
            body = Literal.StringLiteral("handled")
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("test"),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should have boolean constraint for guard expression
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { constraint ->
            when (constraint) {
                is Constraint.Equality -> constraint.right == BuiltinTypes.BOOLEAN
                else -> false
            }
        })
    }

    @Test
    fun `test guard expression with invalid type`() {
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            guardExpression = Literal.StringLiteral("not boolean"), // Invalid guard type
            body = Literal.IntLiteral(0)
        )
        
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should generate constraint violation for non-boolean guard
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { constraint ->
            when (constraint) {
                is Constraint.Equality -> {
                    constraint.left == BuiltinTypes.STRING && constraint.right == BuiltinTypes.BOOLEAN
                }
                else -> false
            }
        })
    }

    // =============================================================================
    // Catch Body Type Consistency Tests
    // =============================================================================

    @Test
    fun `test catch body type consistency with try expression`() {
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            body = Literal.IntLiteral(100) // Same type as try expression
        )
        
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
        
        // Should have equality constraints for type consistency
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { it is Constraint.Equality })
    }

    @Test
    fun `test catch body type mismatch detection`() {
        val catchClause = CatchClause(
            pattern = Pattern.IdentifierPattern("e"),
            body = Literal.StringLiteral("error") // Different type from try expression
        )
        
        val tryExpr = TryExpression(
            expression = Literal.IntLiteral(42),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should generate constraint violation for type mismatch
        val constraintsList = result.constraints.toList()
        assertTrue(constraintsList.any { constraint ->
            when (constraint) {
                is Constraint.Equality -> {
                    (constraint.left == BuiltinTypes.STRING && constraint.right == BuiltinTypes.INT) ||
                    (constraint.left == BuiltinTypes.INT && constraint.right == BuiltinTypes.STRING)
                }
                else -> false
            }
        })
    }

    // =============================================================================
    // Complex Integration Tests
    // =============================================================================

    @Test
    fun `test nested try expressions with different error types`() {
        val innerTry = TryExpression(
            expression = Literal.IntLiteral(10),
            catchClauses = persistentListOf()
        )
        
        val catchClause = CatchClause(
            pattern = Pattern.ConstructorPattern("OuterException", persistentListOf()),
            body = Literal.IntLiteral(0)
        )
        
        val outerTry = TryExpression(
            expression = innerTry,
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.INT)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(outerTry, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.INT, BuiltinTypes.getResultValueType(result.type))
        
        // Should handle nested constraint generation properly
        assertTrue(result.constraints.isNotEmpty())
        val constraintsList = result.constraints.toList()
        
        // Should have constraints for both inner and outer try expressions
        assertTrue(constraintsList.size >= 2)
    }

    @Test
    fun `test try expression with complex function call target`() {
        val functionCall = FunctionCall(
            target = Identifier("processData"),
            arguments = persistentListOf(
                Literal.StringLiteral("input"),
                Literal.IntLiteral(42)
            )
        )
        
        val catchClause = CatchClause(
            pattern = Pattern.ConstructorPattern("ProcessingException", persistentListOf()),
            body = Literal.StringLiteral("default")
        )
        
        val tryExpr = TryExpression(
            expression = functionCall,
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should handle complex expression constraint generation
        assertTrue(result.constraints.isNotEmpty())
        val constraintsList = result.constraints.toList()
        
        // Should have constraints for function call and try expression
        assertTrue(constraintsList.isNotEmpty())
    }

    // =============================================================================
    // Edge Cases and Error Handling Tests
    // =============================================================================

    @Test
    fun `test try expression with empty catch clauses`() {
        val tryExpr = TryExpression(
            expression = Literal.FloatLiteral(2.718),
            catchClauses = persistentListOf() // No catch clauses
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.DOUBLE)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        assertEquals(BuiltinTypes.DOUBLE, BuiltinTypes.getResultValueType(result.type))
        
        // Should still generate appropriate constraints
        assertNotNull(result.constraints)
    }

    @Test
    fun `test try expression with pattern binding in catch`() {
        val catchClause = CatchClause(
            pattern = Pattern.ConstructorPattern(
                constructor = "ValidationError",
                patterns = persistentListOf(
                    Pattern.IdentifierPattern("field"),
                    Pattern.IdentifierPattern("message")
                )
            ),
            body = BinaryOp(
                left = Identifier("field"),
                operator = BinaryOperator.PLUS, // Use PLUS instead of CONCAT
                right = Identifier("message")
            )
        )
        
        val tryExpr = TryExpression(
            expression = Literal.StringLiteral("validation"),
            catchClauses = persistentListOf(catchClause)
        )
        
        val resultReturnType = BuiltinTypes.createResultType(BuiltinTypes.STRING)
        val functionContext = context.withFunctionReturnType(resultReturnType)
        
        val result = collector.collectConstraints(tryExpr, functionContext)
        
        assertTrue(BuiltinTypes.isResultType(result.type))
        
        // Should handle pattern binding constraints
        assertTrue(result.constraints.isNotEmpty())
        val constraintsList = result.constraints.toList()
        
        // Should have constraints for pattern bindings and expression types
        assertTrue(constraintsList.isNotEmpty())
    }
}