package org.taylorlang.codegen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.TypedProgram
import org.taylorlang.typechecker.StatementTypeChecker
import org.taylorlang.typechecker.TypeContext

class VariableBytecodeTest : StringSpec({
    val parser = TaylorLangParser()
    val context = TypeContext.withBuiltins()
    val generator = BytecodeGenerator()
    
    "should generate bytecode for variable declarations and usage" {
        val code = """
            var x: Int = 42
            val y: Int = 10
            println(x + y)
        """.trimIndent()
        
        // Parse the program
        val program = parser.parse(code).getOrThrow()
        
        // Type check statements sequentially to maintain variable context
        val statementChecker = StatementTypeChecker(context)
        val typedStatements = program.statements.map { statement ->
            statement.accept(statementChecker).getOrThrow()
        }
        
        val typedProgram = TypedProgram(typedStatements)
        
        // Generate bytecode (should not throw)
        val result = generator.generateBytecode(typedProgram)
        result.isSuccess shouldBe true
    }
    
    "should generate bytecode for variable assignment" {
        val code = """
            var counter: Int = 0
            counter = counter + 1
            println(counter)
        """.trimIndent()
        
        // Parse the program
        val program = parser.parse(code).getOrThrow()
        
        // Type check statements sequentially to maintain variable context
        val statementChecker = StatementTypeChecker(context)
        val typedStatements = program.statements.map { statement ->
            statement.accept(statementChecker).getOrThrow()
        }
        
        val typedProgram = TypedProgram(typedStatements)
        
        // Generate bytecode (should not throw)
        val result = generator.generateBytecode(typedProgram)
        result.isSuccess shouldBe true
    }
    
    "should generate bytecode for multiple variables" {
        val code = """
            var a: Int = 1
            var b: Int = 2
            var c: Int = 3
            c = a + b
            println(c)
        """.trimIndent()
        
        // Parse the program
        val program = parser.parse(code).getOrThrow()
        
        // Type check statements sequentially to maintain variable context
        val statementChecker = StatementTypeChecker(context)
        val typedStatements = program.statements.map { statement ->
            statement.accept(statementChecker).getOrThrow()
        }
        
        val typedProgram = TypedProgram(typedStatements)
        
        // Generate bytecode (should not throw)
        val result = generator.generateBytecode(typedProgram)
        result.isSuccess shouldBe true
    }
})