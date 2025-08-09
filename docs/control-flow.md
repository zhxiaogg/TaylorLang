# Control Flow

## Conditional Expressions

### If Expressions
```kotlin
// If expressions (return values)
val result = if (score > 90) "A"
            else if (score > 80) "B" 
            else if (score > 70) "C"
            else "F"
```

### Match Expressions
```kotlin
val grade = match score {
  case x if x >= 90 => "A"
  case x if x >= 80 => "B"
  case x if x >= 70 => "C"
  case _ => "F"
}
```

## Loops and Iteration

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