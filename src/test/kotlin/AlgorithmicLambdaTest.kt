package org.taylorlang

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker

class AlgorithmicLambdaTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker.algorithmic()
    
    describe("Algorithmic Lambda Tests") {
        
        it("should type check simple lambda") {
            val code = "val f = x => x"
            
            val parseResult = parser.parse(code)
            parseResult shouldBeSuccess { program ->
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    println("Simple lambda typed successfully!")
                    println("Result: $typedProgram")
                    typedProgram.statements.size shouldBe 1
                }
            }
        }
        
        it("should type check lambda with parameter usage") {
            val code = "val f = x => x * 2"
            
            val parseResult = parser.parse(code)
            parseResult shouldBeSuccess { program ->
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    println("Lambda with parameter usage typed successfully!")
                    println("Result: $typedProgram")
                    typedProgram.statements.size shouldBe 1
                }
            }
        }
    }
})