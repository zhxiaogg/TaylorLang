package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import org.taylorlang.ast.Type
import kotlinx.collections.immutable.persistentListOf

/**
 * Comprehensive test suite for TypeOperations facade.
 * 
 * Tests facade functionality, delegation patterns, and integration
 * with all underlying type operation components. Ensures proper
 * abstraction and consistent behavior across all operations.
 * 
 * Coverage targets:
 * - Facade delegation and integration (30+ tests)
 * - Error handling and edge cases
 * - Performance optimization effectiveness
 * - Migration compatibility layer
 */
class TypeOperationsTest : StringSpec({

    beforeEach {
        // Clear all caches before each test for isolated testing
        TypeOperations.clearAllCaches()
    }

    // Type Equality Operations Tests
    "areEqual should delegate to TypeComparison for primitive types" {
        val int1 = Type.PrimitiveType("Int")
        val int2 = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")

        TypeOperations.areEqual(int1, int2).shouldBeTrue()
        TypeOperations.areEqual(int1, string).shouldBeFalse()
    }

    "areEqual should handle identical references with fast path" {
        val type = Type.PrimitiveType("Int")
        TypeOperations.areEqual(type, type).shouldBeTrue()
    }

    "areEqual should work with generic types" {
        val list1 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        val list2 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        val list3 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("String")))

        TypeOperations.areEqual(list1, list2).shouldBeTrue()
        TypeOperations.areEqual(list1, list3).shouldBeFalse()
    }

    "areEqual should work with function types" {
        val params = persistentListOf(Type.PrimitiveType("Int"))
        val returnType = Type.PrimitiveType("String")
        
        val func1 = Type.FunctionType(params, returnType)
        val func2 = Type.FunctionType(params, returnType)
        val func3 = Type.FunctionType(params, Type.PrimitiveType("Boolean"))

        TypeOperations.areEqual(func1, func2).shouldBeTrue()
        TypeOperations.areEqual(func1, func3).shouldBeFalse()
    }

    "areEqual should work with nested complex types" {
        val innerType = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        val outer1 = Type.GenericType("Map", persistentListOf(Type.PrimitiveType("String"), innerType))
        val outer2 = Type.GenericType("Map", persistentListOf(Type.PrimitiveType("String"), innerType))

        TypeOperations.areEqual(outer1, outer2).shouldBeTrue()
    }

    // Type Compatibility Tests
    "areCompatible should delegate to TypeComparison" {
        val int = Type.PrimitiveType("Int")
        val double = Type.PrimitiveType("Double")
        val string = Type.PrimitiveType("String")

        TypeOperations.areCompatible(int, double).shouldBeTrue()
        TypeOperations.areCompatible(int, string).shouldBeFalse()
    }

    "areCompatible should handle numeric promotions" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val float = Type.PrimitiveType("Float")
        val double = Type.PrimitiveType("Double")

        TypeOperations.areCompatible(int, long).shouldBeTrue()
        TypeOperations.areCompatible(int, float).shouldBeTrue()
        TypeOperations.areCompatible(long, double).shouldBeTrue()
    }

    "areCompatible should handle nullable compatibility" {
        val int = Type.PrimitiveType("Int")
        val nullableInt = Type.NullableType(baseType = int)

        TypeOperations.areCompatible(int, nullableInt).shouldBeTrue()
        TypeOperations.areCompatible(nullableInt, int).shouldBeFalse()
    }

    // Subtype Tests
    "isSubtype should delegate to TypeComparison" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val string = Type.PrimitiveType("String")

        TypeOperations.isSubtype(int, long).shouldBeTrue()
        TypeOperations.isSubtype(long, int).shouldBeFalse()
        TypeOperations.isSubtype(int, string).shouldBeFalse()
    }

    "isSubtype should be reflexive" {
        val type = Type.PrimitiveType("Int")
        TypeOperations.isSubtype(type, type).shouldBeTrue()
    }

    "isSubtype should handle function type variance" {
        val intType = Type.PrimitiveType("Int")
        val longType = Type.PrimitiveType("Long")

        // Function subtyping: (Long) -> Int <: (Int) -> Long
        val subFunc = Type.FunctionType(persistentListOf(longType), intType)
        val superFunc = Type.FunctionType(persistentListOf(intType), longType)

        TypeOperations.isSubtype(subFunc, superFunc).shouldBeTrue()
    }

    // Unification Tests
    "unify should delegate to TypeUnification" {
        val int = Type.PrimitiveType("Int")
        val typeVar = Type.TypeVar("T")

        val result = TypeOperations.unify(int, typeVar)
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
    }

    "unify should handle identical types" {
        val int = Type.PrimitiveType("Int")
        val result = TypeOperations.unify(int, int)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe int
    }

    "unify should fail for incompatible types" {
        val int = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")

        val result = TypeOperations.unify(int, string)
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Failure>()
    }

    "canUnify should provide boolean result" {
        val int = Type.PrimitiveType("Int")
        val typeVar = Type.TypeVar("T")
        val string = Type.PrimitiveType("String")

        TypeOperations.canUnify(int, typeVar).shouldBeTrue()
        TypeOperations.canUnify(int, string).shouldBeFalse()
    }

    "unifyWithSubstitution should handle existing context" {
        val typeVar1 = Type.TypeVar("T")
        val typeVar2 = Type.TypeVar("U")
        val int = Type.PrimitiveType("Int")

        val initialSubst = Substitution.single(TypeVar.named("T"), int)
        val result = TypeOperations.unifyWithSubstitution(typeVar1, typeVar2, initialSubst)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
    }

    // Type Factory Integration Tests
    "createPrimitive should delegate to TypeFactory" {
        val type1 = TypeOperations.createPrimitive("Int")
        val type2 = TypeOperations.createPrimitive("Int")

        type1.name shouldBe "Int"
        // Should return same cached instance
        (type1 === type2).shouldBeTrue()
    }

    "createGeneric should delegate to TypeFactory with caching" {
        val elementType = Type.PrimitiveType("Int")
        val list1 = TypeOperations.createGeneric("List", listOf(elementType))
        val list2 = TypeOperations.createGeneric("List", listOf(elementType))

        list1.name shouldBe "List"
        list1.arguments.size shouldBe 1
        // Should return same cached instance
        (list1 === list2).shouldBeTrue()
    }

    "createFunction should delegate to TypeFactory" {
        val paramTypes = listOf(Type.PrimitiveType("Int"))
        val returnType = Type.PrimitiveType("String")

        val func1 = TypeOperations.createFunction(paramTypes, returnType)
        val func2 = TypeOperations.createFunction(paramTypes, returnType)

        func1.returnType shouldBe returnType
        func1.parameterTypes.size shouldBe 1
        // Should return same cached instance
        (func1 === func2).shouldBeTrue()
    }

    "createTuple should delegate to TypeFactory" {
        val elementTypes = listOf(Type.PrimitiveType("Int"), Type.PrimitiveType("String"))
        
        val tuple1 = TypeOperations.createTuple(elementTypes)
        val tuple2 = TypeOperations.createTuple(elementTypes)

        tuple1.elementTypes.size shouldBe 2
        // Should return same cached instance
        (tuple1 === tuple2).shouldBeTrue()
    }

    "createNullable should delegate to TypeFactory" {
        val baseType = Type.PrimitiveType("Int")
        val nullable1 = TypeOperations.createNullable(baseType)
        val nullable2 = TypeOperations.createNullable(baseType)

        nullable1.baseType shouldBe baseType
        // Should return same cached instance
        (nullable1 === nullable2).shouldBeTrue()
    }

    // Validation Integration Tests
    "validate should delegate to TypeValidation" {
        val validType = Type.PrimitiveType("Int")
        val invalidType = Type.PrimitiveType("UnknownType")

        val validResult = TypeOperations.validate(validType)
        val invalidResult = TypeOperations.validate(invalidType)

        validResult.isValid.shouldBeTrue()
        invalidResult.isValid.shouldBeFalse()
    }

    "canConvert should delegate to TypeValidation" {
        val int = Type.PrimitiveType("Int")
        val double = Type.PrimitiveType("Double")
        val string = Type.PrimitiveType("String")

        TypeOperations.canConvert(int, double).shouldBeTrue()
        TypeOperations.canConvert(double, int).shouldBeFalse()
        TypeOperations.canConvert(int, string).shouldBeFalse()
    }

    "getWiderType should handle numeric widening" {
        val int = Type.PrimitiveType("Int")
        val double = Type.PrimitiveType("Double")

        val widerType = TypeOperations.getWiderType(int, double)
        widerType shouldBe double
    }

    "getWiderType should return null for non-numeric types" {
        val int = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")

        val widerType = TypeOperations.getWiderType(int, string)
        widerType.shouldBeNull()
    }

    // Convenience Methods Tests
    "isPrimitive should identify primitive types correctly" {
        val primitive = Type.PrimitiveType("Int")
        val generic = Type.GenericType("List", persistentListOf(primitive))

        TypeOperations.isPrimitive(primitive).shouldBeTrue()
        TypeOperations.isPrimitive(generic).shouldBeFalse()
    }

    "isNumeric should identify numeric types" {
        val int = Type.PrimitiveType("Int")
        val double = Type.PrimitiveType("Double")
        val string = Type.PrimitiveType("String")
        val boolean = Type.PrimitiveType("Boolean")

        TypeOperations.isNumeric(int).shouldBeTrue()
        TypeOperations.isNumeric(double).shouldBeTrue()
        TypeOperations.isNumeric(string).shouldBeFalse()
        TypeOperations.isNumeric(boolean).shouldBeFalse()
    }

    "containsTypeVariables should detect type variables" {
        val typeVar = Type.TypeVar("T")
        val primitive = Type.PrimitiveType("Int")
        val generic = Type.GenericType("List", persistentListOf(typeVar))

        TypeOperations.containsTypeVariables(typeVar).shouldBeTrue()
        TypeOperations.containsTypeVariables(generic).shouldBeTrue()
        TypeOperations.containsTypeVariables(primitive).shouldBeFalse()
    }

    "extractTypeVariables should collect all type variables" {
        val typeVar1 = Type.TypeVar("T")
        val typeVar2 = Type.TypeVar("U")
        val funcType = Type.FunctionType(
            persistentListOf(typeVar1),
            Type.GenericType("List", persistentListOf(typeVar2))
        )

        val variables = TypeOperations.extractTypeVariables(funcType)
        variables shouldBe setOf("T", "U")
    }

    "Builtins should provide cached primitive types" {
        val int1 = TypeOperations.Builtins.INT
        val int2 = TypeOperations.Builtins.INT

        int1.name shouldBe "Int"
        (int1 === int2).shouldBeTrue() // Should be same cached instance
    }

    // Advanced Operations Tests
    "solveConstraints should delegate to TypeUnification" {
        // Create simple constraint set for testing
        val typeVar = Type.TypeVar("T")
        val int = Type.PrimitiveType("Int")
        val constraint = Constraint.Equality(typeVar, int)
        val constraintSet = ConstraintSet.of(constraint)

        val result = TypeOperations.solveConstraints(constraintSet)
        result.shouldBeInstanceOf<TypeUnification.ConstraintSolutionResult.Success>()
    }

    "validateAll should validate multiple types" {
        val validTypes = listOf(
            Type.PrimitiveType("Int"),
            Type.PrimitiveType("String"),
            Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        )

        val result = TypeOperations.validateAll(validTypes)
        result.isValid.shouldBeTrue()
    }

    "unifyAll should find common supertype" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val types = listOf(int, long)

        val result = TypeOperations.unifyAll(types)
        result.shouldNotBeNull()
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
    }

    "unifyAll should handle empty list" {
        val result = TypeOperations.unifyAll(emptyList())
        result.shouldBeNull()
    }

    "unifyAll should handle single type" {
        val int = Type.PrimitiveType("Int")
        val result = TypeOperations.unifyAll(listOf(int))

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe int
    }

    // Performance and Cache Management Tests
    "getPerformanceStats should return comprehensive statistics" {
        // Create some cached types to generate stats
        TypeOperations.createPrimitive("Int")
        TypeOperations.createPrimitive("String")
        TypeOperations.createGeneric("List", listOf(Type.PrimitiveType("Int")))

        val stats = TypeOperations.getPerformanceStats()
        (stats.cacheStats.totalCacheSize > 0).shouldBeTrue()
        (stats.estimatedMemorySaved >= 0).shouldBeTrue()
    }

    "clearAllCaches should reset cache statistics" {
        // Create cached types
        TypeOperations.createPrimitive("Int")
        TypeOperations.createGeneric("List", listOf(Type.PrimitiveType("Int")))

        val statsBefore = TypeOperations.getPerformanceStats()
        (statsBefore.cacheStats.totalCacheSize > 0).shouldBeTrue()

        TypeOperations.clearAllCaches()

        val statsAfter = TypeOperations.getPerformanceStats()
        statsAfter.cacheStats.totalCacheSize shouldBe 0
        statsAfter.comparisonCacheSize shouldBe 0
    }

    // Migration Compatibility Tests
    @Suppress("DEPRECATION")
    "typesCompatible legacy method should work correctly" {
        val int = Type.PrimitiveType("Int")
        val double = Type.PrimitiveType("Double")
        val string = Type.PrimitiveType("String")

        TypeOperations.typesCompatible(int, double).shouldBeTrue()
        TypeOperations.typesCompatible(int, string).shouldBeFalse()
    }

    @Suppress("DEPRECATION") 
    "typesStructurallyEqual legacy method should work correctly" {
        val int1 = Type.PrimitiveType("Int")
        val int2 = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")

        TypeOperations.typesStructurallyEqual(int1, int2).shouldBeTrue()
        TypeOperations.typesStructurallyEqual(int1, string).shouldBeFalse()
    }

    @Suppress("DEPRECATION")
    "legacy methods should delegate to new implementations" {
        val int1 = Type.PrimitiveType("Int")
        val int2 = Type.PrimitiveType("Int")

        // Should produce identical results
        val newResult = TypeOperations.areEqual(int1, int2)
        val legacyResult = TypeOperations.typesStructurallyEqual(int1, int2)

        newResult shouldBe legacyResult
    }

    // Error Handling and Edge Cases Tests
    "operations should handle null-like scenarios gracefully" {
        val nullableType = Type.NullableType(Type.PrimitiveType("Int"))
        val result = TypeOperations.validate(nullableType)

        result.isValid.shouldBeTrue()
    }

    "complex nested types should be handled correctly" {
        // Create deeply nested generic type
        val innerType = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        val middleType = Type.GenericType("Map", persistentListOf(Type.PrimitiveType("String"), innerType))
        val outerType = Type.GenericType("Option", persistentListOf(middleType))

        val validation = TypeOperations.validate(outerType)
        validation.isValid.shouldBeTrue()

        val variables = TypeOperations.extractTypeVariables(outerType)
        variables.isEmpty().shouldBeTrue() // No type variables in this structure
    }

    "circular type references should be detected in type variable extraction" {
        val typeVar = Type.TypeVar("T")
        val recursive = Type.GenericType("List", persistentListOf(typeVar))
        
        // This doesn't create true circularity, but tests the extraction
        val variables = TypeOperations.extractTypeVariables(recursive)
        variables shouldBe setOf("T")
    }

    "empty collections should be handled appropriately" {
        val emptyTuple = TypeOperations.createTuple(emptyList())
        TypeOperations.validate(emptyTuple).isValid.shouldBeTrue()

        val emptyGeneric = TypeOperations.createGeneric("EmptyGeneric", emptyList())
        emptyGeneric.arguments.size shouldBe 0
    }
})