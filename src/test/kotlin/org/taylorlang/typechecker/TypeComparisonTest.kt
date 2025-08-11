package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import org.taylorlang.ast.Type
import kotlinx.collections.immutable.persistentListOf

class TypeComparisonTest : StringSpec({

    beforeEach {
        TypeComparison.clearCache()
    }

    "structuralEquals should handle identical references with fast path" {
        val type = Type.PrimitiveType("Int")
        TypeComparison.structuralEquals(type, type).shouldBeTrue()
    }

    "structuralEquals should compare primitive types correctly" {
        val int1 = Type.PrimitiveType("Int")
        val int2 = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")

        TypeComparison.structuralEquals(int1, int2).shouldBeTrue()
        TypeComparison.structuralEquals(int1, string).shouldBeFalse()
    }

    "structuralEquals should compare generic types recursively" {
        val elementType = Type.PrimitiveType("Int")
        val generic1 = Type.GenericType("List", persistentListOf(elementType))
        val generic2 = Type.GenericType("List", persistentListOf(elementType))
        val generic3 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("String")))

        TypeComparison.structuralEquals(generic1, generic2).shouldBeTrue()
        TypeComparison.structuralEquals(generic1, generic3).shouldBeFalse()
    }

    "areCompatible should handle numeric promotions" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val float = Type.PrimitiveType("Float")
        val double = Type.PrimitiveType("Double")

        TypeComparison.areCompatible(int, long).shouldBeTrue()
        TypeComparison.areCompatible(int, float).shouldBeTrue()
        TypeComparison.areCompatible(long, double).shouldBeTrue()
    }

    "areCompatible should handle nullable compatibility" {
        val int = Type.PrimitiveType("Int")
        val nullableInt = Type.NullableType(baseType = int)

        TypeComparison.areCompatible(int, nullableInt).shouldBeTrue()
        TypeComparison.areCompatible(nullableInt, int).shouldBeFalse()
    }

    "isSubtype should be reflexive" {
        val type = Type.PrimitiveType("Int")
        TypeComparison.isSubtype(type, type).shouldBeTrue()
    }

    "isSubtype should handle numeric subtyping hierarchy" {
        val int = Type.PrimitiveType("Int")
        val long = Type.PrimitiveType("Long")
        val double = Type.PrimitiveType("Double")

        TypeComparison.isSubtype(int, long).shouldBeTrue()
        TypeComparison.isSubtype(int, double).shouldBeTrue()
        TypeComparison.isSubtype(long, int).shouldBeFalse()
    }

    "containsTypeVariables should detect type variables correctly" {
        val typeVar = Type.TypeVar("T")
        val primitive = Type.PrimitiveType("Int")
        val generic = Type.GenericType("List", persistentListOf(typeVar))

        TypeComparison.containsTypeVariables(typeVar).shouldBeTrue()
        TypeComparison.containsTypeVariables(generic).shouldBeTrue()
        TypeComparison.containsTypeVariables(primitive).shouldBeFalse()
    }

    "extractTypeVariables should collect all type variables" {
        val typeVar1 = Type.TypeVar("T")
        val typeVar2 = Type.TypeVar("U")
        val funcType = Type.FunctionType(
            persistentListOf(typeVar1),
            Type.GenericType("List", persistentListOf(typeVar2))
        )

        val variables = TypeComparison.extractTypeVariables(funcType)
        variables shouldBe setOf("T", "U")
    }

    "getCacheSize should reflect cache utilization" {
        val initialSize = TypeComparison.getCacheSize()

        repeat(5) { i ->
            val type1 = Type.PrimitiveType("Type$i")
            val type2 = Type.PrimitiveType("Type$i")
            TypeComparison.structuralEquals(type1, type2)
        }

        val finalSize = TypeComparison.getCacheSize()
        (finalSize > initialSize).shouldBeTrue()
    }

    "clearCache should reset memoization cache" {
        val type1 = Type.PrimitiveType("CacheTest")
        val type2 = Type.PrimitiveType("CacheTest")

        TypeComparison.structuralEquals(type1, type2)
        (TypeComparison.getCacheSize() > 0).shouldBeTrue()

        TypeComparison.clearCache()
        TypeComparison.getCacheSize() shouldBe 0
    }
})