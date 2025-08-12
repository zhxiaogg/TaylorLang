package org.taylorlang.typechecker

import org.taylorlang.ast.Type

/**
 * Centralized type validation and conversion operations.
 * 
 * This component consolidates scattered type validation logic found throughout
 * the type checking system. It provides consistent validation rules, type
 * conversion capabilities, and comprehensive error reporting for type-related
 * validation failures.
 * 
 * Consolidates validation from:
 * - BuiltinTypes validation scattered across multiple checkers
 * - Type argument validation duplicated in several files
 * - Generic type parameter checking repeated throughout
 * - Type conversion logic spread across expression checkers
 */
object TypeValidation {
    
    // =============================================================================
    // Core Validation Operations
    // =============================================================================
    
    /**
     * Comprehensive type validation.
     * 
     * Validates a type according to TaylorLang type system rules,
     * including structural validity, constraint satisfaction, and
     * semantic consistency.
     * 
     * @param type The type to validate
     * @return ValidationResult indicating success or specific failures
     */
    fun validate(type: Type): ValidationResult {
        return when (type) {
            is Type.PrimitiveType -> validatePrimitiveType(type)
            is Type.NamedType -> validateNamedType(type)
            is Type.GenericType -> validateGenericType(type)
            is Type.TupleType -> validateTupleType(type)
            is Type.FunctionType -> validateFunctionType(type)
            is Type.NullableType -> validateNullableType(type)
            is Type.UnionType -> validateUnionType(type)
            is Type.TypeVar -> validateTypeVar(type)
        }
    }
    
    /**
     * Check if conversion from source type to target type is valid.
     * 
     * Determines whether a value of the source type can be converted
     * to the target type through language-defined conversion rules.
     * 
     * @param sourceType The source type for conversion
     * @param targetType The target type for conversion
     * @return true if conversion is valid
     */
    fun canConvert(sourceType: Type, targetType: Type): Boolean {
        // Direct type equality
        if (TypeComparison.structuralEquals(sourceType, targetType)) return true
        
        // Check nullable conversions first - these have specific conversion semantics
        // that override general subtyping relationships
        if (sourceType is Type.NullableType || targetType is Type.NullableType) {
            return isValidNullableConversion(sourceType, targetType)
        }
        
        // Subtyping relationship (after nullable checks)
        if (TypeComparison.isSubtype(sourceType, targetType)) return true
        
        // Numeric conversions
        if (isValidNumericConversion(sourceType, targetType)) return true
        
        // Generic type conversions (covariance/contravariance)
        if (isValidGenericConversion(sourceType, targetType)) return true
        
        return false
    }
    
    /**
     * Get the wider type for numeric type promotion.
     * 
     * Determines the appropriate target type when two numeric types
     * need to be unified through type widening (e.g., Int + Float → Float).
     * 
     * @param type1 First numeric type
     * @param type2 Second numeric type
     * @return The wider type, or null if not applicable
     */
    fun getWiderType(type1: Type, type2: Type): Type? {
        return BuiltinTypes.getWiderNumericType(type1, type2)
    }
    
    /**
     * Validate type arguments for generic types.
     * 
     * Ensures that type arguments satisfy constraints and bounds
     * defined for generic type parameters.
     * 
     * @param genericType The generic type to validate
     * @return ValidationResult for type arguments
     */
    fun validateTypeArguments(genericType: Type.GenericType): ValidationResult {
        // Validate each type argument individually
        for (argument in genericType.arguments) {
            val argumentValidation = validate(argument)
            if (!argumentValidation.isValid) {
                return ValidationResult.Invalid(
                    errors = listOf(
                        ValidationError.InvalidTypeArgument(
                            genericType = genericType,
                            invalidArgument = argument,
                            reason = argumentValidation.errors.firstOrNull()?.message ?: "Invalid type argument"
                        )
                    )
                )
            }
        }
        
        // Additional generic-specific validations
        return validateGenericConstraints(genericType)
    }
    
    // =============================================================================
    // Specific Type Validation Methods
    // =============================================================================
    
