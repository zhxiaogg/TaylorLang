package org.taylorlang

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker

class DebugLambdaTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker()
    
    describe("Lambda Debug Tests") {
        
        it("should debug simple lambda") {
            val code = "val f = x => x"
            
            val parseResult = parser.parse(code)
            parseResult shouldBeSuccess { program ->
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    typedProgram.statements.size shouldBe 1
                }
            }
        }
        
        it("should debug lambda with parameter usage") {
            val code = "val f = x => x * 2"
            
            val parseResult = parser.parse(code)
            parseResult shouldBeSuccess { program ->
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    typedProgram.statements.size shouldBe 1
                }
            }
        }
    }
})