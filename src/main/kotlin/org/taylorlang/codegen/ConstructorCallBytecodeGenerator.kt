package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for constructor calls.
 * 
 * This component handles the generation of JVM bytecode for:
 * - Union type constructors (Result.Ok/Error, Option.Some/None)
 * - Status enum constructors (Active, Inactive, Pending)
 * - Tuple constructors (Pair with multiple arguments)
 * - Primitive boxing for constructor arguments
 * - Runtime class mapping for union types
 * 
 * Key features:
 * - Automatic primitive boxing for constructor arguments
 * - Proper runtime class resolution for union types
 * - Singleton pattern support for stateless constructors
 * - Type-safe argument generation and conversion
 * - Integration with TaylorLang's type system
 */
class ConstructorCallBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val typeHelper: TypeInferenceBytecodeHelper,
    private val typeInferenceHelper: (Expression) -> Type,
    private val generateExpression: (TypedExpression) -> Unit
) {
    
    /**
     * Generate constructor call for union types
     */
    fun generateConstructorCall(constructorCall: ConstructorCall, expectedType: Type) {
        val constructorName = constructorCall.constructor
        
        // Map constructor calls to their runtime implementations
        val runtimeClassName = getConstructorRuntimeClassName(constructorName, expectedType)
        
        // Generate constructor call based on the runtime type (arguments handled per case)
        when {
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorResult") -> {
                generateResultConstructor(constructorCall, constructorName)
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorOption") -> {
                generateOptionConstructor(constructorCall, constructorName)
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorStatus") -> {
                generateStatusConstructor(constructorName)
            }
            runtimeClassName.startsWith("org/taylorlang/runtime/TaylorTuple2") -> {
                generateTupleConstructor(constructorCall, constructorName)
            }
            else -> generateFallbackConstructor(expectedType)
        }
    }
    
    /**
     * Generate Result type constructors (Ok, Error)
     */
    private fun generateResultConstructor(constructorCall: ConstructorCall, constructorName: String) {
        when (constructorName) {
            "Ok" -> {
                // new TaylorResult.Ok(value)
                methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorResult\$Ok")
                methodVisitor.visitInsn(DUP)
                
                // Generate argument
                val arg = constructorCall.arguments.firstOrNull()
                if (arg != null) {
                    val argType = typeInferenceHelper(arg)
                    generateExpression(TypedExpression(arg, argType))
                    // CRITICAL FIX: Box primitive types to Object for constructor
                    typeHelper.boxPrimitiveToObject(argType)
                } else {
                    methodVisitor.visitInsn(ACONST_NULL)
                }
                
                methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    "org/taylorlang/runtime/TaylorResult\$Ok",
                    "<init>",
                    "(Ljava/lang/Object;)V",
                    false
                )
            }
            "Error" -> {
                // new TaylorResult.Error(error)
                methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorResult\$Error")
                methodVisitor.visitInsn(DUP)
                
                // Generate argument - if String, wrap in RuntimeException
                val arg = constructorCall.arguments.firstOrNull()
                if (arg != null) {
                    val argType = typeInferenceHelper(arg)
                    
                    if ((argType is Type.NamedType && argType.name == "String") || 
                        (argType is Type.PrimitiveType && argType.name == "String")) {
                        // Create RuntimeException from string
                        methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                        methodVisitor.visitInsn(DUP)
                        generateExpression(TypedExpression(arg, argType))
                        methodVisitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "java/lang/RuntimeException",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                        )
                    } else {
                        // Use argument as-is (assuming it's already a Throwable)
                        generateExpression(TypedExpression(arg, argType))
                    }
                } else {
                    // No argument - create default RuntimeException
                    methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException")
                    methodVisitor.visitInsn(DUP)
                    methodVisitor.visitLdcInsn("Unknown error")
                    methodVisitor.visitMethodInsn(
                        INVOKESPECIAL,
                        "java/lang/RuntimeException",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false
                    )
                }
                
                methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    "org/taylorlang/runtime/TaylorResult\$Error",
                    "<init>",
                    "(Ljava/lang/Throwable;)V",
                    false
                )
            }
            else -> generateFallbackConstructor(Type.NamedType("Result"))
        }
    }
    
    /**
     * Generate Option type constructors (Some, None)
     */
    private fun generateOptionConstructor(constructorCall: ConstructorCall, constructorName: String) {
        when (constructorName) {
            "Some" -> {
                // new TaylorOption.Some(value)
                methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorOption\$Some")
                methodVisitor.visitInsn(DUP)
                
                // Generate argument
                val arg = constructorCall.arguments.firstOrNull()
                if (arg != null) {
                    val argType = typeInferenceHelper(arg)
                    generateExpression(TypedExpression(arg, argType))
                    // CRITICAL FIX: Box primitive types to Object for constructor
                    typeHelper.boxPrimitiveToObject(argType)
                } else {
                    methodVisitor.visitInsn(ACONST_NULL)
                }
                
                methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    "org/taylorlang/runtime/TaylorOption\$Some",
                    "<init>",
                    "(Ljava/lang/Object;)V",
                    false
                )
            }
            "None" -> {
                // TaylorOption.None.INSTANCE
                methodVisitor.visitFieldInsn(
                    GETSTATIC,
                    "org/taylorlang/runtime/TaylorOption\$None",
                    "INSTANCE",
                    "Lorg/taylorlang/runtime/TaylorOption\$None;"
                )
            }
            else -> generateFallbackConstructor(Type.NamedType("Option"))
        }
    }
    
    /**
     * Generate Status enum constructors (Active, Inactive, Pending)
     */
    private fun generateStatusConstructor(constructorName: String) {
        when (constructorName) {
            "Active" -> {
                methodVisitor.visitFieldInsn(
                    GETSTATIC,
                    "org/taylorlang/runtime/TaylorStatus\$Active",
                    "INSTANCE",
                    "Lorg/taylorlang/runtime/TaylorStatus\$Active;"
                )
            }
            "Inactive" -> {
                methodVisitor.visitFieldInsn(
                    GETSTATIC,
                    "org/taylorlang/runtime/TaylorStatus\$Inactive",
                    "INSTANCE",
                    "Lorg/taylorlang/runtime/TaylorStatus\$Inactive;"
                )
            }
            "Pending" -> {
                methodVisitor.visitFieldInsn(
                    GETSTATIC,
                    "org/taylorlang/runtime/TaylorStatus\$Pending",
                    "INSTANCE",
                    "Lorg/taylorlang/runtime/TaylorStatus\$Pending;"
                )
            }
            else -> generateFallbackConstructor(Type.NamedType("Status"))
        }
    }
    
    /**
     * Generate Tuple constructors (Pair, etc.)
     */
    private fun generateTupleConstructor(constructorCall: ConstructorCall, constructorName: String) {
        when (constructorName) {
            "Pair" -> {
                // new TaylorTuple2.Pair(first, second)
                methodVisitor.visitTypeInsn(NEW, "org/taylorlang/runtime/TaylorTuple2\$Pair")
                methodVisitor.visitInsn(DUP)
                
                // Generate first argument
                val firstArg = constructorCall.arguments.getOrNull(0)
                if (firstArg != null) {
                    val argType = typeInferenceHelper(firstArg)
                    generateExpression(TypedExpression(firstArg, argType))
                    // CRITICAL FIX: Box primitive types to Object for constructor
                    typeHelper.boxPrimitiveToObject(argType)
                } else {
                    methodVisitor.visitInsn(ACONST_NULL)
                }
                
                // Generate second argument
                val secondArg = constructorCall.arguments.getOrNull(1)
                if (secondArg != null) {
                    val argType = typeInferenceHelper(secondArg)
                    generateExpression(TypedExpression(secondArg, argType))
                    // CRITICAL FIX: Box primitive types to Object for constructor
                    typeHelper.boxPrimitiveToObject(argType)
                } else {
                    methodVisitor.visitInsn(ACONST_NULL)
                }
                
                methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    "org/taylorlang/runtime/TaylorTuple2\$Pair",
                    "<init>",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V",
                    false
                )
            }
            else -> generateFallbackConstructor(Type.NamedType("Tuple2"))
        }
    }
    
    /**
     * Get the runtime class name for a constructor
     */
    private fun getConstructorRuntimeClassName(constructorName: String, expectedType: Type): String {
        val typeName = when (expectedType) {
            is Type.NamedType -> expectedType.name
            is Type.UnionType -> expectedType.name
            is Type.GenericType -> expectedType.name
            else -> "Object"
        }
        
        return when (typeName) {
            "Result" -> "org/taylorlang/runtime/TaylorResult\$$constructorName"
            "Option" -> "org/taylorlang/runtime/TaylorOption\$$constructorName"
            "Status" -> "org/taylorlang/runtime/TaylorStatus\$$constructorName"
            "Tuple2" -> "org/taylorlang/runtime/TaylorTuple2\$$constructorName"
            else -> "$typeName\$$constructorName"
        }
    }
    
    /**
     * Generate fallback constructor for unknown types
     */
    private fun generateFallbackConstructor(expectedType: Type) {
        when (typeHelper.getJvmType(expectedType)) {
            "I", "Z" -> methodVisitor.visitLdcInsn(0)
            "D" -> methodVisitor.visitLdcInsn(0.0)
            else -> methodVisitor.visitInsn(ACONST_NULL)
        }
    }
}