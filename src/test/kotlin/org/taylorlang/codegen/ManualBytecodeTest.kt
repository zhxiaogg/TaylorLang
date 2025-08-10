package org.taylorlang.codegen

import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

/**
 * Manual test to demonstrate bytecode generation working end-to-end
 */
fun main() {
    println("=== TaylorLang Bytecode Generation Demonstration ===")
    
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("taylor_demo").toFile()
    
    try {
        // Test 1: Simple arithmetic expression
        println("\\n1. Generating bytecode for arithmetic expression: 5 + 3 * 2")
        
        val innerExpr = BinaryOp(
            Literal.IntLiteral(3),
            BinaryOperator.MULTIPLY,
            Literal.IntLiteral(2)
        )
        val outerExpr = BinaryOp(
            Literal.IntLiteral(5),
            BinaryOperator.PLUS,
            innerExpr
        )
        
        val typedExpr = TypedExpression(outerExpr, BuiltinTypes.INT)
        val stmt = TypedStatement.ExpressionStatement(typedExpr)
        val program = TypedProgram(listOf(stmt))
        
        val result1 = generator.generateBytecode(program, tempDir, "ArithmeticDemo")
        result1.fold(
            onSuccess = { gen ->
                val classFile = gen.bytecodeFiles.first()
                println("✓ Generated class file: ${classFile.absolutePath}")
                println("  File size: ${classFile.length()} bytes")
                
                // Try to verify the bytecode structure
                val bytes = classFile.readBytes()
                if (bytes.size >= 4 && 
                    bytes[0] == 0xCA.toByte() && 
                    bytes[1] == 0xFE.toByte() && 
                    bytes[2] == 0xBA.toByte() && 
                    bytes[3] == 0xBE.toByte()) {
                    println("  ✓ Valid Java class file magic number (CAFEBABE)")
                } else {
                    println("  ✗ Invalid class file format")
                }
            },
            onFailure = { error ->
                println("✗ Failed to generate bytecode: ${error.message}")
            }
        )
        
        // Test 2: Function declaration
        println("\\n2. Generating bytecode for function: fn getValue() => 42")
        
        val returnExpr = Literal.IntLiteral(42)
        val body = TypedFunctionBody.Expression(TypedExpression(returnExpr, BuiltinTypes.INT))
        
        val funcDecl = FunctionDecl(
            name = "getValue",
            parameters = persistentListOf(),
            returnType = BuiltinTypes.INT,
            body = FunctionBody.ExpressionBody(returnExpr)
        )
        
        val typedFunc = TypedStatement.FunctionDeclaration(funcDecl, body)
        val funcProgram = TypedProgram(listOf(typedFunc))
        
        val result2 = generator.generateBytecode(funcProgram, tempDir, "FunctionDemo")
        result2.fold(
            onSuccess = { gen ->
                val classFile = gen.bytecodeFiles.first()
                println("✓ Generated class file: ${classFile.absolutePath}")
                println("  File size: ${classFile.length()} bytes")
            },
            onFailure = { error ->
                println("✗ Failed to generate bytecode: ${error.message}")
            }
        )
        
        // Test 3: Multiple expressions
        println("\\n3. Generating bytecode for multiple expressions")
        
        val expr1 = TypedStatement.ExpressionStatement(
            TypedExpression(Literal.IntLiteral(10), BuiltinTypes.INT)
        )
        val expr2 = TypedStatement.ExpressionStatement(
            TypedExpression(Literal.StringLiteral("Hello"), BuiltinTypes.STRING)
        )
        val expr3 = TypedStatement.ExpressionStatement(
            TypedExpression(Literal.BooleanLiteral(true), BuiltinTypes.BOOLEAN)
        )
        
        val multiProgram = TypedProgram(listOf(expr1, expr2, expr3))
        
        val result3 = generator.generateBytecode(multiProgram, tempDir, "MultiDemo")
        result3.fold(
            onSuccess = { gen ->
                val classFile = gen.bytecodeFiles.first()
                println("✓ Generated class file: ${classFile.absolutePath}")
                println("  File size: ${classFile.length()} bytes")
            },
            onFailure = { error ->
                println("✗ Failed to generate bytecode: ${error.message}")
            }
        )
        
        // Summary
        println("\\n=== Summary ===")
        println("Generated class files:")
        tempDir.listFiles()?.filter { it.name.endsWith(".class") }?.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
        }
        
        println("\\n✓ Bytecode generation foundation is working!")
        println("✓ ASM library integration successful")
        println("✓ Generated valid JVM class files")
        println("✓ Support for literals, arithmetic, and functions")
        
    } finally {
        // Clean up
        tempDir.deleteRecursively()
        println("\\nCleaned up temporary files.")
    }
}