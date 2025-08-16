# Types and Nullability

## Type System Overview

TaylorLang uses a unified type system with union types, type aliases, and nullable types for safe and expressive type modeling.

## Union Types

Union types represent values that can be one of several types:

```kotlin
// Union type syntax
type User = SystemUser | Customer | Guest

// Standard library types (provided by stdlib)
// type Result<T, E> = Ok(T) | Error(E)  // This is defined in stdlib

// Custom domain-specific types
type ApiResponse = 
  | Success(data: String)
  | Failed(message: String)

type HttpResponse = 
  | Success(status: Int, body: String)
  | Redirect(url: String) 
  | ClientError(code: Int, message: String)
  | ServerError(code: Int, details: String)

// Recursive union types
type List<T> = Nil | Cons(head: T, tail: List<T>)

type BinaryTree<T> = 
  | Empty
  | Node(value: T, left: BinaryTree<T>, right: BinaryTree<T>)
```

### Generic Type Inference Requirements

**CRITICAL SPECIFICATION**: Constructor calls must automatically infer generic type parameters:

```kotlin
// Basic inference (MANDATORY)
type Option<T> = Some(T) | None
val intOption = Some(42)        // MUST infer Option<Int>
val stringOption = Some("hello") // MUST infer Option<String>
val boolOption = Some(true)     // MUST infer Option<Boolean>

// Multi-parameter inference (MANDATORY)
type Result<T, E> = Ok(T) | Error(E)
val success = Ok(42)           // MUST infer Result<Int, ?>
val failure = Error("failed")  // MUST infer Result<?, String>

// Nested constructor inference (MANDATORY)
val nested = Ok(Some(42))      // MUST infer Result<Option<Int>, ?>
```

**IMPLEMENTATION REQUIREMENT**: Type checker MUST resolve generic type parameters from constructor arguments without explicit type annotations.

**TYPE CHECKER IMPLEMENTATION SPECIFICATIONS**:

1. **Constructor Call Analysis** (MANDATORY):
   ```kotlin
   // Type checker MUST implement this analysis for ConstructorCall nodes:
   // 1. Identify union type definition from constructor name
   // 2. Extract type parameters from union type definition  
   // 3. Analyze constructor argument types
   // 4. Unify type parameters with argument types using most general unifier
   // 5. Substitute inferred types back into union type
   ```

2. **Type Unification Algorithm** (MANDATORY):
   - MUST implement Hindley-Milner style unification for type inference
   - MUST handle nested generic types (e.g., `Option<List<Int>>`)
   - MUST support partial type inference with unknown type variables
   - MUST detect and report unification failures with clear error messages

3. **Integration Points** (MANDATORY):
   - Constructor calls in `LiteralExpressionChecker.visitConstructorCall()`
   - Variable declarations with inferred types
   - Function return type inference
   - Pattern matching type refinement

4. **Error Handling** (MANDATORY):
   - MUST report "Cannot infer type parameter T for constructor X" when inference fails
   - MUST provide type mismatch details when unification fails
   - MUST suggest explicit type annotations when inference is ambiguous

5. **Implementation Anti-Patterns** (FORBIDDEN):
   - **Custom Type Comparison Methods**: All type compatibility checking MUST use `TypeOperations.areCompatible()` or `TypeOperations.areEqual()`. Custom `typesCompatible()` methods violate centralized type operations design.
   - **Inconsistent Error Handling**: Mixed error handling patterns that don't follow Result<T,E> specifications are forbidden.
   - **Missing Cycle Detection**: Recursive type processing without cycle detection can cause infinite loops and is not permitted.

6. **Architectural Requirements** (MANDATORY):
   - **Centralized Type Operations**: All type checking components MUST delegate to `TypeOperations` facade
   - **Uniform Result<T,E> Patterns**: Error handling MUST follow standardized Result<T,E> propagation as defined in error-handling.md
   - **Recursive Type Safety**: Union type processing MUST include cycle detection for recursive type definitions

## Product Types

Product types combine multiple values into a single type:

```kotlin
// Simple product types
type Person(name: String, age: Int, email: String)
type Point(x: Double, y: Double)

// With default values
type Config(
  host: String = "localhost",
  port: Int = 8080,
  ssl: Boolean = false
)

// Generic product types
type Pair<A, B>(first: A, second: B)
type Box<T>(value: T)
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

TaylorLang provides a Result type for explicit error handling that directly integrates with JVM's exception hierarchy:

```kotlin
// Standard library Result type (provided by stdlib)
type Result<T, E extends Throwable> = Ok(T) | Error(E)

// Using specific JVM exception types
fn readFile(path: String): Result<String, IOException> => {
  try File(path).readText()
}

fn parseNumber(input: String): Result<Int, NumberFormatException> => {
  try Integer.parseInt(input)
}
```

### Pattern Matching with Results

```kotlin
fn processResult<T>(result: Result<T, Exception>) => match result {
  case Ok(value) => println("Success: ${value}")
  case Error(error) => println("Failed: ${error.message}")
}
```

For comprehensive documentation on error handling patterns, try expressions, and JVM integration, see [Error Handling](error-handling.md).

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

