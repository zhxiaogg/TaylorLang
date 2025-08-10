package org.taylorlang.parser

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import org.taylorlang.ast.Program
import org.taylorlang.grammar.TaylorLangLexer
import org.taylorlang.grammar.TaylorLangParser as AntlrParser

/**
 * Error information for parsing failures
 */
data class ParseError(
    val message: String,
    val line: Int,
    val column: Int,
    val cause: Throwable? = null
)

/**
 * Main parser interface for TaylorLang
 */
class TaylorLangParser {
    
    /**
     * Parse TaylorLang source code into an AST
     */
    fun parse(source: String, fileName: String = "<unknown>"): Result<Program> {
        return try {
            val lexer = TaylorLangLexer(CharStreams.fromString(source, fileName))
            val tokens = CommonTokenStream(lexer)
            val parser = AntlrParser(tokens)
            
            // Configure error handling
            val errorListener = CollectingErrorListener()
            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)
            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)
            
            // Parse program
            val parseTree = parser.program()
            
            // Check for parsing errors
            if (errorListener.errors.isNotEmpty()) {
                val firstError = errorListener.errors.first()
                return Result.failure(RuntimeException("${firstError.message} at ${firstError.line}:${firstError.column}"))
            }
            
            // Build AST
            val astBuilder = ASTBuilder()
            val program = astBuilder.visitProgram(parseTree)
            
            Result.success(program)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Unexpected parsing error: ${e.message}", e))
        }
    }
    
    /**
     * Parse a single expression (useful for testing and REPL)
     */
    fun parseExpression(source: String, fileName: String = "<expression>"): Result<org.taylorlang.ast.Expression> {
        return try {
            val lexer = TaylorLangLexer(CharStreams.fromString(source, fileName))
            val tokens = CommonTokenStream(lexer)
            val parser = AntlrParser(tokens)
            
            // Configure error handling
            val errorListener = CollectingErrorListener()
            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)
            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)
            
            // Parse expression
            val parseTree = parser.expression()
            
            // Check for parsing errors
            if (errorListener.errors.isNotEmpty()) {
                val firstError = errorListener.errors.first()
                return Result.failure(RuntimeException("${firstError.message} at ${firstError.line}:${firstError.column}"))
            }
            
            // Build AST
            val astBuilder = ASTBuilder()
            val expression = astBuilder.visitExpression(parseTree)
            
            Result.success(expression)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Unexpected parsing error: ${e.message}", e))
        }
    }
}

/**
 * Custom error listener that collects parsing errors
 */
private class CollectingErrorListener : BaseErrorListener() {
    data class Error(
        val message: String,
        val line: Int,
        val column: Int,
        val exception: RecognitionException?
    )
    
    val errors = mutableListOf<Error>()
    
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        errors.add(Error(
            message = msg,
            line = line,
            column = charPositionInLine,
            exception = e
        ))
    }
}