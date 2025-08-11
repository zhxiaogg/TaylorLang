import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.codegen.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.nio.file.Files

fun main() {
    println("=== Pattern Matching Debug Test ===")
    
    val typeChecker = RefactoredTypeChecker()
    val generator = BytecodeGenerator()
    val tempDir = Files.createTempDirectory("debug_pattern").toFile()
    
    try {
        // Create a simple pattern matching program
        val program = TestUtils.createProgram(listOf(
            TestUtils.createExpressionStatement(
                MatchExpression(
                    target = Literal.IntLiteral(42),
                    cases = persistentListOf(
                        MatchCase(
                            pattern = Pattern.LiteralPattern(Literal.IntLiteral(42)),
                            expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("matched 42")))
                        ),
                        MatchCase(
                            pattern = Pattern.WildcardPattern,
                            expression = TestUtils.createFunctionCall("println", listOf(Literal.StringLiteral("no match")))
                        )
                    )
                )
            )
        ))
        
        println("Program created successfully")
        println("AST: $program")
        
        // Test type checking
        println("\n=== Type Checking ===")
        val typedProgramResult = typeChecker.typeCheck(program)
        
        typedProgramResult.fold(
            onSuccess = { typedProgram ->
                println("✅ Type checking succeeded!")
                println("Typed program: $typedProgram")
                
                // Test bytecode generation
                println("\n=== Bytecode Generation ===")
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.fold(
                    onSuccess = { generationResult ->
                        println("✅ Bytecode generation succeeded!")
                        println("Generated class: ${generationResult.mainClassName}")
                        println("Bytecode files: ${generationResult.bytecodeFiles}")
                    },
                    onFailure = { error ->
                        println("❌ Bytecode generation failed: ${error.message}")
                        error.printStackTrace()
                    }
                )
            },
            onFailure = { error ->
                println("❌ Type checking failed: ${error.message}")
                error.printStackTrace()
            }
        )
        
    } catch (e: Exception) {
        println("❌ Exception occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        // Clean up
        tempDir.deleteRecursively()
    }
}