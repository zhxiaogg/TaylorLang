# Collections

## Lists

TaylorLang lists are implemented as union types following functional programming principles:

```kotlin
// List type definition
type List<T> = Cons(T, List<T>) | Nil()
```

### List Creation
```kotlin
// Constructor-based creation
val empty = Nil()
val single = Cons(42, Nil())
val numbers = Cons(1, Cons(2, Cons(3, Nil())))

// Using stdlib factory functions (compile to constructors)
val built = listOf3(1, 2, 3)        // Creates: Cons(1, Cons(2, Cons(3, Nil())))
val fromEmpty = emptyList()         // Creates: Nil()
val fromSingle = singletonList(42)  // Creates: Cons(42, Nil())
```

### List Pattern Matching
```kotlin
fn processList<T>(list: List<T>) => match list {
  case Nil() => "Empty list"
  case Cons(head, Nil()) => "Single element: ${head}"
  case Cons(head, tail) => "Head: ${head}, has tail"
}

// Complex patterns
fn getSecond<T>(list: List<T>) => match list {
  case Cons(first, Cons(second, tail)) => Some(second)
  case _ => None()
}
```

### List Operations (Planned)
```kotlin
// Future functional operations
val doubled = numbers.map(x => x * 2)
val evens = numbers.filter(x => x % 2 == 0)
val sum = numbers.fold(0, (a, b) => a + b)
val first = numbers.head()           // T?
val tail = numbers.tail()            // List<T>
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

