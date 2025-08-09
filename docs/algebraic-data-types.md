# Algebraic Data Types

## Simple Enums

```kotlin
enum Status {
  Pending,
  InProgress,
  Completed,
  Failed
}
```

## Enums with Associated Data

```rust
enum Result<T, E> {
  Ok(T),
  Error(E)
}

enum Option<T> {
  Some(T),
  None
}

enum HttpResponse {
  Success(status: Int, body: String),
  Redirect(url: String),
  ClientError(code: Int, message: String),
  ServerError(code: Int, details: String)
}
```

## Complex ADTs with Named Fields

```kotlin
enum Shape {
  Circle(radius: Double),
  Rectangle(width: Double, height: Double),
  Triangle(a: Double, b: Double, c: Double)
}

enum UserAction {
  Login { username: String, timestamp: Long },
  Logout { timestamp: Long },
  UpdateProfile { field: String, newValue: String, timestamp: Long }
}
```

## Recursive ADTs

```haskell
enum List<T> {
  Nil,
  Cons(head: T, tail: List<T>)
}

enum BinaryTree<T> {
  Empty,
  Node(value: T, left: BinaryTree<T>, right: BinaryTree<T>)
}

enum Json {
  Null,
  Bool(Boolean),
  Number(Double),
  String(String),
  Array(List<Json>),
  Object(Map<String, Json>)
}
```

## Data Classes

### Simple Data Classes
```kotlin
data class Person(name: String, age: Int, email: String)
data class Point(x: Double, y: Double)
```

### With Default Values
```kotlin
data class Config(
  host: String = "localhost",
  port: Int = 8080,
  ssl: Boolean = false
)
```

### Generic Data Classes
```kotlin
data class Pair<A, B>(first: A, second: B)
data class Box<T>(value: T)
```

## ADT Design Principles

- Use enums for sum types (OR relationships)
- Use data classes for product types (AND relationships)
- Leverage generics for reusable ADTs
- Make invalid states unrepresentable through type design