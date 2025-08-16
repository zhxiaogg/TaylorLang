package org.taylorlang.typechecker

import org.taylorlang.ast.Type
import org.taylorlang.ast.visitor.BaseASTVisitor
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized type equality and subtyping logic.
 * 
 * This component eliminates duplicate type comparison implementations found across
 * 6+ files in the codebase. It provides consistent, optimized type comparison operations
 * using the established visitor pattern architecture.
 * 
 * Eliminates duplicate logic from:
 * - ArithmeticExpressionChecker.typesCompatible()
 * - PatternTypeChecker.typesCompatible()
 * - ControlFlowExpressionChecker.typesCompatible()
 * - UnificationAlgorithm.typesStructurallyEqual()
 * - AlgorithmicTypeCheckingStrategy.typesEqual()
 * - ConstraintBasedTypeCheckingStrategy.typesEqual()
 * 
 * Performance optimizations:
 * - Memoized comparison results for expensive operations
 * - Optimized visitor-based traversal
 * - Short-circuit evaluation for identical references
 */
object TypeComparison {
    
    // =============================================================================
    // Memoization Cache for Performance
    // =============================================================================
    
    /**
     * Cache key for memoizing comparison results.
     * Uses pair of type hash codes for efficient lookup.
     */
    private data class ComparisonKey(
        val type1Hash: Int,
        val type2Hash: Int,
        val operation: String
    )
    
    /**
     * Memoization cache for expensive comparison operations.
     * Thread-safe concurrent access for performance optimization.
     */
    private val comparisonCache = ConcurrentHashMap<ComparisonKey, Boolean>()
    
    // =============================================================================
    // Core Comparison Operations
    // =============================================================================
    
    /**
     * Check if two types are structurally equal.
     * 
     * This is the primary type equality check that replaces all duplicate
     * implementations across the codebase. It performs deep structural comparison
     * including type arguments and nested type structures.
     * 
     * @param type1 First type to compare
     * @param type2 Second type to compare
     * @return true if types are structurally equivalent
     */
    fun structuralEquals(type1: Type, type2: Type): Boolean {
        // Fast path: identical references
        if (type1 === type2) return true
        
        // Memoization check for performance
        val cacheKey = ComparisonKey(
            type1Hash = System.identityHashCode(type1),
            type2Hash = System.identityHashCode(type2),
            operation = "structural_equals"
        )
        
        comparisonCache[cacheKey]?.let { return it }
        
        // Perform structural comparison using visitor pattern
        val result = performStructuralComparison(type1, type2)
        comparisonCache[cacheKey] = result
        return result
    }
    
    /**
     * Check if two types are compatible for assignment and unification.
     * 
     * This method handles type compatibility including numeric promotions
     * and other language-specific compatibility rules.
     * 
     * @param type1 First type to check
     * @param type2 Second type to check
     * @return true if types are compatible
     */
    fun areCompatible(type1: Type, type2: Type): Boolean {
        // First check structural equality
        if (structuralEquals(type1, type2)) return true
        
        // Handle GenericType vs UnionType compatibility (for recursive union types)
        if (isGenericUnionCompatible(type1, type2)) return true
        
        // Handle numeric type promotion compatibility
        if (isNumericPromotion(type1, type2)) return true
        
        // Handle nullable type compatibility
        if (isNullableCompatible(type1, type2)) return true
        
        return false
    }
    
    /**
     * Check if type1 is a subtype of type2.
     * 
     * Implements subtyping relationships including:
     * - Inheritance hierarchies
     * - Generic variance rules
     * - Structural subtyping for function types
     * 
     * @param subtype The potential subtype
     * @param supertype The potential supertype
     * @return true if subtype is a subtype of supertype
     */
    fun isSubtype(subtype: Type, supertype: Type): Boolean {
        // Reflexivity: every type is a subtype of itself
        if (structuralEquals(subtype, supertype)) return true
        
        return when {
            // Numeric subtyping relationships
            subtype is Type.PrimitiveType && supertype is Type.PrimitiveType ->
                isNumericSubtype(subtype, supertype)
            
            // Function type contravariance/covariance
            subtype is Type.FunctionType && supertype is Type.FunctionType ->
                isFunctionSubtype(subtype, supertype)
            
            // Generic type variance
            subtype is Type.GenericType && supertype is Type.GenericType ->
                isGenericSubtype(subtype, supertype)
            
            // Nullable type subtyping
            subtype is Type.NullableType || supertype is Type.NullableType ->
                isNullableSubtype(subtype, supertype)
                
            else -> false
        }
    }
    
