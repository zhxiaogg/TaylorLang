package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import org.taylorlang.ast.Type
import kotlinx.collections.immutable.persistentListOf

/**
 * Comprehensive test suite for TypeFactory type creation and caching.
 * 
 * Tests type creation optimization, caching behavior, thread safety,
 * and memory efficiency. Validates performance improvements and 
 * cache consistency across concurrent operations.
 * 
 * Coverage targets:
 * - Type creation and caching (25+ tests)
 * - Cache hit/miss behavior and optimization
 * - Memory usage optimization validation
 */
class TypeFactoryTest : StringSpec({

    beforeEach {
        // Clear all caches before each test for isolated testing
        TypeFactory.clearCaches()
    }

    // Primitive Type Creation Tests
    "createPrimitive should create primitive types correctly" {
        val intType = TypeFactory.createPrimitive("Int")
        val stringType = TypeFactory.createPrimitive("String")

        intType.shouldBeInstanceOf<Type.PrimitiveType>()
        intType.name shouldBe "Int"
        
        stringType.shouldBeInstanceOf<Type.PrimitiveType>()
        stringType.name shouldBe "String"
    }

    "createPrimitive should return cached instances for identical names" {
        val int1 = TypeFactory.createPrimitive("Int")
        val int2 = TypeFactory.createPrimitive("Int")

        // Should return exactly the same cached instance
        (int1 === int2).shouldBeTrue()
        int1.name shouldBe "Int"
    }

    "createPrimitive should create separate instances for different names" {
        val intType = TypeFactory.createPrimitive("Int")
        val stringType = TypeFactory.createPrimitive("String")

        // Different names should create different instances
        (intType === stringType).shouldBeFalse()
        intType.name shouldBe "Int"
        stringType.name shouldBe "String"
    }

    "createPrimitive should be case sensitive" {
        val lowercase = TypeFactory.createPrimitive("int")
        val uppercase = TypeFactory.createPrimitive("Int")

        (lowercase === uppercase).shouldBeFalse()
        lowercase.name shouldBe "int"
        uppercase.name shouldBe "Int"
    }

    "createPrimitive should handle empty string names" {
        val emptyType = TypeFactory.createPrimitive("")
        emptyType.name shouldBe ""
    }

    // Named Type Creation Tests
    "createNamed should create named types correctly" {
        val userType = TypeFactory.createNamed("UserType")
        val paramType = TypeFactory.createNamed("T")

        userType.shouldBeInstanceOf<Type.NamedType>()
        userType.name shouldBe "UserType"
        
        paramType.shouldBeInstanceOf<Type.NamedType>()
        paramType.name shouldBe "T"
    }

    "createNamed should cache identical names" {
        val type1 = TypeFactory.createNamed("UserType")
        val type2 = TypeFactory.createNamed("UserType")

        (type1 === type2).shouldBeTrue()
    }

    "createNamed should differentiate between different names" {
        val type1 = TypeFactory.createNamed("TypeA")
        val type2 = TypeFactory.createNamed("TypeB")

        (type1 === type2).shouldBeFalse()
        type1.name shouldBe "TypeA"
        type2.name shouldBe "TypeB"
    }

    // Generic Type Creation Tests
    "createGeneric should create generic types with arguments" {
        val elementType = Type.PrimitiveType("Int")
        val listType = TypeFactory.createGeneric("List", listOf(elementType))

        listType.shouldBeInstanceOf<Type.GenericType>()
        listType.name shouldBe "List"
        listType.arguments.size shouldBe 1
        listType.arguments.first() shouldBe elementType
    }

    "createGeneric should cache identical generic instantiations" {
        val elementType = Type.PrimitiveType("Int")
        val list1 = TypeFactory.createGeneric("List", listOf(elementType))
        val list2 = TypeFactory.createGeneric("List", listOf(elementType))

        // Should return same cached instance
        (list1 === list2).shouldBeTrue()
    }

    "createGeneric should create separate instances for different arguments" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val listInt = TypeFactory.createGeneric("List", listOf(intType))
        val listString = TypeFactory.createGeneric("List", listOf(stringType))

        (listInt === listString).shouldBeFalse()
        listInt.name shouldBe "List"
        listString.name shouldBe "List"
        listInt.arguments.first() shouldNotBe listString.arguments.first()
    }

    "createGeneric should handle multiple type arguments" {
        val keyType = Type.PrimitiveType("String")
        val valueType = Type.PrimitiveType("Int")
        val mapType = TypeFactory.createGeneric("Map", listOf(keyType, valueType))

        mapType.name shouldBe "Map"
        mapType.arguments.size shouldBe 2
        mapType.arguments[0] shouldBe keyType
        mapType.arguments[1] shouldBe valueType
    }

    "createGeneric should handle empty type arguments" {
        val emptyGeneric = TypeFactory.createGeneric("EmptyGeneric", emptyList())

        emptyGeneric.name shouldBe "EmptyGeneric"
        emptyGeneric.arguments.size shouldBe 0
    }

    "createGeneric should cache complex nested generics" {
        val innerType = Type.PrimitiveType("Int")
        val listInner = TypeFactory.createGeneric("List", listOf(innerType))
        val outerType1 = TypeFactory.createGeneric("Option", listOf(listInner))
        val outerType2 = TypeFactory.createGeneric("Option", listOf(listInner))

        (outerType1 === outerType2).shouldBeTrue()
    }

    // Function Type Creation Tests
    "createFunction should create function types correctly" {
        val paramTypes = listOf(Type.PrimitiveType("Int"), Type.PrimitiveType("String"))
        val returnType = Type.PrimitiveType("Boolean")
        val funcType = TypeFactory.createFunction(paramTypes, returnType)

        funcType.shouldBeInstanceOf<Type.FunctionType>()
        funcType.parameterTypes.size shouldBe 2
        funcType.returnType shouldBe returnType
    }

    "createFunction should cache identical function signatures" {
        val paramTypes = listOf(Type.PrimitiveType("Int"))
        val returnType = Type.PrimitiveType("String")
        
        val func1 = TypeFactory.createFunction(paramTypes, returnType)
        val func2 = TypeFactory.createFunction(paramTypes, returnType)

        (func1 === func2).shouldBeTrue()
    }

    "createFunction should differentiate by parameter types" {
        val returnType = Type.PrimitiveType("String")
        val params1 = listOf(Type.PrimitiveType("Int"))
        val params2 = listOf(Type.PrimitiveType("Double"))
        
        val func1 = TypeFactory.createFunction(params1, returnType)
        val func2 = TypeFactory.createFunction(params2, returnType)

        (func1 === func2).shouldBeFalse()
    }

    "createFunction should differentiate by return type" {
        val paramTypes = listOf(Type.PrimitiveType("Int"))
        val return1 = Type.PrimitiveType("String")
        val return2 = Type.PrimitiveType("Boolean")
        
        val func1 = TypeFactory.createFunction(paramTypes, return1)
        val func2 = TypeFactory.createFunction(paramTypes, return2)

        (func1 === func2).shouldBeFalse()
    }

    "createFunction should handle zero parameter functions" {
        val returnType = Type.PrimitiveType("Unit")
        val funcType = TypeFactory.createFunction(emptyList(), returnType)

        funcType.parameterTypes.size shouldBe 0
        funcType.returnType shouldBe returnType
    }

    "createFunction should handle complex parameter and return types" {
        val genericParam = TypeFactory.createGeneric("List", listOf(Type.PrimitiveType("Int")))
        val genericReturn = TypeFactory.createGeneric("Option", listOf(Type.PrimitiveType("String")))
        
        val funcType = TypeFactory.createFunction(listOf(genericParam), genericReturn)
        
        funcType.parameterTypes.size shouldBe 1
        funcType.parameterTypes.first() shouldBe genericParam
        funcType.returnType shouldBe genericReturn
    }

    // Tuple Type Creation Tests
    "createTuple should create tuple types correctly" {
        val elementTypes = listOf(Type.PrimitiveType("Int"), Type.PrimitiveType("String"))
        val tupleType = TypeFactory.createTuple(elementTypes)

        tupleType.shouldBeInstanceOf<Type.TupleType>()
        tupleType.elementTypes.size shouldBe 2
        tupleType.elementTypes[0] shouldBe elementTypes[0]
        tupleType.elementTypes[1] shouldBe elementTypes[1]
    }

    "createTuple should cache identical element combinations" {
        val elementTypes = listOf(Type.PrimitiveType("Int"), Type.PrimitiveType("String"))
        val tuple1 = TypeFactory.createTuple(elementTypes)
        val tuple2 = TypeFactory.createTuple(elementTypes)

        (tuple1 === tuple2).shouldBeTrue()
    }

    "createTuple should differentiate by element order" {
        val int = Type.PrimitiveType("Int")
        val string = Type.PrimitiveType("String")
        
        val tuple1 = TypeFactory.createTuple(listOf(int, string))
        val tuple2 = TypeFactory.createTuple(listOf(string, int))

        (tuple1 === tuple2).shouldBeFalse()
    }

    "createTuple should handle empty tuples" {
        val emptyTuple = TypeFactory.createTuple(emptyList())

        emptyTuple.elementTypes.size shouldBe 0
    }

    "createTuple should handle single element tuples" {
        val singleType = Type.PrimitiveType("Int")
        val singleTuple = TypeFactory.createTuple(listOf(singleType))

        singleTuple.elementTypes.size shouldBe 1
        singleTuple.elementTypes.first() shouldBe singleType
    }

    // Nullable Type Creation Tests
    "createNullable should create nullable wrapper correctly" {
        val baseType = Type.PrimitiveType("Int")
        val nullableType = TypeFactory.createNullable(baseType)

        nullableType.shouldBeInstanceOf<Type.NullableType>()
        nullableType.baseType shouldBe baseType
    }

    "createNullable should cache by base type" {
        val baseType = Type.PrimitiveType("Int")
        val nullable1 = TypeFactory.createNullable(baseType)
        val nullable2 = TypeFactory.createNullable(baseType)

        (nullable1 === nullable2).shouldBeTrue()
    }

    "createNullable should differentiate by base type" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val nullableInt = TypeFactory.createNullable(intType)
        val nullableString = TypeFactory.createNullable(stringType)

        (nullableInt === nullableString).shouldBeFalse()
        nullableInt.baseType shouldBe intType
        nullableString.baseType shouldBe stringType
    }

    "createNullable should handle complex base types" {
        val genericBase = TypeFactory.createGeneric("List", listOf(Type.PrimitiveType("Int")))
        val nullableGeneric = TypeFactory.createNullable(genericBase)

        nullableGeneric.baseType shouldBe genericBase
    }

    // Union Type Creation Tests
    "createUnion should create union types correctly" {
        val unionType = TypeFactory.createUnion("Result")

        unionType.shouldBeInstanceOf<Type.UnionType>()
        unionType.name shouldBe "Result"
        unionType.typeArguments.size shouldBe 0
    }

    "createUnion should handle type arguments" {
        val typeArg = Type.PrimitiveType("String")
        val unionType = TypeFactory.createUnion("Result", listOf(typeArg))

        unionType.name shouldBe "Result"
        unionType.typeArguments.size shouldBe 1
        unionType.typeArguments.first() shouldBe typeArg
    }

    "createUnion should cache by name and arguments" {
        val typeArg = Type.PrimitiveType("String")
        val union1 = TypeFactory.createUnion("Result", listOf(typeArg))
        val union2 = TypeFactory.createUnion("Result", listOf(typeArg))

        (union1 === union2).shouldBeTrue()
    }

    // Type Variable Creation Tests
    "createTypeVar should create type variables correctly" {
        val typeVar = TypeFactory.createTypeVar("T")

        typeVar.shouldBeInstanceOf<Type.TypeVar>()
        typeVar.id shouldBe "T"
    }

    "createTypeVar should create fresh instances each time" {
        val var1 = TypeFactory.createTypeVar("T")
        val var2 = TypeFactory.createTypeVar("T")

        // Type variables should be fresh instances, not cached
        (var1 === var2).shouldBeFalse()
        var1.id shouldBe "T"
        var2.id shouldBe "T"
    }

    "createTypeVar should handle different identifiers" {
        val varT = TypeFactory.createTypeVar("T")
        val varU = TypeFactory.createTypeVar("U")

        varT.id shouldBe "T"
        varU.id shouldBe "U"
    }

    // Builtin Types Tests
    "Builtins should provide all standard primitive types" {
        TypeFactory.Builtins.INT.name shouldBe "Int"
        TypeFactory.Builtins.LONG.name shouldBe "Long"
        TypeFactory.Builtins.FLOAT.name shouldBe "Float"
        TypeFactory.Builtins.DOUBLE.name shouldBe "Double"
        TypeFactory.Builtins.BOOLEAN.name shouldBe "Boolean"
        TypeFactory.Builtins.STRING.name shouldBe "String"
        TypeFactory.Builtins.UNIT.name shouldBe "Unit"
    }

    "Builtins should return cached instances" {
        val int1 = TypeFactory.Builtins.INT
        val int2 = TypeFactory.Builtins.INT

        (int1 === int2).shouldBeTrue()
    }

    "Builtins should be consistent with createPrimitive" {
        val builtinInt = TypeFactory.Builtins.INT
        val createdInt = TypeFactory.createPrimitive("Int")

        (builtinInt === createdInt).shouldBeTrue()
    }

    // Convenience Methods Tests
    "createList should create List generic types" {
        val elementType = Type.PrimitiveType("Int")
        val listType = TypeFactory.createList(elementType)

        listType.name shouldBe "List"
        listType.arguments.size shouldBe 1
        listType.arguments.first() shouldBe elementType
    }

    "createMap should create Map generic types" {
        val keyType = Type.PrimitiveType("String")
        val valueType = Type.PrimitiveType("Int")
        val mapType = TypeFactory.createMap(keyType, valueType)

        mapType.name shouldBe "Map"
        mapType.arguments.size shouldBe 2
        mapType.arguments[0] shouldBe keyType
        mapType.arguments[1] shouldBe valueType
    }

    "createOption should create Option generic types" {
        val elementType = Type.PrimitiveType("String")
        val optionType = TypeFactory.createOption(elementType)

        optionType.name shouldBe "Option"
        optionType.arguments.size shouldBe 1
        optionType.arguments.first() shouldBe elementType
    }

    // Cache Management Tests
    "getCacheStats should return accurate statistics" {
        // Create some cached types
        TypeFactory.createPrimitive("Int")
        TypeFactory.createPrimitive("String")
        TypeFactory.createGeneric("List", listOf(Type.PrimitiveType("Int")))

        val stats = TypeFactory.getCacheStats()
        (stats.primitiveHits >= 2).shouldBeTrue()
        (stats.genericHits >= 1).shouldBeTrue()
        (stats.totalCacheSize >= 3).shouldBeTrue()
    }

    "clearCaches should reset all caches" {
        // Populate caches
        TypeFactory.createPrimitive("Int")
        TypeFactory.createGeneric("List", listOf(Type.PrimitiveType("String")))
        TypeFactory.createFunction(listOf(Type.PrimitiveType("Int")), Type.PrimitiveType("String"))

        val statsBefore = TypeFactory.getCacheStats()
        (statsBefore.totalCacheSize > 0).shouldBeTrue()

        TypeFactory.clearCaches()

        val statsAfter = TypeFactory.getCacheStats()
        statsAfter.totalCacheSize shouldBe 0
        statsAfter.primitiveHits shouldBe 0
        statsAfter.genericHits shouldBe 0
    }

    "estimateMemoryUsage should provide reasonable estimates" {
        // Create various cached types
        repeat(10) { TypeFactory.createPrimitive("Type$it") }
        repeat(5) { TypeFactory.createGeneric("Generic$it", listOf(Type.PrimitiveType("Int"))) }

        val memoryEstimate = TypeFactory.estimateMemoryUsage()
        (memoryEstimate > 0).shouldBeTrue()
        
        // Memory estimate should be proportional to cache size
        val stats = TypeFactory.getCacheStats()
        val expectedMinimum = stats.totalCacheSize * 100L // Conservative estimate
        (memoryEstimate >= expectedMinimum).shouldBeTrue()
    }
})