# Effects and I/O

## Async/Await System

TaylorLang uses `async/await` to unify asynchronous operations and I/O effects. This approach consolidates async and I/O concepts under a single, consistent model.

### Why Async for I/O?

While underlying filesystem and database operations might not be inherently async, using `async/await` provides:
- **Unified mental model**: All side effects use the same pattern
- **Composable operations**: Easy chaining and error handling
- **Non-blocking execution**: Better resource utilization
- **Consistent syntax**: Same patterns for network, file, and database operations

## Async Functions

```kotlin
// Async functions use await for calling other async operations
async fn fetchUser(id: String): User => {
  val response = await http.get("/users/${id}")
  response.json().as<User>()
}

async fn processUsers(ids: List<String>): List<User> => {
  val users = []
  for (id in ids) {
    val user = await fetchUser(id)
    users.append(user)
  }
  users
}
```

## I/O Operations

```kotlin
// File operations using async/await
async fn readFile(path: String): String => {
  await filesystem.readText(path)
}

async fn processFile(path: String): String => {
  val content = await readFile(path)
  content.toUpperCase()
}

// Database operations
async fn findUser(id: String): User? => {
  await database.query("SELECT * FROM users WHERE id = ?", id)
}
```

## Resource Management

```kotlin
// Resource management with async/await
async fn withDatabase<T>(operation: (Database) => T): T => {
  val db = await Database.connect()
  try {
    await operation(db)
  } finally {
    await db.close()
  }
}

// Usage
async fn getUserData(id: String): UserData => {
  withDatabase { db =>
    val user = await db.findUser(id)
    val profile = await db.findProfile(user.id)
    UserData(user, profile)
  }
}
```

## Async/Await Best Practices

- Use `async` functions for any operation that performs I/O
- Always `await` async function calls
- Handle errors using `try/catch` or `Result<T, E>` types
- Manage resources with proper cleanup in `finally` blocks
- Keep pure computation separate from async operations
- Use `async/await` consistently for all side effects

## Sequential vs Concurrent Operations

```kotlin
// Sequential execution
async fn processSequentially(ids: List<String>): List<User> => {
  val users = []
  for (id in ids) {
    val user = await fetchUser(id)  // Wait for each one
    users.append(user)
  }
  users
}

// Concurrent execution
async fn processConcurrently(ids: List<String>): List<User> => {
  val userPromises = ids.map(id => fetchUser(id))  // Start all
  val users = await Promise.all(userPromises)      // Wait for all
  users
}
```