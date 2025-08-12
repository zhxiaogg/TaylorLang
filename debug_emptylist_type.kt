import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import org.taylorlang.test.TestUtils

fun main() {
    println("=== Debug emptyList() type inference ===")
    
    // Create emptyList() function call
    val emptyListCall = TestUtils.createFunctionCall("emptyList", listOf())
    
    // Create expression statement
    val program = TestUtils.createProgram(listOf(
        TestUtils.createExpressionStatement(emptyListCall)
    ))
    
    // Type check
    val typeChecker = RefactoredTypeChecker(TypeContext.withBuiltins())
    val result = typeChecker.typeCheck(program)
    
    when (result) {
        is Result.Success -> {
            println("✓ Type checking succeeded")
            val typedProgram = result.getOrThrow()
            val typedStmt = typedProgram.statements[0] as TypedStatement.ExpressionStatement
            val typedExpr = typedStmt.expression
            println("Function call type: ${typedExpr.type}")
            
            // Check if it's a GenericType with arguments
            if (typedExpr.type is Type.GenericType) {
                val genericType = typedExpr.type as Type.GenericType
                println("Generic type name: ${genericType.name}")
                println("Type arguments: ${genericType.arguments}")
                println("Type arguments size: ${genericType.arguments.size}")
            } else {
                println("Type is not GenericType: ${typedExpr.type::class.simpleName}")
            }
        }
        is Result.Failure -> {
            println("✗ Type checking failed: ${result.exceptionOrNull()}")
        }
    }
}