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
            
            // Try with full frame computation, with better error handling
            val bytecodeResult = try {
                generateWithFrames(typedProgram, outputDirectory, className)
            } catch (e: Exception) {
                // Frame computation failed - log the exact error and fall back
                println("Frame computation failed (${e.javaClass.simpleName}: ${e.message}), falling back to Java 1.6 format")
                e.printStackTrace()
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
        // For proper frame computation, we use COMPUTE_FRAMES which automatically
        // computes both stack frames and max stack/locals
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        
        generateClassContent(typedProgram, outputDirectory, className, classWriter, true)
        return writeClassFile(classWriter, outputDirectory, className)
    }
    
    private fun generateWithoutFrames(
        typedProgram: TypedProgram,
        outputDirectory: File,
        className: String
    ): GenerationResult {
        // Create a new ClassWriter without frame computation
        // Use manual stack/locals computation to avoid all frame issues
        val classWriter = ClassWriter(0) // No automatic computation
        
        generateClassContent(typedProgram, outputDirectory, className, classWriter, false)
        return writeClassFile(classWriter, outputDirectory, className)
    }
    
    private fun generateClassContent(
        typedProgram: TypedProgram,
        outputDirectory: File,
        className: String,
        classWriter: ClassWriter,
        useFrames: Boolean = true
    ) {
        // Initialize class writer
        // Use appropriate bytecode version based on frame computation capability
        val bytecodeVersion = if (useFrames) {
            V1_8 // Use Java 1.8 with frames (more compatible than V17)
        } else {
            V1_7 // Use Java 1.7 for fallback - supports manual stack computation
        }
        
        classWriter.visit(
            bytecodeVersion,
            ACC_PUBLIC + ACC_SUPER,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Generate default constructor
        generateConstructor(classWriter)
        
        // Generate assertion helper method
        generateAssertionHelper(classWriter)
        generateSimpleAssertHelper(classWriter)
        
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
        expressionGenerator = ExpressionBytecodeGenerator(mv, variableSlotManager, { expr ->
            when (expr) {
                is Literal.IntLiteral -> BuiltinTypes.INT
                is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
                is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
                is Literal.StringLiteral -> BuiltinTypes.STRING
                else -> BuiltinTypes.INT
            }
        }, currentClassName)
        
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
     * Generate static helper method for assertion checking
     * Uses runtime assertion to avoid frame computation issues
     */
    private fun generateAssertionHelper(classWriter: ClassWriter) {
        val mv = classWriter.visitMethod(
            ACC_PRIVATE + ACC_STATIC,
            "checkAssertion",
            "(Z)V",
            null,
            null
        )
        mv.visitCode()
        
        // Load the boolean parameter
        mv.visitVarInsn(ILOAD, 0)
        
        // Convert to Boolean object for easier handling
        mv.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/Boolean",
            "valueOf",
            "(Z)Ljava/lang/Boolean;",
            false
        )
        
        // Call Boolean.booleanValue() to get primitive back
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/Boolean",
            "booleanValue",
            "()Z",
            false
        )
        
        // Use Java's built-in assert mechanism (no conditional jumps needed)
        // This is equivalent to: if (!condition) throw new AssertionError()
        mv.visitLdcInsn("Assertion failed")
        mv.visitMethodInsn(
            INVOKESTATIC,
            currentClassName,
            "assertHelper",
            "(ZLjava/lang/String;)V",
            false
        )
        
        mv.visitInsn(RETURN)
        mv.visitMaxs(2, 1)
        mv.visitEnd()
    }
    
    /**
     * Generate a simple assertion helper that uses array access to trigger errors
     * This avoids conditional jumps entirely
     */
    private fun generateSimpleAssertHelper(classWriter: ClassWriter) {
        val mv = classWriter.visitMethod(
            ACC_PRIVATE + ACC_STATIC,
            "assertHelper",
            "(ZLjava/lang/String;)V",
            null,
            null
        )
        mv.visitCode()
        
        // Create a clever trick: use boolean as array index
        // true = 1, false = 0
        // We'll create array [null, "ok"] and access arr[boolValue]
        // If false (0), we get null and cause NPE
        // If true (1), we get "ok" and continue
        
        // Create array with 2 elements: [null, "ok"]
        mv.visitLdcInsn(2)
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String")
        mv.visitInsn(DUP)
        mv.visitLdcInsn(1)
        mv.visitLdcInsn("ok")
        mv.visitInsn(AASTORE)
        
        // Load boolean parameter and use as index
        mv.visitVarInsn(ILOAD, 0)
        mv.visitInsn(AALOAD) // This will be null if false, "ok" if true
        
        // Call length on the result - will NPE if null (assertion failed)
        // If "ok", will return 2 and continue
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/String",
            "length",
            "()I",
            false
        )
        mv.visitInsn(POP) // Discard the length result
        
        mv.visitInsn(RETURN)
        mv.visitMaxs(4, 2)
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
        // Slot 0 is for the args parameter in static main method
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
        // Use conservative estimates for max stack and locals to avoid ASM computation issues
        val maxStack = 10 // Conservative estimate for complex expressions
        val maxLocals = variableSlotManager.getMaxSlots().coerceAtLeast(50) // Ensure enough slots
        methodVisitor!!.visitMaxs(maxStack, maxLocals)
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
                val functionExprGen = ExpressionBytecodeGenerator(methodVisitor, functionSlotManager, { expr ->
                    when (expr) {
                        is Literal.IntLiteral -> BuiltinTypes.INT
                        is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
                        is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
                        is Literal.StringLiteral -> BuiltinTypes.STRING
                        else -> BuiltinTypes.INT
                    }
                }, currentClassName)
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
            is BlockExpression -> {
                // Generate all statements in the block
                for (statement in expression.statements) {
                    generateStatementInBlock(statement)
                }
                
                // Generate final expression (or default value)
                if (expression.expression != null) {
                    val finalExprType = inferExpressionType(expression.expression)
                    generateExpression(TypedExpression(expression.expression, finalExprType))
                } else {
                    // No final expression - push Unit (represented as 0)
                    methodVisitor!!.visitInsn(ICONST_0)
                }
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
     * Generate code for a statement in a block expression
     */
    private fun generateStatementInBlock(statement: Statement) {
        when (statement) {
            is Assignment -> {
                // Generate assignment value
                val assignExprType = inferExpressionType(statement.value)
                generateExpression(TypedExpression(statement.value, assignExprType))
                
                // Store in existing variable slot
                val slot = variableSlotManager.getSlotOrThrow(statement.variable)
                val storeInstruction = variableSlotManager.getStoreInstruction(assignExprType)
                methodVisitor!!.visitVarInsn(storeInstruction, slot)
            }
            is Expression -> {
                // Generate expression and pop its result (since we're in a block)
                val exprType = inferExpressionType(statement)
                generateExpression(TypedExpression(statement, exprType))
                
                // Pop result unless it's void
                if (getJvmType(exprType) != "V") {
                    methodVisitor!!.visitInsn(POP)
                }
            }
            else -> {
                // Other statement types not currently supported in block expressions
                // This includes variable declarations, which would need special handling
            }
        }
    }
    
    /**
     * Infer the type of an expression (helper method)
     */
    private fun inferExpressionType(expr: Expression): Type {
        return when (expr) {
            is Literal.IntLiteral -> BuiltinTypes.INT
            is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
            is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
            is Literal.StringLiteral -> BuiltinTypes.STRING
            is FunctionCall -> {
                // Check if it's a known void function
                when {
                    expr.target is Identifier && (expr.target as Identifier).name in setOf("println", "print", "assert") -> {
                        Type.PrimitiveType("Unit") // Void functions
                    }
                    else -> BuiltinTypes.INT // Default for unknown functions
                }
            }
            is BinaryOp -> {
                when (expr.operator) {
                    BinaryOperator.PLUS, BinaryOperator.MINUS, 
                    BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE -> BuiltinTypes.INT
                    BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
                    BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
                    BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> BuiltinTypes.BOOLEAN
                    else -> BuiltinTypes.INT
                }
            }
            else -> BuiltinTypes.INT // Default fallback
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
                    "unit", "void" -> "V"
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
                            
                            // Completely avoid conditional jumps by using a runtime assertion 
                            // that doesn't need frame computation
                            // Convert boolean to string and use string comparison
                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                "java/lang/String",
                                "valueOf",
                                "(Z)Ljava/lang/String;",
                                false
                            )
                            
                            // Compare with "true" - if not equal, it's an assertion failure
                            methodVisitor.visitLdcInsn("true")
                            methodVisitor.visitMethodInsn(
                                INVOKEVIRTUAL,
                                "java/lang/String",
                                "equals",
                                "(Ljava/lang/Object;)Z",
                                false
                            )
                            
                            // Now use the assertion helper which has simpler logic
                            methodVisitor.visitLdcInsn("Assertion failed")
                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                currentClassName,
                                "assertHelper",
                                "(ZLjava/lang/String;)V",
                                false
                            )
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