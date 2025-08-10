package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Comprehensive tests for BytecodeGenerator
 * 
 * These tests validate:
 * - Basic class file generation
 * - Literal compilation (int, double, boolean, string)
 * - Arithmetic expression compilation (+, -, *, /)
 * - Function declaration handling
 * - Main method generation
 * - End-to-end executable program generation
 */
class BytecodeGeneratorTest : DescribeSpec({
    
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("taylor_bytecode_test").toFile()
    
    afterSpec {
        // Clean up temp directory
        tempDir.deleteRecursively()
    }
    
    describe("Basic class file generation") {
        
        it("should generate valid class file for empty program") {
            val program = TypedProgram(emptyList())
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                generationResult.bytecodeFiles shouldHaveSize 1
                generationResult.mainClassName shouldBe "Program"
                
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
                classFile.name shouldBe "Program.class"
                classFile.length() shouldNotBe 0L
            }
        }
        
        it("should generate class file with custom name") {
            val program = TypedProgram(emptyList())
            
            val result = generator.generateBytecode(program, tempDir, "CustomClass")
            
            result shouldBeSuccess { generationResult ->
                generationResult.mainClassName shouldBe "CustomClass"
                generationResult.bytecodeFiles.first().name shouldBe "CustomClass.class"
            }
        }
    }
    
    describe("Literal compilation") {
        
        it("should compile integer literals") {
            val expr = Literal.IntLiteral(42)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile double literals") {
            val expr = Literal.FloatLiteral(3.14)
            val typedExpr = TypedExpression(expr, BuiltinTypes.DOUBLE)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile boolean literals") {
            val expr = Literal.BooleanLiteral(true)
            val typedExpr = TypedExpression(expr, BuiltinTypes.BOOLEAN)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile string literals") {
            val expr = Literal.StringLiteral("Hello, World!")
            val typedExpr = TypedExpression(expr, BuiltinTypes.STRING)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
    
    describe("Arithmetic expression compilation") {
        
        it("should compile integer addition") {
            val left = Literal.IntLiteral(5)
            val right = Literal.IntLiteral(3)
            val expr = BinaryOp(left, BinaryOperator.PLUS, right)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile integer subtraction") {
            val left = Literal.IntLiteral(10)
            val right = Literal.IntLiteral(4)
            val expr = BinaryOp(left, BinaryOperator.MINUS, right)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile integer multiplication") {
            val left = Literal.IntLiteral(6)
            val right = Literal.IntLiteral(7)
            val expr = BinaryOp(left, BinaryOperator.MULTIPLY, right)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile integer division") {
            val left = Literal.IntLiteral(15)
            val right = Literal.IntLiteral(3)
            val expr = BinaryOp(left, BinaryOperator.DIVIDE, right)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile double arithmetic") {
            val left = Literal.FloatLiteral(5.5)
            val right = Literal.FloatLiteral(2.5)
            val expr = BinaryOp(left, BinaryOperator.PLUS, right)
            val typedExpr = TypedExpression(expr, BuiltinTypes.DOUBLE)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile nested arithmetic expressions") {
            // (5 + 3) * 2
            val innerLeft = Literal.IntLiteral(5)
            val innerRight = Literal.IntLiteral(3)
            val innerExpr = BinaryOp(innerLeft, BinaryOperator.PLUS, innerRight)
            
            val outer = BinaryOp(innerExpr, BinaryOperator.MULTIPLY, Literal.IntLiteral(2))
            val typedExpr = TypedExpression(outer, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
    
    describe("Unary expression compilation") {
        
        it("should compile integer negation") {
            val operand = Literal.IntLiteral(42)
            val expr = UnaryOp(UnaryOperator.MINUS, operand)
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile boolean negation") {
            val operand = Literal.BooleanLiteral(true)
            val expr = UnaryOp(UnaryOperator.NOT, operand)
            val typedExpr = TypedExpression(expr, BuiltinTypes.BOOLEAN)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
    
    describe("Function declaration handling") {
        
        it("should compile simple function with expression body") {
            val returnExpr = Literal.IntLiteral(42)
            val body = TypedFunctionBody.Expression(TypedExpression(returnExpr, BuiltinTypes.INT))
            
            val funcDecl = FunctionDecl(
                name = "getValue",
                parameters = persistentListOf(),
                returnType = BuiltinTypes.INT,
                body = FunctionBody.ExpressionBody(returnExpr)
            )
            
            val typedFunc = TypedStatement.FunctionDeclaration(funcDecl, body)
            val program = TypedProgram(listOf(typedFunc))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile main function") {
            val returnExpr = Literal.StringLiteral("Hello, World!")
            val printCall = FunctionCall(
                target = Identifier("println"),
                arguments = persistentListOf(returnExpr)
            )
            val body = TypedFunctionBody.Expression(TypedExpression(printCall, BuiltinTypes.UNIT))
            
            val funcDecl = FunctionDecl(
                name = "main",
                parameters = persistentListOf(),
                returnType = BuiltinTypes.UNIT,
                body = FunctionBody.ExpressionBody(printCall)
            )
            
            val typedFunc = TypedStatement.FunctionDeclaration(funcDecl, body)
            val program = TypedProgram(listOf(typedFunc))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
    
    describe("PrintLn function call handling") {
        
        it("should compile println with string argument") {
            val arg = Literal.StringLiteral("Hello, Bytecode!")
            val printCall = FunctionCall(
                target = Identifier("println"),
                arguments = persistentListOf(arg)
            )
            val typedCall = TypedExpression(printCall, BuiltinTypes.UNIT)
            val stmt = TypedStatement.ExpressionStatement(typedCall)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
        
        it("should compile println with no arguments") {
            val printCall = FunctionCall(
                target = Identifier("println"),
                arguments = persistentListOf()
            )
            val typedCall = TypedExpression(printCall, BuiltinTypes.UNIT)
            val stmt = TypedStatement.ExpressionStatement(typedCall)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
    
    describe("End-to-end program compilation") {
        
        it("should compile complete program with multiple statements") {
            // Create a program equivalent to:
            // println("Starting calculation")
            // val result = 5 + 3 * 2
            // println("Result: " + result)
            
            val statements = listOf(
                // println("Starting calculation")
                TypedStatement.ExpressionStatement(
                    TypedExpression(
                        FunctionCall(
                            target = Identifier("println"),
                            arguments = persistentListOf(Literal.StringLiteral("Starting calculation"))
                        ),
                        BuiltinTypes.UNIT
                    )
                ),
                // val result = 5 + 3 * 2
                TypedStatement.VariableDeclaration(
                    declaration = ValDecl(
                        name = "result",
                        type = BuiltinTypes.INT,
                        initializer = BinaryOp(
                            Literal.IntLiteral(5),
                            BinaryOperator.PLUS,
                            BinaryOp(
                                Literal.IntLiteral(3),
                                BinaryOperator.MULTIPLY,
                                Literal.IntLiteral(2)
                            )
                        )
                    ),
                    initializer = TypedExpression(
                        BinaryOp(
                            Literal.IntLiteral(5),
                            BinaryOperator.PLUS,
                            BinaryOp(
                                Literal.IntLiteral(3),
                                BinaryOperator.MULTIPLY,
                                Literal.IntLiteral(2)
                            )
                        ),
                        BuiltinTypes.INT
                    ),
                    inferredType = BuiltinTypes.INT
                ),
                // println("Calculation complete")
                TypedStatement.ExpressionStatement(
                    TypedExpression(
                        FunctionCall(
                            target = Identifier("println"),
                            arguments = persistentListOf(Literal.StringLiteral("Calculation complete"))
                        ),
                        BuiltinTypes.UNIT
                    )
                )
            )
            
            val program = TypedProgram(statements)
            
            val result = generator.generateBytecode(program, tempDir, "CompleteProgram")
            
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
                classFile.name shouldBe "CompleteProgram.class"
                classFile.length() shouldNotBe 0L
            }
        }
    }
    
    describe("Error handling") {
        
        it("should handle generation failures gracefully") {
            // Test with a program that might cause issues
            val expr = Identifier("unknownIdentifier") // This should not crash the generator
            val typedExpr = TypedExpression(expr, BuiltinTypes.INT)
            val stmt = TypedStatement.ExpressionStatement(typedExpr)
            val program = TypedProgram(listOf(stmt))
            
            val result = generator.generateBytecode(program, tempDir)
            
            // Should succeed but generate placeholder code
            result shouldBeSuccess { generationResult ->
                val classFile = generationResult.bytecodeFiles.first()
                classFile.exists() shouldBe true
            }
        }
    }
})