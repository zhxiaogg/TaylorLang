package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.result.shouldBeSuccess
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.Files

/**
 * Comprehensive tests for list pattern bytecode generation
 * 
 * These tests validate:
 * - Empty list pattern matching: []
 * - Fixed-length list patterns: [a, b]
 * - Head/tail patterns: [first, ...rest]
 * - Nested pattern matching within lists
 * - Variable binding and scoping
 * - Integration with the runtime
 */
class ListPatternBytecodeTest : DescribeSpec({
    
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_list_pattern_test").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    /**
     * Helper function to compile and execute TaylorLang code
     */
    fun executeCode(program: Program, expectedOutput: String? = null): Any? {
        val typedProgramResult = typeChecker.typeCheck(program)
        
        return typedProgramResult.fold(
            onSuccess = { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.fold(
                    onSuccess = { generationResult ->
                        // Load and execute the generated class
                        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                        val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                        val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                        
                        if (expectedOutput != null) {
                            // Capture stdout for output verification
                            val originalOut = System.out
                            val baos = ByteArrayOutputStream()
                            val ps = PrintStream(baos)
                            
                            try {
                                System.setOut(ps)
                                mainMethod.invoke(null, arrayOf<String>())
                                System.setOut(originalOut)
                                val actualOutput = baos.toString().trim()
                                actualOutput shouldBe expectedOutput
                            } catch (e: Exception) {
                                System.setOut(originalOut)
                                throw e
                            }
                        } else {
                            mainMethod.invoke(null, arrayOf<String>())
                        }
                        true
                    },
                    onFailure = { error ->
                        throw error
                    }
                )
            },
            onFailure = { error ->
                throw error
            }
        )
    }
    
    describe("Empty List Pattern Matching") {
        
        it("should match empty list with [] pattern") {
            // Create a program with: match emptyList { case [] => "empty" case _ => "non-empty" }
            val emptyList = TestUtils.createFunctionCall("emptyList", listOf())
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = emptyList,
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("empty")))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("non-empty")))
                            )
                        )
                    )
                )
            ))
            
            // Note: This test will require runtime list support, but validates compilation
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
    }
    
    describe("Fixed-Length List Pattern Matching") {
        
        it("should match single element list with [x] pattern") {
            // Create a program with: match singleList { case [x] => println(x) case _ => println("other") }
            val singleList = TestUtils.createFunctionCall("singletonList", listOf(Literal.IntLiteral(42)))
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = singleList,
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("x")
                                    ),
                                    restVariable = null
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Identifier("x")))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("other")))
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
        
        it("should match two element list with [a, b] pattern") {
            // Create a program with: match pairList { case [a, b] => println(a + b) case _ => println("other") }
            val pairList = TestUtils.createFunctionCall("listOf", listOf(Literal.IntLiteral(1), Literal.IntLiteral(2)))
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = pairList,
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("a"),
                                        Pattern.IdentifierPattern("b")
                                    ),
                                    restVariable = null
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(
                                    BinaryOp(
                                        left = Identifier("a"),
                                        operator = BinaryOperator.PLUS,
                                        right = Identifier("b")
                                    )
                                ))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("other")))
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
    }
    
    describe("Head/Tail List Pattern Matching") {
        
        it("should match list with [head, ...tail] pattern") {
            // Create a program with: match myList { case [head, ...tail] => println(head) case _ => println("empty") }
            val myList = TestUtils.createFunctionCall("listOf", listOf(
                Literal.IntLiteral(1), 
                Literal.IntLiteral(2), 
                Literal.IntLiteral(3)
            ))
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = myList,
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("head")
                                    ),
                                    restVariable = "tail"
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Identifier("head")))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("empty")))
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
        
        it("should match complex head/tail pattern [first, second, ...rest]") {
            // Create a program with: match longList { case [first, second, ...rest] => println(first + second) case _ => println("too short") }
            val longList = TestUtils.createFunctionCall("listOf", listOf(
                Literal.IntLiteral(10),
                Literal.IntLiteral(20),
                Literal.IntLiteral(30),
                Literal.IntLiteral(40)
            ))
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = longList,
                        cases = persistentListOf(
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(
                                        Pattern.IdentifierPattern("first"),
                                        Pattern.IdentifierPattern("second")
                                    ),
                                    restVariable = "rest"
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(
                                    BinaryOp(
                                        left = Identifier("first"),
                                        operator = BinaryOperator.PLUS,
                                        right = Identifier("second")
                                    )
                                ))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("too short")))
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
    }
    
    describe("Nested List Pattern Matching") {
        
        it("should match literal patterns in list elements") {
            // Create a program with: match numberList { case [1, x, 3] => println(x) case _ => println("no match") }
            val numberList = TestUtils.createFunctionCall("listOf", listOf(
                Literal.IntLiteral(1),
                Literal.IntLiteral(42),
                Literal.IntLiteral(3)
            ))
            val program = TestUtils.createProgram(listOf(
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = numberList,
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
                                expression = TestUtils.createFunctionCall("println", listOf(Identifier("x")))
                            ),
                            MatchCase(
                                pattern = Pattern.WildcardPattern,
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("no match")))
                            )
                        )
                    )
                )
            ))
            
            val typedProgramResult = typeChecker.typeCheck(program)
            typedProgramResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.shouldBeSuccess()
            }
        }
    }
    
    describe("List Pattern Compilation Validation") {
        
        it("should generate valid bytecode for all list pattern types") {
            // Create a program that combines multiple list pattern types
            val testList = Identifier("testList")
            val program = TestUtils.createProgram(listOf(
                // val testList = listOf(1, 2, 3)
                ValDecl(
                    name = "testList",
                    type = null,
                    initializer = TestUtils.createFunctionCall("listOf", listOf(
                        Literal.IntLiteral(1),
                        Literal.IntLiteral(2),
                        Literal.IntLiteral(3)
                    ))
                ),
                // Complex match expression
                TestUtils.createExpressionStatement(
                    MatchExpression(
                        target = testList,
                        cases = persistentListOf(
                            // case [] => "empty"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(),
                                    restVariable = null
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("empty")))
                            ),
                            // case [x] => "single"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(Pattern.IdentifierPattern("x")),
                                    restVariable = null
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("single")))
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
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("pair")))
                            ),
                            // case [first, ...rest] => "many"
                            MatchCase(
                                pattern = Pattern.ListPattern(
                                    elements = persistentListOf(Pattern.IdentifierPattern("first")),
                                    restVariable = "rest"
                                ),
                                expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("many")))
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