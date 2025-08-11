package org.taylorlang.codegen

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*
import java.io.File
import java.io.FileOutputStream

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
 * Main JVM bytecode generator coordinator for TaylorLang using ASM library.
 * 
 * This refactored implementation coordinates specialized generators:
 * - ExpressionBytecodeGenerator: literals, arithmetic, variables
 * - ControlFlowBytecodeGenerator: if/else, while loops, boolean ops
 * - PatternBytecodeCompiler: pattern matching, guards, variable binding
 * - FunctionBytecodeGenerator: function declarations, calls, parameters
 * 
 * Supports compilation of complete TaylorLang programs to executable JVM bytecode.
 */
class BytecodeGenerator {
    
    private var methodVisitor: MethodVisitor? = null
    private var currentClassName: String = "Program"
    private val variableSlotManager = VariableSlotManager()
    
    // Specialized generators (initialized on demand)
    private lateinit var expressionGenerator: ExpressionBytecodeGenerator
    private lateinit var controlFlowGenerator: ControlFlowBytecodeGenerator
    private lateinit var patternCompiler: PatternBytecodeCompiler
    private lateinit var functionGenerator: FunctionBytecodeGenerator
    
    /**
     * Generate bytecode from a typed program
     */
    fun generateBytecode(
        typedProgram: TypedProgram,
        outputDirectory: File = File("."),
        className: String = "Program"
    ): Result<GenerationResult> {
        return try {
            currentClassName = className
            
            // Create a new ClassWriter for each generation to avoid reuse issues
            val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            
            // Initialize class writer
            classWriter.visit(
                V17, // Java 17 bytecode version
                ACC_PUBLIC + ACC_SUPER,
                className,
                null,
                "java/lang/Object",
                null
            )
            
            // Generate default constructor
            generateConstructor(classWriter)
            
            // Check if program has a main function
            val hasMainFunction = typedProgram.statements.any { statement ->
                statement is TypedStatement.FunctionDeclaration && 
                statement.declaration.name == "main"
            }
            
            if (hasMainFunction) {
                // Generate statements as methods
                for (statement in typedProgram.statements) {
                    generateStatement(statement, classWriter)
                }
            } else {
                // Generate main method that executes all statements
                generateMainMethod(typedProgram.statements, classWriter)
            }
            
            classWriter.visitEnd()
            
            // Write class file
            outputDirectory.mkdirs()
            val classFile = File(outputDirectory, "$className.class")
            FileOutputStream(classFile).use { fos ->
                fos.write(classWriter.toByteArray())
            }
            
            Result.success(GenerationResult(
                bytecodeFiles = listOf(classFile),
                mainClassName = className
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate bytecode: ${e.message}", e))
        }
    }
    
    /**
     * Generate bytecode from untyped AST (will run type checker first)
     */
    fun generateBytecode(
        program: Program,
        outputDirectory: File = File(".")
    ): Result<GenerationResult> {
        // Run type checker first
        val typeChecker = RefactoredTypeChecker()
        return typeChecker.typeCheck(program)
            .mapCatching { typedProgram ->
                generateBytecode(typedProgram, outputDirectory).getOrThrow()
            }
    }
    
    /**
     * Initialize specialized generators for this method visitor
     */
    private fun initializeGenerators() {
        val mv = methodVisitor!!
        
        // Create expression generator first with proper type inference
        expressionGenerator = ExpressionBytecodeGenerator(mv, variableSlotManager) { expr ->
            when (expr) {
                is Literal.IntLiteral -> BuiltinTypes.INT
                is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
                is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
                is Literal.StringLiteral -> BuiltinTypes.STRING
                else -> BuiltinTypes.INT
            }
        }
        
        // Create other generators that depend on expression generator
        controlFlowGenerator = ControlFlowBytecodeGenerator(mv, expressionGenerator, ::generateExpression)
        patternCompiler = PatternBytecodeCompiler(mv, variableSlotManager, expressionGenerator, ::generateExpression)
        functionGenerator = FunctionBytecodeGenerator(currentClassName, variableSlotManager, 
            expressionGenerator, controlFlowGenerator, ::generateExpression)
        functionGenerator.setMethodVisitor(mv)
    }
    
    /**
     * Generate default constructor
     */
    private fun generateConstructor(classWriter: ClassWriter) {
        val mv = classWriter.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        mv.visitCode()
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv.visitInsn(RETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }
    
    /**
     * Generate main method that executes statements
     */
    private fun generateMainMethod(statements: List<TypedStatement>, classWriter: ClassWriter) {
        methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            "main",
            "([Ljava/lang/String;)V",
            null,
            null
        )
        methodVisitor!!.visitCode()
        
        // CRITICAL FIX: Static methods need different slot allocation
        // In static methods, slot 0 is for first parameter (String[] args), 
        // so variables start at slot 1. But the VariableSlotManager defaults to slot 1
        // thinking slot 0 is for 'this'. We need to reset it to account for static method.
        variableSlotManager.clear()
        // For static main method: slot 0 = String[] args, variables start at slot 1
        variableSlotManager.setStartingSlot(1)
        
        // Initialize specialized generators
        initializeGenerators()
        
        // Generate code for each statement
        for (statement in statements) {
            when (statement) {
                is TypedStatement.ExpressionStatement -> {
                    generateExpression(statement.expression)
                    // Pop result if it's not void - need to check if expression returns a value
                    val shouldPop = when (statement.expression.expression) {
                        is FunctionCall -> {
                            val call = statement.expression.expression as FunctionCall
                            // println returns void, so no need to pop
                            if (call.target is Identifier && call.target.name == "println") {
                                false
                            } else {
                                getJvmType(statement.expression.type) != "V"
                            }
                        }
                        is WhileExpression -> {
                            // While expressions return Unit/void, so no need to pop
                            false
                        }
                        else -> getJvmType(statement.expression.type) != "V"
                    }
                    
                    if (shouldPop) {
                        methodVisitor!!.visitInsn(POP)
                    }
                }
                is TypedStatement.FunctionDeclaration -> {
                    // Function declarations are handled separately
                }
                is TypedStatement.VariableDeclaration -> {
                    // Generate variable initialization value
                    generateExpression(statement.initializer)
                    
                    // Allocate slot and store variable
                    val slot = variableSlotManager.allocateSlot(
                        statement.declaration.name, 
                        statement.inferredType
                    )
                    val storeInstruction = variableSlotManager.getStoreInstruction(statement.inferredType)
                    methodVisitor!!.visitVarInsn(storeInstruction, slot)
                }
                is TypedStatement.MutableVariableDeclaration -> {
                    // Generate mutable variable initialization value
                    generateExpression(statement.initializer)
                    
                    // Allocate slot and store variable
                    val slot = variableSlotManager.allocateSlot(
                        statement.declaration.name, 
                        statement.inferredType
                    )
                    val storeInstruction = variableSlotManager.getStoreInstruction(statement.inferredType)
                    methodVisitor!!.visitVarInsn(storeInstruction, slot)
                }
                is TypedStatement.Assignment -> {
                    // Generate assignment value
                    generateExpression(statement.value)
                    
                    // Store in existing variable slot
                    val slot = variableSlotManager.getSlotOrThrow(statement.assignment.variable)
                    val storeInstruction = variableSlotManager.getStoreInstruction(statement.value.type)
                    methodVisitor!!.visitVarInsn(storeInstruction, slot)
                }
                is TypedStatement.ReturnStatement -> {
                    // Generate return statement
                    if (statement.expression != null) {
                        generateExpression(statement.expression)
                        functionGenerator.generateReturn(statement.expression.type, methodVisitor!!)
                    } else {
                        methodVisitor!!.visitInsn(RETURN)
                    }
                    // Return statements terminate execution, so break out of loop
                    break
                }
                is TypedStatement.TypeDeclaration -> {
                    // Type declarations don't generate runtime code
                }
            }
        }
        
        // Return normally from main method (no System.exit needed)
        methodVisitor!!.visitInsn(RETURN)
        methodVisitor!!.visitMaxs(10, variableSlotManager.getMaxSlots()) // Use actual slot count
        methodVisitor!!.visitEnd()
    }
    
    /**
     * Generate code for a statement
     */
    private fun generateStatement(statement: TypedStatement, classWriter: ClassWriter) {
        when (statement) {
            is TypedStatement.FunctionDeclaration -> {
                // Create dedicated generators for function scope
                val dummyMv = classWriter.visitMethod(ACC_PRIVATE, "dummy", "()V", null, null)
                val tempExprGen = ExpressionBytecodeGenerator(dummyMv, VariableSlotManager()) { BuiltinTypes.INT }
                val tempControlFlowGen = ControlFlowBytecodeGenerator(dummyMv, tempExprGen)
                
                val funcGenerator = FunctionBytecodeGenerator(
                    currentClassName, 
                    VariableSlotManager(), // New slot manager for function scope
                    tempExprGen,
                    tempControlFlowGen
                    // Don't pass callback - let function generator handle its own expression generation
                )
                funcGenerator.generateFunctionDeclaration(statement, classWriter)
            }
            is TypedStatement.ExpressionStatement -> {
                // Expression statements are handled in main method
            }
            is TypedStatement.VariableDeclaration -> {
                // Variable declarations are handled in main method
            }
            is TypedStatement.MutableVariableDeclaration -> {
                // Mutable variable declarations are handled in main method
            }
            is TypedStatement.Assignment -> {
                // Assignments are handled in main method
            }
            is TypedStatement.ReturnStatement -> {
                // Return statements are handled in main method
            }
            is TypedStatement.TypeDeclaration -> {
                // Type declarations don't generate runtime code
            }
        }
    }
    
    /**
     * Generate code for an expression (delegates to specialized generators)
     */
    private fun generateExpression(expr: TypedExpression) {
        // Ensure generators are initialized
        if (!::expressionGenerator.isInitialized) {
            initializeGenerators()
        }
        
        when (val expression = expr.expression) {
            is IfExpression -> {
                controlFlowGenerator.generateIfExpression(expression, expr.type)
            }
            is WhileExpression -> {
                controlFlowGenerator.generateWhileExpression(expression, expr.type)
            }
            is FunctionCall -> {
                functionGenerator.generateFunctionCall(expression, expr.type)
            }
            is MatchExpression -> {
                patternCompiler.generateMatchExpression(expression, expr.type)
            }
            else -> {
                // Delegate to expression generator for basic expressions
                expressionGenerator.generateExpression(expr)
            }
        }
    }
    
    /**
     * Map TaylorLang type to JVM type descriptor
     */
    private fun getJvmType(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit", "void" -> "V"
                    else -> "Ljava/lang/Object;"
                }
            }
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Check if bytecode generation is supported for the given program
     */
    fun isSupported(program: Program): Boolean {
        // Basic support for simple programs with literals and arithmetic
        return true
    }
    
    /**
     * Get list of features that are not yet supported
     */
    fun getUnsupportedFeatures(): List<String> {
        return listOf(
            "Complex pattern matching",
            "Lambda expressions", 
            "Union types runtime",
            "Generic type instantiation",
            "Module imports",
            "Full standard library"
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