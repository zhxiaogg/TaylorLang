# Unification Algorithm Design

## Problem Statement

With constraint collection implemented, we now have a set of type constraints generated from the AST. The next critical step is to solve these constraints to determine concrete types for all type variables. The unification algorithm is the core mechanism that will resolve these constraints and produce type substitutions.

## Requirements

### Functional Requirements

1. **Constraint Solving**
   - Process equality constraints (Type1 = Type2)
   - Handle subtype constraints (Type1 <: Type2)
   - Support instance constraints (TypeVar instantiates TypeScheme)
   - Return either a successful substitution or meaningful error

2. **Type Operations**
   - Apply substitutions to types (replace TypeVars with concrete types)
   - Compose multiple substitutions correctly
   - Maintain substitution consistency across the constraint set

3. **Safety Checks**
   - Implement occurs check to prevent infinite types (e.g., T = List[T])
   - Validate type constructor arity (List requires 1 parameter, Map requires 2)
   - Ensure type variable kinds match when unifying

4. **Generic Type Support**
   - Unify parameterized types (List[Int] with List[T])
   - Handle nested generics (Map[String, List[Int]])
   - Support union type unification

### Non-Functional Requirements

1. **Performance**
   - O(n * α(n)) time complexity for n constraints (near-linear with union-find)
   - Minimize redundant substitution applications
   - Early termination on unification failure

2. **Error Reporting**
   - Preserve source locations for error messages
   - Provide clear explanations of type mismatches
   - Show the unification path that led to failure

3. **Maintainability**
   - Clear separation between unification logic and type representation
   - Immutable data structures for thread safety
   - Comprehensive test coverage

## Solution Architecture

### Core Components

1. **Substitution**
   ```kotlin
   class Substitution {
       // Mapping from TypeVar to Type
       private val mapping: Map<TypeVar, Type>
       
       // Apply substitution to a type
       fun apply(type: Type): Type
       
       // Compose with another substitution
       fun compose(other: Substitution): Substitution
       
       // Check if empty
       fun isEmpty(): Boolean
   }
   ```

2. **Unifier**
   ```kotlin
   class Unifier {
       // Main unification entry point
       fun solve(constraints: ConstraintSet): Result<Substitution>
       
       // Unify two types
       fun unify(type1: Type, type2: Type, subst: Substitution): Result<Substitution>
       
       // Apply substitution throughout constraint set
       fun applySubstitution(constraints: ConstraintSet, subst: Substitution): ConstraintSet
   }
   ```

3. **Algorithm Selection: Robinson's Algorithm with Extensions**
   - Classic, well-understood algorithm
   - Extended for subtyping and parametric polymorphism
   - Proven correctness and completeness for Hindley-Milner

### Implementation Strategy

1. **Phase 1: Basic Unification**
   - Start with equality constraints only
   - Implement occurs check
   - Handle simple types and type variables

2. **Phase 2: Generic Types**
   - Unify type constructors (List, Option, Map)
   - Recursive unification for type parameters
   - Arity checking for type constructors

3. **Phase 3: Advanced Constraints**
   - Subtype constraint handling
   - Instance constraint resolution
   - Union type unification rules

4. **Phase 4: Error Handling**
   - Detailed error messages with types involved
   - Source location tracking
   - Unification trace for debugging

## Implementation Notes

### Key Algorithms

1. **Occurs Check**
   ```
   occurs(tv: TypeVar, type: Type): Boolean
     if type is TypeVar and type.id == tv.id: return true
     if type is ParameterizedType: 
       return any(occurs(tv, param) for param in type.parameters)
     return false
   ```

2. **Unification Rules**
   ```
   unify(t1, t2, subst):
     t1' = subst.apply(t1)
     t2' = subst.apply(t2)
     
     match (t1', t2'):
       (TypeVar(a), TypeVar(b)) if a == b -> subst
       (TypeVar(a), t) -> if occurs(a, t) fail else subst.extend(a -> t)
       (t, TypeVar(a)) -> if occurs(a, t) fail else subst.extend(a -> t)
       (SimpleType(n1), SimpleType(n2)) -> if n1 == n2 then subst else fail
       (Param(c1, args1), Param(c2, args2)) -> 
         if c1 == c2 then unifyList(args1, args2, subst) else fail
       _ -> fail
   ```

3. **Substitution Composition**
   ```
   compose(s1, s2):
     // s2 ∘ s1 means apply s1 first, then s2
     result = empty map
     for (tv, type) in s1:
       result[tv] = s2.apply(type)
     for (tv, type) in s2:
       if tv not in result:
         result[tv] = type
     return Substitution(result)
   ```

### Testing Strategy

1. **Unit Tests**
   - Test each unification rule separately
   - Occurs check edge cases
   - Substitution composition properties

2. **Property Tests**
   - Unification is symmetric: unify(a, b) = unify(b, a)
   - Substitution is idempotent: s.apply(s.apply(t)) = s.apply(t)
   - Composition associativity: (s1 ∘ s2) ∘ s3 = s1 ∘ (s2 ∘ s3)

3. **Integration Tests**
   - End-to-end constraint solving
   - Complex nested type scenarios
   - Error cases with good messages

## Dependencies

### Depends On
- Constraint Data Model (TypeVar, Constraint, ConstraintSet) - COMPLETED
- Type hierarchy (Type and its subclasses)
- Result type for error handling

### Depended On By
- TypeChecker integration (next task after this)
- Error reporting system
- Future type inference optimizations

## Research References

1. **Robinson's Unification Algorithm** (1965)
   - Original algorithm paper
   - Foundation for type inference

2. **"Types and Programming Languages"** - Benjamin Pierce
   - Chapter 22: Type Reconstruction
   - Detailed algorithm explanations

3. **OCaml Implementation**
   - Production-tested unification
   - Good error message strategies

4. **Kotlin Compiler**
   - Local type inference implementation
   - Subtyping rules for JVM types

## Success Criteria

1. All unit tests pass (minimum 30 tests)
2. Correctly solves constraints from ConstraintCollectorTest examples
3. Occurs check prevents infinite types
4. Clear error messages with type information
5. Performance: <100ms for typical programs (1000 constraints)
6. Integration test showing end-to-end inference for sample programs

## Risk Mitigation

1. **Complexity Risk**: Start with simple equality, add features incrementally
2. **Performance Risk**: Use union-find for equivalence classes if needed
3. **Correctness Risk**: Extensive testing, property-based tests
4. **Integration Risk**: Design clean interface early, mock for testing