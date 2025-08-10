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
async fn fetchData(url: String): String => {
  val response = await http.get(url)
  response.text()
}

async fn processData(urls: List<String>): List<String> => {
  val results = []
  for (url in urls) {
    val data = await fetchData(url)
    results.append(data)
  }
  results
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
async fn queryData(id: String): String? => {
  await database.query("SELECT data FROM items WHERE id = ?", id)
}
```

## Resource Management

```kotlin
// Traditional resource management with async/await
async fn withDatabase<T>(operation: (Database) => T): Result<T, IOError> => {
  val db = await Database.connect()
  try {
    await operation(db)
  } finally {
    await db.close()
  }
}

// Java-style try-with-resources
async fn withDatabase<T>(operation: (Database) => T): Result<T, IOError> => {
  try (val db = await Database.connect()) {
    await operation(db)
  }
}
```

The try-with-resources syntax automatically closes resources that implement the `Closable` interface. The `Database.connect()` and `db.close()` operations return `Result<>` types for error handling.

// Usage
async fn getUserData(id: String): UserData => {
  withDatabase { db =>
    val user = await db.findUser(id)
    val profile = await db.findProfile(user.id)
    UserData(user, profile)
  }
}
```


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