    /**
     * Validate primitive type correctness.
     */
    private fun validatePrimitiveType(type: Type.PrimitiveType): ValidationResult {
        return if (BuiltinTypes.primitiveNames.contains(type.name)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                errors = listOf(
                    ValidationError.UnknownPrimitiveType(type.name)
                )
            )
        }
    }
    
    /**
     * Validate named type (could be user-defined or type parameter).
     */
    private fun validateNamedType(type: Type.NamedType): ValidationResult {
        // Named types are generally valid - resolution happens in type checking context
        // Additional validation would require context information
        return ValidationResult.Valid
    }
    
    /**
     * Validate generic type structure and arguments.
     */
    private fun validateGenericType(type: Type.GenericType): ValidationResult {
        // Validate type arguments recursively
        val argumentValidations = type.arguments.map { validate(it) }
        val invalidArguments = argumentValidations.filter { !it.isValid }
        
        return if (invalidArguments.isEmpty()) {
            validateGenericConstraints(type)
        } else {
            ValidationResult.Invalid(
                errors = invalidArguments.flatMap { it.errors }
            )
        }
    }
    
    /**
     * Validate tuple type elements.
     */
    private fun validateTupleType(type: Type.TupleType): ValidationResult {
        // Empty tuples are valid (unit-like)
        if (type.elementTypes.isEmpty()) return ValidationResult.Valid
        
        // Validate each element type
        val elementValidations = type.elementTypes.map { validate(it) }
        val invalidElements = elementValidations.filter { !it.isValid }
        
        return if (invalidElements.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                errors = invalidElements.flatMap { it.errors }
            )
        }
    }
    
    /**
     * Validate function type signature.
     */
    private fun validateFunctionType(type: Type.FunctionType): ValidationResult {
        // Validate parameter types
        val paramValidations = type.parameterTypes.map { validate(it) }
        val invalidParams = paramValidations.filter { !it.isValid }
        
        // Validate return type
        val returnValidation = validate(type.returnType)
        
        return if (invalidParams.isEmpty() && returnValidation.isValid) {
            ValidationResult.Valid
        } else {
            val allErrors = invalidParams.flatMap { it.errors } + 
                          if (returnValidation.isValid) emptyList() else returnValidation.errors
            ValidationResult.Invalid(errors = allErrors)
        }
    }
    
    /**
     * Validate nullable type wrapper.
     */
    private fun validateNullableType(type: Type.NullableType): ValidationResult {
        return validate(type.baseType)
    }
    
    /**
     * Validate union type reference.
     */
    private fun validateUnionType(type: Type.UnionType): ValidationResult {
        // Validate type arguments if present
        if (type.typeArguments.isNotEmpty()) {
            val argValidations = type.typeArguments.map { validate(it) }
            val invalidArgs = argValidations.filter { !it.isValid }
            
            if (invalidArgs.isNotEmpty()) {
                return ValidationResult.Invalid(
                    errors = invalidArgs.flatMap { it.errors }
                )
            }
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Validate type variable.
     */
    private fun validateTypeVar(type: Type.TypeVar): ValidationResult {
        // Type variables are valid - they represent unresolved inference variables
        return ValidationResult.Valid
    }
    
    // =============================================================================
    // Conversion Validation Helpers
    // =============================================================================
    
    /**
     * Check if numeric conversion is valid according to type promotion rules.
     */
    private fun isValidNumericConversion(sourceType: Type, targetType: Type): Boolean {
        if (sourceType !is Type.PrimitiveType || targetType !is Type.PrimitiveType) return false
        
        val numericHierarchy = mapOf(
            "Int" to 0,
            "Long" to 1, 
            "Float" to 2,
            "Double" to 3
        )
        
        val sourceLevel = numericHierarchy[sourceType.name] ?: return false
        val targetLevel = numericHierarchy[targetType.name] ?: return false
        
        // Allow widening conversions
        return sourceLevel <= targetLevel
    }
    
    /**
     * Check if nullable conversion is valid.
     */
    private fun isValidNullableConversion(sourceType: Type, targetType: Type): Boolean {
        return when {
            // T → T? (make nullable)
            targetType is Type.NullableType -> 
                TypeComparison.structuralEquals(sourceType, targetType.baseType)
            // T? → T requires explicit null check (not automatic conversion)
            sourceType is Type.NullableType -> false
            else -> false
        }
    }
    
    /**
     * Check if generic type conversion is valid (considering variance).
     */
    private fun isValidGenericConversion(sourceType: Type, targetType: Type): Boolean {
        if (sourceType !is Type.GenericType || targetType !is Type.GenericType) return false
        if (sourceType.name != targetType.name) return false
        if (sourceType.arguments.size != targetType.arguments.size) return false
        
        // For now, implement invariant generics (could be extended for variance)
        return sourceType.arguments.zip(targetType.arguments)
            .all { (sourceArg, targetArg) -> 
                TypeComparison.structuralEquals(sourceArg, targetArg) 
            }
    }
    
    /**
     * Validate generic type constraints including Result type Throwable constraints.
     */
    private fun validateGenericConstraints(genericType: Type.GenericType): ValidationResult {
        // Special validation for Result<T, E> types
        if (BuiltinTypes.isResultType(genericType)) {
            return validateResultTypeConstraints(genericType)
        }
        
        // Other generic type constraints would go here
        // For now, other generic types are valid
        return ValidationResult.Valid
    }
    
    /**
     * Validate Result<T, E> type constraints.
     * Ensures that the error type E is a subtype of Throwable.
     */
    private fun validateResultTypeConstraints(resultType: Type.GenericType): ValidationResult {
        if (!BuiltinTypes.isResultType(resultType)) {
            return ValidationResult.Invalid(
                errors = listOf(
                    ValidationError.TypeConstraintViolation(
                        type = resultType,
                        constraint = "Result type structure",
                        reason = "Type is not a valid Result<T, E>"
                    )
                )
            )
        }
        
        // Validate that error type is Throwable subtype
        if (!BuiltinTypes.validateResultErrorType(resultType)) {
            val errorType = BuiltinTypes.getResultErrorType(resultType)
            return ValidationResult.Invalid(
                errors = listOf(
                    ValidationError.ResultErrorTypeViolation(
                        resultType = resultType,
                        errorType = errorType,
                        reason = "Error type must be a subtype of Throwable"
                    )
                )
            )
        }
        
        return ValidationResult.Valid
    }
    
    // =============================================================================
    // Validation Result Types
    // =============================================================================
    
    /**
     * Result of type validation operation.
     */
    sealed class ValidationResult {
        abstract val isValid: Boolean
        abstract val errors: List<ValidationError>
        
        /**
         * Successful validation result.
         */
        object Valid : ValidationResult() {
            override val isValid: Boolean = true
            override val errors: List<ValidationError> = emptyList()
        }
        
        /**
         * Failed validation with specific errors.
         */
        data class Invalid(
            override val errors: List<ValidationError>
        ) : ValidationResult() {
            override val isValid: Boolean = false
        }
    }
    
    /**
     * Specific validation errors with detailed information.
     */
    sealed class ValidationError {
        abstract val message: String
        
        data class UnknownPrimitiveType(val typeName: String) : ValidationError() {
            override val message: String = "Unknown primitive type: $typeName"
        }
        
        data class InvalidTypeArgument(
            val genericType: Type.GenericType,
            val invalidArgument: Type,
            val reason: String
        ) : ValidationError() {
            override val message: String = 
                "Invalid type argument $invalidArgument for ${genericType.name}: $reason"
        }
        
        data class TypeConstraintViolation(
            val type: Type,
            val constraint: String,
            val reason: String
        ) : ValidationError() {
            override val message: String = 
                "Type $type violates constraint $constraint: $reason"
        }
        
        data class InvalidConversion(
            val sourceType: Type,
            val targetType: Type,
            val reason: String
        ) : ValidationError() {
            override val message: String = 
                "Invalid conversion from $sourceType to $targetType: $reason"
        }
        
        data class CircularTypeReference(
            val type: Type,
            val cycle: List<Type>
        ) : ValidationError() {
            override val message: String = 
                "Circular type reference detected: ${cycle.joinToString(" -> ")}"
        }
        
        data class ResultErrorTypeViolation(
            val resultType: Type.GenericType,
            val errorType: Type?,
            val reason: String
        ) : ValidationError() {
            override val message: String = 
                "Result type ${resultType} has invalid error type ${errorType}: $reason"
        }
    }
    
    // =============================================================================
    // Utility Methods
    // =============================================================================
    
    /**
     * Validate a list of types and collect all errors.
     */
    fun validateAll(types: List<Type>): ValidationResult {
        val validations = types.map { validate(it) }
        val allErrors = validations.flatMap { it.errors }
        
        return if (allErrors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors = allErrors)
        }
    }
    
    /**
     * Check if a type is a valid function parameter type.
     */
    fun isValidParameterType(type: Type): Boolean {
        return when (validate(type)) {
            is ValidationResult.Valid -> true
            is ValidationResult.Invalid -> false
        }
    }
    
    /**
     * Check if a type is a valid function return type.
     */
    fun isValidReturnType(type: Type): Boolean {
        // Same rules as parameter types in TaylorLang
        return isValidParameterType(type)
    }
}