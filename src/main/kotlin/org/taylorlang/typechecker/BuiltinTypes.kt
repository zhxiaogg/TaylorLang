package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Built-in primitive types for the TaylorLang type system.
 * 
 * This object provides definitions for all primitive types that are available
 * by default in TaylorLang. These types are automatically available in all
 * type checking contexts without requiring imports or declarations.
 * 
 * Design principles:
 * - All primitive types are singletons for memory efficiency
 * - Type names match common programming language conventions
 * - Provides both individual type access and bulk operations
 */
object BuiltinTypes {
    
    // =============================================================================
    // Primitive Type Definitions
    // =============================================================================
    
    /**
     * 32-bit signed integer type.
     * Range: -2^31 to 2^31 - 1
     */
    val INT = Type.PrimitiveType("Int")
    
    /**
     * 64-bit signed integer type.
     * Range: -2^63 to 2^63 - 1
     */
    val LONG = Type.PrimitiveType("Long")
    
    /**
     * 32-bit IEEE 754 floating-point type.
     */
    val FLOAT = Type.PrimitiveType("Float")
    
    /**
     * 64-bit IEEE 754 floating-point type.
     */
    val DOUBLE = Type.PrimitiveType("Double")
    
    /**
     * Boolean type with values true and false.
     */
    val BOOLEAN = Type.PrimitiveType("Boolean")
    
    /**
     * UTF-8 string type.
     */
    val STRING = Type.PrimitiveType("String")
    
    /**
     * Unit type representing no meaningful value.
     * Similar to void in other languages, but is a proper type with one value: ().
     */
    val UNIT = Type.PrimitiveType("Unit")
    
    // =============================================================================
    // Type Collections and Utilities
    // =============================================================================
    
    /**
     * Map of primitive type names to their corresponding Type objects.
     * This is used for type name resolution and context initialization.
     */
    val primitives: Map<String, Type> = mapOf(
        "Int" to INT,
        "Long" to LONG,
        "Float" to FLOAT,
        "Double" to DOUBLE,
        "Boolean" to BOOLEAN,
        "String" to STRING,
        "Unit" to UNIT
    )
    
    /**
     * Set of all primitive type names.
     * Useful for checking if a type name refers to a built-in primitive.
     */
    val primitiveNames: Set<String> = primitives.keys
    
    /**
     * List of all numeric types in order of widening conversion precedence.
     * Used for type promotion and compatibility checking.
     */
    val numericTypes: List<Type> = listOf(INT, LONG, FLOAT, DOUBLE)
    
    /**
     * Set of integral (integer) types.
     */
    val integralTypes: Set<Type> = setOf(INT, LONG)
    
    /**
     * Set of floating-point types.
     */
    val floatingPointTypes: Set<Type> = setOf(FLOAT, DOUBLE)
    
    // =============================================================================
    // Type Checking Utilities
    // =============================================================================
    
    /**
     * Check if a type is a built-in primitive type.
     * @param type The type to check
     * @return true if the type is a primitive type
     */
    fun isPrimitive(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> primitiveNames.contains(type.name)
            else -> false
        }
    }
    
    /**
     * Check if a type is a numeric type (integer or floating-point).
     * @param type The type to check
     * @return true if the type is numeric
     */
    fun isNumeric(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name) {
                    "Int", "Long", "Float", "Double" -> true
                    else -> false
                }
            }
            else -> false
        }
    }
    
    /**
     * Check if a type is an integral type.
     * @param type The type to check
     * @return true if the type is integral (Int or Long)
     */
    fun isIntegral(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name) {
                    "Int", "Long" -> true
                    else -> false
                }
            }
            else -> false
        }
    }
    
    /**
     * Check if a type is a floating-point type.
     * @param type The type to check
     * @return true if the type is floating-point (Float or Double)
     */
    fun isFloatingPoint(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name) {
                    "Float", "Double" -> true
                    else -> false
                }
            }
            else -> false
        }
    }
    
    /**
     * Get the wider type for numeric type promotion.
     * Used in binary operations to determine the result type.
     * 
     * @param type1 First numeric type
     * @param type2 Second numeric type
     * @return The wider type, or null if types cannot be unified
     */
    fun getWiderNumericType(type1: Type, type2: Type): Type? {
        if (!isNumeric(type1) || !isNumeric(type2)) {
            return null
        }
        
        // Map type names to their promotion precedence
        fun getNumericPrecedence(type: Type): Int? {
            return when {
                type is Type.PrimitiveType -> when (type.name) {
                    "Int" -> 0
                    "Long" -> 1
                    "Float" -> 2
                    "Double" -> 3
                    else -> null
                }
                else -> null
            }
        }
        
        val precedence1 = getNumericPrecedence(type1)
        val precedence2 = getNumericPrecedence(type2)
        
        return if (precedence1 != null && precedence2 != null) {
            // Return the canonical builtin type with the higher precedence
            val widerPrecedence = maxOf(precedence1, precedence2)
            when (widerPrecedence) {
                0 -> INT
                1 -> LONG
                2 -> FLOAT
                3 -> DOUBLE
                else -> null
            }
        } else {
            null
        }
    }
    
    /**
     * Lookup a primitive type by name.
     * @param name The name of the primitive type
     * @return The Type object if found, null otherwise
     */
    fun lookupPrimitive(name: String): Type? {
        return primitives[name]
    }
}