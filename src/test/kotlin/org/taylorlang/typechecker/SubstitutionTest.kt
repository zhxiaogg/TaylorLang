package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.Type

/**
 * Comprehensive test suite for the Substitution class.
 * 
 * Tests all aspects of type substitution including:
 * - Basic construction and properties
 * - Type application and substitution
 * - Composition of substitutions  
 * - Edge cases and error conditions
 * - Mathematical properties (associativity, identity, etc.)
 */
class SubstitutionTest : StringSpec({

    // Test data setup
    val typeVarT = TypeVar("T")
    val typeVarU = TypeVar("U") 
    val typeVarV = TypeVar("V")
    
    val intType = BuiltinTypes.INT
    val stringType = BuiltinTypes.STRING
    val boolType = BuiltinTypes.BOOLEAN
    
    val listIntType = Type.GenericType("List", persistentListOf(intType))
    val listTType = Type.GenericType("List", persistentListOf(Type.NamedType("T")))
    val listUType = Type.GenericType("List", persistentListOf(Type.NamedType("U")))
    
    beforeEach {
        // Reset type variable counter for predictable test results
        TypeVar.resetCounter()
    }

    // =============================================================================
    // Basic Construction and Properties Tests
    // =============================================================================

    "empty substitution should be empty and have size 0" {
        val subst = Substitution.empty()
        
        subst.isEmpty() shouldBe true
        subst.isNotEmpty() shouldBe false
        subst.size() shouldBe 0
        subst.domain().shouldBeEmpty()
        subst.range().shouldBeEmpty()
    }

    "single substitution should contain one mapping" {
        val subst = Substitution.single(typeVarT, intType)
        
        subst.isEmpty() shouldBe false
        subst.isNotEmpty() shouldBe true
        subst.size() shouldBe 1
        subst.contains(typeVarT) shouldBe true
        subst.get(typeVarT) shouldBe intType
        subst.domain() shouldHaveSize 1
        subst.domain() shouldContain typeVarT
        subst.range() shouldContain intType
    }

    "substitution from map should contain all mappings" {
        val mappings = mapOf(
            typeVarT to intType,
            typeVarU to stringType
        )
        val subst = Substitution.of(mappings)
        
        subst.size() shouldBe 2
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe stringType
        subst.domain() shouldHaveSize 2
        subst.range() shouldHaveSize 2
    }

    "substitution from pairs should contain all mappings" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType,
            typeVarV to boolType
        )
        
        subst.size() shouldBe 3
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe stringType  
        subst.get(typeVarV) shouldBe boolType
    }

    // =============================================================================
    // Type Application Tests
    // =============================================================================

    "apply should substitute type variable with mapped type" {
        val subst = Substitution.single(typeVarT, intType)
        val typeVar = Type.NamedType("T")
        
        val result = subst.apply(typeVar)
        
        result shouldBe intType
    }

    "apply should leave unmapped type variables unchanged" {
        val subst = Substitution.single(typeVarT, intType)
        val typeVar = Type.NamedType("U")
        
        val result = subst.apply(typeVar)
        
        result shouldBe typeVar
    }

    "apply should leave primitive types unchanged" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.apply(stringType)
        
        result shouldBe stringType
    }

    "apply should substitute in generic type arguments" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.apply(listTType)
        
        result shouldBe listIntType
    }

    "apply should substitute in nullable types" {
        val subst = Substitution.single(typeVarT, intType)
        val nullableTType = Type.NullableType(Type.NamedType("T"))
        val expectedType = Type.NullableType(intType)
        
        val result = subst.apply(nullableTType)
        
        result shouldBe expectedType
    }

    "apply should substitute in tuple types" {
        val subst = Substitution.single(typeVarT, intType)
        val tupleType = Type.TupleType(persistentListOf(
            Type.NamedType("T"),
            stringType,
            Type.NamedType("T")
        ))
        val expectedType = Type.TupleType(persistentListOf(
            intType,
            stringType, 
            intType
        ))
        
        val result = subst.apply(tupleType)
        
        result shouldBe expectedType
    }

    "apply should substitute in function types" {
        val subst = Substitution.single(typeVarT, intType)
        val functionType = Type.FunctionType(
            persistentListOf(Type.NamedType("T"), stringType),
            Type.NamedType("T")
        )
        val expectedType = Type.FunctionType(
            persistentListOf(intType, stringType),
            intType
        )
        
        val result = subst.apply(functionType)
        
        result shouldBe expectedType
    }

    "apply should substitute in union types" {
        val subst = Substitution.single(typeVarT, intType)
        val unionType = Type.UnionType("Option", persistentListOf(Type.NamedType("T")))
        val expectedType = Type.UnionType("Option", persistentListOf(intType))
        
        val result = subst.apply(unionType)
        
        result shouldBe expectedType
    }

    "apply should be idempotent" {
        val subst = Substitution.single(typeVarT, intType)
        val typeVar = Type.NamedType("T")
        
        val result1 = subst.apply(typeVar)
        val result2 = subst.apply(result1)
        
        result1 shouldBe result2
    }

    "apply should handle chained substitutions" {
        // T -> U, U -> Int
        val subst = Substitution.of(
            typeVarT to Type.NamedType("U"),
            typeVarU to intType
        )
        val typeVar = Type.NamedType("T")
        
        val result = subst.apply(typeVar)
        
        // With proper chaining through NamedType lookup, T should resolve to Int
        result shouldBe intType
    }

    // =============================================================================
    // Substitution Composition Tests
    // =============================================================================

    "compose with empty substitution should return original" {
        val subst = Substitution.single(typeVarT, intType)
        val empty = Substitution.empty()
        
        val result1 = subst.compose(empty)
        val result2 = empty.compose(subst)
        
        result1 shouldBe subst
        result2 shouldBe subst
    }

    "compose should combine non-overlapping substitutions" {
        val subst1 = Substitution.single(typeVarT, intType)
        val subst2 = Substitution.single(typeVarU, stringType)
        
        val result = subst1.compose(subst2)
        
        result.size() shouldBe 2
        result.get(typeVarT) shouldBe intType
        result.get(typeVarU) shouldBe stringType
    }

    "compose should handle overlapping substitutions correctly" {
        val subst1 = Substitution.single(typeVarT, Type.NamedType("U"))
        val subst2 = Substitution.single(typeVarU, intType)
        
        val result = subst1.compose(subst2)
        
        result.size() shouldBe 2
        result.get(typeVarT) shouldBe intType // U gets substituted with Int
        result.get(typeVarU) shouldBe intType
    }

    "compose should be associative" {
        val subst1 = Substitution.single(typeVarT, Type.NamedType("U"))
        val subst2 = Substitution.single(typeVarU, Type.NamedType("V")) 
        val subst3 = Substitution.single(typeVarV, intType)
        
        val result1 = subst1.compose(subst2).compose(subst3)
        val result2 = subst1.compose(subst2.compose(subst3))
        
        // Both should produce the same final mappings when applied
        val testType = Type.NamedType("T")
        result1.apply(testType) shouldBe result2.apply(testType)
    }

    "compose should handle self-substitution correctly" {
        val subst1 = Substitution.single(typeVarT, intType)
        val subst2 = Substitution.single(typeVarT, stringType) // Override
        
        val result = subst1.compose(subst2)
        
        // In composition s1.compose(s2), s1 mappings are transformed by s2, 
        // but s2 mappings are added as-is if not in s1
        result.get(typeVarT) shouldBe intType // s1's mapping, not overridden by s2
    }

    // =============================================================================
    // Substitution Modification Tests
    // =============================================================================

    "extend should add new mapping" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.extend(typeVarU, stringType)
        
        result.size() shouldBe 2
        result.get(typeVarT) shouldBe intType
        result.get(typeVarU) shouldBe stringType
    }

    "extend should replace existing mapping" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.extend(typeVarT, stringType)
        
        result.size() shouldBe 1
        result.get(typeVarT) shouldBe stringType
    }

    "remove should remove existing mapping" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType
        )
        
        val result = subst.remove(typeVarT)
        
        result.size() shouldBe 1
        result.contains(typeVarT) shouldBe false
        result.get(typeVarU) shouldBe stringType
    }

    "remove should return same substitution if variable not present" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.remove(typeVarU)
        
        result shouldBe subst
    }

    "restrictTo should keep only specified variables" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType,
            typeVarV to boolType
        )
        
        val result = subst.restrictTo(setOf(typeVarT, typeVarV))
        
        result.size() shouldBe 2
        result.contains(typeVarT) shouldBe true
        result.contains(typeVarU) shouldBe false
        result.contains(typeVarV) shouldBe true
    }

    "restrictTo with empty set should return empty substitution" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.restrictTo(emptySet())
        
        result.isEmpty() shouldBe true
    }

    // =============================================================================
    // Free Type Variables Tests
    // =============================================================================

    "freeTypeVars should return empty set for primitive types" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.freeTypeVars()
        
        result.shouldBeEmpty()
    }

    "freeTypeVars should return type variables in complex types" {
        val complexType = Type.GenericType("List", persistentListOf(Type.NamedType("U")))
        val subst = Substitution.single(typeVarT, complexType)
        
        val result = subst.freeTypeVars()
        
        result shouldHaveSize 1
        result shouldContain TypeVar("U")
    }

    "freeTypeVars should handle nested type structures" {
        val nestedType = Type.FunctionType(
            persistentListOf(Type.NamedType("U")),
            Type.GenericType("Option", persistentListOf(Type.NamedType("V")))
        )
        val subst = Substitution.single(typeVarT, nestedType)
        
        val result = subst.freeTypeVars()
        
        result shouldHaveSize 2
        result shouldContain TypeVar("U")
        result shouldContain TypeVar("V")
    }

    // =============================================================================
    // Utility Methods Tests
    // =============================================================================

    "filter should return substitution with matching mappings" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType,
            typeVarV to boolType
        )
        
        val result = subst.filter { typeVar, type -> 
            type == intType || type == boolType 
        }
        
        result.size() shouldBe 2
        result.contains(typeVarT) shouldBe true
        result.contains(typeVarU) shouldBe false
        result.contains(typeVarV) shouldBe true
    }

    "mapTypes should transform all types in substitution" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType
        )
        
        val result = subst.mapTypes { type ->
            Type.NullableType(type)
        }
        
        result.size() shouldBe 2
        result.get(typeVarT).shouldBeInstanceOf<Type.NullableType>()
        result.get(typeVarU).shouldBeInstanceOf<Type.NullableType>()
        val nullableInt = result.get(typeVarT) as Type.NullableType
        nullableInt.baseType shouldBe intType
    }

    "toMap should return equivalent map" {
        val originalMap = mapOf(
            typeVarT to intType,
            typeVarU to stringType
        )
        val subst = Substitution.of(originalMap)
        
        val result = subst.toMap()
        
        result shouldBe originalMap
    }

    // =============================================================================
    // Equality and Hash Code Tests
    // =============================================================================

    "substitutions with same mappings should be equal" {
        val subst1 = Substitution.of(typeVarT to intType, typeVarU to stringType)
        val subst2 = Substitution.of(typeVarU to stringType, typeVarT to intType)
        
        subst1 shouldBe subst2
        subst1.hashCode() shouldBe subst2.hashCode()
    }

    "substitutions with different mappings should not be equal" {
        val subst1 = Substitution.single(typeVarT, intType)
        val subst2 = Substitution.single(typeVarT, stringType)
        
        subst1 shouldNotBe subst2
    }

    "empty substitutions should be equal" {
        val empty1 = Substitution.empty()
        val empty2 = Substitution.of(emptyMap())
        
        empty1 shouldBe empty2
        empty1.hashCode() shouldBe empty2.hashCode()
    }

    // =============================================================================
    // String Representation Tests
    // =============================================================================

    "empty substitution toString should show empty symbol" {
        val subst = Substitution.empty()
        
        subst.toString() shouldBe "∅"
    }

    "single substitution toString should show mapping" {
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.toString()
        
        result shouldBe "{T ↦ PrimitiveType(name=Int, sourceLocation=null)}"
    }

    "multiple substitution toString should show all mappings" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to stringType
        )
        
        val result = subst.toString()
        
        // Should contain both mappings (order may vary)
        result.contains("T ↦") shouldBe true
        result.contains("U ↦") shouldBe true
        result.startsWith("{") shouldBe true
        result.endsWith("}") shouldBe true
    }

    // =============================================================================
    // Mathematical Properties Tests
    // =============================================================================

    "identity property: empty substitution should be left identity for composition" {
        val subst = Substitution.single(typeVarT, intType)
        val empty = Substitution.empty()
        
        val result = empty.compose(subst)
        
        result shouldBe subst
    }

    "identity property: empty substitution should be right identity for composition" {
        val subst = Substitution.single(typeVarT, intType)
        val empty = Substitution.empty()
        
        val result = subst.compose(empty)
        
        result shouldBe subst
    }

    "idempotent property: applying substitution twice should equal applying once" {
        val subst = Substitution.of(
            typeVarT to intType,
            typeVarU to Type.GenericType("List", persistentListOf(stringType))
        )
        val complexType = Type.FunctionType(
            persistentListOf(Type.NamedType("T"), Type.NamedType("U")),
            Type.NamedType("T")
        )
        
        val once = subst.apply(complexType)
        val twice = subst.apply(once)
        
        once shouldBe twice
    }

    // =============================================================================
    // Edge Cases and Error Conditions
    // =============================================================================

    "should handle type variables that look like regular names" {
        val longVarName = TypeVar("MyLongTypeVariable")
        val subst = Substitution.single(longVarName, intType)
        val namedType = Type.NamedType("MyLongTypeVariable")
        
        val result = subst.apply(namedType)
        
        result shouldBe intType
    }

    "should handle deeply nested type structures" {
        val deepType = Type.FunctionType(
            persistentListOf(
                Type.GenericType("List", persistentListOf(
                    Type.TupleType(persistentListOf(
                        Type.NamedType("T"),
                        Type.NullableType(Type.NamedType("T"))
                    ))
                ))
            ),
            Type.NamedType("T")
        )
        val subst = Substitution.single(typeVarT, intType)
        
        val result = subst.apply(deepType)
        
        // Should substitute T throughout the nested structure
        result.shouldBeInstanceOf<Type.FunctionType>()
        val functionType = result as Type.FunctionType
        functionType.returnType shouldBe intType
        
        val paramType = functionType.parameterTypes.first()
        paramType.shouldBeInstanceOf<Type.GenericType>()
        val genericType = paramType as Type.GenericType
        val tupleType = genericType.arguments.first() as Type.TupleType
        tupleType.elementTypes[0] shouldBe intType
        val nullableType = tupleType.elementTypes[1] as Type.NullableType
        nullableType.baseType shouldBe intType
    }
})