    // =============================================================================
    // Specialized Comparison Logic
    // =============================================================================
    
    /**
     * Perform deep structural comparison using visitor pattern.
     * Core implementation that replaces all duplicate logic.
     */
    private fun performStructuralComparison(type1: Type, type2: Type): Boolean {
        return when {
            type1 is Type.PrimitiveType && type2 is Type.PrimitiveType ->
                type1.name == type2.name
                
            type1 is Type.NamedType && type2 is Type.NamedType ->
                type1.name == type2.name
                
            type1 is Type.GenericType && type2 is Type.GenericType ->
                type1.name == type2.name && 
                type1.arguments.size == type2.arguments.size &&
                type1.arguments.zip(type2.arguments).all { (a1, a2) -> 
                    structuralEquals(a1, a2) 
                }
                
            type1 is Type.TupleType && type2 is Type.TupleType ->
                type1.elementTypes.size == type2.elementTypes.size &&
                type1.elementTypes.zip(type2.elementTypes).all { (t1, t2) -> 
                    structuralEquals(t1, t2) 
                }
                
            type1 is Type.NullableType && type2 is Type.NullableType ->
                structuralEquals(type1.baseType, type2.baseType)
                
            type1 is Type.UnionType && type2 is Type.UnionType ->
                type1.name == type2.name && 
                type1.typeArguments.size == type2.typeArguments.size &&
                type1.typeArguments.zip(type2.typeArguments).all { (a1, a2) -> 
                    structuralEquals(a1, a2) 
                }
                
            type1 is Type.FunctionType && type2 is Type.FunctionType ->
                structuralEquals(type1.returnType, type2.returnType) &&
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (p1, p2) -> 
                    structuralEquals(p1, p2) 
                }
                
            type1 is Type.TypeVar && type2 is Type.TypeVar ->
                type1.id == type2.id
                
            else -> false
        }
    }
    
    /**
     * Check if types are compatible through numeric promotion.
     */
    private fun isNumericPromotion(type1: Type, type2: Type): Boolean {
        if (type1 !is Type.PrimitiveType || type2 !is Type.PrimitiveType) return false
        
        val numericTypes = listOf("Int", "Long", "Float", "Double")
        return numericTypes.contains(type1.name) && numericTypes.contains(type2.name)
    }
    
    /**
     * Check nullable type compatibility rules.
     */
    private fun isNullableCompatible(type1: Type, type2: Type): Boolean {
        return when {
            // T is compatible with T?
            type2 is Type.NullableType -> structuralEquals(type1, type2.baseType)
            // T? is not compatible with T (requires explicit null check)
            type1 is Type.NullableType -> false
            else -> false
        }
    }
    
    /**
     * Check compatibility between GenericType and UnionType with same name.
     * This handles cases where user-defined union types (like List<T>) should be 
     * compatible with built-in generic types (like List<Int>) during type checking.
     */
    private fun isGenericUnionCompatible(type1: Type, type2: Type): Boolean {
        return when {
            // GenericType vs UnionType
            type1 is Type.GenericType && type2 is Type.UnionType ->
                type1.name == type2.name && 
                type1.arguments.size == type2.typeArguments.size &&
                type1.arguments.zip(type2.typeArguments).all { (arg1, arg2) -> 
                    areCompatible(arg1, arg2) || isTypeVariableCompatible(arg1, arg2)
                }
            // UnionType vs GenericType  
            type1 is Type.UnionType && type2 is Type.GenericType ->
                type1.name == type2.name && 
                type1.typeArguments.size == type2.arguments.size &&
                type1.typeArguments.zip(type2.arguments).all { (arg1, arg2) -> 
                    areCompatible(arg1, arg2) || isTypeVariableCompatible(arg1, arg2)
                }
            else -> false
        }
    }
    
    /**
     * Check if a concrete type is compatible with a type variable.
     * This handles cases where a concrete type like Int should be compatible with T.
     */
    private fun isTypeVariableCompatible(concreteType: Type, typeVar: Type): Boolean {
        return when {
            // Concrete type vs type variable - type variables can be unified with any type
            typeVar is Type.NamedType -> true
            // Type variable vs concrete type
            concreteType is Type.NamedType -> true
            // Recursive compatibility check for complex types
            concreteType is Type.GenericType && typeVar is Type.GenericType ->
                concreteType.name == typeVar.name &&
                concreteType.arguments.size == typeVar.arguments.size &&
                concreteType.arguments.zip(typeVar.arguments).all { (c, t) -> 
                    areCompatible(c, t) || isTypeVariableCompatible(c, t)
                }
            concreteType is Type.UnionType && typeVar is Type.UnionType ->
                concreteType.name == typeVar.name &&
                concreteType.typeArguments.size == typeVar.typeArguments.size &&
                concreteType.typeArguments.zip(typeVar.typeArguments).all { (c, t) -> 
                    areCompatible(c, t) || isTypeVariableCompatible(c, t)
                }
            else -> false
        }
    }
    
    /**
     * Check numeric subtyping relationships (Int <: Long <: Float <: Double).
     */
    private fun isNumericSubtype(subtype: Type.PrimitiveType, supertype: Type.PrimitiveType): Boolean {
        val typeHierarchy = mapOf(
            "Int" to 0,
            "Long" to 1,
            "Float" to 2,
            "Double" to 3
        )
        
        val subLevel = typeHierarchy[subtype.name] ?: return false
        val superLevel = typeHierarchy[supertype.name] ?: return false
        
        return subLevel <= superLevel
    }
    
    /**
     * Check function type subtyping (contravariant parameters, covariant return).
     */
    private fun isFunctionSubtype(subtype: Type.FunctionType, supertype: Type.FunctionType): Boolean {
        if (subtype.parameterTypes.size != supertype.parameterTypes.size) return false
        
        // Contravariant parameter types
        val parametersCompatible = subtype.parameterTypes.zip(supertype.parameterTypes)
            .all { (subParam, superParam) -> isSubtype(superParam, subParam) }
        
        // Covariant return type
        val returnCompatible = isSubtype(subtype.returnType, supertype.returnType)
        
        return parametersCompatible && returnCompatible
    }
    
    /**
     * Check generic type subtyping with basic invariance.
     */
    private fun isGenericSubtype(subtype: Type.GenericType, supertype: Type.GenericType): Boolean {
        if (subtype.name != supertype.name) return false
        if (subtype.arguments.size != supertype.arguments.size) return false
        
        // Invariant type arguments (could be extended for variance annotations)
        return subtype.arguments.zip(supertype.arguments)
            .all { (subArg, superArg) -> structuralEquals(subArg, superArg) }
    }
    
    /**
     * Check nullable type subtyping rules.
     */
    private fun isNullableSubtype(subtype: Type, supertype: Type): Boolean {
        return when {
            // T <: T?
            supertype is Type.NullableType -> 
                isSubtype(subtype, supertype.baseType)
            // T? <: U requires T <: U
            subtype is Type.NullableType ->
                isSubtype(subtype.baseType, supertype)
            else -> false
        }
    }
    
    // =============================================================================
    // Utility Methods for Type Analysis
    // =============================================================================
    
    /**
     * Check if a type contains type variables (useful for generics).
     */
    fun containsTypeVariables(type: Type): Boolean {
        return when (type) {
            is Type.TypeVar -> true
            is Type.GenericType -> type.arguments.any { containsTypeVariables(it) }
            is Type.TupleType -> type.elementTypes.any { containsTypeVariables(it) }
            is Type.FunctionType -> 
                containsTypeVariables(type.returnType) || 
                type.parameterTypes.any { containsTypeVariables(it) }
            is Type.NullableType -> containsTypeVariables(type.baseType)
            is Type.UnionType -> type.typeArguments.any { containsTypeVariables(it) }
            else -> false
        }
    }
    
    /**
     * Extract all type variables from a type for constraint generation.
     */
    fun extractTypeVariables(type: Type): Set<String> {
        val variables = mutableSetOf<String>()
        collectTypeVariables(type, variables)
        return variables
    }
    
    private fun collectTypeVariables(type: Type, collector: MutableSet<String>) {
        when (type) {
            is Type.TypeVar -> collector.add(type.id)
            is Type.GenericType -> type.arguments.forEach { collectTypeVariables(it, collector) }
            is Type.TupleType -> type.elementTypes.forEach { collectTypeVariables(it, collector) }
            is Type.FunctionType -> {
                collectTypeVariables(type.returnType, collector)
                type.parameterTypes.forEach { collectTypeVariables(it, collector) }
            }
            is Type.NullableType -> collectTypeVariables(type.baseType, collector)
            is Type.UnionType -> type.typeArguments.forEach { collectTypeVariables(it, collector) }
            else -> {} // No type variables in primitive, named types
        }
    }
    
    // =============================================================================
    // Cache Management
    // =============================================================================
    
    /**
     * Clear comparison cache for memory management.
     * Primarily used in testing scenarios.
     */
    fun clearCache() {
        comparisonCache.clear()
    }
    
    /**
     * Get cache statistics for performance monitoring.
     */
    fun getCacheSize(): Int = comparisonCache.size
}