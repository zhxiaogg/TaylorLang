package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Occurs check implementation for preventing infinite types during unification.
 * 
 * The occurs check is a crucial component of the unification algorithm that prevents
 * the creation of infinite type structures. It determines whether a type variable
 * occurs within a type, which would create a circular reference if unified.
 * 
 * For example, attempting to unify T with List[T] would create an infinite type:
 * T = List[T] = List[List[T]] = List[List[List[T]]] = ...
 * 
 * Key responsibilities:
 * - Detecting type variable occurrences in type structures
 * - Recursive traversal of complex type hierarchies
 * - Prevention of infinite type creation
 * - Support for all type system constructs
 * 
 * This implementation is self-contained and focused solely on cycle detection,
 * allowing it to be reused across different unification contexts.
 */
internal class OccursChecker {
    
    /**
     * Occurs check: determine if a type variable occurs in a type.
     * 
     * This prevents infinite types like T = List[T] by checking if
     * the type variable being unified appears in the type it's being
     * unified with.
     * 
     * @param typeVar The type variable to check for
     * @param type The type to check in
     * @return true if the type variable occurs in the type
     */
    fun occursCheck(typeVar: TypeVar, type: Type): Boolean {
        return when (type) {
            is Type.TypeVar -> typeVar.id == type.id
            
            is Type.NamedType -> {
                // Check if this NamedType represents the same type variable
                isTypeVariable(type) && typeVar.id == type.name
            }
            
            is Type.GenericType -> {
                type.arguments.any { occursCheck(typeVar, it) }
            }
            
            is Type.UnionType -> {
                type.typeArguments.any { occursCheck(typeVar, it) }
            }
            
            is Type.NullableType -> {
                occursCheck(typeVar, type.baseType)
            }
            
            is Type.TupleType -> {
                type.elementTypes.any { occursCheck(typeVar, it) }
            }
            
            is Type.FunctionType -> {
                type.parameterTypes.any { occursCheck(typeVar, it) } ||
                occursCheck(typeVar, type.returnType)
            }
            
            is Type.PrimitiveType -> false
        }
    }
    
    /**
     * Check if a type contains any type variables.
     * 
     * This is useful for determining if a type is fully concrete
     * or still contains unknown type variables.
     * 
     * @param type The type to analyze
     * @return true if the type contains any type variables
     */
    fun containsTypeVars(type: Type): Boolean {
        return when (type) {
            is Type.TypeVar -> true
            
            is Type.NamedType -> isTypeVariable(type)
            
            is Type.GenericType -> {
                type.arguments.any { containsTypeVars(it) }
            }
            
            is Type.UnionType -> {
                type.typeArguments.any { containsTypeVars(it) }
            }
            
            is Type.NullableType -> {
                containsTypeVars(type.baseType)
            }
            
            is Type.TupleType -> {
                type.elementTypes.any { containsTypeVars(it) }
            }
            
            is Type.FunctionType -> {
                type.parameterTypes.any { containsTypeVars(it) } ||
                containsTypeVars(type.returnType)
            }
            
            is Type.PrimitiveType -> false
        }
    }
    
    /**
     * Collect all type variables that occur within a type.
     * 
     * This returns a set of all type variables found during traversal,
     * which is useful for dependency analysis and variable tracking.
     * 
     * @param type The type to analyze
     * @return A set of all type variables found in the type
     */
    fun collectTypeVars(type: Type): Set<TypeVar> {
        return when (type) {
            is Type.TypeVar -> setOf(TypeVar(type.id))
            
            is Type.NamedType -> {
                if (isTypeVariable(type)) {
                    setOf(TypeVar(type.name))
                } else {
                    emptySet()
                }
            }
            
            is Type.GenericType -> {
                type.arguments.flatMap { collectTypeVars(it) }.toSet()
            }
            
            is Type.UnionType -> {
                type.typeArguments.flatMap { collectTypeVars(it) }.toSet()
            }
            
            is Type.NullableType -> {
                collectTypeVars(type.baseType)
            }
            
            is Type.TupleType -> {
                type.elementTypes.flatMap { collectTypeVars(it) }.toSet()
            }
            
            is Type.FunctionType -> {
                val paramVars = type.parameterTypes.flatMap { collectTypeVars(it) }.toSet()
                val returnVars = collectTypeVars(type.returnType)
                paramVars + returnVars
            }
            
            is Type.PrimitiveType -> emptySet()
        }
    }
    
    /**
     * Check if a specific type variable occurs in a set of types.
     * 
     * This is an optimized version for checking multiple types at once,
     * which can short-circuit as soon as the variable is found.
     * 
     * @param typeVar The type variable to search for
     * @param types The types to search in
     * @return true if the type variable occurs in any of the types
     */
    fun occursInAny(typeVar: TypeVar, types: Collection<Type>): Boolean {
        return types.any { occursCheck(typeVar, it) }
    }
    
    /**
     * Check the occurs relationship between two types.
     * 
     * This determines if there would be any occurs check violations
     * if these two types were unified, considering both directions.
     * 
     * @param type1 The first type
     * @param type2 The second type
     * @return true if unifying these types would create an occurs check violation
     */
    fun wouldViolateOccursCheck(type1: Type, type2: Type): Boolean {
        val vars1 = collectTypeVars(type1)
        val vars2 = collectTypeVars(type2)
        
        // Check if any variable from type1 occurs in type2
        val type1VarsInType2 = vars1.any { occursCheck(it, type2) }
        
        // Check if any variable from type2 occurs in type1
        val type2VarsInType1 = vars2.any { occursCheck(it, type1) }
        
        return type1VarsInType2 || type2VarsInType1
    }
    
    /**
     * Check if a NamedType represents a type variable.
     * 
     * This is a heuristic based on naming conventions used throughout
     * the type system. Type variables typically follow specific patterns.
     */
    private fun isTypeVariable(type: Type.NamedType): Boolean {
        // Type variables are typically single uppercase letters or start with T followed by digits
        // Built-in type names are explicitly excluded
        val builtinTypeNames = setOf("Int", "String", "Boolean", "Double", "Float", "Long", "Unit")
        
        return !builtinTypeNames.contains(type.name) && (
               // Single uppercase letter (T, U, V, etc.)
               (type.name.length == 1 && type.name[0].isUpperCase()) ||
               // T followed by digits (T1, T2, T3, etc.)
               (type.name.startsWith("T") && type.name.drop(1).all { it.isDigit() })
        )
    }
}