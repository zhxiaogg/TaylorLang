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

// For loops with single expressions
for (item in items) processItem(item)

// For loops with blocks  
for (num in numbers) {
  val processed = transform(num)
  println(processed)
}
```

### Functional Iteration
```kotlin
// Functional iteration (recommended approach)
numbers.forEach(println)
numbers.map(x => x * 2).filter(x => x > 10)

// Creating lists using stdlib
val numbers = List.of(1, 2, 3, 4, 5)
val processed = numbers.map(x => x * 2)
```

