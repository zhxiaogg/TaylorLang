// Minimal test to debug the double literal issue
import org.taylorlang.ast.*
import org.taylorlang.codegen.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

fun main() {
    println("=== DEBUG: Double Literal Pattern Matching Issue ===")
    
    val tempDir = Files.createTempDirectory("taylor_double_debug").toFile()
    
    try {
        // Simplest possible double literal case
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
                                    arguments = persistentListOf(Literal.StringLiteral("matched pi"))
                                )
                            )
                        )
                    )
                )
            )
        )
        
        println("Created program with double literal pattern matching")
        
        val typeChecker = RefactoredTypeChecker()
        val typedResult = typeChecker.typeCheck(program)
        
        if (typedResult.isFailure) {
            println("Type checking failed: ${typedResult.exceptionOrNull()?.message}")
            return
        }
        
        println("Type checking successful")
        val typedProgram = typedResult.getOrThrow()
        
        val generator = BytecodeGenerator()
        val bytecodeResult = generator.generateBytecode(typedProgram, tempDir)
        
        if (bytecodeResult.isFailure) {
            println("Bytecode generation failed: ${bytecodeResult.exceptionOrNull()?.message}")
            bytecodeResult.exceptionOrNull()?.printStackTrace()
            return
        }
        
        println("SUCCESS: Bytecode generation completed!")
        
    } catch (e: Exception) {
        println("Exception: ${e.message}")
        e.printStackTrace()
    } finally {
        tempDir.deleteRecursively()
    }
}