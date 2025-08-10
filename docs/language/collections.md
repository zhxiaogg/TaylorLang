# Collections

## Lists

### List Creation (Using Stdlib)
```kotlin
val numbers = List.of(1, 2, 3, 4, 5)
val names = List.of("Alice", "Bob", "Charlie") 
val empty: List<String> = List.empty()

// Alternative builder syntax
val range = List.range(1, 10)
val repeated = List.repeat(42, 5)      // [42, 42, 42, 42, 42]
```

### List Operations
```kotlin
val doubled = numbers.map(x => x * 2)
val evens = numbers.filter(x => x % 2 == 0)
val sum = numbers.reduce((a, b) => a + b)
val first = numbers.head()           // Int?
val tail = numbers.tail()            // List<Int>
val appended = numbers.append(6)     // List<Int>
```

## Maps

### Map Creation (Using Stdlib)
```kotlin
val config = Map.of(
  "host", "localhost",
  "port", 8080,
  "ssl", true
)

val scores: Map<String, Int> = Map.of(
  "Alice", 95,
  "Bob", 87,
  "Charlie", 92
)

val empty: Map<String, Int> = Map.empty()
```

### Map Operations
```kotlin
val alice_score = scores.get("Alice")    // Int?
val updated = scores.put("David", 88)    // Map<String, Int>
val keys = scores.keys()                 // List<String>
val values = scores.values()             // List<Int>
val hasKey = scores.contains("Alice")    // Boolean
```

## Sets

### Set Creation (Using Stdlib)
```kotlin
val unique = Set.of(1, 2, 3, 2, 1)      // Set(1, 2, 3)
val letters = Set.of('a', 'b', 'c')
val empty: Set<Int> = Set.empty()

val combined = set1.union(set2)
val common = set1.intersection(set2) 
val difference = set1.difference(set2)
val contains = unique.contains(2)        // Boolean
```

## Tuples

```kotlin
val point = (10, 20)
val person = ("Alice", 30, "alice@example.com")
val nested = ((1, 2), (3, 4))

// Tuple access
val x = point._1
val y = point._2

// Destructuring
val (x, y) = point
val (name, age, email) = person
```

