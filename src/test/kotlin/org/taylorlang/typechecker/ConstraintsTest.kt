package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldMatch
import org.taylorlang.ast.SourceLocation
import org.taylorlang.ast.Type

/**
 * Comprehensive test suite for the constraint data model.
 * Tests all components including TypeVar, TypeScheme, Constraint variants, and ConstraintSet.
 */
class ConstraintsTest : StringSpec({

    // Reset type variable counter before each test to ensure predictable IDs
    beforeEach {
        TypeVar.resetCounter()
    }

    // =============================================================================
    // TypeVar Tests
    // =============================================================================

    "TypeVar.fresh() should generate unique IDs" {
        val var1 = TypeVar.fresh()
        val var2 = TypeVar.fresh()
        val var3 = TypeVar.fresh()
        
        var1.id shouldBe "T1"
        var2.id shouldBe "T2"
        var3.id shouldBe "T3"
        
        // Ensure they are different objects
        var1 shouldNotBe var2
        var2 shouldNotBe var3
    }

    "TypeVar.named() should create variable with specific name" {
        val varA = TypeVar.named("A")
        val varB = TypeVar.named("B")
        
        varA.id shouldBe "A"
        varB.id shouldBe "B"
        varA shouldNotBe varB
    }

    "TypeVar should have default kind STAR" {
        val var1 = TypeVar.fresh()
        val var2 = TypeVar.named("Alpha")
        
        var1.kind shouldBe TypeKind.STAR
        var2.kind shouldBe TypeKind.STAR
    }

    "TypeVar equality should be based on ID and kind" {
        val var1 = TypeVar("T1", TypeKind.STAR)
        val var2 = TypeVar("T1", TypeKind.STAR)
        val var3 = TypeVar("T2", TypeKind.STAR)
        
        var1 shouldBe var2
        var1 shouldNotBe var3
    }

    "TypeVar toString should return the ID" {
        val var1 = TypeVar.fresh()
        val var2 = TypeVar.named("Alpha")
        
        var1.toString() shouldBe "T1"
        var2.toString() shouldBe "Alpha"
    }

    // =============================================================================
    // TypeScheme Tests
    // =============================================================================

    "TypeScheme.monomorphic() should create scheme with no quantified variables" {
        val intType = Type.PrimitiveType("Int")
        val scheme = TypeScheme.monomorphic(intType)
        
        scheme.quantifiedVars.shouldBeEmpty()
        scheme.type shouldBe intType
        scheme.isMonomorphic() shouldBe true
    }

    "TypeScheme should track quantified variables" {
        val var1 = TypeVar.fresh()
        val var2 = TypeVar.fresh()
        val intType = Type.PrimitiveType("Int")
        val scheme = TypeScheme(setOf(var1, var2), intType)
        
        scheme.quantifiedVars shouldContain var1
        scheme.quantifiedVars shouldContain var2
        scheme.quantifiedVars shouldHaveSize 2
        scheme.isMonomorphic() shouldBe false
    }

    "TypeScheme toString should show quantified variables" {
        val var1 = TypeVar.named("A")
        val var2 = TypeVar.named("B")
        val intType = Type.PrimitiveType("Int")
        
        val monoScheme = TypeScheme.monomorphic(intType)
        val polyScheme = TypeScheme(setOf(var1, var2), intType)
        
        monoScheme.toString() shouldMatch ".*Int.*"
        polyScheme.toString() shouldMatch "∀.*\\. .*Int.*"
    }

    // =============================================================================
    // Constraint Tests
    // =============================================================================

    "Equality constraint should store left and right types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val location = SourceLocation(1, 10, "test.tl")
        
        val constraint = Constraint.Equality(intType, stringType, location)
        
        constraint.left shouldBe intType
        constraint.right shouldBe stringType
        constraint.location shouldBe location
    }

    "Equality constraint toString should use ~ symbol" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint = Constraint.Equality(intType, stringType)
        
        constraint.toString() shouldMatch ".*~.*"
    }

    "Subtype constraint should store subtype and supertype" {
        val intType = Type.PrimitiveType("Int")
        val numberType = Type.PrimitiveType("Number")
        val location = SourceLocation(2, 5, "test.tl")
        
        val constraint = Constraint.Subtype(intType, numberType, location)
        
        constraint.subtype shouldBe intType
        constraint.supertype shouldBe numberType
        constraint.location shouldBe location
    }

    "Subtype constraint toString should use <: symbol" {
        val intType = Type.PrimitiveType("Int")
        val numberType = Type.PrimitiveType("Number")
        val constraint = Constraint.Subtype(intType, numberType)
        
        constraint.toString() shouldMatch ".*<:.*"
    }

    "Instance constraint should store type variable and scheme" {
        val typeVar = TypeVar.fresh()
        val intType = Type.PrimitiveType("Int")
        val scheme = TypeScheme.monomorphic(intType)
        val location = SourceLocation(3, 15, "test.tl")
        
        val constraint = Constraint.Instance(typeVar, scheme, location)
        
        constraint.typeVar shouldBe typeVar
        constraint.scheme shouldBe scheme
        constraint.location shouldBe location
    }

    "Instance constraint toString should use ∈ symbol" {
        val typeVar = TypeVar.fresh()
        val intType = Type.PrimitiveType("Int")
        val scheme = TypeScheme.monomorphic(intType)
        val constraint = Constraint.Instance(typeVar, scheme)
        
        constraint.toString() shouldMatch ".*∈.*"
    }

    "Constraint should preserve source location for error reporting" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val location = SourceLocation(5, 20, "example.tl")
        
        val equality = Constraint.Equality(intType, stringType, location)
        val subtype = Constraint.Subtype(intType, stringType, location)
        val instance = Constraint.Instance(TypeVar.fresh(), TypeScheme.monomorphic(intType), location)
        
        equality.location shouldBe location
        subtype.location shouldBe location
        instance.location shouldBe location
    }

    // =============================================================================
    // ConstraintSet Tests
    // =============================================================================

    "ConstraintSet.empty() should create empty set" {
        val set = ConstraintSet.empty()
        
        set.isEmpty() shouldBe true
        set.isNotEmpty() shouldBe false
        set.size() shouldBe 0
        set.toList().shouldBeEmpty()
    }

    "ConstraintSet.of() should create set with given constraints" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint1 = Constraint.Equality(intType, stringType)
        val constraint2 = Constraint.Subtype(intType, stringType)
        
        val set = ConstraintSet.of(constraint1, constraint2)
        
        set.size() shouldBe 2
        set.toList() shouldContain constraint1
        set.toList() shouldContain constraint2
    }

    "ConstraintSet.fromCollection() should create set from collection" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraints = listOf(
            Constraint.Equality(intType, stringType),
            Constraint.Subtype(intType, stringType)
        )
        
        val set = ConstraintSet.fromCollection(constraints)
        
        set.size() shouldBe 2
        set.toList() shouldContainExactly constraints
    }

    "ConstraintSet.add() should return new set with additional constraint" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint1 = Constraint.Equality(intType, stringType)
        val constraint2 = Constraint.Subtype(intType, stringType)
        
        val set1 = ConstraintSet.of(constraint1)
        val set2 = set1.add(constraint2)
        
        // Original set should be unchanged (immutability)
        set1.size() shouldBe 1
        set1.toList() shouldContain constraint1
        
        // New set should contain both constraints
        set2.size() shouldBe 2
        set2.toList() shouldContain constraint1
        set2.toList() shouldContain constraint2
    }

    "ConstraintSet.addAll() should return new set with all additional constraints" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val boolType = Type.PrimitiveType("Boolean")
        
        val constraint1 = Constraint.Equality(intType, stringType)
        val constraint2 = Constraint.Subtype(intType, stringType)
        val constraint3 = Constraint.Equality(boolType, intType)
        
        val set1 = ConstraintSet.of(constraint1)
        val set2 = set1.addAll(listOf(constraint2, constraint3))
        
        // Original set unchanged
        set1.size() shouldBe 1
        
        // New set contains all constraints
        set2.size() shouldBe 3
        set2.toList() shouldContain constraint1
        set2.toList() shouldContain constraint2
        set2.toList() shouldContain constraint3
    }

    "ConstraintSet.merge() should combine two constraint sets" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val boolType = Type.PrimitiveType("Boolean")
        
        val constraint1 = Constraint.Equality(intType, stringType)
        val constraint2 = Constraint.Subtype(intType, stringType)
        val constraint3 = Constraint.Equality(boolType, intType)
        
        val set1 = ConstraintSet.of(constraint1, constraint2)
        val set2 = ConstraintSet.of(constraint3)
        val merged = set1.merge(set2)
        
        // Original sets unchanged
        set1.size() shouldBe 2
        set2.size() shouldBe 1
        
        // Merged set contains all constraints
        merged.size() shouldBe 3
        merged.toList() shouldContain constraint1
        merged.toList() shouldContain constraint2
        merged.toList() shouldContain constraint3
    }

    "ConstraintSet.filter() should return filtered constraint set" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val equality1 = Constraint.Equality(intType, stringType)
        val equality2 = Constraint.Equality(stringType, intType)
        val subtype = Constraint.Subtype(intType, stringType)
        
        val set = ConstraintSet.of(equality1, equality2, subtype)
        val equalityConstraints = set.filter { it is Constraint.Equality }
        
        equalityConstraints.size() shouldBe 2
        equalityConstraints.toList() shouldContain equality1
        equalityConstraints.toList() shouldContain equality2
        equalityConstraints.toList().none { it is Constraint.Subtype } shouldBe true
    }

    "ConstraintSet.partition() should split constraints based on predicate" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val equality = Constraint.Equality(intType, stringType)
        val subtype = Constraint.Subtype(intType, stringType)
        val instance = Constraint.Instance(TypeVar.fresh(), TypeScheme.monomorphic(intType))
        
        val set = ConstraintSet.of(equality, subtype, instance)
        val (equalityConstraints, otherConstraints) = set.partition { it is Constraint.Equality }
        
        equalityConstraints.size() shouldBe 1
        equalityConstraints.toList() shouldContain equality
        
        otherConstraints.size() shouldBe 2
        otherConstraints.toList() shouldContain subtype
        otherConstraints.toList() shouldContain instance
    }

    "ConstraintSet equality should be based on constraint content" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint = Constraint.Equality(intType, stringType)
        
        val set1 = ConstraintSet.of(constraint)
        val set2 = ConstraintSet.of(constraint)
        val set3 = ConstraintSet.empty()
        
        set1 shouldBe set2
        set1 shouldNotBe set3
        set1.hashCode() shouldBe set2.hashCode()
    }

    "ConstraintSet toString should show constraints in readable format" {
        val emptySet = ConstraintSet.empty()
        emptySet.toString() shouldBe "∅"
        
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint = Constraint.Equality(intType, stringType)
        val set = ConstraintSet.of(constraint)
        
        set.toString() shouldMatch "\\{.*~.*\\}"
    }

    "ConstraintSet should maintain immutability throughout operations" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val boolType = Type.PrimitiveType("Boolean")
        
        val originalSet = ConstraintSet.of(Constraint.Equality(intType, stringType))
        val originalSize = originalSet.size()
        val originalConstraints = originalSet.toList()
        
        // Perform various operations
        originalSet.add(Constraint.Subtype(intType, stringType))
        originalSet.addAll(listOf(Constraint.Equality(boolType, intType)))
        originalSet.merge(ConstraintSet.of(Constraint.Instance(TypeVar.fresh(), TypeScheme.monomorphic(intType))))
        originalSet.filter { it is Constraint.Equality }
        
        // Original set should be completely unchanged
        originalSet.size() shouldBe originalSize
        originalSet.toList() shouldBe originalConstraints
    }

    // =============================================================================
    // Edge Cases and Error Scenarios
    // =============================================================================

    "ConstraintSet should handle empty operations correctly" {
        val emptySet = ConstraintSet.empty()
        
        emptySet.addAll(emptyList()).isEmpty() shouldBe true
        emptySet.merge(ConstraintSet.empty()).isEmpty() shouldBe true
        emptySet.filter { true }.isEmpty() shouldBe true
        
        val (part1, part2) = emptySet.partition { true }
        part1.isEmpty() shouldBe true
        part2.isEmpty() shouldBe true
    }

    "ConstraintSet should handle duplicate constraints" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val constraint = Constraint.Equality(intType, stringType)
        
        val set = ConstraintSet.of(constraint, constraint, constraint)
        
        // Should store all instances, even duplicates (no deduplication)
        set.size() shouldBe 3
    }

    "TypeVar counter should be thread-safe" {
        // This test ensures the AtomicInteger provides thread safety
        val threads = (1..10).map {
            Thread {
                repeat(10) { TypeVar.fresh() }
            }
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        // After 10 threads each creating 10 variables, plus the variables created in other tests,
        // the counter should be at least 100
        val finalVar = TypeVar.fresh()
        finalVar.id.substring(1).toInt() shouldBe 101 // T101 (accounting for previous tests)
    }
})