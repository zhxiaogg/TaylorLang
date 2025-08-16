package org.taylorlang.debug

import org.taylorlang.typechecker.*
import org.taylorlang.parser.TaylorParser

fun main() {
    val source = """
        fn process(x: Int): String => {
            val y = x + "hello";
            val z = y * 2;
            z
        }
    """.trimIndent()
    
    val parser = TaylorParser()
    val ast = parser.parseProgram(source, "debug_test")
    
    val typeChecker = RefactoredTypeChecker()
    val result = typeChecker.typeCheck(ast)
    
    println("Type check result: $result")
    result.fold(
        onSuccess = { println("SUCCESS: $it") },
        onFailure = { error ->
            println("ERROR: $error")
            println("ERROR TYPE: ${error::class.simpleName}")
            if (error is TypeError.MultipleErrors) {
                println("MULTIPLE ERRORS (${error.errors.size}):")
                error.errors.forEachIndexed { i, e ->
                    println("  $i: ${e::class.simpleName} - $e")
                }
            }
        }
    )
}