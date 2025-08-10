package org.taylorlang.codegen

import org.taylorlang.ast.Program
import org.taylorlang.typechecker.TypedProgram
import java.io.File

/**
 * Error information for bytecode generation failures
 */
sealed class CodegenError {
    data class UnsupportedFeature(
        val feature: String,
        val message: String
    ) : CodegenError()
    
    data class GenerationFailure(
        val message: String,
        val cause: Throwable? = null
    ) : CodegenError()
}

/**
 * Result of bytecode generation
 */
data class GenerationResult(
    val bytecodeFiles: List<File>,
    val mainClassName: String?
)

/**
 * Minimal bytecode generator stub for TaylorLang
 * 
 * This is a placeholder implementation that provides the foundation
 * for future bytecode generation using ASM library.
 * 
 * TODO: Implement full bytecode generation
 * - Generate JVM bytecode from typed AST
 * - Handle function declarations and calls
 * - Support expressions and statements
 * - Generate proper class files
 */
class BytecodeGenerator {
    
    /**
     * Generate bytecode from a typed program
     * Currently returns an error indicating the feature is not implemented
     */
    fun generateBytecode(
        typedProgram: TypedProgram,
        outputDirectory: File = File(".")
    ): Result<GenerationResult> {
        return Result.failure(RuntimeException("""
            Bytecode generation is not yet implemented.
            
            The TypeChecker successfully validates the program structure and types,
            but code generation to JVM bytecode requires additional implementation.
            
            Next steps for implementation:
            1. Use ASM library to generate JVM bytecode
            2. Map TaylorLang types to JVM types
            3. Generate class files with proper method signatures
            4. Handle function calls and expressions
            
            For now, the compiler can parse and type-check programs successfully.
        """.trimIndent()))
    }
    
    /**
     * Generate bytecode from untyped AST (will run type checker first)
     * Currently returns an error indicating the feature is not implemented
     */
    fun generateBytecode(
        program: Program,
        outputDirectory: File = File(".")
    ): Result<GenerationResult> {
        // TODO: Run type checker first, then generate bytecode
        return Result.failure(RuntimeException("Bytecode generation requires type checking first. Use typed program version."))
    }
    
    /**
     * Check if bytecode generation is supported for the given program
     * Currently always returns false as generation is not implemented
     */
    fun isSupported(program: Program): Boolean {
        return false
    }
    
    /**
     * Get list of features that are not yet supported
     */
    fun getUnsupportedFeatures(): List<String> {
        return listOf(
            "JVM bytecode generation",
            "Class file creation",
            "Method generation",
            "Expression compilation",
            "Standard library integration",
            "Runtime system"
        )
    }
}

/**
 * Extension function to provide a simple interface for bytecode generation
 */
fun TypedProgram.generateBytecode(
    outputDirectory: File = File(".")
): Result<GenerationResult> {
    return BytecodeGenerator().generateBytecode(this, outputDirectory)
}