package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.taylorlang.ast.Type
import kotlinx.collections.immutable.persistentListOf

class TypeUnificationTest : StringSpec({

    "unify should succeed for identical types" {
        val intType = Type.PrimitiveType("Int")
        val result = TypeUnification.unify(intType, intType)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe intType
        result.substitution shouldBe Substitution.empty()
    }

    "unify should use fast path optimization for structurally equal types" {
        val list1 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        val list2 = Type.GenericType("List", persistentListOf(Type.PrimitiveType("Int")))
        
        val result = TypeUnification.unify(list1, list2)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe list1
        result.substitution shouldBe Substitution.empty()
    }

    "unify should handle type variable unification" {
        val typeVar = Type.TypeVar("T")
        val concreteType = Type.PrimitiveType("Int")
        
        val result = TypeUnification.unify(typeVar, concreteType)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe concreteType
    }

    "unify should handle numeric type promotion" {
        val intType = Type.PrimitiveType("Int")
        val doubleType = Type.PrimitiveType("Double")
        
        val result = TypeUnification.unify(intType, doubleType)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe doubleType
    }

    "unify should fail for incompatible types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val result = TypeUnification.unify(intType, stringType)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Failure>()
        result.type1 shouldBe intType
        result.type2 shouldBe stringType
    }

    "unifyWithSubstitution should apply existing substitution" {
        val typeVar1 = Type.TypeVar("T")
        val typeVar2 = Type.TypeVar("U")
        val intType = Type.PrimitiveType("Int")
        
        val existingSubst = Substitution.single(TypeVar.named("T"), intType)
        val result = TypeUnification.unifyWithSubstitution(typeVar1, typeVar2, existingSubst)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
    }

    "unifyWithSubstitution should handle failure cases" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val emptySubst = Substitution.empty()
        
        val result = TypeUnification.unifyWithSubstitution(intType, stringType, emptySubst)

        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Failure>()
    }

    "solveConstraints should handle simple equality constraints" {
        val typeVar = Type.TypeVar("T")
        val intType = Type.PrimitiveType("Int")
        val constraint = Constraint.Equality(typeVar, intType)
        val constraintSet = ConstraintSet.of(constraint)
        
        val result = TypeUnification.solveConstraints(constraintSet)

        result.shouldBeInstanceOf<TypeUnification.ConstraintSolutionResult.Success>()
    }

    "solveConstraints should handle empty constraint sets" {
        val emptyConstraintSet = ConstraintSet.empty()
        
        val result = TypeUnification.solveConstraints(emptyConstraintSet)

        result.shouldBeInstanceOf<TypeUnification.ConstraintSolutionResult.Success>()
        result.substitution shouldBe Substitution.empty()
    }

    "canUnify should return true for unifiable types" {
        val typeVar = Type.TypeVar("T")
        val intType = Type.PrimitiveType("Int")
        
        TypeUnification.canUnify(typeVar, intType).shouldBeTrue()
        TypeUnification.canUnify(intType, intType).shouldBeTrue()
    }

    "canUnify should return false for non-unifiable types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        TypeUnification.canUnify(intType, stringType).shouldBeFalse()
    }

    "getMostGeneralUnifier should return substitution for unifiable types" {
        val typeVar = Type.TypeVar("T")
        val intType = Type.PrimitiveType("Int")
        
        val unifier = TypeUnification.getMostGeneralUnifier(typeVar, intType)
        unifier.shouldNotBeNull()
    }

    "getMostGeneralUnifier should return null for non-unifiable types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        val unifier = TypeUnification.getMostGeneralUnifier(intType, stringType)
        unifier.shouldBeNull()
    }

    "unifyTypes should handle empty list" {
        val result = TypeUnification.unifyTypes(emptyList())
        result.shouldBeNull()
    }

    "unifyTypes should handle single type" {
        val intType = Type.PrimitiveType("Int")
        val result = TypeUnification.unifyTypes(listOf(intType))
        
        result.shouldNotBeNull()
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe intType
    }

    "unifyTypes should find common supertype for compatible types" {
        val intType = Type.PrimitiveType("Int")
        val longType = Type.PrimitiveType("Long")
        val types = listOf(intType, longType)
        
        val result = TypeUnification.unifyTypes(types)
        
        result.shouldNotBeNull()
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
        result.unifiedType shouldBe longType
    }

    "unifyTypes should fail for incompatible types" {
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        val types = listOf(intType, stringType)
        
        val result = TypeUnification.unifyTypes(types)
        
        result.shouldNotBeNull()
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Failure>()
    }

    "should properly delegate to existing Unifier system" {
        val typeVar = Type.TypeVar("T")
        val intType = Type.PrimitiveType("Int")
        
        val result = TypeUnification.unify(typeVar, intType)
        result.shouldBeInstanceOf<TypeUnification.UnificationResult.Success>()
    }
})