import org.taylorlang.codegen.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.TestUtils
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

fun main() {
    val generator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker()
    val tempDir = Files.createTempDirectory("taylor_pattern_debug").toFile()
    
    try {
        println("Creating simple pattern matching program...")
        
        // Create the simplest possible pattern matching program
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
        
        println("Program created: $program")
        
        // Type check
        println("Starting type checking...")
        val typedProgramResult = typeChecker.typeCheck(program)
        typedProgramResult.fold(
            onSuccess = { typedProgram ->
                println("Type checking SUCCESS")
                
                // Generate bytecode
                println("Starting bytecode generation...")
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.fold(
                    onSuccess = { generationResult ->
                        println("Bytecode generation SUCCESS")
                        println("Main class name: ${generationResult.mainClassName}")
                        println("Bytecode files: ${generationResult.bytecodeFiles}")
                        
                        // Try to load and execute
                        println("Loading class...")
                        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                        val clazz = classLoader.loadClass(generationResult.mainClassName!!)
                        val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
                        
                        println("About to invoke main method...")
                        mainMethod.invoke(null, arrayOf<String>())
                        println("Main method executed successfully!")
                    },
                    onFailure = { error ->
                        println("Bytecode generation FAILED: ${error.message}")
                        error.printStackTrace()
                    }
                )
            },
            onFailure = { error ->
                println("Type checking FAILED: ${error.message}")
                error.printStackTrace()
            }
        )
        
    } catch (e: Exception) {
        println("Exception occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        // Clean up
        tempDir.deleteRecursively()
    }
}