# Effects and I/O

## IO Type

```typescript
// IO operations are explicitly typed
fn main(): IO<Unit> => {
  println("Enter your name:")
  val name <- readLine()
  println("Hello, ${name}!")
}

fn readFile(path: String): IO<Result<String, IOError>> => {
  // File reading implementation
}
```

## Async Operations

```typescript
// Async functions return Future<T>
async fn fetchUser(id: String): Future<User> => {
  val response <- http.get("/users/${id}")
  response.json().as<User>()
}

async fn processUsers(ids: List<String>): Future<List<User>> => {
  val futures = ids.map(id => fetchUser(id))
  Future.all(futures)
}
```

## Effect Composition

```kotlin
fn processFile(path: String): IO<Result<String, String>> => {
  readFile(path).flatMap { result =>
    match result {
      case Ok(content) => IO.pure(Ok(content.toUpperCase()))
      case Error(err) => IO.pure(Error("Failed to read: ${err}"))
    }
  }
}
```

## Resource Management

```typescript
fn withDatabase<T>(operation: (Database) => IO<T>): IO<T> => {
  val db <- Database.connect()
  try {
    operation(db)
  } finally {
    db.close()
  }
}
```

## Effect System Best Practices

- Use explicit effect types to track side effects
- Compose effects using monadic operations
- Handle errors explicitly through Result types
- Manage resources safely with proper cleanup
- Prefer async operations for I/O bound tasks
- Keep pure and effectful code separate