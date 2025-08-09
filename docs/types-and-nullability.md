# Types and Nullability

## Type System Overview

TaylorLang uses a unified type system with union types, type aliases, and nullable types for safe and expressive type modeling.

## Union Types

Union types represent values that can be one of several types:

```kotlin
// Union type syntax
type User = SystemUser | Customer | Guest

type Result<T, E> = Ok(T) | Error(E)

type HttpResponse = 
  | Success(status: Int, body: String)
  | Redirect(url: String) 
  | ClientError(code: Int, message: String)
  | ServerError(code: Int, details: String)
```

## Type Aliases

Create aliases for existing types to improve readability and maintainability:

```kotlin
// Simple type alias
type UserId = String
type Email = String

// Nullable type aliases
type NullableUser = User?
type NullableUser = User | Null  // equivalent syntax

// Complex aliases
type UserMap = Map<UserId, User>
type ValidationResult = Result<User, String>
```

## Nullable Types

### Nullable Type Syntax

Use `?` to indicate nullable types:

```kotlin
fn findUser(id: String): User? => {
  // Implementation that may return null
  if (userExists(id)) {
    getUserById(id)
  } else {
    null
  }
}

// Alternative union syntax
fn findUser(id: String): User | Null => {
  // Same implementation
}
```

### Working with Nullable Values

```kotlin
val maybeUser: User? = findUser("123")

// Pattern matching
val message = match maybeUser {
  case user: User => "Found user: ${user.name}"
  case null => "User not found"
}

// Safe navigation
val userName = maybeUser?.name
val userEmail = maybeUser?.profile?.email

// Null coalescing
val name = maybeUser?.name ?: "Unknown"
val displayName = user?.fullName ?: user?.name ?: "Anonymous"
```

### Null Checks and Conditionals

```kotlin
// Simple null check
if (user != null) {
  println("User name: ${user.name}")
}

// Combined null checks
if (user != null && user.profile != null) {
  println("Profile: ${user.profile.bio}")
}

// Pattern matching with multiple nullables
match (user1, user2) {
  case (u1: User, u2: User) => println("Both users found")
  case (null, u2: User) => println("Only second user found")
  case (u1: User, null) => println("Only first user found") 
  case (null, null) => println("No users found")
}
```

## Result Types and Error Handling

### Result Type Definition

```kotlin
type Result<T, E> = Ok(T) | Error(E)

type DatabaseError = 
  | ConnectionFailed(message: String)
  | UserNotFound(id: String)
  | ValidationError(field: String, reason: String)
```

### Using try for Error Propagation

```kotlin
// Basic try syntax - unwraps Result or propagates error
fn findUserSafe(id: String): Result<User?, DatabaseError> => {
  val user = try database.findUser(id)  // database.findUser returns Result
  Ok(user)
}

// try with null handling
fn getUserName(id: String): Result<String?, DatabaseError> => {
  val userName = try database.findUser(id)?.name
  Ok(userName)
}

// Explicit error handling
fn findUserWithLogging(id: String): Result<User?, DatabaseError> => {
  try {
    val user = try database.findUser(id)
    Ok(user)
  } catch {
    case ConnectionFailed(msg) => {
      log.error("Database connection failed: ${msg}")
      Error(ConnectionFailed(msg))
    }
    case err => Error(err)
  }
}
```

### Error Propagation Rules

1. **Functions returning Result**: Can use `try` to unwrap other Results
2. **Functions not returning Result**: Compile error when using `try`
3. **Automatic propagation**: Errors bubble up automatically unless caught

```kotlin
// ✅ Valid - function returns Result
fn processUser(id: String): Result<String, DatabaseError> => {
  val user = try database.findUser(id)
  Ok(user?.name ?: "Anonymous")
}

// ❌ Compile error - function doesn't return Result
fn processUser(id: String): String => {
  val user = try database.findUser(id)  // ERROR: No Result in return type
  user?.name ?: "Anonymous"
}

// ✅ Valid - explicit unwrap with error handling
fn processUser(id: String): String => {
  match database.findUser(id) {
    case Ok(user) => user?.name ?: "Anonymous"
    case Error(_) => "Error occurred"
  }
}
```

## Pattern Matching with Types

### Union Type Pattern Matching

```kotlin
fn handleUser(user: User) => match user {
  case SystemUser(id, permissions) => handleSystemUser(id, permissions)
  case Customer(id, tier) => handleCustomer(id, tier)
  case Guest(sessionId) => handleGuest(sessionId)
}

fn processResult<T, E>(result: Result<T, E>) => match result {
  case Ok(value) => println("Success: ${value}")
  case Error(error) => println("Failed: ${error}")
}
```

### Type Aliases in Pattern Matching

```kotlin
type NullableString = String?

fn processValue(value: NullableString) => match value {
  case str: String => str.toUpperCase()
  case null => "EMPTY"
}

// Works with complex type aliases too
type UserResult = Result<User?, DatabaseError>

fn handleUserResult(result: UserResult) => match result {
  case Ok(user: User) => "Found: ${user.name}"
  case Ok(null) => "User not found"
  case Error(err) => "Database error: ${err}"
}
```

## Best Practices

### Prefer Explicit Nullability

```kotlin
// ✅ Good: Explicit nullable type
fn findUser(id: String): User? 

// ❌ Avoid: Hidden nullability
fn findUser(id: String): User  // May actually return null
```

### Use Safe Navigation

```kotlin
// ✅ Good: Safe navigation chain
val email = user?.profile?.contactInfo?.email

// ❌ Avoid: Multiple null checks
val email = if (user != null && user.profile != null && user.profile.contactInfo != null) {
  user.profile.contactInfo.email
} else {
  null
}
```

### Handle All Cases

```kotlin
// ✅ Good: Exhaustive matching
match optionalValue {
  case value: T => processValue(value)
  case null => handleMissing()
}

// ❌ Avoid: Incomplete handling
if (optionalValue != null) {
  processValue(optionalValue)
}
// Missing null case handling
```

### Use Result Types for Errors

```kotlin
// ✅ Good: Explicit error handling
fn parseUser(json: String): Result<User, ParseError>

// ❌ Avoid: Exception-based errors in pure functions
fn parseUser(json: String): User  // May throw exceptions
```

The type system ensures null safety and error handling through explicit types and exhaustive pattern matching, preventing runtime null pointer errors and unhandled exceptions.