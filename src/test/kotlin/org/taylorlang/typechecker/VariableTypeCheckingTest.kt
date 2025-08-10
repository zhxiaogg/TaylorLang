package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import org.taylorlang.ast.*
import org.taylorlang.parser.TaylorLangParser

class VariableTypeCheckingTest : StringSpec({
    val parser = TaylorLangParser()
    val context = TypeContext.withBuiltins()
    
    "should type check variable declarations correctly" {
        val code = """
            var x: Int = 42
            val name: String = "Hello"
        """.trimIndent()
        
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        
        // Type check first statement (var declaration)
        val varResult = program.statements[0].accept(statementChecker)
        varResult.isSuccess shouldBe true
        val varStatement = varResult.getOrThrow()
        varStatement should beInstanceOf<TypedStatement.MutableVariableDeclaration>()
        
        val varDecl = varStatement as TypedStatement.MutableVariableDeclaration
        varDecl.declaration.name shouldBe "x"
        varDecl.inferredType should beInstanceOf<Type.PrimitiveType>()
        (varDecl.inferredType as Type.PrimitiveType).name shouldBe "Int"
        
        // Type check second statement (val declaration)  
        val valResult = program.statements[1].accept(statementChecker)
        valResult.isSuccess shouldBe true
        val valStatement = valResult.getOrThrow()
        valStatement should beInstanceOf<TypedStatement.VariableDeclaration>()
        
        val valDecl = valStatement as TypedStatement.VariableDeclaration
        valDecl.declaration.name shouldBe "name"
        valDecl.inferredType should beInstanceOf<Type.PrimitiveType>()
        (valDecl.inferredType as Type.PrimitiveType).name shouldBe "String"
    }
    
    "should detect assignment to immutable variable" {
        val code = """
            val x: Int = 10
            x = 20
        """.trimIndent()
        
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        
        // Type check val declaration (should succeed)
        val varDeclResult = program.statements[0].accept(statementChecker)
        varDeclResult.isSuccess shouldBe true
        
        // Type check assignment (should fail - immutable)
        val assignmentResult = program.statements[1].accept(statementChecker)
        assignmentResult.isFailure shouldBe true
        val error = assignmentResult.exceptionOrNull()
        error should beInstanceOf<TypeError.InvalidOperation>()
    }
    
    "should detect assignment to undefined variable" {
        val code = "undefined_var = 42"
        
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        
        // Type check assignment (should fail - undefined variable)
        val result = program.statements[0].accept(statementChecker)
        result.isFailure shouldBe true
        val error = result.exceptionOrNull()
        error should beInstanceOf<TypeError.UnresolvedSymbol>()
    }
    
    "should detect type mismatch in assignment" {
        val code = """
            var x: Int = 10
            x = "string"
        """.trimIndent()
        
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        
        // Type check var declaration (should succeed)
        val varDeclResult = program.statements[0].accept(statementChecker)
        varDeclResult.isSuccess shouldBe true
        
        // Type check assignment (should fail - type mismatch)
        val assignmentResult = program.statements[1].accept(statementChecker)
        assignmentResult.isFailure shouldBe true
        val error = assignmentResult.exceptionOrNull()
        error should beInstanceOf<TypeError.TypeMismatch>()
    }
    
    "should handle duplicate variable declarations" {
        val code = """
            var x: Int = 10
            var x: String = "duplicate"
        """.trimIndent()
        
        val program = parser.parse(code).getOrThrow()
        val statementChecker = StatementTypeChecker(context)
        
        // First declaration should succeed
        val firstResult = program.statements[0].accept(statementChecker)
        firstResult.isSuccess shouldBe true
        
        // Second declaration should fail (duplicate)
        val secondResult = program.statements[1].accept(statementChecker)
        secondResult.isFailure shouldBe true
        val error = secondResult.exceptionOrNull()
        error should beInstanceOf<TypeError.DuplicateDefinition>()
    }
})