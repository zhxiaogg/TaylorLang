package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Central facade for all type-related operations in TaylorLang.
 * 
 * This facade provides the single entry point for all type operations, coordinating
 * specialized services following proven delegation patterns established in the codebase.
 * It eliminates scattered type logic and provides consistent, optimized operations
 * across all type checking components.
 * 
 * Architecture following established patterns:
 * - Main coordinator delegates to specialized services
 * - Clean separation of concerns across components
 * - Performance optimization through caching and delegation
 * - Backwards-compatible API for smooth migration
 * 
 * Replaces scattered operations from:
 * - 6+ duplicate typesCompatible() implementations
 * - 15+ files with inconsistent type validation
 * - Manual type creation throughout the codebase
 * - Repeated type traversal and analysis patterns
 */
object TypeOperations {
    
    // =============================================================================
    // Core Type Comparison Operations
    // =============================================================================
    
    /**
     * Check if two types are structurally equal.
     * 
     * This replaces all duplicate implementations of type equality checking
     * found across ArithmeticExpressionChecker, PatternTypeChecker, 
     * ControlFlowExpressionChecker, and others.
     * 
     * @param type1 First type to compare
     * @param type2 Second type to compare
     * @return true if types are structurally equivalent
     */
    fun areEqual(type1: Type, type2: Type): Boolean {
        return TypeComparison.structuralEquals(type1, type2)
    }
    
    /**
     * Check if two types are compatible for assignment and operations.
     * 
     * Handles compatibility including numeric promotions and nullable conversions.
     * Replaces scattered compatibility logic throughout type checkers.
     * 
     * @param type1 First type to check
     * @param type2 Second type to check  
     * @return true if types are compatible
     */
    fun areCompatible(type1: Type, type2: Type): Boolean {
        return TypeComparison.areCompatible(type1, type2)
    }
    
    /**
     * Check if type1 is a subtype of type2.
     * 
     * Implements comprehensive subtyping relationships including inheritance,
     * generic variance, and structural subtyping for function types.
     * 
     * @param subtype The potential subtype
     * @param supertype The potential supertype
     * @return true if subtype relationship holds
     */
    fun isSubtype(subtype: Type, supertype: Type): Boolean {
        return TypeComparison.isSubtype(subtype, supertype)
    }
    
    // =============================================================================
    // Type Unification Operations
    // =============================================================================
    
    /**
     * Unify two types with comprehensive error handling.
     * 
     * Provides optimized unification workflow leveraging existing Unifier system
     * with pre-unification optimizations and performance enhancements.
     * 
     * @param type1 First type to unify
     * @param type2 Second type to unify
     * @return UnificationResult with success/failure details
     */
    fun unify(type1: Type, type2: Type): TypeUnification.UnificationResult {
        return TypeUnification.unify(type1, type2)
    }
    
    /**
     * Check if two types can be unified without computing substitution.
     * 
     * Optimized check for unification possibility, useful for type compatibility
     * testing without the overhead of computing the actual unifier.
     * 
     * @param type1 First type to test
     * @param type2 Second type to test
     * @return true if types can be unified
     */
    fun canUnify(type1: Type, type2: Type): Boolean {
        return TypeUnification.canUnify(type1, type2)
    }
    
    /**
     * Unify types with existing substitution context.
     * 
     * Useful for constraint solving scenarios where partial substitution exists.
     * Composes substitutions efficiently while maintaining correctness.
     * 
     * @param type1 First type to unify
     * @param type2 Second type to unify
     * @param substitution Existing substitution context
     * @return UnificationResult with updated substitution
     */
    fun unifyWithSubstitution(
        type1: Type, 
        type2: Type, 
        substitution: Substitution
    ): TypeUnification.UnificationResult {
        return TypeUnification.unifyWithSubstitution(type1, type2, substitution)
    }
    
    // =============================================================================
    // Type Creation Operations
    // =============================================================================
    
