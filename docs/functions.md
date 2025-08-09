# Functions

## Function Definitions

### Basic Functions
```kotlin
// Basic function with type annotations
fn add(x: Int, y: Int): Int => x + y

// Type inference (annotations optional)
fn multiply(x, y) => x * y

// Multi-line function body
fn factorial(n: Int): Int => {
  if (n <= 1) 1
  else n * factorial(n - 1)
}
```

### Generic Functions
```kotlin
fn identity<T>(x: T): T => x
fn map<T, U>(list: List<T>, transform: (T) => U): List<U> => {
  // implementation
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
val numbers = [1, 2, 3, 4, 5]
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
fn applyTwice<T>(f: (T) => T, value: T): T => f(f(value))
fn compose<A, B, C>(f: (B) => C, g: (A) => B): (A) => C => x => f(g(x))
```

## Function Composition

Functions can be composed using standard functional programming patterns:

```kotlin
val addOne = x => x + 1
val double = x => x * 2
val addOneThenDouble = compose(double, addOne)

// Result: addOneThenDouble(5) => 12
```