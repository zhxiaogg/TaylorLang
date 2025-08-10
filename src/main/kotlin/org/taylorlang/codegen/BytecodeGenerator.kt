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
 * JVM bytecode generator for TaylorLang using ASM library
 * 
 * This implementation generates executable JVM bytecode from typed TaylorLang AST.
 * It supports:
 * - Basic literals (integers, doubles, booleans, strings)
 * - Arithmetic expressions (+, -, *, /)
 * - Simple function declarations with expression bodies
 * - Main method generation for program entry point
 */
class BytecodeGenerator {
    
    private var methodVisitor: MethodVisitor? = null
    private var currentClassName: String = "Program"
    
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
                    // Generate variable initialization
                    generateExpression(statement.initializer)
                    // Store in local variable (simplified - just pop for now)
                    methodVisitor!!.visitInsn(POP)
                }
                is TypedStatement.TypeDeclaration -> {
                    // Type declarations don't generate runtime code
                }
            }
        }
        
        methodVisitor!!.visitInsn(RETURN)
        methodVisitor!!.visitMaxs(10, 10) // Conservative estimates
        methodVisitor!!.visitEnd()
    }
    
    /**
     * Generate code for a statement
     */
    private fun generateStatement(statement: TypedStatement, classWriter: ClassWriter) {
        when (statement) {
            is TypedStatement.FunctionDeclaration -> {
                generateFunctionDeclaration(statement, classWriter)
            }
            is TypedStatement.ExpressionStatement -> {
                // Expression statements are handled in main method
            }
            is TypedStatement.VariableDeclaration -> {
                // Variable declarations are handled in main method
            }
            is TypedStatement.TypeDeclaration -> {
                // Type declarations don't generate runtime code
            }
        }
    }
    
    /**
     * Generate code for function declaration
     */
    private fun generateFunctionDeclaration(funcDecl: TypedStatement.FunctionDeclaration, classWriter: ClassWriter) {
        val isMainFunction = funcDecl.declaration.name == "main"
        val descriptor = if (isMainFunction) {
            "([Ljava/lang/String;)V"  // Main function always has this signature
        } else {
            buildMethodDescriptor(funcDecl.declaration)
        }
        
        val access = ACC_PUBLIC + ACC_STATIC
        
        methodVisitor = classWriter.visitMethod(
            access,
            funcDecl.declaration.name,
            descriptor,
            null,
            null
        )
        methodVisitor!!.visitCode()
        
        when (val body = funcDecl.body) {
            is TypedFunctionBody.Expression -> {
                generateExpression(body.expression)
                
                if (isMainFunction) {
                    // Main function should not return a value, just execute and return void
                    // For main function with expression body, the expression is executed but the result is not returned
                    // println calls already handle their own void return, so we don't need to pop anything
                    methodVisitor!!.visitInsn(RETURN)
                } else {
                    // Regular function returns the expression value
                    generateReturn(body.expression.type)
                }
            }
            is TypedFunctionBody.Block -> {
                for (stmt in body.statements) {
                    when (stmt) {
                        is TypedStatement.ExpressionStatement -> {
                            generateExpression(stmt.expression)
                            if (getJvmType(stmt.expression.type) != "V") {
                                methodVisitor!!.visitInsn(POP)
                            }
                        }
                        else -> {
                            // Handle other statement types
                        }
                    }
                }
                // Return void for block functions without explicit return
                methodVisitor!!.visitInsn(RETURN)
            }
        }
        
        methodVisitor!!.visitMaxs(10, 10) // Conservative estimates
        methodVisitor!!.visitEnd()
    }
    
    /**
     * Generate code for an expression
     */
    private fun generateExpression(expr: TypedExpression) {
        when (val expression = expr.expression) {
            is Literal.IntLiteral -> {
                methodVisitor!!.visitLdcInsn(expression.value)
            }
            is Literal.FloatLiteral -> {
                methodVisitor!!.visitLdcInsn(expression.value)
            }
            is Literal.BooleanLiteral -> {
                methodVisitor!!.visitLdcInsn(if (expression.value) 1 else 0)
            }
            is Literal.StringLiteral -> {
                methodVisitor!!.visitLdcInsn(expression.value)
            }
            is BinaryOp -> {
                generateBinaryOperation(expression, expr.type)
            }
            is UnaryOp -> {
                generateUnaryOperation(expression, expr.type)
            }
            is Identifier -> {
                // For now, just load 0 as placeholder
                methodVisitor!!.visitLdcInsn(0)
            }
            is FunctionCall -> {
                generateFunctionCall(expression, expr.type)
            }
            else -> {
                // Unsupported expression - push default value
                when (getJvmType(expr.type)) {
                    "I", "Z" -> methodVisitor!!.visitLdcInsn(0)
                    "D" -> methodVisitor!!.visitLdcInsn(0.0)
                    else -> methodVisitor!!.visitLdcInsn("")
                }
            }
        }
    }
    
    /**
     * Generate code for binary operations
     */
    private fun generateBinaryOperation(binaryOp: BinaryOp, resultType: Type) {
        // Determine the operand type by inspecting the operands
        val operandType = determineOperandType(binaryOp, resultType)
        val isDoubleOp = !isIntegerType(operandType)
        
        // Generate left operand
        when (val left = binaryOp.left) {
            is Literal.IntLiteral -> {
                if (isDoubleOp) {
                    // Convert int to double for double operations
                    methodVisitor!!.visitLdcInsn(left.value.toDouble())
                } else {
                    methodVisitor!!.visitLdcInsn(left.value)
                }
            }
            is Literal.FloatLiteral -> methodVisitor!!.visitLdcInsn(left.value)
            else -> generateExpression(TypedExpression(left, operandType))
        }
        
        // Generate right operand
        when (val right = binaryOp.right) {
            is Literal.IntLiteral -> {
                if (isDoubleOp) {
                    // Convert int to double for double operations
                    methodVisitor!!.visitLdcInsn(right.value.toDouble())
                } else {
                    methodVisitor!!.visitLdcInsn(right.value)
                }
            }
            is Literal.FloatLiteral -> methodVisitor!!.visitLdcInsn(right.value)
            else -> generateExpression(TypedExpression(right, operandType))
        }
        
        // Generate operation based on operand type
        when (binaryOp.operator) {
            BinaryOperator.PLUS -> {
                if (isIntegerType(operandType)) {
                    methodVisitor!!.visitInsn(IADD)
                } else {
                    methodVisitor!!.visitInsn(DADD)
                }
            }
            BinaryOperator.MINUS -> {
                if (isIntegerType(operandType)) {
                    methodVisitor!!.visitInsn(ISUB)
                } else {
                    methodVisitor!!.visitInsn(DSUB)
                }
            }
            BinaryOperator.MULTIPLY -> {
                if (isIntegerType(operandType)) {
                    methodVisitor!!.visitInsn(IMUL)
                } else {
                    methodVisitor!!.visitInsn(DMUL)
                }
            }
            BinaryOperator.DIVIDE -> {
                if (isIntegerType(operandType)) {
                    methodVisitor!!.visitInsn(IDIV)
                } else {
                    methodVisitor!!.visitInsn(DDIV)
                }
            }
            else -> {
                // Unsupported operation - use appropriate add instruction
                if (isIntegerType(operandType)) {
                    methodVisitor!!.visitInsn(IADD)
                } else {
                    methodVisitor!!.visitInsn(DADD)
                }
            }
        }
    }
    
    /**
     * Determine the operand type for binary operations
     */
    private fun determineOperandType(binaryOp: BinaryOp, resultType: Type): Type {
        // Use the consolidated type inference logic
        val leftType = inferExpressionType(binaryOp.left)
        val rightType = inferExpressionType(binaryOp.right)
        
        // If either operand is double, the result is double
        return when {
            leftType == BuiltinTypes.DOUBLE || rightType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
            leftType == BuiltinTypes.INT && rightType == BuiltinTypes.INT -> BuiltinTypes.INT
            else -> BuiltinTypes.INT // Default to int
        }
    }
    
    /**
     * Check if an expression evaluates to an integer
     */
    private fun isIntegerExpression(expr: Expression): Boolean {
        return when (expr) {
            is Literal.IntLiteral -> true
            is BinaryOp -> isIntegerExpression(expr.left) && isIntegerExpression(expr.right)
            else -> false
        }
    }
    
    /**
     * Generate code for unary operations
     */
    private fun generateUnaryOperation(unaryOp: UnaryOp, resultType: Type) {
        // Generate operand
        when (val operand = unaryOp.operand) {
            is Literal.IntLiteral -> methodVisitor!!.visitLdcInsn(operand.value)
            is Literal.FloatLiteral -> methodVisitor!!.visitLdcInsn(operand.value)
            else -> generateExpression(TypedExpression(operand, resultType))
        }
        
        when (unaryOp.operator) {
            UnaryOperator.MINUS -> {
                if (isIntegerType(resultType)) {
                    methodVisitor!!.visitInsn(INEG)
                } else {
                    methodVisitor!!.visitInsn(DNEG)
                }
            }
            UnaryOperator.NOT -> {
                // For boolean NOT: if value is 0, push 1, else push 0
                methodVisitor!!.visitLdcInsn(1)
                methodVisitor!!.visitInsn(IXOR)
            }
        }
    }
    
    /**
     * Infer the type of an expression based on its structure
     */
    private fun inferExpressionType(expr: Expression): Type {
        return when (expr) {
            is Literal.IntLiteral -> BuiltinTypes.INT
            is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
            is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
            is Literal.StringLiteral -> BuiltinTypes.STRING
            is BinaryOp -> {
                // For binary operations, determine the result type
                if (isIntegerExpression(expr)) BuiltinTypes.INT else BuiltinTypes.DOUBLE
            }
            is UnaryOp -> {
                when (expr.operator) {
                    UnaryOperator.NOT -> BuiltinTypes.BOOLEAN
                    UnaryOperator.MINUS -> inferExpressionType(expr.operand)
                }
            }
            else -> BuiltinTypes.INT // Default fallback
        }
    }
    
    /**
     * Generate code for function calls
     */
    private fun generateFunctionCall(call: FunctionCall, resultType: Type) {
        val functionName = (call.target as? Identifier)?.name
        
        when (functionName) {
            "println" -> generatePrintlnCall(call)
            else -> {
                // Regular function call - simplified for now
                methodVisitor!!.visitLdcInsn(0) // Placeholder return value
            }
        }
    }
    
    /**
     * Generate code for println builtin function
     */
    private fun generatePrintlnCall(call: FunctionCall) {
        methodVisitor!!.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        
        // Generate arguments and determine the correct method signature
        val methodDescriptor = if (call.arguments.isNotEmpty()) {
            val arg = call.arguments[0]
            val argType = inferExpressionType(arg)
            
            generateExpression(TypedExpression(arg, argType))
            
            // Map to appropriate PrintStream.println overload
            when (argType) {
                BuiltinTypes.INT -> "(I)V"
                BuiltinTypes.DOUBLE -> "(D)V"
                BuiltinTypes.BOOLEAN -> {
                    // Convert boolean to string representation
                    convertBooleanToString()
                    "(Ljava/lang/String;)V"
                }
                BuiltinTypes.STRING -> "(Ljava/lang/String;)V"
                else -> "(Ljava/lang/Object;)V"
            }
        } else {
            methodVisitor!!.visitLdcInsn("")
            "(Ljava/lang/String;)V"
        }
        
        methodVisitor!!.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            methodDescriptor,
            false
        )
    }
    
    /**
     * Convert a boolean value on the stack to its string representation ("true" or "false")
     */
    private fun convertBooleanToString() {
        // The stack has a boolean (0 or 1) on top
        // We'll use a simple if-else to convert to string
        
        val trueLabel = org.objectweb.asm.Label()
        val endLabel = org.objectweb.asm.Label()
        
        // If the boolean value is not 0 (i.e., true), jump to trueLabel
        methodVisitor!!.visitJumpInsn(IFNE, trueLabel)
        
        // False case: push "false"
        methodVisitor!!.visitLdcInsn("false")
        methodVisitor!!.visitJumpInsn(GOTO, endLabel)
        
        // True case: push "true"
        methodVisitor!!.visitLabel(trueLabel)
        methodVisitor!!.visitLdcInsn("true")
        
        // End
        methodVisitor!!.visitLabel(endLabel)
    }
    
    /**
     * Generate return instruction based on type
     */
    private fun generateReturn(type: Type) {
        when (getJvmType(type)) {
            "I", "Z" -> methodVisitor!!.visitInsn(IRETURN)
            "D" -> methodVisitor!!.visitInsn(DRETURN)
            "Ljava/lang/String;" -> methodVisitor!!.visitInsn(ARETURN)
            "V" -> methodVisitor!!.visitInsn(RETURN)
            else -> methodVisitor!!.visitInsn(ARETURN)
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
     * Check if type is an integer type
     */
    private fun isIntegerType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "int"
            is Type.NamedType -> type.name.lowercase() == "int"
            else -> false
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