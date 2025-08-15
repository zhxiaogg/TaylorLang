// Debug script to check what type the test expression has
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf

fun main() {
    val typeChecker = RefactoredTypeChecker(TypeCheckingMode.CONSTRAINT_BASED)
    
    // Create the same program structure as the failing test
    val okFunction = FunctionDecl(
        name = "getOkValue",
        parameters = persistentListOf(),
        returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE),
        body = FunctionBody.ExpressionBody(
            FunctionCall(
                target = Identifier("TaylorResult.ok"),
                arguments = persistentListOf(Literal.IntLiteral(42))
            )
        )
    )
    
    // Create main function that uses try expression
    val tryExpr = TryExpression(
        expression = FunctionCall(
            target = Identifier("getOkValue"),
            arguments = persistentListOf()
        ),
        catchClauses = persistentListOf()
    )
    
    // Wrap the try expression result in TaylorResult.ok()
    val wrappedResult = FunctionCall(
        target = Identifier("TaylorResult.ok"),
        arguments = persistentListOf(tryExpr)
    )
    
    val mainFunction = FunctionDecl(
        name = "main",
        parameters = persistentListOf(
            Parameter("args", BuiltinTypes.STRING)
        ),
        returnType = BuiltinTypes.createResultType(BuiltinTypes.INT, BuiltinTypes.THROWABLE),
        body = FunctionBody.ExpressionBody(wrappedResult)
    )
    
    val program = Program(persistentListOf(okFunction, mainFunction))
    
    // Type check
    val typedProgramResult = typeChecker.typeCheck(program)
    if (typedProgramResult.isFailure) {
        println("Type checking failed: ${typedProgramResult.exceptionOrNull()}")
        return
    }
    
    val typedProgram = typedProgramResult.getOrThrow()
    
    // Find the main function body and print its type
    val mainFunctionTyped = typedProgram.statements.find { stmt ->
        stmt is TypedStatement.FunctionDeclaration && stmt.declaration.name == "main"
    } as? TypedStatement.FunctionDeclaration
    
    if (mainFunctionTyped != null) {
        val body = mainFunctionTyped.body
        if (body is TypedFunctionBody.Expression) {
            println("Main function body expression type: ${body.expression.type}")
            println("Main function body expression: ${body.expression.expression}")
        }
    }
}