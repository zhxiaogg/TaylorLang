# String Handling

## String Literals

```javascript
val simple = "Hello, World!"
val empty = ""

// Multi-line strings
val multiLine = """
  This is a
  multi-line
  string
"""
```

## String Interpolation

```javascript
val name = "Alice"
val age = 30

val greeting = "Hello, ${name}!"
val info = "Name: ${name}, Age: ${age}"
val calculation = "Result: ${2 + 3}"

// Complex expressions
val formatted = "User ${user.name.toUpperCase()} (${user.age} years old)"
```

## String Operations

```kotlin
val text = "Hello, World!"

val upper = text.toUpperCase()          // "HELLO, WORLD!"
val lower = text.toLowerCase()          // "hello, world!"
val length = text.length()              // 13
val trimmed = "  hello  ".trim()        // "hello"

val contains = text.contains("World")   // true
val starts = text.startsWith("Hello")   // true
val ends = text.endsWith("!")           // true

val split = "a,b,c".split(",")          // ["a", "b", "c"]
val joined = ["a", "b", "c"].join(", ") // "a, b, c"
```

