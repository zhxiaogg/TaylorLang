package org.taylorlang.ast.visitor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*

class UtilityVisitorTest : StringSpec({

    "IdentifierCollector should find all identifier references" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "test",
                    parameters = persistentListOf(
                        Parameter("x", Type.PrimitiveType("Int")),
                        Parameter("y", Type.PrimitiveType("String"))
                    ),
                    returnType = Type.PrimitiveType("Int"),
                    body = FunctionBody.ExpressionBody(
                        expression = BinaryOp(
                            left = FunctionCall(
                                target = Identifier("toString"),
                                arguments = persistentListOf(Identifier("x"))
                            ),
                            operator = BinaryOperator.PLUS,
                            right = Identifier("y")
                        )
                    )
                )
            )
        )
        
        val collector = IdentifierCollector()
        val identifiers = program.accept(collector)
        
        identifiers shouldHaveSize 3
        identifiers shouldContain "toString"
        identifiers shouldContain "x"
        identifiers shouldContain "y"
    }
    
    "TypeReferenceCollector should find all type references" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "test",
                    parameters = persistentListOf(
                        Parameter("list", Type.GenericType(
                            name = "List",
                            arguments = persistentListOf(Type.PrimitiveType("String"))
                        ))
                    ),
                    returnType = Type.GenericType(
                        name = "Option",
                        arguments = persistentListOf(Type.PrimitiveType("Int"))
                    ),
                    body = FunctionBody.ExpressionBody(
                        expression = Identifier("list")
                    )
                )
            )
        )
        
        val collector = TypeReferenceCollector()
        val types = program.accept(collector)
        
        types shouldHaveSize 4
        types shouldContain "List"
        types shouldContain "String"
        types shouldContain "Option"
        types shouldContain "Int"
    }
    
    "ComplexityAnalyzer should calculate complexity metrics correctly" {
        val complexFunction = FunctionDecl(
            name = "complex",
            parameters = persistentListOf(),
            returnType = Type.PrimitiveType("Int"),
            body = FunctionBody.ExpressionBody(
                expression = IfExpression(
                    condition = BinaryOp(
                        left = FunctionCall(
                            target = Identifier("getValue"),
                            arguments = persistentListOf()
                        ),
                        operator = BinaryOperator.GREATER_THAN,
                        right = Literal.IntLiteral(0)
                    ),
                    thenExpression = MatchExpression(
                        target = Identifier("data"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.IdentifierPattern("value"),
                                expression = BinaryOp(
                                    left = Identifier("value"),
                                    operator = BinaryOperator.MULTIPLY,
                                    right = Literal.IntLiteral(2)
                                )
                            )
                        )
                    ),
                    elseExpression = Literal.IntLiteral(-1)
                )
            )
        )
        
        val analyzer = ComplexityAnalyzer()
        val metrics = complexFunction.accept(analyzer)
        
        metrics.expressions shouldBeGreaterThan 0
        metrics.binaryOps shouldBe 2
        metrics.functionCalls shouldBe 1
        metrics.matchExpressions shouldBe 1
        metrics.ifExpressions shouldBe 1
        metrics.lambdas shouldBe 0
        metrics.complexityScore shouldBeGreaterThan 10
    }
    
    "TypeValidator should detect undefined types" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "test",
                    parameters = persistentListOf(
                        Parameter("x", Type.NamedType("CustomType")),
                        Parameter("y", Type.GenericType("List", persistentListOf(Type.NamedType("UndefinedType"))))
                    ),
                    returnType = Type.PrimitiveType("Int"),
                    body = FunctionBody.ExpressionBody(
                        expression = Identifier("x")
                    )
                )
            )
        )
        
        val definedTypes = setOf("CustomType")
        val validator = TypeValidator(definedTypes)
        val errors = program.accept(validator)
        
        errors shouldHaveSize 1
        errors.first().contains("UndefinedType") shouldBe true
    }
    
    "FunctionSignatureExtractor should extract function signatures" {
        val program = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "add",
                    typeParams = persistentListOf("T"),
                    parameters = persistentListOf(
                        Parameter("x", Type.TypeVar("T")),
                        Parameter("y", Type.TypeVar("T"))
                    ),
                    returnType = Type.TypeVar("T"),
                    body = FunctionBody.ExpressionBody(
                        expression = BinaryOp(
                            left = Identifier("x"),
                            operator = BinaryOperator.PLUS,
                            right = Identifier("y")
                        )
                    )
                ),
                FunctionDecl(
                    name = "toString",
                    parameters = persistentListOf(
                        Parameter("value", Type.PrimitiveType("Int"))
                    ),
                    returnType = Type.PrimitiveType("String"),
                    body = FunctionBody.ExpressionBody(
                        expression = Identifier("value")
                    )
                )
            )
        )
        
        val extractor = FunctionSignatureExtractor()
        val signatures = program.accept(extractor)
        
        signatures shouldHaveSize 2
        
        val addSignature = signatures.find { it.name == "add" }!!
        addSignature.typeParameters shouldBe listOf("T")
        addSignature.parameters shouldHaveSize 2
        addSignature.parameters[0].name shouldBe "x"
        
        val toStringSignature = signatures.find { it.name == "toString" }!!
        toStringSignature.typeParameters shouldBe emptyList()
        toStringSignature.parameters shouldHaveSize 1
    }
    
    "UnusedVariableDetector should detect unused variables" {
        val functionWithUsage = FunctionDecl(
            name = "test",
            parameters = persistentListOf(
                Parameter("used", Type.PrimitiveType("Int")),
                Parameter("unused", Type.PrimitiveType("String"))
            ),
            returnType = Type.PrimitiveType("Int"),
            body = FunctionBody.ExpressionBody(
                expression = BinaryOp(
                    left = Identifier("used"),
                    operator = BinaryOperator.PLUS,
                    right = Literal.IntLiteral(42)
                )
            )
        )
        
        val detector = UnusedVariableDetector()
        val report = functionWithUsage.accept(detector)
        
        report.declared shouldHaveSize 2  // "used" and "unused" parameters
        report.declared shouldContain "used"
        report.declared shouldContain "unused"
        
        report.used shouldHaveSize 1
        report.used shouldContain "used"
        
        report.unused shouldHaveSize 1
        report.unused shouldContain "unused"
    }
    
    "Utility visitors should demonstrate code reduction" {
        // This test demonstrates how the visitor pattern reduces code duplication
        // Previously, each analysis would need its own traversal logic
        // Now, they can reuse the visitor infrastructure
        
        val sampleProgram = Program(
            statements = persistentListOf(
                FunctionDecl(
                    name = "example",
                    parameters = persistentListOf(
                        Parameter("input", Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int"))))
                    ),
                    returnType = Type.PrimitiveType("Bool"),
                    body = FunctionBody.ExpressionBody(
                        expression = BinaryOp(
                            left = PropertyAccess(Identifier("input"), "size"),
                            operator = BinaryOperator.GREATER_THAN,
                            right = Literal.IntLiteral(0)
                        )
                    )
                )
            )
        )
        
        // Multiple analyses can be performed with minimal code duplication
        val identifiers = sampleProgram.accept(IdentifierCollector())
        val typeRefs = sampleProgram.accept(TypeReferenceCollector())
        val complexity = sampleProgram.accept(ComplexityAnalyzer())
        val signatures = sampleProgram.accept(FunctionSignatureExtractor())
        
        // Each visitor focuses on its specific concern without duplicating traversal logic  
        identifiers shouldHaveSize 1 // "input" (size is a property, not an identifier reference)
        typeRefs shouldContain "List"
        typeRefs shouldContain "Int"
        typeRefs shouldContain "Bool"
        complexity.expressions shouldBeGreaterThan 0
        signatures shouldHaveSize 1
        signatures[0].name shouldBe "example"
    }
})