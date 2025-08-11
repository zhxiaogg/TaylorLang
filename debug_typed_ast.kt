import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

fun main() {
    println("=== Typed AST Debug ===")
    
    // Create: while (false) { println("SHOULD NOT PRINT") }
    val whileExpression = WhileExpression(
        condition = Literal.BooleanLiteral(false),
        body = FunctionCall(
            target = Identifier("println"),
            arguments = persistentListOf(Literal.StringLiteral("SHOULD NOT PRINT"))
        )
    )
    
    val program = Program(persistentListOf(whileExpression))
    
    // Type check
    val typeChecker = RefactoredTypeChecker()
    val typedResult = typeChecker.typeCheck(program)
    
    if (typedResult.isSuccess) {
        val typedProgram = typedResult.getOrThrow()
        println("✓ Type checking succeeded")
        
        // Examine the typed statements
        println("Number of typed statements: ${typedProgram.statements.size}")
        
        typedProgram.statements.forEachIndexed { index, typedStatement ->
            println("Statement $index: ${typedStatement::class.simpleName}")
            when (typedStatement) {
                is TypedStatement.ExpressionStatement -> {
                    println("  Expression type: ${typedStatement.expression.type}")
                    println("  Expression AST: ${typedStatement.expression.expression::class.simpleName}")
                    
                    // If it's a WhileExpression, examine its parts
                    if (typedStatement.expression.expression is WhileExpression) {
                        val while = typedStatement.expression.expression as WhileExpression
                        println("  While condition: ${while.condition}")
                        println("  While body: ${while.body}")
                    }
                }
                else -> {
                    println("  Other statement: $typedStatement")
                }
            }
        }
    } else {
        println("✗ Type checking failed: ${typedResult.exceptionOrNull()}")
    }
}