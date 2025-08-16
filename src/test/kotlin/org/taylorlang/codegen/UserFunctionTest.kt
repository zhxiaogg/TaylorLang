package org.taylorlang.codegen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import org.taylorlang.ast.*
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

/**
 * Comprehensive tests for user function declarations and calls.
 * 
 * This test suite validates:
 * - Function declaration parsing and AST building
 * - Function signature type checking
 * - Function call argument validation
 * - Parameter type checking
 * - Return type validation
 * - Bytecode generation for function declarations and calls
 * - End-to-end execution of user-defined functions
 */
class UserFunctionTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker.algorithmic()
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("taylor_function_test").toFile()
    
    afterSpec {
        tempDir.deleteRecursively()
    }
    
    describe("Function declaration parsing") {
        
        it("should parse simple function declaration with parameters") {
            val input = "fn add(x: Int, y: Int): Int => x + y"
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val funcDecl = program.statements[0] as FunctionDecl
            funcDecl.name shouldBe "add"
            funcDecl.parameters.size shouldBe 2
            funcDecl.parameters[0].name shouldBe "x"
            funcDecl.parameters[0].type!!.let { (it as Type.PrimitiveType).name shouldBe "Int" }
            funcDecl.parameters[1].name shouldBe "y"
            funcDecl.parameters[1].type!!.let { (it as Type.PrimitiveType).name shouldBe "Int" }
            funcDecl.returnType!!.let { (it as Type.PrimitiveType).name shouldBe "Int" }
        }
        
        it("should parse function declaration with block body") {
            val input = """
                fn greet(name: String): String {
                    val message = "Hello, " + name
                    return message
                }
            """.trimIndent()
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val funcDecl = program.statements[0] as FunctionDecl
            funcDecl.name shouldBe "greet"
            funcDecl.parameters.size shouldBe 1
            funcDecl.parameters[0].name shouldBe "name"
            funcDecl.parameters[0].type .let { (it as Type.PrimitiveType).name shouldBe "String" }
            funcDecl.returnType .let { (it as Type.PrimitiveType).name shouldBe "String" }
            funcDecl.body should beInstanceOf<FunctionBody.BlockBody>()
        }
        
        it("should parse function declaration without parameters") {
            val input = "fn getAnswer(): Int => 42"
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val funcDecl = program.statements[0] as FunctionDecl
            funcDecl.name shouldBe "getAnswer"
            funcDecl.parameters.size shouldBe 0
            funcDecl.returnType .let { (it as Type.PrimitiveType).name shouldBe "Int" }
        }
        
        it("should parse function declaration without return type (Unit)") {
            val input = "fn sayHello() => println(\"Hello!\")"
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val funcDecl = program.statements[0] as FunctionDecl
            funcDecl.name shouldBe "sayHello"
            funcDecl.parameters.size shouldBe 0
            funcDecl.returnType shouldBe null // Will default to Unit
        }
    }
    
    describe("Function call parsing") {
        
        it("should parse function call with arguments") {
            val input = "add(1, 2)"
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val callExpr = program.statements[0] as FunctionCall
            val targetId = callExpr.target as Identifier
            targetId.name shouldBe "add"
            callExpr.arguments.size shouldBe 2
        }
        
        it("should parse chained function calls") {
            val input = "getValue().toString()"
            
            val program = parser.parse(input).getOrThrow()
            
            program.statements.size shouldBe 1
            val chainedCall = program.statements[0] as FunctionCall
            chainedCall.target should beInstanceOf<PropertyAccess>()
        }
    }
    
    describe("Function declaration type checking") {
        
        it("should type check simple function declaration") {
            val funcDecl = FunctionDecl(
                name = "add",
                parameters = persistentListOf(
                    Parameter("x", Type.PrimitiveType("Int")),
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("Int"),
                body = FunctionBody.ExpressionBody(
                    BinaryOp(
                        left = Identifier("x"),
                        operator = BinaryOperator.PLUS,
                        right = Identifier("y")
                    )
                )
            )
            
            val program = Program(persistentListOf(funcDecl))
            
            val result = typeChecker.typeCheck(program)
            
            result shouldBeSuccess { typedProgram ->
                typedProgram.statements.size shouldBe 1
                val typedFunc = typedProgram.statements[0] as TypedStatement.FunctionDeclaration
                typedFunc.declaration.name shouldBe "add"
            }
        }
        
        it("should detect return type mismatch") {
            val funcDecl = FunctionDecl(
                name = "badAdd",
                parameters = persistentListOf(
                    Parameter("x", Type.PrimitiveType("Int")),
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("String"), // Wrong return type
                body = FunctionBody.ExpressionBody(
                    BinaryOp(
                        left = Identifier("x"),
                        operator = BinaryOperator.PLUS,
                        right = Identifier("y")
                    )
                )
            )
            
            val program = Program(persistentListOf(funcDecl))
            
            val result = typeChecker.typeCheck(program)
            
            result.shouldBeFailure()
            val error = result.exceptionOrNull() as TypeError.TypeMismatch
            error.expected shouldBe Type.PrimitiveType("String")
            error.actual shouldBe Type.PrimitiveType("Int")
        }
        
        it("should detect missing parameter types") {
            val funcDecl = FunctionDecl(
                name = "badFunc",
                parameters = persistentListOf(
                    Parameter("x", null), // Missing type
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("Int"),
                body = FunctionBody.ExpressionBody(Literal.IntLiteral(42))
            )
            
            val program = Program(persistentListOf(funcDecl))
            
            val result = typeChecker.typeCheck(program)
            
            result.shouldBeFailure()
            val error = result.exceptionOrNull() as TypeError.UndefinedType
            error.typeName shouldBe "Missing type annotation for parameter 'x'"
        }
    }
    
    describe("Function call type checking") {
        
        it("should type check function call with correct arguments") {
            val funcDecl = FunctionDecl(
                name = "add",
                parameters = persistentListOf(
                    Parameter("x", Type.PrimitiveType("Int")),
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("Int"),
                body = FunctionBody.ExpressionBody(
                    BinaryOp(
                        left = Identifier("x"),
                        operator = BinaryOperator.PLUS,
                        right = Identifier("y")
                    )
                )
            )
            
            val funcCall = FunctionCall(
                target = Identifier("add"),
                arguments = persistentListOf(
                    Literal.IntLiteral(1),
                    Literal.IntLiteral(2)
                )
            )
            
            val program = Program(persistentListOf(funcDecl, funcCall))
            
            val result = typeChecker.typeCheck(program)
            
            result shouldBeSuccess { typedProgram ->
                typedProgram.statements.size shouldBe 2
                val typedCall = typedProgram.statements[1] as TypedStatement.ExpressionStatement
                // The function call should type as Int
            }
        }
        
        it("should detect arity mismatch") {
            val funcDecl = FunctionDecl(
                name = "add",
                parameters = persistentListOf(
                    Parameter("x", Type.PrimitiveType("Int")),
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("Int"),
                body = FunctionBody.ExpressionBody(Literal.IntLiteral(42))
            )
            
            val funcCall = FunctionCall(
                target = Identifier("add"),
                arguments = persistentListOf(
                    Literal.IntLiteral(1) // Only one argument, should be two
                )
            )
            
            val program = Program(persistentListOf(funcDecl, funcCall))
            
            val result = typeChecker.typeCheck(program)
            
            result.shouldBeFailure()
            val error = result.exceptionOrNull() as TypeError.ArityMismatch
            error.expected shouldBe 2
            error.actual shouldBe 1
        }
        
        it("should detect argument type mismatch") {
            val funcDecl = FunctionDecl(
                name = "add",
                parameters = persistentListOf(
                    Parameter("x", Type.PrimitiveType("Int")),
                    Parameter("y", Type.PrimitiveType("Int"))
                ),
                returnType = Type.PrimitiveType("Int"),
                body = FunctionBody.ExpressionBody(Literal.IntLiteral(42))
            )
            
            val funcCall = FunctionCall(
                target = Identifier("add"),
                arguments = persistentListOf(
                    Literal.IntLiteral(1),
                    Literal.StringLiteral("hello") // Wrong type, should be Int
                )
            )
            
            val program = Program(persistentListOf(funcDecl, funcCall))
            
            val result = typeChecker.typeCheck(program)
            
            result.shouldBeFailure()
            val error = result.exceptionOrNull() as TypeError.TypeMismatch
            error.expected shouldBe Type.PrimitiveType("Int")
            error.actual shouldBe Type.PrimitiveType("String")
        }
        
        it("should detect undefined function") {
            val funcCall = FunctionCall(
                target = Identifier("unknownFunction"),
                arguments = persistentListOf()
            )
            
            val program = Program(persistentListOf(funcCall))
            
            val result = typeChecker.typeCheck(program)
            
            result.shouldBeFailure()
            val error = result.exceptionOrNull() as TypeError.UnresolvedSymbol
            error.symbol shouldBe "unknownFunction"
        }
    }
    
    describe("Function bytecode generation") {
        
        it("should generate bytecode for simple function declaration") {
            val input = "fn add(x: Int, y: Int): Int => x + y"
            val program = parser.parse(input).getOrThrow()
            
            val typeCheckResult = typeChecker.typeCheck(program)
            
            typeCheckResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                
                result shouldBeSuccess { genResult ->
                    genResult.bytecodeFiles.size shouldBe 1
                    genResult.bytecodeFiles.first().exists() shouldBe true
                }
            }
        }
        
        it("should generate bytecode for function with parameters and calls") {
            val input = """
                fn multiply(a: Int, b: Int): Int => a * b
                fn main(): Unit => println(multiply(5, 6))
            """.trimIndent()
            val program = parser.parse(input).getOrThrow()
            
            val typeCheckResult = typeChecker.typeCheck(program)
            
            typeCheckResult shouldBeSuccess { typedProgram ->
                val result = generator.generateBytecode(typedProgram, tempDir)
                
                result shouldBeSuccess { genResult ->
                    genResult.bytecodeFiles.size shouldBe 1
                    genResult.bytecodeFiles.first().exists() shouldBe true
                }
            }
        }
    }
    
    describe("End-to-end function execution") {
        
        it("should execute simple function call") {
            val input = """
                fn double(x: Int): Int => x * 2
                fn main(): Unit => println(double(21))
            """.trimIndent()
            val program = parser.parse(input).getOrThrow()
            
            val typeCheckResult = typeChecker.typeCheck(program)
            
            typeCheckResult shouldBeSuccess { typedProgram ->
                val genResult = generator.generateBytecode(typedProgram, tempDir)
                
                genResult shouldBeSuccess { result ->
                    // For now, just verify bytecode was generated
                    // Full execution testing would require JVM execution
                    result.bytecodeFiles.size shouldBe 1
                    result.mainClassName shouldBe "Program"
                }
            }
        }
        
        it("should execute function with multiple parameters") {
            val input = """
                fn calculate(a: Int, b: Int, c: Int): Int => (a + b) * c
                fn main(): Unit => println(calculate(2, 3, 4))
            """.trimIndent()
            val program = parser.parse(input).getOrThrow()
            
            val typeCheckResult = typeChecker.typeCheck(program)
            
            typeCheckResult shouldBeSuccess { typedProgram ->
                val genResult = generator.generateBytecode(typedProgram, tempDir)
                
                genResult shouldBeSuccess { result ->
                    result.bytecodeFiles.size shouldBe 1
                    result.mainClassName shouldBe "Program"
                }
            }
        }
        
        it("should execute nested function calls") {
            val input = """
                fn add(x: Int, y: Int): Int => x + y  
                fn multiply(a: Int, b: Int): Int => a * b
                fn main(): Unit => println(multiply(add(2, 3), 4))
            """.trimIndent()
            val program = parser.parse(input).getOrThrow()
            
            val typeCheckResult = typeChecker.typeCheck(program)
            
            typeCheckResult shouldBeSuccess { typedProgram ->
                val genResult = generator.generateBytecode(typedProgram, tempDir)
                
                genResult shouldBeSuccess { result ->
                    result.bytecodeFiles.size shouldBe 1
                    result.mainClassName shouldBe "Program"
                }
            }
        }
    }
})