    /**
     * Create primitive type with caching optimization.
     * 
     * Replaces direct Type.PrimitiveType construction throughout codebase
     * with cached instances for optimal memory usage and performance.
     * 
     * @param name Primitive type name (e.g., "Int", "String")
     * @return Cached primitive type instance
     */
    fun createPrimitive(name: String): Type.PrimitiveType {
        return TypeFactory.createPrimitive(name)
    }
    
    /**
     * Create generic type with argument caching.
     * 
     * Optimized generic type creation with composite key caching for
     * frequently used generic instantiations like List<Int>, Map<String, T>.
     * 
     * @param name Generic type name
     * @param arguments Type arguments
     * @return Cached generic type instance
     */
    fun createGeneric(name: String, arguments: List<Type>): Type.GenericType {
        return TypeFactory.createGeneric(name, arguments)
    }
    
    /**
     * Create function type with signature caching.
     * 
     * Critical optimization for type checking performance, caching function
     * types by complete signature (parameters + return type).
     * 
     * @param parameterTypes Function parameter types
     * @param returnType Function return type  
     * @return Cached function type instance
     */
    fun createFunction(parameterTypes: List<Type>, returnType: Type): Type.FunctionType {
        return TypeFactory.createFunction(parameterTypes, returnType)
    }
    
    /**
     * Create tuple type with element caching.
     * 
     * Optimizes tuple type creation for common patterns, reducing object
     * creation overhead for frequently used tuple structures.
     * 
     * @param elementTypes Tuple element types
     * @return Cached tuple type instance
     */
    fun createTuple(elementTypes: List<Type>): Type.TupleType {
        return TypeFactory.createTuple(elementTypes)
    }
    
    /**
     * Create nullable type wrapper with caching.
     * 
     * @param baseType Type to make nullable
     * @return Cached nullable type instance
     */
    fun createNullable(baseType: Type): Type.NullableType {
        return TypeFactory.createNullable(baseType)
    }
    
    // =============================================================================
    // Type Validation Operations
    // =============================================================================
    
    /**
     * Comprehensive type validation.
     * 
     * Validates type according to TaylorLang type system rules,
     * consolidating scattered validation logic into consistent checks.
     * 
     * @param type Type to validate
     * @return ValidationResult with detailed error information
     */
    fun validate(type: Type): TypeValidation.ValidationResult {
        return TypeValidation.validate(type)
    }
    
    /**
     * Check if type conversion is valid.
     * 
     * Determines conversion validity according to language-defined rules
     * including numeric promotion, subtyping, and nullable conversions.
     * 
     * @param sourceType Source type for conversion
     * @param targetType Target type for conversion
     * @return true if conversion is valid
     */
    fun canConvert(sourceType: Type, targetType: Type): Boolean {
        return TypeValidation.canConvert(sourceType, targetType)
    }
    
    /**
     * Get wider type for numeric promotion.
     * 
     * Finds appropriate target type for numeric type widening in
     * binary operations and arithmetic expressions.
     * 
     * @param type1 First numeric type
     * @param type2 Second numeric type
     * @return Wider type or null if not applicable
     */
    fun getWiderType(type1: Type, type2: Type): Type? {
        return TypeValidation.getWiderType(type1, type2)
    }
    
    // =============================================================================
    // Convenience Methods for Common Patterns
    // =============================================================================
    
    /**
     * Check if type is a primitive builtin type.
     */
    fun isPrimitive(type: Type): Boolean {
        return type is Type.PrimitiveType && BuiltinTypes.isPrimitive(type)
    }
    
    /**
     * Check if type is numeric (supports arithmetic operations).
     */
    fun isNumeric(type: Type): Boolean {
        return BuiltinTypes.isNumeric(type)
    }
    
    /**
     * Check if type contains type variables (useful for generics).
     */
    fun containsTypeVariables(type: Type): Boolean {
        return TypeComparison.containsTypeVariables(type)
    }
    
