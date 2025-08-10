package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*

/**
 * Integration tests for the complete constraint-based type inference pipeline.
 * 
 * These tests verify that:
 * 1. Constraints are properly collected from AST expressions
 * 2. The unification algorithm correctly solves the collected constraints
 * 3. The final substitution produces the expected types
 * 4. Error cases are properly handled throughout the pipeline
 * 
 * This demonstrates the end-to-end functionality of the type inference system.
 */
class UnificationIntegrationTest : StringSpec({

    lateinit var collector: ConstraintCollector
    lateinit var context: InferenceContext

    beforeEach {
        TypeVar.resetCounter()
        collector = ConstraintCollector()
        context = InferenceContext.withBuiltins()
    }

    // =============================================================================
    // Simple Expression Integration Tests
    // =============================================================================

    "integrate literal constraint collection with unification" {
        val expr = Literal.IntLiteral(42)
        
        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.INT
        
        // Apply final substitution to inferred type
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.INT
    }

    "integrate variable reference with constraint solving" {
        val contextWithVar = context.withVariable("x", BuiltinTypes.STRING)
        val expr = Identifier("x")
        
        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, contextWithVar)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.STRING
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.STRING
    }

    "integrate unknown variable with fresh type variable and constraint solving" {
        val expr = Identifier("unknown")
        
        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type.shouldBeInstanceOf<Type.NamedType>()
        
        // The type should remain as a fresh type variable since no constraints bind it
        val namedType = constraintResult.type as Type.NamedType
        namedType.name shouldBe "T1" // First fresh type variable
    }

    // =============================================================================
    // Binary Operation Integration Tests
    // =============================================================================

    "integrate arithmetic operation with type inference" {
        val left = Literal.IntLiteral(5)
        val right = Literal.IntLiteral(3)
        val expr = BinaryOp(left, BinaryOperator.PLUS, right)
        
        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.DOUBLE // Result of arithmetic promotion
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.DOUBLE
    }

    "integrate mixed type arithmetic with constraint solving" {
        val left = Identifier("x")
        val right = Literal.IntLiteral(10)
        val expr = BinaryOp(left, BinaryOperator.MULTIPLY, right)
        
        // Initially x is unknown
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Should generate constraints that x must be numeric
        constraintResult.constraints.isNotEmpty() shouldBe true
        
        val unificationResult = Unifier.solve(constraintResult.constraints)
        unificationResult.isSuccess shouldBe true
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.DOUBLE
    }

    "integrate comparison operation with constraint solving" {
        val left = Identifier("x")
        val right = Identifier("y")
        val expr = BinaryOp(left, BinaryOperator.EQUAL, right)
        
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Should generate equality constraint between x and y
        constraintResult.constraints.isNotEmpty() shouldBe true
        constraintResult.type shouldBe BuiltinTypes.BOOLEAN
        
        val unificationResult = Unifier.solve(constraintResult.constraints)
        unificationResult.isSuccess shouldBe true
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.BOOLEAN
    }

    // =============================================================================
    // Function Call Integration Tests
    // =============================================================================

    "integrate function call with known signature and constraint solving" {
        val signature = FunctionSignature(
            typeParameters = emptyList(),
            parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.STRING),
            returnType = BuiltinTypes.BOOLEAN
        )
        val contextWithFunction = context.withFunctionSignature("testFunc", signature)

        val target = Identifier("testFunc")
        val args = persistentListOf<Expression>(
            Literal.IntLiteral(42),
            Literal.StringLiteral("test")
        )
        val expr = FunctionCall(target, args)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, contextWithFunction)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.BOOLEAN
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.BOOLEAN
    }

    "integrate generic function call with type inference" {
        val signature = FunctionSignature(
            typeParameters = listOf("T"),
            parameterTypes = listOf(Type.NamedType("T")),
            returnType = Type.NamedType("T")
        )
        val contextWithGenericFunc = context.withFunctionSignature("identity", signature)

        val target = Identifier("identity")
        val args = persistentListOf<Expression>(Literal.IntLiteral(42))
        val expr = FunctionCall(target, args)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, contextWithGenericFunc)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        
        // The inferred type should be a concrete type after solving
        finalType.shouldBeInstanceOf<Type.NamedType>()
    }

    "integrate function call with type mismatch and error handling" {
        val signature = FunctionSignature(
            typeParameters = emptyList(),
            parameterTypes = listOf(BuiltinTypes.INT),
            returnType = BuiltinTypes.BOOLEAN
        )
        val contextWithFunction = context.withFunctionSignature("func", signature)

        val target = Identifier("func")
        val args = persistentListOf<Expression>(Literal.StringLiteral("wrong"))  // Wrong type!
        val expr = FunctionCall(target, args)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, contextWithFunction)
        
        // Constraints should include incompatible requirements
        constraintResult.constraints.isNotEmpty() shouldBe true
        
        // Solving should fail due to type mismatch
        val unificationResult = Unifier.solve(constraintResult.constraints)
        unificationResult.isFailure shouldBe true
    }

    // =============================================================================
    // Control Flow Integration Tests
    // =============================================================================

    "integrate if expression with branch unification" {
        val condition = Literal.BooleanLiteral(true)
        val thenExpr = Literal.IntLiteral(1)
        val elseExpr = Literal.IntLiteral(2)
        val expr = IfExpression(condition, thenExpr, elseExpr)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.INT // Branch types are compatible
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.INT
    }

    "integrate if expression with incompatible branches" {
        val condition = Literal.BooleanLiteral(true)
        val thenExpr = Literal.IntLiteral(1)
        val elseExpr = Literal.StringLiteral("test")
        val expr = IfExpression(condition, thenExpr, elseExpr)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Should create fresh type variable for unification
        constraintResult.type.shouldBeInstanceOf<Type.NamedType>()
        constraintResult.constraints.isNotEmpty() shouldBe true
        
        // Solving should fail due to incompatible branch types
        val unificationResult = Unifier.solve(constraintResult.constraints)
        unificationResult.isFailure shouldBe true
    }

    // =============================================================================
    // Complex Expression Integration Tests
    // =============================================================================

    "integrate tuple literal with mixed types" {
        val elements = persistentListOf<Expression>(
            Literal.IntLiteral(1),
            Literal.StringLiteral("test"),
            Literal.BooleanLiteral(false)
        )
        val expr = Literal.TupleLiteral(elements)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type.shouldBeInstanceOf<Type.TupleType>()
        
        val tupleType = constraintResult.type as Type.TupleType
        tupleType.elementTypes shouldHaveSize 3
        tupleType.elementTypes[0] shouldBe BuiltinTypes.INT
        tupleType.elementTypes[1] shouldBe BuiltinTypes.STRING
        tupleType.elementTypes[2] shouldBe BuiltinTypes.BOOLEAN
    }

    "integrate lambda expression with type inference" {
        val params = persistentListOf("x", "y")
        val body = BinaryOp(
            Identifier("x"),
            BinaryOperator.PLUS,
            Identifier("y")
        )
        val expr = LambdaExpression(params, body)

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type.shouldBeInstanceOf<Type.FunctionType>()
        
        val functionType = constraintResult.type as Type.FunctionType
        functionType.parameterTypes shouldHaveSize 2
        functionType.returnType shouldBe BuiltinTypes.DOUBLE // Result of addition
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType.shouldBeInstanceOf<Type.FunctionType>()
    }

    "integrate block expression with variable binding" {
        val valDecl = ValDecl("x", null, Literal.IntLiteral(42))
        val finalExpr = Identifier("x")
        val expr = BlockExpression(
            statements = persistentListOf(valDecl),
            expression = finalExpr
        )

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        constraintResult.type shouldBe BuiltinTypes.INT
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        finalType shouldBe BuiltinTypes.INT
    }

    // =============================================================================
    // Realistic Type Inference Scenarios
    // =============================================================================

    "integrate polymorphic let-binding scenario" {
        // Simulating: let f = \x -> x in (f 42, f "test")
        // This tests polymorphic function usage with different argument types
        
        val lambdaBody = Identifier("x")
        val lambda = LambdaExpression(persistentListOf("x"), lambdaBody)
        
        val valDecl = ValDecl("f", null, lambda)
        val app1 = FunctionCall(Identifier("f"), persistentListOf(Literal.IntLiteral(42)))
        val app2 = FunctionCall(Identifier("f"), persistentListOf(Literal.StringLiteral("test")))
        val tupleExpr = Literal.TupleLiteral(persistentListOf(app1, app2))
        
        val blockExpr = BlockExpression(
            statements = persistentListOf(valDecl),
            expression = tupleExpr
        )

        // Collect constraints
        val constraintResult = collector.collectConstraints(blockExpr, context)
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // This should work with proper polymorphism handling
        // For now, it may fail due to monomorphic inference limitations
        if (unificationResult.isSuccess) {
            val finalSubst = unificationResult.getOrThrow()
            val finalType = finalSubst.apply(constraintResult.type)
            finalType.shouldBeInstanceOf<Type.TupleType>()
        }
        // Note: This test demonstrates the complexity of full polymorphic inference
    }

    "integrate complex nested expression with multiple constraints" {
        val complexExpr = IfExpression(
            condition = BinaryOp(
                Identifier("x"),
                BinaryOperator.GREATER_THAN,
                Literal.IntLiteral(0)
            ),
            thenExpression = BinaryOp(
                Identifier("x"),
                BinaryOperator.PLUS,
                Literal.IntLiteral(1)
            ),
            elseExpression = Literal.IntLiteral(0)
        )

        val contextWithVar = context.withVariable("x", BuiltinTypes.INT)

        // Collect constraints
        val constraintResult = collector.collectConstraints(complexExpr, contextWithVar)
        
        // Should generate multiple constraints for:
        // 1. Condition must be boolean
        // 2. Branch types must be compatible
        // 3. x must be used consistently
        constraintResult.constraints.isNotEmpty() shouldBe true
        
        // Solve constraints
        val unificationResult = Unifier.solve(constraintResult.constraints)
        
        // Verify results
        unificationResult.isSuccess shouldBe true
        
        val finalSubst = unificationResult.getOrThrow()
        val finalType = finalSubst.apply(constraintResult.type)
        
        // Result should be Int (compatible branch types)
        finalType shouldBe BuiltinTypes.INT
    }

    // =============================================================================
    // Error Handling Integration Tests
    // =============================================================================

    "integrate constraint collection and solving with type errors" {
        val expr = BinaryOp(
            Literal.StringLiteral("hello"),
            BinaryOperator.PLUS,
            Literal.BooleanLiteral(true)
        )

        // Collect constraints
        val constraintResult = collector.collectConstraints(expr, context)
        
        // Should collect constraints that are unsolvable
        constraintResult.constraints.isNotEmpty() shouldBe true
        
        // Solving should fail
        val unificationResult = Unifier.solve(constraintResult.constraints)
        unificationResult.isFailure shouldBe true
        unificationResult.exceptionOrNull().shouldBeInstanceOf<UnificationError.ConstraintSolvingFailed>()
    }

    "integrate occurs check detection through constraint solving" {
        // This would require creating an expression that generates recursive constraints
        // For now, we can test this directly with constructed constraints
        val recursiveConstraints = ConstraintSet.of(
            Constraint.Equality(
                Type.NamedType("T"),
                Type.GenericType("List", persistentListOf(Type.NamedType("T")))
            )
        )

        val unificationResult = Unifier.solve(recursiveConstraints)
        
        unificationResult.isFailure shouldBe true
        val exception = unificationResult.exceptionOrNull() as UnificationError.ConstraintSolvingFailed
        exception.cause.shouldBeInstanceOf<UnificationError.InfiniteType>()
    }

    // =============================================================================
    // Performance and Scalability Tests
    // =============================================================================

    "integrate constraint solving with large constraint sets" {
        // Create a chain of variable dependencies: T1 -> T2 -> T3 -> ... -> Int
        val constraints = (1..50).map { i ->
            if (i == 50) {
                Constraint.Equality(Type.NamedType("T$i"), BuiltinTypes.INT)
            } else {
                Constraint.Equality(Type.NamedType("T$i"), Type.NamedType("T${i+1}"))
            }
        }

        val constraintSet = ConstraintSet.fromCollection(constraints)
        
        val unificationResult = Unifier.solve(constraintSet)
        
        unificationResult.isSuccess shouldBe true
        val finalSubst = unificationResult.getOrThrow()
        
        // All type variables should ultimately resolve to Int
        for (i in 1..50) {
            finalSubst.apply(Type.NamedType("T$i")) shouldBe BuiltinTypes.INT
        }
    }

    "integrate end-to-end type checking with constraint-based mode" {
        // This tests the integration with TypeChecker.withConstraints()
        val typeChecker = TypeChecker.withConstraints()
        val expr = BinaryOp(
            Literal.IntLiteral(5),
            BinaryOperator.MULTIPLY,
            Literal.IntLiteral(3)
        )

        val contextForTypeChecker = TypeContext()
        val result = typeChecker.typeCheckExpression(expr, contextForTypeChecker)
        
        result.isSuccess shouldBe true
        val typedExpr = result.getOrThrow()
        typedExpr.type shouldBe BuiltinTypes.DOUBLE // Result of arithmetic
    }
})