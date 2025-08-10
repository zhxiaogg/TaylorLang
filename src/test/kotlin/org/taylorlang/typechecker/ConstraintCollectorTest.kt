package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Comprehensive test suite for the ConstraintCollector.
 * Tests constraint generation for all expression types and scenarios.
 */
class ConstraintCollectorTest : StringSpec({

    lateinit var collector: ConstraintCollector
    lateinit var context: InferenceContext

    beforeEach {
        TypeVar.resetCounter()
        collector = ConstraintCollector()
        context = InferenceContext.withBuiltins()
    }

    // =============================================================================
    // Literal Expression Tests
    // =============================================================================

    "IntLiteral should generate correct type and constraints in synthesis mode" {
        val literal = Literal.IntLiteral(42)
        val result = collector.collectConstraints(literal, context)

        result.type shouldBe BuiltinTypes.INT
        result.constraints.isEmpty() shouldBe true
    }

    "IntLiteral should generate equality constraint in checking mode" {
        val literal = Literal.IntLiteral(42)
        val result = collector.collectConstraintsWithExpected(literal, BuiltinTypes.INT, context)

        result.type shouldBe BuiltinTypes.INT
        result.constraints.size() shouldBe 1
        result.constraints.toList().first().shouldBeInstanceOf<Constraint.Equality>()
    }

    "FloatLiteral should infer Double type" {
        val literal = Literal.FloatLiteral(3.14)
        val result = collector.collectConstraints(literal, context)

        result.type shouldBe BuiltinTypes.DOUBLE
        result.constraints.isEmpty() shouldBe true
    }

    "StringLiteral should infer String type" {
        val literal = Literal.StringLiteral("hello")
        val result = collector.collectConstraints(literal, context)

        result.type shouldBe BuiltinTypes.STRING
        result.constraints.isEmpty() shouldBe true
    }

    "BooleanLiteral should infer Boolean type" {
        val literal = Literal.BooleanLiteral(true)
        val result = collector.collectConstraints(literal, context)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.isEmpty() shouldBe true
    }

    "NullLiteral should create nullable type with fresh type variable" {
        val literal = Literal.NullLiteral
        val result = collector.collectConstraints(literal, context)

        result.type.shouldBeInstanceOf<Type.NullableType>()
        val nullableType = result.type as Type.NullableType
        nullableType.baseType.shouldBeInstanceOf<Type.NamedType>()
    }

    "TupleLiteral should collect constraints from all elements" {
        val elements = persistentListOf<Expression>(
            Literal.IntLiteral(1),
            Literal.StringLiteral("test"),
            Literal.BooleanLiteral(false)
        )
        val literal = Literal.TupleLiteral(elements)
        val result = collector.collectConstraints(literal, context)

        result.type.shouldBeInstanceOf<Type.TupleType>()
        val tupleType = result.type as Type.TupleType
        tupleType.elementTypes shouldHaveSize 3
        tupleType.elementTypes[0] shouldBe BuiltinTypes.INT
        tupleType.elementTypes[1] shouldBe BuiltinTypes.STRING
        tupleType.elementTypes[2] shouldBe BuiltinTypes.BOOLEAN
    }

    // =============================================================================
    // Variable and Identifier Tests
    // =============================================================================

    "Identifier should lookup variable in context" {
        val varType = BuiltinTypes.INT
        val contextWithVar = context.withVariable("x", varType)
        val identifier = Identifier("x")

        val result = collector.collectConstraints(identifier, contextWithVar)

        result.type shouldBe varType
        result.constraints.isEmpty() shouldBe true
    }

    "Unknown identifier should create fresh type variable" {
        val identifier = Identifier("unknown")
        val result = collector.collectConstraints(identifier, context)

        result.type.shouldBeInstanceOf<Type.NamedType>()
        val namedType = result.type as Type.NamedType
        namedType.name shouldBe "T1"  // First fresh type variable
    }

    "Polymorphic variable should be instantiated" {
        val typeVar = TypeVar.fresh()
        val scheme = TypeScheme(setOf(typeVar), Type.NamedType(typeVar.id))
        val contextWithPolyVar = context.withVariableScheme("polyVar", scheme)
        val identifier = Identifier("polyVar")

        val result = collector.collectConstraints(identifier, contextWithPolyVar)

        result.type.shouldBeInstanceOf<Type.NamedType>()
        result.constraints.isNotEmpty() shouldBe true
    }

    // =============================================================================
    // Binary Operation Tests
    // =============================================================================

    "Addition should generate numeric constraints" {
        val left = Literal.IntLiteral(5)
        val right = Literal.IntLiteral(3)
        val binaryOp = BinaryOp(left, BinaryOperator.PLUS, right)

        val result = collector.collectConstraints(binaryOp, context)

        result.type shouldBe BuiltinTypes.DOUBLE  // Result of arithmetic promotion
        result.constraints.size() shouldBe 2  // Two subtype constraints for operands
    }

    "Comparison should generate Boolean result type" {
        val left = Literal.IntLiteral(5)
        val right = Literal.IntLiteral(3)
        val binaryOp = BinaryOp(left, BinaryOperator.LESS_THAN, right)

        val result = collector.collectConstraints(binaryOp, context)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.size() shouldBe 2  // Comparability constraints for operands
    }

    "Equality should generate type equality constraint" {
        val left = Literal.IntLiteral(5)
        val right = Literal.StringLiteral("test")
        val binaryOp = BinaryOp(left, BinaryOperator.EQUAL, right)

        val result = collector.collectConstraints(binaryOp, context)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.size() shouldBe 1  // Equality constraint between operand types
        result.constraints.toList().first().shouldBeInstanceOf<Constraint.Equality>()
    }

    "Logical operations should require Boolean operands" {
        val left = Literal.BooleanLiteral(true)
        val right = Literal.BooleanLiteral(false)
        val binaryOp = BinaryOp(left, BinaryOperator.AND, right)

        val result = collector.collectConstraints(binaryOp, context)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.size() shouldBe 2  // Boolean constraints for both operands
    }

    // =============================================================================
    // Unary Operation Tests
    // =============================================================================

    "Numeric negation should preserve operand type" {
        val operand = Literal.IntLiteral(42)
        val unaryOp = UnaryOp(UnaryOperator.MINUS, operand)

        val result = collector.collectConstraints(unaryOp, context)

        result.type shouldBe BuiltinTypes.INT
        result.constraints.size() shouldBe 1  // Numeric subtype constraint
    }

    "Logical negation should require Boolean operand" {
        val operand = Literal.BooleanLiteral(true)
        val unaryOp = UnaryOp(UnaryOperator.NOT, operand)

        val result = collector.collectConstraints(unaryOp, context)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.size() shouldBe 1  // Boolean equality constraint
    }

    // =============================================================================
    // Function Call Tests
    // =============================================================================

    "Function call with known signature should generate argument constraints" {
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
        val call = FunctionCall(target, args)

        val result = collector.collectConstraints(call, contextWithFunction)

        result.type shouldBe BuiltinTypes.BOOLEAN
        result.constraints.size() shouldBe 2  // Subtype constraints for arguments
    }

    "Function call with unknown function should create fresh types" {
        val target = Identifier("unknownFunc")
        val args = persistentListOf<Expression>(
            Literal.IntLiteral(42)
        )
        val call = FunctionCall(target, args)

        val result = collector.collectConstraints(call, context)

        result.type.shouldBeInstanceOf<Type.NamedType>()  // Fresh type variable
        result.constraints.size() shouldBe 1  // Argument type constraint
    }

    "Generic function call should instantiate type parameters" {
        val signature = FunctionSignature(
            typeParameters = listOf("T"),
            parameterTypes = listOf(Type.NamedType("T")),
            returnType = Type.NamedType("T")
        )
        val contextWithGenericFunc = context.withFunctionSignature("identity", signature)

        val target = Identifier("identity")
        val args = persistentListOf<Expression>(Literal.IntLiteral(42))
        val call = FunctionCall(target, args)

        val result = collector.collectConstraints(call, contextWithGenericFunc)

        result.type.shouldBeInstanceOf<Type.NamedType>()  // Instantiated type variable
        result.constraints.size() shouldBe 1  // Argument subtype constraint
    }

    // =============================================================================
    // Constructor Call Tests
    // =============================================================================

    "Constructor call for known union type should generate correct type" {
        val variantDef = VariantDef("Some", listOf(BuiltinTypes.INT))
        val unionTypeDef = TypeDefinition.UnionTypeDef(emptyList(), listOf(variantDef))
        val contextWithType = context.withTypeDefinition("Option", unionTypeDef)

        val args = persistentListOf<Expression>(Literal.IntLiteral(42))
        val call = ConstructorCall("Some", args)

        val result = collector.collectConstraints(call, contextWithType)

        result.type.shouldBeInstanceOf<Type.UnionType>()
        val unionType = result.type as Type.UnionType
        unionType.name shouldBe "Option"
        result.constraints.size() shouldBe 1  // Argument constraint
    }

    "Constructor call with unknown constructor should create fresh type" {
        val args = persistentListOf<Expression>(Literal.IntLiteral(42))
        val call = ConstructorCall("Unknown", args)

        val result = collector.collectConstraints(call, context)

        result.type.shouldBeInstanceOf<Type.NamedType>()  // Fresh type variable
    }

    // =============================================================================
    // Control Flow Expression Tests
    // =============================================================================

    "If expression should require Boolean condition" {
        val condition = Literal.BooleanLiteral(true)
        val thenExpr = Literal.IntLiteral(1)
        val elseExpr = Literal.IntLiteral(2)
        val ifExpr = IfExpression(condition, thenExpr, elseExpr)

        val result = collector.collectConstraints(ifExpr, context)

        result.type shouldBe BuiltinTypes.INT  // Branch types are compatible
        result.constraints.size() shouldBe 1  // Condition Boolean constraint
    }

    "If expression without else should create nullable result" {
        val condition = Literal.BooleanLiteral(true)
        val thenExpr = Literal.IntLiteral(1)
        val ifExpr = IfExpression(condition, thenExpr, null)

        val result = collector.collectConstraints(ifExpr, context)

        result.type.shouldBeInstanceOf<Type.NullableType>()
        val nullableType = result.type as Type.NullableType
        nullableType.baseType shouldBe BuiltinTypes.INT
    }

    "If expression with incompatible branches should create fresh type variable" {
        val condition = Literal.BooleanLiteral(true)
        val thenExpr = Literal.IntLiteral(1)
        val elseExpr = Literal.StringLiteral("test")
        val ifExpr = IfExpression(condition, thenExpr, elseExpr)

        val result = collector.collectConstraints(ifExpr, context)

        result.type.shouldBeInstanceOf<Type.NamedType>()  // Fresh type variable for unification
        result.constraints.size() shouldBe 3  // Condition + two branch equality constraints
    }

    // =============================================================================
    // Match Expression Tests
    // =============================================================================

    "Match expression should process all cases" {
        val target = Identifier("value")
        val pattern1 = Pattern.LiteralPattern(Literal.IntLiteral(1))
        val expr1 = Literal.StringLiteral("one")
        val case1 = MatchCase(pattern1, expr1)

        val pattern2 = Pattern.LiteralPattern(Literal.IntLiteral(2))
        val expr2 = Literal.StringLiteral("two")
        val case2 = MatchCase(pattern2, expr2)

        val cases = persistentListOf(case1, case2)
        val matchExpr = MatchExpression(target, cases)

        val contextWithValue = context.withVariable("value", BuiltinTypes.INT)
        val result = collector.collectConstraints(matchExpr, contextWithValue)

        result.type shouldBe BuiltinTypes.STRING  // All cases return String
        result.constraints.isNotEmpty() shouldBe true
    }

    "Match expression with constructor patterns should bind variables" {
        val unionTypeDef = TypeDefinition.UnionTypeDef(
            emptyList(),
            listOf(
                VariantDef("Some", listOf(BuiltinTypes.INT)),
                VariantDef("None", emptyList())
            )
        )
        val contextWithUnion = context
            .withTypeDefinition("Option", unionTypeDef)
            .withVariable("option", Type.UnionType("Option"))

        val target = Identifier("option")
        val somePattern = Pattern.ConstructorPattern(
            "Some", 
            persistentListOf(Pattern.IdentifierPattern("x"))
        )
        val someExpr = Identifier("x")  // Use bound variable
        val someCase = MatchCase(somePattern, someExpr)

        val nonePattern = Pattern.ConstructorPattern("None", persistentListOf())
        val noneExpr = Literal.IntLiteral(0)
        val noneCase = MatchCase(nonePattern, noneExpr)

        val cases = persistentListOf(someCase, noneCase)
        val matchExpr = MatchExpression(target, cases)

        val result = collector.collectConstraints(matchExpr, contextWithUnion)

        result.type shouldBe BuiltinTypes.INT  // Both cases return Int (bound variable and literal)
        result.constraints.isNotEmpty() shouldBe true
    }

    // =============================================================================
    // Block Expression Tests
    // =============================================================================

    "Block expression should process statements and return final expression type" {
        val valDecl = ValDecl("x", null, Literal.IntLiteral(42))
        val finalExpr = Identifier("x")
        val block = BlockExpression(
            statements = persistentListOf(valDecl),
            expression = finalExpr
        )

        val result = collector.collectConstraints(block, context)

        result.type shouldBe BuiltinTypes.INT
        result.constraints.isEmpty() shouldBe true
    }

    "Block expression without final expression should return Unit" {
        val valDecl = ValDecl("x", null, Literal.IntLiteral(42))
        val block = BlockExpression(
            statements = persistentListOf(valDecl),
            expression = null
        )

        val result = collector.collectConstraints(block, context)

        result.type shouldBe BuiltinTypes.UNIT
        result.constraints.isEmpty() shouldBe true
    }

    "Block expression should handle variable scoping" {
        val innerBlock = BlockExpression(
            statements = persistentListOf(
                ValDecl("y", null, Literal.StringLiteral("inner"))
            ),
            expression = Identifier("y")
        )
        val outerBlock = BlockExpression(
            statements = persistentListOf(
                ValDecl("x", null, Literal.IntLiteral(42))
            ),
            expression = innerBlock
        )

        val result = collector.collectConstraints(outerBlock, context)

        result.type shouldBe BuiltinTypes.STRING
        result.constraints.isEmpty() shouldBe true
    }

    // =============================================================================
    // Lambda Expression Tests
    // =============================================================================

    "Lambda expression should create function type" {
        val params = persistentListOf("x", "y")
        val body = BinaryOp(
            Identifier("x"),
            BinaryOperator.PLUS,
            Identifier("y")
        )
        val lambda = LambdaExpression(params, body)

        val result = collector.collectConstraints(lambda, context)

        result.type.shouldBeInstanceOf<Type.FunctionType>()
        val functionType = result.type as Type.FunctionType
        functionType.parameterTypes shouldHaveSize 2
        functionType.returnType shouldBe BuiltinTypes.DOUBLE  // Result of addition
    }

    "Lambda expression should bind parameters in body scope" {
        val params = persistentListOf("x")
        val body = Identifier("x")  // Reference parameter
        val lambda = LambdaExpression(params, body)

        val result = collector.collectConstraints(lambda, context)

        result.type.shouldBeInstanceOf<Type.FunctionType>()
        val functionType = result.type as Type.FunctionType
        functionType.parameterTypes shouldHaveSize 1
        functionType.parameterTypes[0].shouldBeInstanceOf<Type.NamedType>()  // Fresh type variable
    }

    // =============================================================================
    // Pattern Processing Tests
    // =============================================================================

    "Wildcard pattern should match any type with no constraints" {
        val pattern = Pattern.WildcardPattern
        val result = collector.collectConstraints(
            // Use pattern in a match expression to test it
            MatchExpression(
                Literal.IntLiteral(42),
                persistentListOf(MatchCase(pattern, Literal.StringLiteral("matched")))
            ),
            context
        )

        result.constraints.isEmpty() shouldBe true  // Wildcard matches with no additional constraints
    }

    "Identifier pattern should bind variable" {
        val pattern = Pattern.IdentifierPattern("x")
        val matchExpr = MatchExpression(
            Literal.IntLiteral(42),
            persistentListOf(MatchCase(pattern, Identifier("x")))  // Use bound variable
        )

        val result = collector.collectConstraints(matchExpr, context)

        result.type shouldBe BuiltinTypes.INT  // Bound variable has target type
    }

    "Guard pattern should add Boolean constraint for guard expression" {
        val innerPattern = Pattern.IdentifierPattern("x")
        val guardExpr = BinaryOp(
            Identifier("x"),
            BinaryOperator.GREATER_THAN,
            Literal.IntLiteral(0)
        )
        val guardPattern = Pattern.GuardPattern(innerPattern, guardExpr)

        val matchExpr = MatchExpression(
            Literal.IntLiteral(42),
            persistentListOf(MatchCase(guardPattern, Literal.StringLiteral("positive")))
        )

        val result = collector.collectConstraints(matchExpr, context)

        result.constraints.size() shouldBe 3  // Pattern constraints, guard constraints, and boolean check
    }

    // =============================================================================
    // Edge Cases and Integration Tests
    // =============================================================================

    "Complex nested expressions should accumulate all constraints" {
        val complexExpr = IfExpression(
            condition = BinaryOp(
                Identifier("x"),
                BinaryOperator.GREATER_THAN,
                Literal.IntLiteral(0)
            ),
            thenExpression = FunctionCall(
                target = Identifier("toString"),
                arguments = persistentListOf(Identifier("x"))
            ),
            elseExpression = Literal.StringLiteral("negative")
        )

        val contextWithVar = context.withVariable("x", BuiltinTypes.INT)
        val result = collector.collectConstraints(complexExpr, contextWithVar)

        result.constraints.isNotEmpty() shouldBe true
        result.constraints.size() shouldBe 6  // Constraints from condition, then branch, function call etc.
    }

    "Type checking mode should add expected type constraints" {
        val expr = Literal.IntLiteral(42)
        val expectedType = BuiltinTypes.STRING  // Intentionally wrong type

        val result = collector.collectConstraintsWithExpected(expr, expectedType, context)

        result.type shouldBe BuiltinTypes.INT  // Actual type
        result.constraints.size() shouldBe 1
        val constraint = result.constraints.toList().first() as Constraint.Equality
        constraint.left shouldBe BuiltinTypes.INT
        constraint.right shouldBe expectedType
    }

    "Fresh type variables should have unique IDs" {
        val expr1 = Identifier("unknown1")
        val expr2 = Identifier("unknown2")

        val result1 = collector.collectConstraints(expr1, context)
        val result2 = collector.collectConstraints(expr2, context)

        result1.type shouldNotBe result2.type
        val type1 = result1.type as Type.NamedType
        val type2 = result2.type as Type.NamedType
        type1.name shouldNotBe type2.name
    }

    "Constraint result should support adding constraints" {
        val baseResult = ConstraintResult.withType(BuiltinTypes.INT)
        val constraint = Constraint.Equality(BuiltinTypes.INT, BuiltinTypes.STRING)

        val newResult = baseResult.addConstraint(constraint)

        newResult.type shouldBe BuiltinTypes.INT
        newResult.constraints.size() shouldBe 1
        newResult.constraints.toList() shouldContain constraint
    }

    "Context scoping should work correctly in nested expressions" {
        val innerBlock = BlockExpression(
            statements = persistentListOf(
                ValDecl("inner", null, Literal.StringLiteral("test"))
            ),
            expression = Identifier("inner")
        )
        
        val outerContext = context.withVariable("outer", BuiltinTypes.INT)
        val result = collector.collectConstraints(innerBlock, outerContext)

        result.type shouldBe BuiltinTypes.STRING
        result.constraints.isEmpty() shouldBe true
    }
})