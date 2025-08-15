package org.taylorlang

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.typechecker.RefactoredTypeChecker
import java.io.File

class LambdaIntegrationTestDebug : DescribeSpec({
    
    val parser = TaylorLangParser()
    val typeChecker = RefactoredTypeChecker()
    
    describe("Lambda Integration Test Debug") {
        
        it("should replicate the exact integration test scenario") {
            val testFile = File("src/test/resources/test_cases/test_lambda_expressions.taylor")
            val sourceCode = testFile.readText()
            
            val parseResult = parser.parse(sourceCode)
            parseResult shouldBeSuccess { program ->
                val typedResult = typeChecker.typeCheck(program)
                typedResult shouldBeSuccess { typedProgram ->
                    println("Lambda expressions test PASSED! Found ${typedProgram.statements.size} statements")
                }
            }
        }
    }
})