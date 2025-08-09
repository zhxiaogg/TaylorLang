package org.taylorlang.compiler

import org.taylorlang.parser.TaylorLangParser
import java.io.File
import kotlin.system.exitProcess

/**
 * Main entry point for the TaylorLang compiler
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: taylorlang <source-file>")
        exitProcess(1)
    }
    
    val sourceFile = File(args[0])
    if (!sourceFile.exists()) {
        println("Error: File '${args[0]}' not found")
        exitProcess(1)
    }
    
    try {
        val source = sourceFile.readText()
        val parser = TaylorLangParser()
        
        when (val result = parser.parse(source, sourceFile.name)) {
            is arrow.core.Either.Left -> {
                println("Parse error at ${result.value.line}:${result.value.column}: ${result.value.message}")
                exitProcess(1)
            }
            is arrow.core.Either.Right -> {
                val program = result.value
                println("Successfully parsed ${sourceFile.name}")
                println("Program contains ${program.statements.size} statements")
                
                // For now, just print the AST structure
                program.statements.forEachIndexed { index, statement ->
                    println("Statement ${index + 1}: ${statement::class.simpleName}")
                }
            }
        }
    } catch (e: Exception) {
        println("Error reading file: ${e.message}")
        exitProcess(1)
    }
}