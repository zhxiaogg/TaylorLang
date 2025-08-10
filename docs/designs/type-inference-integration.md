# Type Inference Integration Design

## Problem Statement

We have successfully implemented three foundational components of type inference:
1. Constraint Data Model (TypeVar, Constraints, ConstraintSet)
2. Constraint Collection (bidirectional type checking, constraint generation)
3. Unification Algorithm (Robinson's algorithm with substitution)

Now we need to integrate these components with the existing TypeChecker to enable automatic type inference for missing type annotations while maintaining backward compatibility.

## Requirements

### Functional Requirements

1. **Automatic Type Inference**:
   - Infer types for variables without explicit annotations
   - Infer return types for functions when not specified
   - Infer type arguments for generic function calls
   - Support let-polymorphism for local bindings

2. **Explicit Annotation Respect**:
   - Honor all user-provided type annotations
   - Use annotations as constraints during inference
   - Validate inferred types against explicit annotations
   - Report errors when inference conflicts with annotations

3. **Backward Compatibility**:
   - All existing tests must continue to pass
   - Existing explicitly-typed code behavior unchanged
   - Support both inference and non-inference modes
   - Graceful fallback when inference fails

4. **Error Reporting**:
   - Show inferred types in error messages
   - Indicate when type inference failed
   - Provide clear constraint solving errors
   - Include source locations for all errors

### Non-Functional Requirements

1. **Performance**:
   - Inference should not significantly slow down type checking
   - Cache inferred types to avoid redundant computation
   - Use bidirectional checking to minimize constraint generation

2. **Maintainability**:
   - Clean separation between inference and checking logic
   - Modular design allowing incremental improvements
   - Clear API boundaries between components

3. **Debuggability**:
   - Optional debug mode showing constraint solving steps
   - Ability to dump collected constraints
   - Clear trace of type inference decisions

## Solution Design

### Architecture Overview

```
TypeChecker (main entry point)
    │
    ├── InferenceEngine (new component)
    │   ├── ConstraintCollector
    │   ├── Unifier
    │   └── Substitution
    │
    └── AlgorithmicChecker (existing)
        └── Traditional type checking
```

### Key Components

#### 1. InferenceEngine Class

```kotlin
class InferenceEngine(
    private val collector: ConstraintCollector,
    private val unifier: Unifier
) {
    fun inferProgram(program: Program): Result<TypedProgram>
    fun inferFunction(function: Function): Result<TypedFunction>
    fun inferExpression(expr: Expression, context: InferenceContext): Result<Pair<Type, Substitution>>
}
```

#### 2. TypeChecker Integration

```kotlin
class TypeChecker {
    fun typeCheck(program: Program): Result<TypedProgram> {
        return when (mode) {
            ALGORITHMIC -> algorithmicCheck(program)
            INFERENCE -> inferenceEngine.inferProgram(program)
            HYBRID -> hybridCheck(program)  // Try inference, fall back to algorithmic
        }
    }
}
```

#### 3. Type Annotation Handling

- Collect explicit annotations as equality constraints
- Use bidirectional checking:
  - Synthesis mode: Generate fresh type variable, collect constraints
  - Checking mode: Check against expected type, generate subtype constraint
- Apply substitution after solving constraints

### Integration Points

1. **Variable Declarations**:
   - If type annotation present: Use as constraint
   - If type annotation missing: Infer from initializer
   - Support let-polymorphism for immutable bindings

2. **Function Definitions**:
   - Infer parameter types from usage if not specified
   - Infer return type from body if not specified
   - Handle recursive functions with fixed-point iteration

3. **Generic Functions**:
   - Infer type arguments from call site
   - Support partial type argument specification
   - Handle higher-rank polymorphism (future)

4. **Pattern Matching**:
   - Infer pattern variable types from scrutinee
   - Propagate type information through branches
   - Use exhaustiveness for refinement

### Error Handling Strategy

1. **Constraint Solving Failures**:
   - Collect all unification errors
   - Report most specific error first
   - Suggest type annotations to resolve ambiguity

2. **Annotation Conflicts**:
   - Report mismatch between inferred and annotated
   - Show constraint that led to inference
   - Provide fix suggestions

3. **Ambiguous Types**:
   - Detect under-constrained type variables
   - Request explicit annotations
   - Show possible type instantiations

## Implementation Plan

### Phase 1: Basic Integration (Current Task)
1. Create InferenceEngine class
2. Integrate with TypeChecker
3. Support basic variable/function inference
4. Update existing tests

### Phase 2: Advanced Features (Future)
1. Let-polymorphism refinement
2. Recursive function handling
3. Generic type argument inference
4. Bidirectional optimization

### Phase 3: Polish (Future)
1. Error message improvements
2. Debug mode implementation
3. Performance optimization
4. Documentation updates

## Testing Strategy

1. **Unit Tests**:
   - Test inference for each expression type
   - Test annotation conflict detection
   - Test substitution application

2. **Integration Tests**:
   - End-to-end inference scenarios
   - Mixed annotated/inferred code
   - Error recovery scenarios

3. **Regression Tests**:
   - All existing TypeChecker tests must pass
   - No behavior change for fully annotated code

## Success Criteria

1. TypeChecker successfully uses inference for missing annotations
2. All existing tests continue to pass
3. At least 20 new tests for inference scenarios
4. Clean separation between inference and checking logic
5. Error messages include inferred type information

## References

- "Types and Programming Languages" - Benjamin Pierce, Chapter 22
- OCaml's type inference implementation
- Kotlin's local type inference
- GHC's bidirectional type checking