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
async fn printNumbers(numbers: List<Int>) => Unit {
  for (num in numbers) {
    println(num)
  }
}
```

### Functional Iteration
```kotlin
// Functional iteration
numbers.forEach(println)
numbers.map(x => x * 2).filter(x => x > 10)
```

