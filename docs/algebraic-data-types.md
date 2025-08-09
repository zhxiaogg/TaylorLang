# Algebraic Data Types

## Union Types (Sum Types)

Union types represent values that can be one of several variants:

```kotlin
// Simple union types
type Status = Pending | InProgress | Completed | Failed

// Union types with associated data
type Result<T, E> = Ok(T) | Error(E)

type HttpResponse = 
  | Success(status: Int, body: String)
  | Redirect(url: String)
  | ClientError(code: Int, message: String)
  | ServerError(code: Int, details: String)
```

## Complex Union Types with Named Fields

```kotlin
type Shape = 
  | Circle(radius: Double)
  | Rectangle(width: Double, height: Double)
  | Triangle(a: Double, b: Double, c: Double)

type UserAction =
  | Login(username: String, timestamp: Long)
  | Logout(timestamp: Long)
  | UpdateProfile(field: String, newValue: String, timestamp: Long)
```

## Recursive Union Types

```kotlin
type List<T> = Nil | Cons(head: T, tail: List<T>)

type BinaryTree<T> = 
  | Empty
  | Node(value: T, left: BinaryTree<T>, right: BinaryTree<T>)

type Json =
  | Null
  | Bool(Boolean)
  | Number(Double)
  | String(String)
  | Array(List<Json>)
  | Object(Map<String, Json>)
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

## Type Design Principles

- Use union types for OR relationships (one of many variants)
- Use product types for AND relationships (combination of fields) 
- Use type aliases for better readability and maintainability
- Leverage generics for reusable types
- Make invalid states unrepresentable through type design
- Prefer explicit union syntax with `|` for clarity