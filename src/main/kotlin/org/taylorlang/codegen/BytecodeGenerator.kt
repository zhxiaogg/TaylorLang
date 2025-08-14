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
            
            // Try with full frame computation first, fall back to maxs only if it fails
            val bytecodeResult = try {
                generateWithFrames(typedProgram, outputDirectory, className)
            } catch (e: ArrayIndexOutOfBoundsException) {
                // Frame computation failed, try without frames
                generateWithoutFrames(typedProgram, outputDirectory, className)
            }
            
            Result.success(bytecodeResult)
        } catch (e: Exception) {
            // Print stack trace for debugging
            e.printStackTrace()
            Result.failure(Exception("Failed to generate bytecode: ${e.message}", e))
        }
    }
    
    private fun generateWithFrames(
        typedProgram: TypedProgram,
        outputDirectory: File,
        className: String
    ): GenerationResult {
        // Create a new ClassWriter for each generation to avoid reuse issues
        // Use full automatic computation to avoid slot management issues
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        
        generateClassContent(typedProgram, outputDirectory, className, classWriter)
        return writeClassFile(classWriter, outputDirectory, className)
    }
    
    private fun generateWithoutFrames(
        typedProgram: TypedProgram,
        outputDirectory: File,
        className: String
    ): GenerationResult {
        // Create a new ClassWriter without frame computation
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        
        generateClassContent(typedProgram, outputDirectory, className, classWriter)
        return writeClassFile(classWriter, outputDirectory, className)
    }
    
    private fun generateClassContent(
        typedProgram: TypedProgram,
        outputDirectory: File,
        className: String,
        classWriter: ClassWriter
    ) {
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
    }
    
    private fun writeClassFile(
        classWriter: ClassWriter,
        outputDirectory: File,
        className: String
    ): GenerationResult {
        // Write class file
        outputDirectory.mkdirs()
        val classFile = File(outputDirectory, "$className.class")
        FileOutputStream(classFile).use { fos ->
            fos.write(classWriter.toByteArray())
        }
        
        return GenerationResult(
            bytecodeFiles = listOf(classFile),
            mainClassName = className
        )
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
        
        // Set up try expression support with pattern compiler integration
        expressionGenerator.setPatternCompiler(patternCompiler)
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
                            // println and assert return void, so no need to pop
                            if (call.target is Identifier && (call.target.name == "println" || call.target.name == "assert")) {
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
                        // CRITICAL FIX: Handle double-width values properly when popping
                        if (getJvmType(statement.expression.type) == "D") {
                            methodVisitor!!.visitInsn(POP2) // Pop double value (2 slots)
                        } else {
                            methodVisitor!!.visitInsn(POP) // Pop single value
                        }
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
        // Let ASM compute maxs automatically with conservative hints
        methodVisitor!!.visitMaxs(0, 0) // ASM will compute automatically
        methodVisitor!!.visitEnd()
    }
    
    /**
     * Generate code for a statement
     */
    private fun generateStatement(statement: TypedStatement, classWriter: ClassWriter) {
        when (statement) {
            is TypedStatement.FunctionDeclaration -> {
                // For function declarations, directly generate the function using the class writer
                val funcDecl = statement.declaration
                val isMainFunction = funcDecl.name == "main"
                
                val descriptor = if (isMainFunction) {
                    "([Ljava/lang/String;)V"  // Main function always has this signature
                } else {
                    buildMethodDescriptor(funcDecl)
                }
                
                val access = ACC_PUBLIC + ACC_STATIC
                
                val methodVisitor = classWriter.visitMethod(
                    access,
                    funcDecl.name,
                    descriptor,
                    null,
                    null
                )
                methodVisitor.visitCode()
                
                // Set up proper generators for this method
                val functionSlotManager = VariableSlotManager()
                val functionExprGen = ExpressionBytecodeGenerator(methodVisitor, functionSlotManager) { expr ->
                    when (expr) {
                        is Literal.IntLiteral -> BuiltinTypes.INT
                        is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
                        is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
                        is Literal.StringLiteral -> BuiltinTypes.STRING
                        else -> BuiltinTypes.INT
                    }
                }
                val functionControlFlowGen = ControlFlowBytecodeGenerator(methodVisitor, functionExprGen) { expr ->
                    functionExprGen.generateExpression(expr)
                }
                
                // Generate function body
                when (val body = statement.body) {
                    is TypedFunctionBody.Expression -> {
                        // Generate the expression
                        generateFunctionExpression(body.expression, functionExprGen, functionControlFlowGen, methodVisitor)
                        
                        if (isMainFunction) {
                            // Main function should return void
                            methodVisitor.visitInsn(RETURN)
                        } else {
                            // Regular function returns the expression value
                            generateReturnInstruction(body.expression.type, methodVisitor)
                        }
                    }
                    is TypedFunctionBody.Block -> {
                        // Handle block body - not implemented in this fix
                        methodVisitor.visitInsn(RETURN)
                    }
                }
                
                methodVisitor.visitMaxs(10, 10) // Conservative estimates
                methodVisitor.visitEnd()
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
            is TryExpression -> {
                // Try expressions are handled by the expression generator
                expressionGenerator.generateExpression(expr)
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
     * Generate function expression
     */
    private fun generateFunctionExpression(
        expr: TypedExpression, 
        exprGen: ExpressionBytecodeGenerator, 
        controlFlowGen: ControlFlowBytecodeGenerator,
        methodVisitor: MethodVisitor
    ) {
        when (val expression = expr.expression) {
            is FunctionCall -> {
                val functionName = (expression.target as? Identifier)?.name
                when (functionName) {
                    "println" -> {
                        // Generate println call
                        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                        
                        if (expression.arguments.isNotEmpty()) {
                            val arg = expression.arguments[0]
                            val argType = exprGen.inferExpressionType(arg)
                            exprGen.generateExpression(TypedExpression(arg, argType))
                            
                            val methodDescriptor = when (argType) {
                                BuiltinTypes.INT -> "(I)V"
                                BuiltinTypes.DOUBLE -> "(D)V" 
                                BuiltinTypes.BOOLEAN -> {
                                    controlFlowGen.convertBooleanToString()
                                    "(Ljava/lang/String;)V"
                                }
                                BuiltinTypes.STRING -> "(Ljava/lang/String;)V"
                                else -> "(Ljava/lang/Object;)V"
                            }
                            
                            methodVisitor.visitMethodInsn(
                                INVOKEVIRTUAL,
                                "java/io/PrintStream", 
                                "println",
                                methodDescriptor,
                                false
                            )
                        }
                    }
                    "assert" -> {
                        // Generate assert call
                        if (expression.arguments.isEmpty()) {
                            // Invalid assert call - should have been caught by type checker
                            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                            methodVisitor.visitInsn(DUP)
                            methodVisitor.visitLdcInsn("assert() called without condition")
                            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false)
                            methodVisitor.visitInsn(ATHROW)
                        } else {
                            // Generate the condition expression
                            val conditionArg = expression.arguments[0]
                            val conditionType = exprGen.inferExpressionType(conditionArg)
                            exprGen.generateExpression(TypedExpression(conditionArg, conditionType))
                            
                            // Create a label for when assertion passes
                            val assertPassLabel = org.objectweb.asm.Label()
                            
                            // If condition is true (1), jump to pass label
                            methodVisitor.visitJumpInsn(IFNE, assertPassLabel)
                            
                            // Assertion failed - print error message to stderr and exit
                            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;")
                            methodVisitor.visitLdcInsn("Assertion failed")
                            methodVisitor.visitMethodInsn(
                                INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false
                            )
                            
                            // Exit with code 1
                            methodVisitor.visitLdcInsn(1)
                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                "java/lang/System",
                                "exit",
                                "(I)V",
                                false
                            )
                            
                            // Label for when assertion passes - continue execution
                            methodVisitor.visitLabel(assertPassLabel)
                        }
                    }
                    else -> {
                        // Other function calls - delegate to expression generator
                        exprGen.generateExpression(expr)
                    }
                }
            }
            is IfExpression -> {
                controlFlowGen.generateIfExpression(expression, expr.type)
            }
            is WhileExpression -> {
                controlFlowGen.generateWhileExpression(expression, expr.type)
            }
            else -> {
                exprGen.generateExpression(expr)
            }
        }
    }
    
    /**
     * Generate return instruction for given type
     */
    private fun generateReturnInstruction(type: Type, methodVisitor: MethodVisitor) {
        when (getJvmType(type)) {
            "I", "Z" -> methodVisitor.visitInsn(IRETURN)
            "D" -> methodVisitor.visitInsn(DRETURN)
            "Ljava/lang/String;" -> methodVisitor.visitInsn(ARETURN)
            "V" -> methodVisitor.visitInsn(RETURN)
            else -> methodVisitor.visitInsn(ARETURN)
        }
    }
    
    /**
     * Build method descriptor from function declaration
     */
    private fun buildMethodDescriptor(funcDecl: FunctionDecl): String {
        val paramTypes = funcDecl.parameters.map { param ->
            param.type?.let(::getJvmType) ?: "Ljava/lang/Object;"
        }
        val returnType = funcDecl.returnType?.let(::getJvmType) ?: "V"
        
        return "(${paramTypes.joinToString("")})$returnType"
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