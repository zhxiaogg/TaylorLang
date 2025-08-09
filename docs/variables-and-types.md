# Variables and Types

## Variable Declarations

### Immutable Variables (Default)
```kotlin
// Immutable by default
val name = "Alice"
val age = 30
val pi = 3.14159

// Explicit type annotations (optional)
val count: Int = 42
val message: String = "Hello"
val isValid: Boolean = true
```

### Mutable Variables
```kotlin
// Mutable variables (explicit)
var counter = 0
var status: String = "pending"
```

## Type Annotations

```kotlin
// Optional - type inference works without them
val numbers = [1, 2, 3, 4]              // inferred as List<Int>
val pairs = [(1, "one"), (2, "two")]    // inferred as List<(Int, String)>

// Explicit when needed for clarity
val users: List<User> = []
val config: Map<String, String> = {}
```

## Destructuring

### Tuples
```kotlin
// Tuples
val point = (10, 20)
val (x, y) = point
```

### Lists
```kotlin
val numbers = [1, 2, 3, 4, 5]
val (first, rest) = numbers.split()
val [head, ...tail] = numbers
```

### Records
```kotlin
data class Person(name: String, age: Int, email: String)
val person = Person("Alice", 30, "alice@example.com")
val (name, age, email) = person.destructure()
```

## Primitive Types

### Numbers
```kotlin
val integer: Int = 42
val long: Long = 42L
val float: Float = 3.14f
val double: Double = 3.14159
```

### Text
```kotlin
val char: Char = 'A'
val string: String = "Hello, World!"
```

### Boolean
```kotlin
val isTrue: Boolean = true
val isFalse: Boolean = false
```

### Unit Type
```kotlin
val unit: Unit = ()  // Similar to void
```