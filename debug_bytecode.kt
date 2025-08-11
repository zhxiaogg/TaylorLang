import org.taylorlang.ast.*
import org.taylorlang.codegen.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File

fun main() {
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    
    // Simple match expression: match 42 { 42 -> println("matched") }
    val matchExpr = MatchExpression(
        target = Literal.IntLiteral(42),
        cases = persistentListOf(
            MatchCase(
                pattern = Pattern.LiteralPattern(Literal.IntLiteral(42)),
                expression = FunctionCall(
                    target = Identifier("println"),
                    arguments = persistentListOf(Literal.StringLiteral("matched"))
                )
            )
        )
    )
    
    val program = Program(
        statements = persistentListOf(matchExpr),
        sourceLocation = null
    )
    
    println("=== AST ===")
    println(program)
    
    println("\n=== TYPE CHECKING ===")
    val typedResult = typeChecker.typeCheck(program)
    typedResult.fold(
        onSuccess = { typedProgram ->
            println("Type checking succeeded")
            println(typedProgram)
            
            println("\n=== BYTECODE GENERATION ===")
            val result = generator.generateBytecode(typedProgram, File("."))
            result.fold(
                onSuccess = { generationResult ->
                    println("Bytecode generation succeeded")
                    println("Class files: ${generationResult.bytecodeFiles}")
                },
                onFailure = { error ->
                    println("Bytecode generation failed: $error")
                    error.printStackTrace()
                }
            )
        },
        onFailure = { error ->
            println("Type checking failed: $error")
            error.printStackTrace()
        }
    )
}