    /**
     * Extract all type variables from a type.
     */
    fun extractTypeVariables(type: Type): Set<String> {
        return TypeComparison.extractTypeVariables(type)
    }
    
    /**
     * Get builtin primitive types with optimal caching.
     */
    object Builtins {
        val INT: Type.PrimitiveType by lazy { TypeFactory.Builtins.INT }
        val LONG: Type.PrimitiveType by lazy { TypeFactory.Builtins.LONG }
        val FLOAT: Type.PrimitiveType by lazy { TypeFactory.Builtins.FLOAT }
        val DOUBLE: Type.PrimitiveType by lazy { TypeFactory.Builtins.DOUBLE }
        val BOOLEAN: Type.PrimitiveType by lazy { TypeFactory.Builtins.BOOLEAN }
        val STRING: Type.PrimitiveType by lazy { TypeFactory.Builtins.STRING }
        val UNIT: Type.PrimitiveType by lazy { TypeFactory.Builtins.UNIT }
    }
    
    // =============================================================================
    // Advanced Operations for Complex Scenarios
    // =============================================================================
    
    /**
     * Solve constraint set with optimizations.
     * 
     * Provides direct access to constraint solving with pre-processing
     * optimizations for enhanced performance.
     * 
     * @param constraints Set of type constraints
     * @return ConstraintSolutionResult with substitution or errors
     */
    fun solveConstraints(constraints: ConstraintSet): TypeUnification.ConstraintSolutionResult {
        return TypeUnification.solveConstraints(constraints)
    }
    
    /**
     * Validate multiple types with aggregated error reporting.
     */
    fun validateAll(types: List<Type>): TypeValidation.ValidationResult {
        return TypeValidation.validateAll(types)
    }
    
    /**
     * Unify list of types to find common supertype.
     */
    fun unifyAll(types: List<Type>): TypeUnification.UnificationResult? {
        return TypeUnification.unifyTypes(types)
    }
    
    // =============================================================================
    // Performance Monitoring and Cache Management
    // =============================================================================
    
    /**
     * Performance statistics for monitoring optimization effectiveness.
     */
    data class PerformanceStats(
        val cacheStats: TypeFactory.CacheStats,
        val comparisonCacheSize: Int,
        val estimatedMemorySaved: Long
    )
    
    /**
     * Get comprehensive performance statistics.
     * 
     * Useful for monitoring the effectiveness of caching and optimization
     * strategies across all type operations.
     */
    fun getPerformanceStats(): PerformanceStats {
        return PerformanceStats(
            cacheStats = TypeFactory.getCacheStats(),
            comparisonCacheSize = TypeComparison.getCacheSize(),
            estimatedMemorySaved = TypeFactory.estimateMemoryUsage()
        )
    }
    
    /**
     * Clear all caches for memory management.
     * 
     * Use cautiously as it eliminates performance optimizations.
     * Primarily for testing and memory pressure scenarios.
     */
    fun clearAllCaches() {
        TypeFactory.clearCaches()
        TypeComparison.clearCache()
    }
    
    // =============================================================================
    // Migration Helper Methods
    // =============================================================================
    
    /**
     * Legacy compatibility method for existing typesCompatible() calls.
     * 
     * Provides backwards compatibility during migration phase.
     * Maps to the new areCompatible() method with identical semantics.
     * 
     * @deprecated Use areCompatible() instead
     */
    @Deprecated(
        message = "Use areCompatible() instead", 
        replaceWith = ReplaceWith("areCompatible(type1, type2)")
    )
    fun typesCompatible(type1: Type, type2: Type): Boolean {
        return areCompatible(type1, type2)
    }
    
    /**
     * Legacy compatibility method for structural equality.
     * 
     * @deprecated Use areEqual() instead
     */
    @Deprecated(
        message = "Use areEqual() instead",
        replaceWith = ReplaceWith("areEqual(type1, type2)")
    )
    fun typesStructurallyEqual(type1: Type, type2: Type): Boolean {
        return areEqual(type1, type2)
    }
}