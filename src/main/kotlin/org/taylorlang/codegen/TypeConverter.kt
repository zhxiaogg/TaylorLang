package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.Type
import org.taylorlang.typechecker.*

/**
 * Handles type conversion and boxing/unboxing operations for bytecode generation.
 * 
 * This service provides:
 * - Object to primitive type conversions
 * - Primitive boxing and unboxing operations
 * - Type compatibility validation
 * - JVM type descriptor generation
 * - Proper error handling for invalid conversions
 */
class TypeConverter(private val methodVisitor: MethodVisitor) {
    
    /**
     * Convert a value on the stack from one type to another.
     * This handles the Object -> primitive conversions that were causing VerifyError.
     * 
     * @param fromType The source type of the value on the stack
     * @param toType The target type to convert to
     * @throws IllegalArgumentException if conversion is not supported
     */
    fun convertType(fromType: Type, toType: Type) {
        val fromJvmType = getJvmType(fromType)
        val toJvmType = getJvmType(toType)
        
        if (fromJvmType == toJvmType) {
            return // No conversion needed
        }
        
        when {
            // Object to primitive conversions (fixes VerifyError)
            fromJvmType == "Ljava/lang/Object;" && toJvmType == "I" -> {
                unboxInteger()
            }
            fromJvmType == "Ljava/lang/Object;" && toJvmType == "D" -> {
                unboxDouble()
            }
            fromJvmType == "Ljava/lang/Object;" && toJvmType == "Z" -> {
                unboxBoolean()
            }
            fromJvmType == "Ljava/lang/Object;" && toJvmType.startsWith("L") -> {
                castToClass(toJvmType)
            }
            
            // Primitive to Object conversions (boxing)
            fromJvmType == "I" && toJvmType == "Ljava/lang/Object;" -> {
                boxInteger()
            }
            fromJvmType == "D" && toJvmType == "Ljava/lang/Object;" -> {
                boxDouble()
            }
            fromJvmType == "Z" && toJvmType == "Ljava/lang/Object;" -> {
                boxBoolean()
            }
            
            // Numeric conversions
            fromJvmType == "I" && toJvmType == "D" -> {
                methodVisitor.visitInsn(I2D)
            }
            fromJvmType == "D" && toJvmType == "I" -> {
                methodVisitor.visitInsn(D2I)
            }
            
            else -> {
                throw IllegalArgumentException("Unsupported type conversion from $fromType to $toType")
            }
        }
    }
    
    /**
     * Unbox an Integer object to primitive int.
     */
    private fun unboxInteger() {
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer")
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/Integer",
            "intValue",
            "()I",
            false
        )
    }
    
    /**
     * Unbox a Double object to primitive double.
     */
    private fun unboxDouble() {
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double")
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/Double",
            "doubleValue",
            "()D",
            false
        )
    }
    
    /**
     * Unbox a Boolean object to primitive boolean.
     */
    private fun unboxBoolean() {
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean")
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/Boolean",
            "booleanValue",
            "()Z",
            false
        )
    }
    
    /**
     * Box primitive int to Integer object.
     */
    private fun boxInteger() {
        methodVisitor.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/Integer",
            "valueOf",
            "(I)Ljava/lang/Integer;",
            false
        )
    }
    
    /**
     * Box primitive double to Double object.
     */
    private fun boxDouble() {
        methodVisitor.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/Double",
            "valueOf",
            "(D)Ljava/lang/Double;",
            false
        )
    }
    
    /**
     * Box primitive boolean to Boolean object.
     */
    private fun boxBoolean() {
        methodVisitor.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/Boolean",
            "valueOf",
            "(Z)Ljava/lang/Boolean;",
            false
        )
    }
    
    /**
     * Cast object to specific class type.
     */
    private fun castToClass(targetJvmType: String) {
        val className = targetJvmType.substring(1, targetJvmType.length - 1) // Remove L and ;
        methodVisitor.visitTypeInsn(CHECKCAST, className)
    }
    
    /**
     * Get JVM type descriptor for a TaylorLang type.
     */
    fun getJvmType(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit" -> "V"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name) {
                    "String" -> "Ljava/lang/String;"
                    "Int" -> "I"
                    "Double" -> "D"
                    "Boolean" -> "Z"
                    "Unit" -> "V"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.GenericType -> "Ljava/lang/Object;"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Get full JVM type descriptor including generic parameters.
     */
    fun getJvmTypeDescriptor(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit" -> "V"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name) {
                    "String" -> "Ljava/lang/String;"
                    "Int" -> "Ljava/lang/Integer;"
                    "Double" -> "Ljava/lang/Double;"
                    "Boolean" -> "Ljava/lang/Boolean;"
                    "Unit" -> "V"
                    else -> "L${type.name.replace('.', '/')};"
                }
            }
            is Type.GenericType -> "Ljava/lang/Object;"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Check if two types are compatible for assignment.
     */
    fun areTypesCompatible(fromType: Type, toType: Type): Boolean {
        // Same type
        if (fromType == toType) return true
        
        // Object can hold any reference type
        if (getJvmType(toType) == "Ljava/lang/Object;") return true
        
        // Check specific compatibility rules
        return when {
            // Primitive compatibility
            isIntegerType(fromType) && isIntegerType(toType) -> true
            isNumericType(fromType) && isNumericType(toType) -> true
            
            // String compatibility
            isStringType(fromType) && isStringType(toType) -> true
            
            else -> false
        }
    }
    
    /**
     * Check if a type is an integer type.
     */
    private fun isIntegerType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "int"
            is Type.NamedType -> type.name == "Int"
            else -> false
        }
    }
    
    /**
     * Check if a type is a numeric type.
     */
    private fun isNumericType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() in setOf("int", "double", "float")
            is Type.NamedType -> type.name in setOf("Int", "Double", "Float")
            else -> false
        }
    }
    
    /**
     * Check if a type is a string type.
     */
    private fun isStringType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "string"
            is Type.NamedType -> type.name == "String"
            else -> false
        }
    }
    
    /**
     * Generate appropriate default value for a type.
     */
    fun generateDefaultValue(type: Type) {
        when (getJvmType(type)) {
            "V" -> { /* No value needed for void */ }
            "I", "Z" -> methodVisitor.visitLdcInsn(0)
            "D" -> methodVisitor.visitLdcInsn(0.0)
            "Ljava/lang/String;" -> methodVisitor.visitLdcInsn("")
            else -> methodVisitor.visitInsn(ACONST_NULL)
        }
    }
    
    companion object {
        // JVM type constants to reduce magic strings
        const val OBJECT_TYPE = "Ljava/lang/Object;"
        const val STRING_TYPE = "Ljava/lang/String;"
        const val INTEGER_TYPE = "Ljava/lang/Integer;"
        const val DOUBLE_TYPE = "Ljava/lang/Double;"
        const val BOOLEAN_TYPE = "Ljava/lang/Boolean;"
        const val VOID_TYPE = "V"
        const val INT_TYPE = "I"
        const val DOUBLE_PRIMITIVE_TYPE = "D"
        const val BOOLEAN_PRIMITIVE_TYPE = "Z"
    }
}