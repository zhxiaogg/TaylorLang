# Control Flow

## Conditional Expressions

### If Expressions
```kotlin
// If expressions (return values)
val result = if (score > 90) "A"
            else if (score > 80) "B" 
            else if (score > 70) "C"
            else "F"

// Ternary-style
val status = isValid ? "active" : "inactive"
```

### When Expressions
```kotlin
val grade = score when {
  >= 90 => "A"
  >= 80 => "B"
  >= 70 => "C"
  else => "F"
}
```

## Loops and Iteration

### For Comprehensions
```kotlin
// For comprehensions
val doubled = [for x in numbers => x * 2]
val evens = [for x in numbers if x % 2 == 0 => x]
```

### Traditional Iteration with Effects
```kotlin
fn printNumbers(numbers: List<Int>): IO<Unit> => {
  for (num in numbers) {
    println(num)
  }
}
```

### Functional Iteration (Preferred)
```kotlin
// Functional iteration (preferred)
numbers.forEach(println)
numbers.map(x => x * 2).filter(x => x > 10)
```

## Control Flow Best Practices

- Prefer expressions over statements when possible
- Use pattern matching for complex conditional logic
- Favor functional iteration methods over traditional loops
- Leverage comprehensions for data transformation