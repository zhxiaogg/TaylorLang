package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldBeEmpty
import org.taylorlang.ast.Type
import kotlinx.collections.immutable.persistentListOf

class TypeValidationTest : StringSpec({

    "validate should accept valid primitive types" {
        val validPrimitives = listOf("Int", "Long", "Float", "Double", "Boolean", "String", "Unit")

        validPrimitives.forEach { typeName ->
            val type = Type.PrimitiveType(typeName)
            val result = TypeValidation.validate(type)
            result.isValid.shouldBeTrue()
            result.errors.shouldBeEmpty()
        }
    }

    "validate should reject unknown primitive types" {
        val invalidPrimitives = listOf("UnknownType", "InvalidPrimitive")

        invalidPrimitives.forEach { typeName ->
            val type = Type.PrimitiveType(typeName)
            val result = TypeValidation.validate(type)
            result.isValid.shouldBeFalse()
            result.errors.shouldNotBeEmpty()
            
            val error = result.errors.first()
            error.shouldBeInstanceOf<TypeValidation.ValidationError.UnknownPrimitiveType>()
            error.typeName shouldBe typeName
        }
    }

    "validate should accept named types" {
        val namedTypes = listOf("UserType", "T", "MyClass")

        namedTypes.forEach { typeName ->
            val type = Type.NamedType(typeName)
            val result = TypeValidation.validate(type)
            result.isValid.shouldBeTrue()
        }
    }

    "validate should accept type variables" {
        val typeVars = listOf("T", "U", "Element")

        typeVars.forEach { varName ->
            val type = Type.TypeVar(varName)
            val result = TypeValidation.validate(type)
            result.isValid.shouldBeTrue()
        }
    }

    "validate should accept valid generic types" {
        val elementType = Type.PrimitiveType("Int")
        val genericType = Type.GenericType("List", persistentListOf(elementType))
        
        val result = TypeValidation.validate(genericType)
        result.isValid.shouldBeTrue()
        result.errors.shouldBeEmpty()
    }

    "validate should reject generic types with invalid arguments" {
        val invalidElement = Type.PrimitiveType("InvalidType")
        val genericType = Type.GenericType("List", persistentListOf(invalidElement))
        
        val result = TypeValidation.validate(genericType)
        result.isValid.shouldBeFalse()
        result.errors.shouldNotBeEmpty()
    }

    "validate should accept valid function types" {
        val paramType = Type.PrimitiveType("Int")
        val returnType = Type.PrimitiveType("String")
        val funcType = Type.FunctionType(persistentListOf(paramType), returnType)
        
        val result = TypeValidation.validate(funcType)
        result.isValid.shouldBeTrue()
    }

    "validate should reject functions with invalid parameter types" {
        val invalidParam = Type.PrimitiveType("InvalidType")
        val returnType = Type.PrimitiveType("String")
        val funcType = Type.FunctionType(persistentListOf(invalidParam), returnType)
        
        val result = TypeValidation.validate(funcType)
        result.isValid.shouldBeFalse()
    }

    "canConvert should allow identical types" {
        val intType = Type.PrimitiveType("Int")
        TypeValidation.canConvert(intType, intType).shouldBeTrue()
    }

    "canConvert should allow numeric widening conversions" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val double = Type.PrimitiveType("Double")

        TypeValidation.canConvert(int, long).shouldBeTrue()
        TypeValidation.canConvert(int, double).shouldBeTrue()
        TypeValidation.canConvert(long, double).shouldBeTrue()
    }

    "canConvert should reject numeric narrowing conversions" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")

        TypeValidation.canConvert(long, int).shouldBeFalse()
    }

    "canConvert should allow nullable conversions" {
        val intType = Type.PrimitiveType("Int")
        val nullableInt = Type.NullableType(baseType = intType)

        TypeValidation.canConvert(intType, nullableInt).shouldBeTrue()
        TypeValidation.canConvert(nullableInt, intType).shouldBeFalse()
    }

    "getWiderType should return wider type for numeric pairs" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val double = Type.PrimitiveType("Double")

        TypeValidation.getWiderType(int, long) shouldBe long
        TypeValidation.getWiderType(long, int) shouldBe long
        TypeValidation.getWiderType(int, double) shouldBe double
    }

    "getWiderType should return null for non-numeric types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")

        TypeValidation.getWiderType(intType, stringType).shouldBeNull()
    }

    "validateAll should accept all valid types" {
        val validTypes = listOf(
            Type.PrimitiveType("Int"),
            Type.PrimitiveType("String"),
            Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        )

        val result = TypeValidation.validateAll(validTypes)
        result.isValid.shouldBeTrue()
        result.errors.shouldBeEmpty()
    }

    "validateAll should reject lists containing invalid types" {
        val mixedTypes = listOf(
            Type.PrimitiveType("Int"),
            Type.PrimitiveType("InvalidType"),
            Type.PrimitiveType("String")
        )

        val result = TypeValidation.validateAll(mixedTypes)
        result.isValid.shouldBeFalse()
        result.errors.shouldNotBeEmpty()
    }

    "isValidParameterType should validate parameter types correctly" {
        val validParam = Type.PrimitiveType("Int")
        val invalidParam = Type.PrimitiveType("InvalidType")

        TypeValidation.isValidParameterType(validParam).shouldBeTrue()
        TypeValidation.isValidParameterType(invalidParam).shouldBeFalse()
    }

    "isValidReturnType should validate return types correctly" {
        val validReturn = Type.PrimitiveType("String")
        val invalidReturn = Type.PrimitiveType("InvalidType")

        TypeValidation.isValidReturnType(validReturn).shouldBeTrue()
        TypeValidation.isValidReturnType(invalidReturn).shouldBeFalse()
    }

    "ValidationResult Valid should have correct properties" {
        val valid = TypeValidation.ValidationResult.Valid
        valid.isValid.shouldBeTrue()
        valid.errors.shouldBeEmpty()
    }

    "ValidationResult Invalid should have correct properties" {
        val error = TypeValidation.ValidationError.UnknownPrimitiveType("InvalidType")
        val invalid = TypeValidation.ValidationResult.Invalid(listOf(error))
        
        invalid.isValid.shouldBeFalse()
        invalid.errors.size shouldBe 1
        invalid.errors.first() shouldBe error
    }
})