import org.taylorlang.parser.TaylorLangParser
import org.taylorlang.parser.ASTBuilder
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*

fun main() {
    val testCases = listOf(
        "match x { case [] => 0 }",
        "match x { case [a] => a }",
        "match x { case [a, b] => a + b }",
        "match x { case [first, ...rest] => first }"
    )
    
    for (testCase in testCases) {
        println("Testing: $testCase")
        try {
            val input = CharStreams.fromString(testCase)
            val lexer = org.taylorlang.grammar.TaylorLangLexer(input)
            val tokens = CommonTokenStream(lexer)
            val parser = org.taylorlang.grammar.TaylorLangParser(tokens)
            
            val tree = parser.program()
            val builder = ASTBuilder()
            val ast = builder.visit(tree)
            println("✓ Parsed successfully: $ast")
        } catch (e: Exception) {
            println("✗ Parse failed: ${e.message}")
            e.printStackTrace()
        }
        println()
    }
}