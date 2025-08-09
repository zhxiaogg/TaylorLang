# Null Safety

TaylorLang provides null safety through its type system, preventing null pointer exceptions at compile time.

## Nullable Types

### Optional Values

Use `Option<T>` for values that may or may not exist:

```kotlin
type User(name: String, email: String)

fn findUser(id: String): Option<User> => {
  // Implementation that may or may not find a user
  if (userExists(id)) {
    Some(User("Alice", "alice@example.com"))
  } else {
    None
  }
}
```

### Working with Options

```kotlin
val maybeUser = findUser("123")

// Pattern matching
val message = match maybeUser {
  case Some(user) => "Found user: ${user.name}"
  case None => "User not found"
}

// Safe access with default
val userName = maybeUser
  .map(user => user.name)
  .getOrElse("Unknown")

// Chaining operations
val userEmail = findUser("123")
  .map(user => user.email)
  .filter(email => email.contains("@"))
  .getOrElse("No valid email")
```

## Nullable References

For interoperability scenarios, nullable references are supported with the `?` syntax:

```kotlin
// Nullable reference type
fn processNullable(value: String?): String => {
  match value {
    case Some(str) => str.toUpperCase()
    case None => "DEFAULT"
  }
}

// Safe navigation
fn getLength(value: String?): Int => {
  value?.length ?: 0
}

// Null coalescing
fn getName(user: User?): String => {
  user?.name ?: "Anonymous"
}
```

## Safe Unwrapping

### Option Methods

```kotlin
val maybeValue: Option<String> = Some("hello")

// Safe unwrapping
val length = maybeValue.map(s => s.length)              // Option<Int>
val upper = maybeValue.map(s => s.toUpperCase())        // Option<String>

// Filtering
val filtered = maybeValue.filter(s => s.startsWith("h")) // Option<String>

// Default values
val result = maybeValue.getOrElse("default")             // String

// Chaining
val processed = maybeValue
  .map(s => s.trim())
  .filter(s => s.length > 0)
  .map(s => s.toUpperCase())
  .getOrElse("EMPTY")
```

### Combining Options

```kotlin
fn combineUsers(id1: String, id2: String): Option<String> => {
  val user1 = findUser(id1)
  val user2 = findUser(id2)
  
  // Combine two options
  user1.flatMap { u1 =>
    user2.map { u2 =>
      "${u1.name} and ${u2.name}"
    }
  }
}

// Using for comprehension-like syntax
fn combineUsersAlt(id1: String, id2: String): Option<String> => {
  val user1 <- findUser(id1)
  val user2 <- findUser(id2)
  Some("${user1.name} and ${user2.name}")
}
```

## Error Handling with Result

Combine null safety with error handling using `Result<T, E>`:

```kotlin
type DatabaseError {
  ConnectionFailed(message: String),
  UserNotFound(id: String),
  ValidationError(field: String, reason: String)
}

fn findUserSafe(id: String): Result<Option<User>, DatabaseError> => {
  try {
    val user = database.findUser(id)  // May return null or throw
    match user {
      case null => Ok(None)
      case user => Ok(Some(user))
    }
  } catch {
    case ConnectionException(msg) => Error(ConnectionFailed(msg))
    case _ => Error(UserNotFound(id))
  }
}

fn processUserSafely(id: String): Result<String, DatabaseError> => {
  findUserSafe(id).flatMap { maybeUser =>
    match maybeUser {
      case Some(user) => Ok("User: ${user.name}")
      case None => Error(UserNotFound(id))
    }
  }
}
```

## Best Practices

### Prefer Option over Nullable

```kotlin
// Good: Use Option for optional values
fn findUser(id: String): Option<User>

// Avoid: Using nullable references unnecessarily  
fn findUser(id: String): User?
```

### Use Safe Navigation

```kotlin
// Good: Safe navigation
val length = maybeString?.length ?: 0

// Avoid: Explicit null checks
val length = if (maybeString != null) maybeString.length else 0
```

### Chain Operations

```kotlin
// Good: Chain operations safely
val result = getValue()
  .map(transform)
  .filter(isValid)
  .getOrElse(defaultValue)

// Avoid: Nested null checks
val result = {
  val value = getValue()
  if (value != null) {
    val transformed = transform(value)
    if (transformed != null && isValid(transformed)) {
      transformed
    } else {
      defaultValue
    }
  } else {
    defaultValue
  }
}
```

### Handle All Cases

```kotlin
// Good: Exhaustive matching
match optionalValue {
  case Some(value) => processValue(value)
  case None => handleMissing()
}

// Avoid: Ignoring None case
match optionalValue {
  case Some(value) => processValue(value)
  // Missing None case - compile error
}
```

The type system ensures null safety by making nullability explicit and forcing you to handle all cases at compile time.