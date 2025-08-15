package org.taylorlang.debug

import org.taylorlang.ast.*
import org.taylorlang.codegen.*
import org.taylorlang.typechecker.*
import kotlinx.collections.immutable.persistentListOf
import java.io.File

/**
 * Debug utility to generate and save the problematic bytecode for analysis
 */
fun main() {
    val bytecodeGenerator = BytecodeGenerator()
    val typeChecker = RefactoredTypeChecker(TypeCheckingMode.CONSTRAINT_BASED)
    val outputDir = File("debug_output")
    outputDir.mkdirs()
    
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
    
    // Generate bytecode
    val result = bytecodeGenerator.generateBytecode(typedProgram, outputDir, "OkExecutionTest")
    if (result.isFailure) {
        println("Bytecode generation failed: ${result.exceptionOrNull()}")
        return
    }
    
    println("Bytecode generated successfully in: ${outputDir.absolutePath}")
    println("Class file: ${File(outputDir, "OkExecutionTest.class").absolutePath}")
}