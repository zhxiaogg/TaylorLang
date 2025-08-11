@file:JvmName("DebugSimplePattern")

fun main() {
    try {
        println("Testing pattern matching setup...")
        
        // Test basic imports
        println("1. Testing imports...")
        val generator = org.taylorlang.codegen.BytecodeGenerator()
        val typeChecker = org.taylorlang.typechecker.RefactoredTypeChecker()
        println("✅ Imports successful")
        
        // Test temp directory creation
        println("2. Testing temp directory...")
        val tempDir = java.nio.file.Files.createTempDirectory("debug_pattern").toFile()
        println("✅ Temp directory created: ${tempDir.absolutePath}")
        
        // Test program creation 
        println("3. Testing program creation...")
        val program = org.taylorlang.TestUtils.createProgram(listOf(
            org.taylorlang.TestUtils.createExpressionStatement(
                org.taylorlang.ast.Literal.IntLiteral(42)
            )
        ))
        println("✅ Simple program created: $program")
        
        // Test type checking
        println("4. Testing type checking...")
        val typedResult = typeChecker.typeCheck(program)
        typedResult.fold(
            onSuccess = { println("✅ Type checking succeeded") },
            onFailure = { error -> 
                println("❌ Type checking failed: ${error.message}")
                throw error
            }
        )
        
        println("All basic tests passed! The issue is likely in the pattern matching AST construction.")
        
        // Now test pattern matching AST creation
        println("5. Testing pattern matching AST...")
        val matchExpr = org.taylorlang.ast.MatchExpression(
            target = org.taylorlang.ast.Literal.IntLiteral(42),
            cases = kotlinx.collections.immutable.persistentListOf(
                org.taylorlang.ast.MatchCase(
                    pattern = org.taylorlang.ast.Pattern.LiteralPattern(org.taylorlang.ast.Literal.IntLiteral(42)),
                    expression = org.taylorlang.TestUtils.createFunctionCall("println", listOf(org.taylorlang.ast.Literal.StringLiteral("matched 42")))
                )
            )
        )
        println("✅ MatchExpression created: $matchExpr")
        
        // Test pattern matching program
        println("6. Testing pattern matching program...")
        val patternProgram = org.taylorlang.TestUtils.createProgram(listOf(
            org.taylorlang.TestUtils.createExpressionStatement(matchExpr)
        ))
        println("✅ Pattern matching program created")
        
        // Test type checking pattern matching
        println("7. Testing pattern matching type checking...")
        val patternTypedResult = typeChecker.typeCheck(patternProgram)
        patternTypedResult.fold(
            onSuccess = { typedProgram ->
                println("✅ Pattern matching type checking succeeded")
                
                // Test bytecode generation
                println("8. Testing bytecode generation...")
                val result = generator.generateBytecode(typedProgram, tempDir)
                result.fold(
                    onSuccess = { generationResult ->
                        println("✅ Pattern matching bytecode generation succeeded!")
                        println("   Generated class: ${generationResult.mainClassName}")
                        println("   Files: ${generationResult.bytecodeFiles}")
                    },
                    onFailure = { error ->
                        println("❌ Pattern matching bytecode generation failed: ${error.message}")
                        if (error is Throwable) {
                            error.printStackTrace()
                        }
                    }
                )
            },
            onFailure = { error ->
                println("❌ Pattern matching type checking failed: ${error.message}")
                if (error is Throwable) {
                    error.printStackTrace()
                }
            }
        )
        
        // Clean up
        tempDir.deleteRecursively()
        
    } catch (e: Exception) {
        println("❌ Exception: ${e.message}")
        e.printStackTrace()
    }
}