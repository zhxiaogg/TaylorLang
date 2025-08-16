# Functions

## Function Definitions

### Basic Functions
```kotlin
// Expression body syntax
fn add(x: Int, y: Int): Int => x + y

// Block body syntax  
fn multiply(x: Int, y: Int): Int {
  return x * y
}

// Type inference (annotations optional)
fn subtract(x, y) => x - y

// Multi-line function body
fn factorial(n: Int): Int {
  if (n <= 1) {
    return 1
  } else {
    return n * factorial(n - 1)
  }
}
```

### Generic Functions
```kotlin
// Expression body
fn identity<T>(x: T): T => x

// Block body
fn map<T, U>(list: List<T>, transform: (T) => U): List<U> {
  // implementation
  return transformedList
}
```

## Lambda Expressions

### Arrow Function Syntax
```kotlin
// Arrow function syntax
val double = x => x * 2
val add = (x, y) => x + y

// With type annotations
val multiply = (x: Int, y: Int) => x * y
```

### Lambda Type Inference

Lambda expressions support sophisticated type inference that integrates with Taylor's constraint-based type system:

```kotlin
// Parameter type inference from usage context
val numbers = [1, 2, 3, 4, 5]
val doubled = numbers.map(x => x * 2)  // x inferred as Int
val filtered = numbers.filter(x => x > 3)  // x inferred as Int

// Type constraints from operators
val isPositive = x => x > 0    // Constrains x to numeric type (Int, Long, Float, Double)
val isAdult = age => age >= 18  // Constrains age to numeric type

// Multiple parameter inference
val compare = (a, b) => a > b   // Both a and b constrained to same comparable type
val calculate = (x, y) => x * y + 1  // Both x and y constrained to numeric types
```

**CRITICAL IMPLEMENTATION REQUIREMENT**: Lambda parameter type inference MUST use Taylor's constraint-based type checking system. Type variables generated during lambda analysis must be properly integrated with the global constraint solver to ensure sound type inference.

#### Type Constraint Propagation

Lambda expressions establish type constraints that propagate through the constraint system:

1. **Operator Constraints**: Usage of operators (`>`, `+`, `*`, etc.) constrains lambda parameters to compatible types
2. **Context Constraints**: Function calls receiving lambdas establish constraints based on expected parameter types  
3. **Return Type Constraints**: Lambda body expressions constrain the return type
4. **Cross-Parameter Constraints**: Operations between parameters establish relationships between their types

```kotlin
// Example constraint establishment:
val processor = (input, threshold) => {
  if (input.length > threshold) {  // input: String, threshold: Int
    input.toUpperCase()            // return: String
  } else {
    "default"
  }
}
// Final inferred type: (String, Int) => String
```

#### Type System Integration Requirements

**MANDATORY**: Lambda type inference implementations must adhere to these Taylor language design requirements:

1. **Constraint-Based Architecture**: All lambda parameter type inference must flow through the `ConstraintCollector` and `ConstraintSolver` systems. Direct type checking bypasses are forbidden.

2. **Type Variable Management**: Type variables for lambda parameters must be created through `TypeFactory` and managed by the global type context. Ad-hoc type variable naming schemes violate language consistency.

3. **Unification Integration**: Lambda parameter constraints must be resolved through `TypeOperations.unify()` to maintain type system coherence.

4. **Error Propagation**: Type inference failures must generate meaningful error messages through the standard `TypeError` hierarchy.

**IMPLEMENTATION ANTI-PATTERNS** (Must be avoided):
- Direct arithmetic type checking for lambda parameters
- Hardcoded type variable recognition patterns  
- Bypassing the constraint resolution system
- Assuming type constraints without formal establishment

### Multi-line Lambdas
```kotlin
val process = data => {
  val cleaned = data.trim()
  val validated = validate(cleaned)
  validated.toUpperCase()
}
```

### Higher-order Functions
```kotlin
val numbers = Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Nil())))))
val doubled = numbers.map(x => x * 2)
val evens = numbers.filter(x => x % 2 == 0)
```

## Function Types

### Function Type Annotations
```kotlin
// Function type annotations
val operation: (Int, Int) => Int = (x, y) => x + y
val predicate: (String) => Boolean = s => s.length > 0
```

### Higher-order Function Parameters
```kotlin
// Expression body
fn applyTwice<T>(f: (T) => T, value: T): T => f(f(value))

// Block body returning lambda
fn compose<A, B, C>(f: (B) => C, g: (A) => B): (A) => C {
  return x => f(g(x))
}
```

## Function Composition

Functions can be composed using standard functional programming patterns:

```kotlin
val addOne = x => x + 1
val double = x => x * 2
val addOneThenDouble = compose(double, addOne)

// Result: addOneThenDouble(5) => 12
```