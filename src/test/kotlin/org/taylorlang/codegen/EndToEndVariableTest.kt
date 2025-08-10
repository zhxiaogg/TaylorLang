package org.taylorlang.codegen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.TypedProgram
import org.taylorlang.typechecker.StatementTypeChecker
import org.taylorlang.typechecker.TypeContext

class EndToEndVariableTest : StringSpec({
    val parser = TaylorLangParser()
    val context = TypeContext.withBuiltins()
    val generator = BytecodeGenerator()
    
    "should handle complete variable program end-to-end" {
        val code = """
            var x: Int = 5
            var y: Int = 10
            y = x + y
            println(y)
        """.trimIndent()
        
        // Parse the program
        val program = parser.parse(code).getOrThrow()
        program.statements.size shouldBe 4
        
        // Type check statements sequentially
        val statementChecker = StatementTypeChecker(context)
        val typedStatements = program.statements.map { statement ->
            statement.accept(statementChecker).getOrThrow()
        }
        
        val typedProgram = TypedProgram(typedStatements)
        
        // Generate bytecode
        val result = generator.generateBytecode(typedProgram)
        result.isSuccess shouldBe true
        
        // Verify bytecode files were generated
        val generationResult = result.getOrThrow()
        generationResult.bytecodeFiles.isNotEmpty() shouldBe true
        generationResult.mainClassName shouldBe "Program"
    }
    
    "should handle immutable vs mutable variable semantics" {
        val code = """
            val immutable: Int = 42
            var mutable: Int = 0
            mutable = immutable + 10
            println(mutable)
        """.trimIndent()
        
        // Parse and type check
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        val typedStatements = program.statements.map { statement ->
            statement.accept(statementChecker).getOrThrow()
        }
        
        val typedProgram = TypedProgram(typedStatements)
        
        // Generate bytecode - should succeed
        val result = generator.generateBytecode(typedProgram)
        result.isSuccess shouldBe true
    }
})