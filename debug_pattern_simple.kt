// Simple debug test for double literal pattern matching
import org.taylorlang.ast.*
import org.taylorlang.codegen.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File

fun main() {
    println("Debug: Testing double literal pattern matching")
    
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = File.createTempFile("test", "dir").apply { 
        delete()
        mkdir()
    }
    
    // Create simple double literal match
    val program = Program(
        statements = persistentListOf(
            ExpressionStatement(
                MatchExpression(
                    target = Literal.FloatLiteral(3.14),
                    cases = persistentListOf(
                        MatchCase(
                            pattern = Pattern.LiteralPattern(Literal.FloatLiteral(3.14)),
                            expression = FunctionCall(
                                target = Identifier("println"),
                                arguments = persistentListOf(Literal.StringLiteral("pi case"))
                            )
                        ),
                        MatchCase(
                            pattern = Pattern.WildcardPattern,
                            expression = FunctionCall(
                                target = Identifier("println"),
                                arguments = persistentListOf(Literal.StringLiteral("other case"))
                            )
                        )
                    )
                )
            )
        )
    )
    
    println("Program: $program")
    
    val typedProgramResult = typeChecker.typeCheck(program)
    println("Type checking: ${typedProgramResult.isSuccess}")
    
    if (typedProgramResult.isSuccess) {
        val typedProgram = typedProgramResult.getOrThrow()
        val result = generator.generateBytecode(typedProgram, tempDir)
        println("Bytecode generation: ${result.isSuccess}")
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
    } else {
        typedProgramResult.exceptionOrNull()?.printStackTrace()
    }
    
    tempDir.deleteRecursively()
}