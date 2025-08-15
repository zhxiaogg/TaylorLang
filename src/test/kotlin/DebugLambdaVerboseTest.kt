package org.taylorlang

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker

class DebugLambdaVerboseTest : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker.withConstraints()
    
    describe("Lambda Debug Verbose Tests") {
        
        it("should show what goes wrong with simple lambda") {
            val code = "val f = x => x"
            
            val parseResult = parser.parse(code)
            parseResult shouldBeSuccess { program ->
                println("Successfully parsed: $program")
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeFailure { error ->
                    println("Type checking failed with error: $error")
                }
            }
        }
    }
})