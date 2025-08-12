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
 * Validation tests for list pattern bytecode generation
 * 
 * These tests focus on ensuring that:
 * - List patterns compile successfully
 * - Bytecode generation doesn't crash
 * - All list pattern types are handled by the compiler
 * - Variable bindings are processed correctly
 */
class ListPatternBytecodeValidationTest : DescribeSpec({
    
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_list_pattern_validation").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    describe("List Pattern Bytecode Validation") {
        
        it("should compile empty list pattern") {
            // match someList { case [] => 0 }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
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
        
        it("should compile single element list pattern") {
            // match someList { case [x] => x }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("x")
                                    ),
                                    restVariable = null
                                ),
                                expression = Identifier("x")
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
        
        it("should compile fixed-length list pattern") {
            // match someList { case [a, b] => a }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("a"),
                                        Pattern.IdentifierPattern("b")
                                    ),
                                    restVariable = null
                                ),
                                expression = Identifier("a")
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
        
        it("should compile head/tail list pattern") {
            // match someList { case [head, ...tail] => head }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("head")
                                    ),
                                    restVariable = "tail"
                                ),
                                expression = Identifier("head")
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
        
        it("should compile complex head/tail list pattern") {
            // match someList { case [first, second, ...rest] => first }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("first"),
                                        Pattern.IdentifierPattern("second")
                                    ),
                                    restVariable = "rest"
                                ),
                                expression = Identifier("first")
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
        
        it("should compile list pattern with literal elements") {
            // match someList { case [1, x, 3] => x }
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.LiteralPattern(Literal.IntLiteral(1)),
                                        Pattern.IdentifierPattern("x"),
                                        Pattern.LiteralPattern(Literal.IntLiteral(3))
                                    ),
                                    restVariable = null
                                ),
                                expression = Identifier("x")
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
        
        it("should compile multiple list pattern cases") {
            // Complex match with multiple list pattern cases
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = Identifier("someList"),
                        cases = persistentListOf(
                            // case [] => "empty"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
                                expression = Literal.StringLiteral("empty")
                            ),
                            // case [x] => "single"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(Pattern.IdentifierPattern("x")),
                                    restVariable = null
                                ),
                                expression = Literal.StringLiteral("single")
                            ),
                            // case [a, b] => "pair"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("a"),
                                        Pattern.IdentifierPattern("b")
                                    ),
                                    restVariable = null
                                ),
                                expression = Literal.StringLiteral("pair")
                            ),
                            // case [first, ...rest] => "many"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(Pattern.IdentifierPattern("first")),
                                    restVariable = "rest"
                                ),
                                expression = Literal.StringLiteral("many")
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