package org.taylorlang.codegen

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for function operations.
 * 
 * Handles compilation of:
 * - Function declarations with proper method descriptors
 * - Function calls (both user-defined and builtin)
 * - Parameter passing and return values
 * - Method setup and parameter slot management
 */
class FunctionBytecodeGenerator(
    private val currentClassName: String,
    private val variableSlotManager: VariableSlotManager,
    private val expressionGenerator: ExpressionBytecodeGenerator,
    private val controlFlowGenerator: ControlFlowBytecodeGenerator,
    private val generateExpressionCallback: ((TypedExpression) -> Unit)? = null
) {
    
    private lateinit var methodVisitor: MethodVisitor
    
    fun setMethodVisitor(visitor: MethodVisitor) {
        this.methodVisitor = visitor
    }
    
    /**
     * Generate code for function declaration
     */
    fun generateFunctionDeclaration(
        funcDecl: TypedStatement.FunctionDeclaration, 
        classWriter: ClassWriter
    ): MethodVisitor {
        val isMainFunction = funcDecl.declaration.name == "main"
        val descriptor = if (isMainFunction) {
            "([Ljava/lang/String;)V"  // Main function always has this signature
        } else {
            buildMethodDescriptor(funcDecl.declaration)
        }
        
        
        val access = ACC_PUBLIC + ACC_STATIC
        
        val methodVisitor = classWriter.visitMethod(
            access,
            funcDecl.declaration.name,
            descriptor,
            null,
            null
        )
        methodVisitor.visitCode()
        
        // CRITICAL FIX: Set the method visitor for this function so all method calls use the correct visitor
        this.methodVisitor = methodVisitor
        
        // CRITICAL FIX: Create new generators with the correct method visitor for this specific function
        val localExpressionGenerator = ExpressionBytecodeGenerator(methodVisitor, variableSlotManager, expressionGenerator::inferExpressionType)
        val localControlFlowGenerator = ControlFlowBytecodeGenerator(methodVisitor, localExpressionGenerator) { expr ->
            // Use local expression generator for any expressions needed by control flow
            localExpressionGenerator.generateExpression(expr)
        }
        
        // Set up parameter slots for user-defined functions (not main)
        if (!isMainFunction) {
            setupFunctionParameterSlots(funcDecl.declaration)
        }
        
        when (val body = funcDecl.body) {
            is TypedFunctionBody.Expression -> {
                // Generate the expression using the local generators with correct method visitor
                generateExpressionWithLocalGenerators(body.expression, localExpressionGenerator, localControlFlowGenerator)
                
                if (isMainFunction) {
                    // Main function should not return a value, just execute and return void
                    // For main functions, we don't need to worry about return values on the stack
                    // The println() call already handles its own stack management and returns void
                    
                    // Check if this is a function call that returns void
                    val isVoidExpression = when (val expr = body.expression.expression) {
                        is FunctionCall -> {
                            val functionName = (expr.target as? Identifier)?.name
                            functionName == "println" || functionName == "assert" // println and assert return void
                        }
                        else -> false
                    }
                    
                    if (!isVoidExpression) {
                        // Only pop if the expression actually leaves something on the stack
                        val exprType = getJvmType(body.expression.type)
                        if (exprType != "V") {
                            methodVisitor.visitInsn(POP)
                        }
                    }
                    // Return normally from main method (no System.exit needed)
                    methodVisitor.visitInsn(RETURN)
                } else {
                    // Regular function returns the expression value
                    generateReturn(body.expression.type, methodVisitor)
                }
            }
            is TypedFunctionBody.Block -> {
                for (stmt in body.statements) {
                    when (stmt) {
                        is TypedStatement.ExpressionStatement -> {
                            // Generate statement expression using local generators with correct method visitor
                            generateExpressionWithLocalGenerators(stmt.expression, localExpressionGenerator, localControlFlowGenerator)
                            if (getJvmType(stmt.expression.type) != "V") {
                                methodVisitor.visitInsn(POP)
                            }
                        }
                        else -> {
                            // Handle other statement types
                        }
                    }
                }
                // Return void for block functions without explicit return
                methodVisitor.visitInsn(RETURN)
            }
        }
        
        methodVisitor.visitMaxs(10, 10) // Conservative estimates
        methodVisitor.visitEnd()
        
        return methodVisitor
    }
    
    /**
     * Generate code for function calls
     */
    fun generateFunctionCall(call: FunctionCall, resultType: Type) {
        val functionName = (call.target as? Identifier)?.name
        
        when (functionName) {
            "println" -> generatePrintlnCall(call)
            "assert" -> generateAssertCall(call)
            // CRITICAL FIX: Handle constructor calls that are parsed as function calls
            "Ok", "Error" -> {
                // Convert function call to constructor call for union type constructors
                val constructorCall = ConstructorCall(functionName, call.arguments, call.sourceLocation)
                expressionGenerator.generateExpression(TypedExpression(constructorCall, resultType))
            }
            "Some", "None" -> {
                // Option type constructors
                val constructorCall = ConstructorCall(functionName, call.arguments, call.sourceLocation)
                expressionGenerator.generateExpression(TypedExpression(constructorCall, resultType))
            }
            "Active", "Inactive", "Pending" -> {
                // Status type constructors
                val constructorCall = ConstructorCall(functionName, call.arguments, call.sourceLocation)
                expressionGenerator.generateExpression(TypedExpression(constructorCall, resultType))
            }
            "Pair" -> {
                // Tuple2 constructor
                val constructorCall = ConstructorCall(functionName, call.arguments, call.sourceLocation)
                expressionGenerator.generateExpression(TypedExpression(constructorCall, resultType))
            }
            else -> {
                // User-defined function call
                generateUserFunctionCall(call, functionName, resultType)
            }
        }
    }
    
    /**
     * Generate code for user-defined function calls
     */
    private fun generateUserFunctionCall(call: FunctionCall, functionName: String?, resultType: Type) {
        if (functionName == null) {
            // Complex function expressions not supported yet
            when (getJvmType(resultType)) {
                "I", "Z" -> methodVisitor.visitLdcInsn(0)
                "D" -> methodVisitor.visitLdcInsn(0.0)
                "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
                "V" -> { /* No value to push for void */ }
                else -> methodVisitor.visitInsn(ACONST_NULL)
            }
            return
        }
        
        // Generate arguments in order
        for (argument in call.arguments) {
            val argType = expressionGenerator.inferExpressionType(argument)
            generateExpression(TypedExpression(argument, argType))
        }
        
        // Build method descriptor for the user function call
        val paramTypes = call.arguments.map { arg ->
            val argType = expressionGenerator.inferExpressionType(arg)
            getJvmType(argType)
        }
        val returnType = getJvmType(resultType)
        val methodDescriptor = "(${paramTypes.joinToString("")})$returnType"
        
        // Generate static method call to the user function
        methodVisitor.visitMethodInsn(
            INVOKESTATIC,
            currentClassName,
            functionName,
            methodDescriptor,
            false
        )
    }
    
    /**
     * Generate code for println builtin function
     */
    fun generatePrintlnCall(call: FunctionCall) {
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        
        // Generate arguments and determine the correct method signature
        val methodDescriptor = if (call.arguments.isNotEmpty()) {
            val arg = call.arguments[0]
            val argType = expressionGenerator.inferExpressionType(arg)
            
            generateExpression(TypedExpression(arg, argType))
            
            // Map to appropriate PrintStream.println overload
            when (argType) {
                BuiltinTypes.INT -> "(I)V"
                BuiltinTypes.DOUBLE -> "(D)V"
                BuiltinTypes.BOOLEAN -> {
                    // Convert boolean to string representation
                    controlFlowGenerator.convertBooleanToString()
                    "(Ljava/lang/String;)V"
                }
                BuiltinTypes.STRING -> "(Ljava/lang/String;)V"
                else -> "(Ljava/lang/Object;)V"
            }
        } else {
            methodVisitor.visitLdcInsn("")
            "(Ljava/lang/String;)V"
        }
        
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            methodDescriptor,
            false
        )
    }
    
    /**
     * Generate code for assert builtin function
     */
    fun generateAssertCall(call: FunctionCall) {
        if (call.arguments.isEmpty()) {
            // Invalid assert call - should have been caught by type checker
            // Generate a runtime exception
            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
            methodVisitor.visitInsn(DUP)
            methodVisitor.visitLdcInsn("assert() called without condition")
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false)
            methodVisitor.visitInsn(ATHROW)
            return
        }
        
        // Generate the condition expression
        val conditionArg = call.arguments[0]
        val conditionType = expressionGenerator.inferExpressionType(conditionArg)
        generateExpression(TypedExpression(conditionArg, conditionType))
        
        // Create a label for when assertion passes
        val assertPassLabel = org.objectweb.asm.Label()
        
        // If condition is true (1), jump to pass label
        methodVisitor.visitJumpInsn(IFNE, assertPassLabel)
        
        // Assertion failed - print error message to stderr and exit
        // Get System.err
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
    
    /**
     * Set up variable slots for function parameters
     */
    private fun setupFunctionParameterSlots(functionDecl: FunctionDecl) {
        // Clear existing slots since we're starting a new function
        variableSlotManager.clear()
        
        // For static methods, parameters start at slot 0 (no 'this' parameter)
        var currentSlot = 0
        
        // Create a custom variable slot manager that handles parameters correctly
        val paramSlotManager = VariableSlotManager()
        paramSlotManager.clear()
        
        for (param in functionDecl.parameters) {
            val paramType = param.type ?: BuiltinTypes.UNIT
            
            // Manually assign parameter to its slot (parameters are pre-allocated by JVM)
            // We simulate this by directly setting the slot mapping
            paramSlotManager.allocateSlot(param.name, paramType)
            
            currentSlot += getSlotCount(paramType)
        }
        
        // Replace the current variable slot manager for this function
        // This is a bit of a hack, but it allows us to handle parameters correctly
        this.variableSlotManager.clear()
        for (param in functionDecl.parameters) {
            val paramType = param.type ?: BuiltinTypes.UNIT
            variableSlotManager.allocateSlot(param.name, paramType)
        }
    }
    
    /**
     * Get the number of JVM slots required for a given type.
     */
    private fun getSlotCount(type: Type): Int {
        return when (type) {
            is Type.PrimitiveType -> when (type.name) {
                "Double", "Long" -> 2
                else -> 1
            }
            else -> 1 // Objects, arrays, etc. use 1 slot
        }
    }
    
    /**
     * Helper method to generate expressions - uses callback if available, otherwise falls back to expressionGenerator
     */
    private fun generateExpression(expr: TypedExpression) {
        if (generateExpressionCallback != null) {
            generateExpressionCallback.invoke(expr)
        } else {
            expressionGenerator.generateExpression(expr)
        }
    }
    
    /**
     * Generate return instruction based on type
     */
    fun generateReturn(type: Type, methodVisitor: MethodVisitor) {
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
     * Generate expression using local generators with correct method visitor.
     * This ensures the bytecode is generated into the actual function's method visitor
     * rather than the dummy method visitor used for setup.
     */
    private fun generateExpressionWithLocalGenerators(
        expr: TypedExpression,
        localExpressionGenerator: ExpressionBytecodeGenerator,
        localControlFlowGenerator: ControlFlowBytecodeGenerator
    ) {
        when (val expression = expr.expression) {
            is IfExpression -> {
                localControlFlowGenerator.generateIfExpression(expression, expr.type)
            }
            is WhileExpression -> {
                localControlFlowGenerator.generateWhileExpression(expression, expr.type)
            }
            is FunctionCall -> {
                // Handle function calls specially - need to pass local generators for argument generation
                val functionName = (expression.target as? Identifier)?.name
                when (functionName) {
                    "println" -> {
                        generatePrintlnCallWithLocalGenerators(expression, localExpressionGenerator)
                    }
                    "assert" -> {
                        generateAssertCallWithLocalGenerators(expression, localExpressionGenerator)
                    }
                    else -> generateFunctionCall(expression, expr.type)
                }
            }
            else -> {
                // Use local expression generator for basic expressions
                localExpressionGenerator.generateExpression(expr)
            }
        }
    }
    
    /**
     * Generate println call with local generators to ensure argument generation uses correct method visitor
     */
    private fun generatePrintlnCallWithLocalGenerators(call: FunctionCall, localExpressionGenerator: ExpressionBytecodeGenerator) {
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        
        // Generate arguments and determine the correct method signature
        val methodDescriptor = if (call.arguments.isNotEmpty()) {
            val arg = call.arguments[0]
            val argType = localExpressionGenerator.inferExpressionType(arg)
            
            // Use local expression generator for argument generation
            localExpressionGenerator.generateExpression(TypedExpression(arg, argType))
            
            // Map to appropriate PrintStream.println overload
            when (argType) {
                BuiltinTypes.INT -> "(I)V"
                BuiltinTypes.DOUBLE -> "(D)V"
                BuiltinTypes.BOOLEAN -> {
                    // Convert boolean to string representation
                    // TODO: This might need local control flow generator too if it becomes complex
                    controlFlowGenerator.convertBooleanToString()
                    "(Ljava/lang/String;)V"
                }
                BuiltinTypes.STRING -> "(Ljava/lang/String;)V"
                else -> "(Ljava/lang/Object;)V"
            }
        } else {
            methodVisitor.visitLdcInsn("")
            "(Ljava/lang/String;)V"
        }
        
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            methodDescriptor,
            false
        )
    }
    
    /**
     * Generate assert call with local generators to ensure argument generation uses correct method visitor
     */
    private fun generateAssertCallWithLocalGenerators(call: FunctionCall, localExpressionGenerator: ExpressionBytecodeGenerator) {
        if (call.arguments.isEmpty()) {
            // Invalid assert call - should have been caught by type checker
            // Generate a runtime exception
            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
            methodVisitor.visitInsn(DUP)
            methodVisitor.visitLdcInsn("assert() called without condition")
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false)
            methodVisitor.visitInsn(ATHROW)
            return
        }
        
        // Generate the condition expression using local generator
        val conditionArg = call.arguments[0]
        val conditionType = localExpressionGenerator.inferExpressionType(conditionArg)
        localExpressionGenerator.generateExpression(TypedExpression(conditionArg, conditionType))
        
        // Create a label for when assertion passes
        val assertPassLabel = org.objectweb.asm.Label()
        
        // If condition is true (1), jump to pass label
        methodVisitor.visitJumpInsn(IFNE, assertPassLabel)
        
        // Assertion failed - print error message to stderr and exit
        // Get System.err
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

