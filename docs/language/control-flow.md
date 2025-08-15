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

## Error Handling with Try Expressions

TaylorLang provides try expressions for type-safe error handling. For comprehensive documentation on error handling, see [Error Handling](error-handling.md).

### Basic Try Expression Usage

```kotlin
// Try expressions work with any expression type
fn getTimestamp(): Result<Long, Throwable> => {
  try System.currentTimeMillis()  // Wraps result or catches exceptions
}

// Result type pass-through
fn processFiles(): Result<String, IOException> => {
  val content = try readFile("data.txt")  // Result<String, IOException> -> String
  Ok(content.toUpperCase())
}

// Exception handling
fn robustOperation(): Result<Data, Exception> => {
  try {
    val data = loadData()  // May throw various exceptions
    Ok(data)
  } catch {
    case IOException(e) => Error(e)
    case e: RuntimeException => throw e  // Re-throw unexpected errors
  }
}
```

For detailed error handling patterns, type rules, and JVM integration, see the [Error Handling](error-handling.md) documentation.

