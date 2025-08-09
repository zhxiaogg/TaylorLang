# Collections

## Lists

### List Literals
```javascript
val numbers = [1, 2, 3, 4, 5]
val names = ["Alice", "Bob", "Charlie"]
val empty: List<String> = []
```

### List Operations
```kotlin
val doubled = numbers.map(x => x * 2)
val evens = numbers.filter(x => x % 2 == 0)
val sum = numbers.reduce((a, b) => a + b)
val first = numbers.head()           // Option<Int>
val tail = numbers.tail()            // List<Int>
```

## Maps

### Map Literals
```javascript
val config = {
  "host": "localhost",
  "port": 8080,
  "ssl": true
}

val scores: Map<String, Int> = {
  "Alice": 95,
  "Bob": 87,
  "Charlie": 92
}
```

### Map Operations
```kotlin
val alice_score = scores.get("Alice")    // Option<Int>
val updated = scores.put("David", 88)    // Map<String, Int>
val keys = scores.keys()                 // List<String>
val values = scores.values()             // List<Int>
```

## Sets

```kotlin
val unique = Set.of(1, 2, 3, 2, 1)      // Set(1, 2, 3)
val letters = Set.of('a', 'b', 'c')

val combined = set1.union(set2)
val common = set1.intersection(set2)
val difference = set1.difference(set2)
```

## Tuples

```kotlin
val point = (10, 20)
val person = ("Alice", 30, "alice@example.com")
val nested = ((1, 2), (3, 4))

// Tuple access
val x = point._1
val y = point._2

// Destructuring (preferred)
val (x, y) = point
val (name, age, email) = person
```

## Collection Best Practices

- Use immutable collections by default
- Prefer functional operations (map, filter, reduce) over loops
- Choose the right collection type for your use case
- Leverage destructuring for cleaner code
- Use comprehensions for data transformation