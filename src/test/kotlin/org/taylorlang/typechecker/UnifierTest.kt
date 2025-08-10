package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.collections.immutable.persistentListOf
import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

/**
 * Comprehensive test suite for the Unifier class.
 * 
 * Tests all aspects of the unification algorithm including:
 * - Basic unification rules (var-var, var-type, type-type)
 * - Complex type unification (generics, functions, tuples)
 * - Occurs check and infinite type detection
 * - Constraint solving with multiple constraints
 * - Error conditions and edge cases
 * - Integration with substitution composition
 */
class UnifierTest : StringSpec({

    // Test data setup
    val typeVarT = TypeVar("T")
    val typeVarU = TypeVar("U") 
    val typeVarV = TypeVar("V")
    
    val intType = BuiltinTypes.INT
    val stringType = BuiltinTypes.STRING
    val boolType = BuiltinTypes.BOOLEAN
    val doubleType = BuiltinTypes.DOUBLE
    
    val tVarType = Type.NamedType("T")
    val uVarType = Type.NamedType("U")
    val vVarType = Type.NamedType("V")
    
    val listIntType = Type.GenericType("List", persistentListOf(intType))
    val listTType = Type.GenericType("List", persistentListOf(tVarType))
    val listUType = Type.GenericType("List", persistentListOf(uVarType))
    
    beforeEach {
        TypeVar.resetCounter()
    }

    // =============================================================================
    // Basic Unification Rule Tests
    // =============================================================================

    "unify identical primitive types should succeed with empty substitution" {
        val result = Unifier.unify(intType, intType)
        
        result.isSuccess shouldBe true
        result.getOrThrow().isEmpty() shouldBe true
    }

    "unify different primitive types should fail" {
        val result = Unifier.unify(intType, stringType)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.TypeMismatch>()
    }

    "unify type variable with concrete type should create substitution" {
        val result = Unifier.unify(tVarType, intType)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.isEmpty() shouldBe false
        subst.get(typeVarT) shouldBe intType
    }

    "unify concrete type with type variable should create substitution" {
        val result = Unifier.unify(intType, tVarType)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
    }

    "unify two identical type variables should succeed with empty substitution" {
        val result = Unifier.unify(tVarType, tVarType)
        
        result.isSuccess shouldBe true
        result.getOrThrow().isEmpty() shouldBe true
    }

    "unify two different type variables should create substitution" {
        val result = Unifier.unify(tVarType, uVarType)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.size() shouldBe 1
        // Either T -> U or U -> T is valid
        val hasMapping = subst.get(typeVarT) == uVarType || subst.get(typeVarU) == tVarType
        hasMapping shouldBe true
    }

    // =============================================================================
    // Occurs Check Tests
    // =============================================================================

    "unify type variable with type containing itself should fail with infinite type error" {
        val listTType = Type.GenericType("List", persistentListOf(tVarType))
        
        val result = Unifier.unify(tVarType, listTType)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.InfiniteType>()
    }

    "unify type variable with deeply nested type containing itself should fail" {
        val nestedType = Type.FunctionType(
            persistentListOf(Type.GenericType("List", persistentListOf(tVarType))),
            intType
        )
        
        val result = Unifier.unify(tVarType, nestedType)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.InfiniteType>()
    }

    "unify type variable with type not containing itself should succeed" {
        val listIntType = Type.GenericType("List", persistentListOf(intType))
        
        val result = Unifier.unify(tVarType, listIntType)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe listIntType
    }

    // =============================================================================
    // Generic Type Unification Tests
    // =============================================================================

    "unify generic types with same constructor should unify arguments" {
        val listT = Type.GenericType("List", persistentListOf(tVarType))
        val listInt = Type.GenericType("List", persistentListOf(intType))
        
        val result = Unifier.unify(listT, listInt)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
    }

    "unify generic types with different constructors should fail" {
        val listT = Type.GenericType("List", persistentListOf(tVarType))
        val setT = Type.GenericType("Set", persistentListOf(tVarType))
        
        val result = Unifier.unify(listT, setT)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.TypeMismatch>()
    }

    "unify generic types with different arities should fail" {
        val mapTU = Type.GenericType("Map", persistentListOf(tVarType, uVarType))
        val listT = Type.GenericType("List", persistentListOf(tVarType))
        
        // Try to unify Map[T, U] with List[T] - should fail due to arity mismatch
        val result = Unifier.unify(mapTU, listT)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.TypeMismatch>()
    }

    "unify nested generic types should work recursively" {
        val listListT = Type.GenericType("List", persistentListOf(
            Type.GenericType("List", persistentListOf(tVarType))
        ))
        val listListInt = Type.GenericType("List", persistentListOf(
            Type.GenericType("List", persistentListOf(intType))
        ))
        
        val result = Unifier.unify(listListT, listListInt)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "unify generic types with multiple type variables should create multiple substitutions" {
        val mapTU = Type.GenericType("Map", persistentListOf(tVarType, uVarType))
        val mapStringInt = Type.GenericType("Map", persistentListOf(stringType, intType))
        
        val result = Unifier.unify(mapTU, mapStringInt)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe stringType
        subst.get(typeVarU) shouldBe intType
    }

    // =============================================================================
    // Function Type Unification Tests
    // =============================================================================

    "unify function types with matching signatures should succeed" {
        val funcTT = Type.FunctionType(persistentListOf(tVarType), tVarType)
        val funcIntInt = Type.FunctionType(persistentListOf(intType), intType)
        
        val result = Unifier.unify(funcTT, funcIntInt)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "unify function types with different parameter counts should fail" {
        val func1 = Type.FunctionType(persistentListOf(tVarType), intType)
        val func2 = Type.FunctionType(persistentListOf(tVarType, uVarType), intType)
        
        val result = Unifier.unify(func1, func2)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.ArityMismatch>()
    }

    "unify complex function types should unify all components" {
        val complexFunc1 = Type.FunctionType(
            persistentListOf(tVarType, Type.GenericType("List", persistentListOf(uVarType))),
            Type.NullableType(tVarType)
        )
        val complexFunc2 = Type.FunctionType(
            persistentListOf(intType, Type.GenericType("List", persistentListOf(stringType))),
            Type.NullableType(intType)
        )
        
        val result = Unifier.unify(complexFunc1, complexFunc2)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe stringType
    }

    // =============================================================================
    // Tuple Type Unification Tests
    // =============================================================================

    "unify tuple types with matching elements should succeed" {
        val tuple1 = Type.TupleType(persistentListOf(tVarType, stringType, uVarType))
        val tuple2 = Type.TupleType(persistentListOf(intType, stringType, boolType))
        
        val result = Unifier.unify(tuple1, tuple2)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe boolType
    }

    "unify tuple types with different lengths should fail" {
        val tuple1 = Type.TupleType(persistentListOf(tVarType, stringType))
        val tuple2 = Type.TupleType(persistentListOf(tVarType, stringType, boolType))
        
        val result = Unifier.unify(tuple1, tuple2)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.ArityMismatch>()
    }

    // =============================================================================
    // Nullable Type Unification Tests
    // =============================================================================

    "unify nullable types should unify base types" {
        val nullableT = Type.NullableType(tVarType)
        val nullableInt = Type.NullableType(intType)
        
        val result = Unifier.unify(nullableT, nullableInt)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "unify nullable type with non-nullable type should unify base types" {
        val nullableT = Type.NullableType(tVarType)
        
        val result = Unifier.unify(nullableT, intType)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "unify non-nullable type with nullable type should unify with base type" {
        val nullableT = Type.NullableType(tVarType)
        
        val result = Unifier.unify(intType, nullableT)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    // =============================================================================
    // Union Type Unification Tests
    // =============================================================================

    "unify union types with same name should unify type arguments" {
        val optionT = Type.UnionType("Option", persistentListOf(tVarType))
        val optionInt = Type.UnionType("Option", persistentListOf(intType))
        
        val result = Unifier.unify(optionT, optionInt)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "unify union types with different names should fail" {
        val optionT = Type.UnionType("Option", persistentListOf(tVarType))
        val eitherT = Type.UnionType("Either", persistentListOf(tVarType))
        
        val result = Unifier.unify(optionT, eitherT)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.TypeMismatch>()
    }

    // =============================================================================
    // Constraint Solving Tests
    // =============================================================================

    "solve single equality constraint should work" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(tVarType, intType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "solve multiple independent constraints should work" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(tVarType, intType),
            Constraint.Equality(uVarType, stringType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe stringType
    }

    "solve dependent constraints should propagate substitutions" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(tVarType, uVarType),
            Constraint.Equality(uVarType, intType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        // Both T and U should ultimately map to Int
        subst.apply(tVarType) shouldBe intType
        subst.apply(uVarType) shouldBe intType
    }

    "solve constraints with generic types should work" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(
                Type.GenericType("List", persistentListOf(tVarType)),
                Type.GenericType("List", persistentListOf(intType))
            )
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "solve conflicting constraints should fail" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(tVarType, intType),
            Constraint.Equality(tVarType, stringType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<UnificationError.ConstraintSolvingFailed>()
    }

    "solve constraints with occurs check violation should fail" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(
                tVarType,
                Type.GenericType("List", persistentListOf(tVarType))
            )
        )
        
        val result = Unifier.solve(constraints)
        
        result.isFailure shouldBe true
        val exception = result.exceptionOrNull() as UnificationError.ConstraintSolvingFailed
        exception.cause.shouldBeInstanceOf<UnificationError.InfiniteType>()
    }

    // =============================================================================
    // Subtype Constraint Tests (Simplified)
    // =============================================================================

    "solve subtype constraint should treat as equality for now" {
        val constraints = ConstraintSet.of(
            Constraint.Subtype(tVarType, intType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    // =============================================================================
    // Instance Constraint Tests
    // =============================================================================

    "solve instance constraint with monomorphic scheme should work" {
        val scheme = TypeScheme.monomorphic(intType)
        val constraints = ConstraintSet.of(
            Constraint.Instance(typeVarT, scheme)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        result.getOrThrow().get(typeVarT) shouldBe intType
    }

    "solve instance constraint with polymorphic scheme should instantiate fresh variables" {
        val schemeTypeVar = TypeVar("Alpha")
        val scheme = TypeScheme(
            setOf(schemeTypeVar),
            Type.GenericType("List", persistentListOf(Type.NamedType("Alpha")))
        )
        val constraints = ConstraintSet.of(
            Constraint.Instance(typeVarT, scheme)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        val instantiatedType = subst.get(typeVarT)
        instantiatedType.shouldBeInstanceOf<Type.GenericType>()
        val genericType = instantiatedType as Type.GenericType
        genericType.name shouldBe "List"
        // The type argument should be a fresh type variable (not "Alpha")
        val typeArg = genericType.arguments.first()
        typeArg.shouldBeInstanceOf<Type.NamedType>()
        val namedType = typeArg as Type.NamedType
        namedType.name shouldNotBe "Alpha" // Should be fresh
    }

    // =============================================================================
    // Complex Integration Tests
    // =============================================================================

    "solve complex constraint set with multiple dependent constraints" {
        val constraints = ConstraintSet.of(
            Constraint.Equality(
                Type.FunctionType(persistentListOf(tVarType), uVarType),
                Type.FunctionType(persistentListOf(intType), Type.GenericType("List", persistentListOf(vVarType)))
            ),
            Constraint.Equality(vVarType, stringType)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarV) shouldBe stringType
        // Apply substitution to get final resolved type for U
        val resolvedU = subst.apply(uVarType)
        val expectedListString = Type.GenericType("List", persistentListOf(stringType))
        resolvedU shouldBe expectedListString
    }

    "solve constraints from realistic type inference scenario" {
        // Simulating constraints from: let f = \x -> [x]; f 42
        val listT = Type.GenericType("List", persistentListOf(tVarType))
        val funcType = Type.FunctionType(persistentListOf(tVarType), listT)
        val listInt = Type.GenericType("List", persistentListOf(intType))
        
        val constraints = ConstraintSet.of(
            // Function type constraint: \x -> [x] has type T -> List[T]
            Constraint.Equality(uVarType, funcType),
            // Application constraint: f 42 requires T = Int
            Constraint.Equality(tVarType, intType),
            // Result constraint: f 42 has type List[Int]
            Constraint.Equality(vVarType, listInt)
        )
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        val expectedFuncType = Type.FunctionType(persistentListOf(intType), listInt)
        subst.apply(uVarType) shouldBe expectedFuncType
        subst.get(typeVarV) shouldBe listInt
    }

    // =============================================================================
    // Error Handling and Edge Cases
    // =============================================================================

    "solve empty constraint set should return empty substitution" {
        val constraints = ConstraintSet.empty()
        
        val result = Unifier.solve(constraints)
        
        result.isSuccess shouldBe true
        result.getOrThrow().isEmpty() shouldBe true
    }

    "unify with source location should preserve location in error" {
        val location = SourceLocation(10, 5, "test.tl")
        val intTypeWithLocation = Type.PrimitiveType("Int", location)
        val stringTypeWithLocation = Type.PrimitiveType("String", location)
        
        val result = Unifier.unify(intTypeWithLocation, stringTypeWithLocation)
        
        result.isFailure shouldBe true
        val exception = result.exceptionOrNull() as UnificationError.TypeMismatch
        exception.location shouldBe location
    }

    "unify very complex nested structure should work" {
        val complexType1 = Type.FunctionType(
            persistentListOf(
                Type.TupleType(persistentListOf(
                    tVarType,
                    Type.GenericType("Option", persistentListOf(uVarType))
                ))
            ),
            Type.GenericType("Result", persistentListOf(tVarType, stringType))
        )
        
        val complexType2 = Type.FunctionType(
            persistentListOf(
                Type.TupleType(persistentListOf(
                    intType,
                    Type.GenericType("Option", persistentListOf(boolType))
                ))
            ),
            Type.GenericType("Result", persistentListOf(intType, stringType))
        )
        
        val result = Unifier.unify(complexType1, complexType2)
        
        result.isSuccess shouldBe true
        val subst = result.getOrThrow()
        subst.get(typeVarT) shouldBe intType
        subst.get(typeVarU) shouldBe boolType
    }

    "unify should handle type variables that look like concrete types" {
        // Type variable named "String" should NOT be treated as a type var
        val stringVarType = Type.NamedType("String")  // This should be treated as concrete String type
        
        val result = Unifier.unify(stringVarType, intType)
        
        // Should fail because String != Int
        result.isFailure shouldBe true
    }
})