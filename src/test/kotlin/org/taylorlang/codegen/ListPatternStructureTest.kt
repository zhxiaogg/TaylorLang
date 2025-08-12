package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.result.shouldBeSuccess
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.nio.file.Files

/**
 * Structure tests for list pattern support
 * 
 * These tests validate that:
 * - List patterns are correctly represented in AST
 * - Type checking handles list patterns
 * - Bytecode generation processes list patterns without crashes
 */
class ListPatternStructureTest : DescribeSpec({
    
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_list_pattern_structure").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    describe("List Pattern Structure Tests") {
        
        it("should create and compile program with list pattern AST") {
            // Create a program with a variable declaration followed by match
            val program = TestUtils.createProgram(listOf(
                // val myList = emptyList<Int>()  // proper list value
                ValDecl(
                    name = "myList",
                    type = null,
                    initializer = TestUtils.createFunctionCall("emptyList", listOf())
                ),
                // match myList { case [] => 0 case _ => 1 }
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("myList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
                                expression = Literal.IntLiteral(0)
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = Literal.IntLiteral(1)
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result shouldBeSuccess { generationResult ->
                    generationResult.bytecodeFiles shouldNotBe null
                    generationResult.mainClassName shouldNotBe null
                }
            }
        }
        
        it("should compile list pattern with multiple cases") {
            val program = TestUtils.createProgram(listOf(
                // val x = listOf2(1, 2)  // Use explicit arity function
                ValDecl(
                    name = "x",
                    type = null,
                    initializer = TestUtils.createFunctionCall("listOf2", listOf(Literal.IntLiteral(1), Literal.IntLiteral(2)))
                ),
                // Complex match expression with multiple list patterns
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("x"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
                                expression = Literal.StringLiteral("empty")
                            ),
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(Pattern.IdentifierPattern("a")),
                                    restVariable = null
                                ),
                                expression = Literal.StringLiteral("single")
                            ),
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("first"),
                                        Pattern.IdentifierPattern("second")
                                    ),
                                    restVariable = "rest"
                                ),
                                expression = Literal.StringLiteral("head_tail")
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = Literal.StringLiteral("default")
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result shouldBeSuccess { generationResult ->
                    generationResult.bytecodeFiles shouldNotBe null
                    generationResult.mainClassName shouldNotBe null
                }
            }
        }
        
        it("should handle variable binding in list patterns") {
            val program = TestUtils.createProgram(listOf(
                ValDecl(
                    name = "data",
                    type = null,
                    initializer = TestUtils.createFunctionCall("listOf2", listOf(Literal.IntLiteral(10), Literal.IntLiteral(20)))
                ),
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("data"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("x"),
                                        Pattern.IdentifierPattern("y")
                                    ),
                                    restVariable = "z"
                                ),
                                // Use the bound variables in expression
                                expression = BinaryOp(
                                    left = Identifier("x"),
                                    operator = BinaryOperator.PLUS,
                                    right = Identifier("y")
                                )
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = Literal.IntLiteral(0)
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result shouldBeSuccess { generationResult ->
                    generationResult.bytecodeFiles shouldNotBe null
                    generationResult.mainClassName shouldNotBe null
                }
            }
        }
    }
})