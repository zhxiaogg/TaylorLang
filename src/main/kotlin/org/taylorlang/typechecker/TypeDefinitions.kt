package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Type definitions for user-defined types in the TaylorLang type system.
 * 
 * This file contains the data structures that represent type definitions
 * created by user type declarations. These definitions are stored in the
 * type checking context and used for type validation and constructor resolution.
 */

/**
 * Base sealed class for all type definitions.
 * Currently supports union types, with potential for extension to other kinds
 * of type definitions (records, interfaces, etc.) in the future.
 */
sealed class TypeDefinition {
    
    /**
     * Definition of a union type (also known as sum types or algebraic data types).
     * 
     * Union types represent a type that can be one of several variants, where each
     * variant can carry different data. This is the primary mechanism for creating
     * custom data types in TaylorLang.
     * 
     * Example:
     * ```
     * type Option<T> = Some(T) | None
     * ```
     * 
     * @param typeParameters Generic type parameters (e.g., ["T"] for Option<T>)
     * @param variants List of variant definitions that make up this union type
     */
    data class UnionTypeDef(
        val typeParameters: List<String>,
        val variants: List<VariantDef>
    ) : TypeDefinition() {
        
        /**
         * Find a variant by name.
         * @param name The name of the variant to find
         * @return The variant definition if found, null otherwise
         */
        fun findVariant(name: String): VariantDef? {
            return variants.find { it.name == name }
        }
        
        /**
         * Get all variant names for exhaustiveness checking.
         * @return Set of all variant names in this union type
         */
        fun getAllVariantNames(): Set<String> {
            return variants.map { it.name }.toSet()
        }
        
        /**
         * Check if this union type is generic (has type parameters).
         * @return true if the union type has type parameters
         */
        fun isGeneric(): Boolean {
            return typeParameters.isNotEmpty()
        }
    }
}

/**
 * Definition of a single variant within a union type.
 * 
 * Each variant has a name (used as a constructor) and a list of field types
 * that represent the data carried by this variant.
 * 
 * Example variants:
 * - `Some(T)` -> VariantDef("Some", [T])
 * - `None` -> VariantDef("None", [])
 * - `Point(Int, Int)` -> VariantDef("Point", [Int, Int])
 * 
 * @param name The name of the variant (also serves as constructor name)
 * @param fields The types of fields/parameters this variant carries
 */
data class VariantDef(
    val name: String,
    val fields: List<Type>
) {
    /**
     * Get the arity (number of fields) of this variant.
     * @return The number of fields this variant carries
     */
    fun arity(): Int = fields.size
    
    /**
     * Check if this variant carries no data (is a simple enum-like variant).
     * @return true if this variant has no fields
     */
    fun isNullary(): Boolean = fields.isEmpty()
    
    /**
     * Check if this variant carries exactly one field.
     * @return true if this variant has exactly one field
     */
    fun isUnary(): Boolean = fields.size == 1
}

/**
 * Function signature information used for type checking function calls and declarations.
 * 
 * This represents the type signature of a function, including any generic type parameters,
 * parameter types, and the return type.
 * 
 * @param typeParameters Generic type parameters (e.g., ["T", "U"] for a function with two generic types)
 * @param parameterTypes Types of the function parameters in order
 * @param returnType The return type of the function
 */
data class FunctionSignature(
    val typeParameters: List<String> = emptyList(),
    val parameterTypes: List<Type>,
    val returnType: Type
) {
    /**
     * Check if this function is generic (has type parameters).
     * @return true if the function has type parameters
     */
    fun isGeneric(): Boolean = typeParameters.isNotEmpty()
    
    /**
     * Get the arity (number of parameters) of this function.
     * @return The number of parameters this function takes
     */
    fun arity(): Int = parameterTypes.size
    
    /**
     * Check if this function takes no parameters.
     * @return true if this function has no parameters
     */
    fun isNullary(): Boolean = parameterTypes.isEmpty()
}