# Algebraic Data Types

## Simple Sum Types

```kotlin
type Status {
  Pending,
  InProgress,
  Completed,
  Failed
}
```

## Sum Types with Associated Data

```kotlin
type Result<T, E> {
  Ok(T),
  Error(E)
}

type Option<T> {
  Some(T),
  None
}

type HttpResponse {
  Success(status: Int, body: String),
  Redirect(url: String),
  ClientError(code: Int, message: String),
  ServerError(code: Int, details: String)
}
```

## Complex Sum Types with Named Fields

```kotlin
type Shape {
  Circle(radius: Double),
  Rectangle(width: Double, height: Double),
  Triangle(a: Double, b: Double, c: Double)
}

type UserAction {
  Login(username: String, timestamp: Long),
  Logout(timestamp: Long),
  UpdateProfile(field: String, newValue: String, timestamp: Long)
}
```

## Recursive ADTs

```kotlin
type List<T> {
  Nil,
  Cons(head: T, tail: List<T>)
}

type BinaryTree<T> {
  Empty,
  Node(value: T, left: BinaryTree<T>, right: BinaryTree<T>)
}

type Json {
  Null,
  Bool(Boolean),
  Number(Double),
  String(String),
  Array(List<Json>),
  Object(Map<String, Json>)
}
```

## Product Types

### Simple Product Types
```kotlin
type Person(name: String, age: Int, email: String)
type Point(x: Double, y: Double)
```

### With Default Values
```kotlin
type Config(
  host: String = "localhost",
  port: Int = 8080,
  ssl: Boolean = false
)
```

### Generic Product Types
```kotlin
type Pair<A, B>(first: A, second: B)
type Box<T>(value: T)
```

## ADT Design Principles

- Use sum types for OR relationships (one of many variants)
- Use product types for AND relationships (combination of fields)
- Leverage generics for reusable ADTs
- Make invalid states unrepresentable through type design
- Prefer consistent syntax with parentheses for parameters