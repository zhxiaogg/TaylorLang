# Try Expression Semantics Redesign

**Document Version**: 2.0  
**Created**: 2025-08-15  
**Author**: Taylor Language Expert  
**Status**: Specification Update  

## Executive Summary

This document redesigns try expression semantics to solve the fundamental bootstrapping problem where try expressions can only work with Result types, but you need try expressions to create Result types. The new design enables try expressions to work with any expression type, automatically wrapping non-Result values.

## The Bootstrapping Problem

### Current Specification Issue

The original specification states:
- Try expressions can only be used in functions returning `Result<T, E>`
- Try expressions can only work with expressions that already return `Result<T, E>`

**The Problem**: This creates a circular dependency where you cannot use try expressions to build the foundational Result-returning functions that the rest of the system depends on.

**Examples that should work but currently fail**:
```kotlin
// Basic literal wrapping
fn createNumber(): Result<Int, Throwable> => {
    try 1  // Should work: wrap 1 in Ok(1)
}

// Function call wrapping  
fn processValue(): Result<String, IOException> => {
    try readFile("config.txt")  // Should work: wrap or propagate
}

// Block expression wrapping
fn computation(): Result<Double, MathError> => {
    try {
        val x = 10
        x * 2.5
    }  // Should work: wrap result in Ok
}
```

## New Try Expression Semantics

### Core Principle: Universal Wrapping

Try expressions now work with **any expression type** using these rules:

1. **Result Type Pass-Through**: If expression returns `Result<T, E>`, unwrap on success, propagate on error
2. **Value Wrapping**: If expression returns non-Result type `T`, wrap in `Result<T, Throwable>`  
3. **Exception Catching**: If expression throws, catch and wrap in `Result.Error`
4. **Type Inference**: Infer appropriate Result type based on context

### Detailed Semantic Rules

#### Rule 1: Result Type Handling
```kotlin
// Expression returns Result<String, FileError>
val result = try findUser(id)  // Type: String (unwrapped value)
// If findUser returns Error(e), propagate as Error(e)
// If findUser returns Ok(value), continue with value
```

#### Rule 2: Non-Result Value Wrapping  
```kotlin
// Expression returns Int
val result = try 42  // Type: Int, but function must return Result<Int, E>
// Compiles to: Ok(42)
```

#### Rule 3: Exception Catching
```kotlin
// Expression may throw IOException
val result = try File("test.txt").readText()  
// Type: String, catches IOException and wraps as Error
// Function returns Result<String, IOException>
```

#### Rule 4: Block Expression Handling
```kotlin
val result = try {
    val x = computeValue()  // May throw
    val y = x * 2
    y.toString()
}  // Catches any exceptions, wraps final value
```

### Function Return Type Requirements

Try expressions now impose **minimal constraints** on function return types:

1. **Preferred**: Function returns `Result<T, E>` - try provides value of type `T`
2. **Auto-inference**: Function return type inferred as `Result<T, E>` if not specified
3. **Error**: Function returns non-Result type - try expressions not allowed

```kotlin
// Explicit Result return type
fn process(): Result<String, MyError> => {
    val value = try someOperation()  // value: String
    Ok(value.uppercase())
}

// Inferred Result return type
fn process() => {
    try someOperation()  // Function type inferred as Result<T, Throwable>
}

// Error case
fn process(): String => {
    try someOperation()  // ERROR: Cannot use try in non-Result function
}
```

## Type Inference Rules

### Expression Type Analysis

For `try <expression>`, analyze the expression type:

```kotlin
sealed class TryExpressionType {
    data class ResultType(val valueType: Type, val errorType: Type) : TryExpressionType()
    data class ValueType(val type: Type) : TryExpressionType()
    data class ThrowingType(val type: Type, val exceptionTypes: Set<Type>) : TryExpressionType()
}

fun analyzeTryExpression(expr: Expression): TryExpressionType {
    return when (val exprType = inferType(expr)) {
        is ResultType -> TryExpressionType.ResultType(exprType.valueType, exprType.errorType)
        else -> {
            val exceptions = analyzeExceptions(expr)
            if (exceptions.isNotEmpty()) {
                TryExpressionType.ThrowingType(exprType, exceptions)
            } else {
                TryExpressionType.ValueType(exprType)
            }
        }
    }
}
```

### Result Type Construction

Based on expression analysis, construct the appropriate Result type:

```kotlin
fun constructResultType(
    tryType: TryExpressionType, 
    functionReturnType: Type?
): Type {
    return when (tryType) {
        is TryExpressionType.ResultType -> {
            // Already a Result type - extract value type
            tryType.valueType
        }
        is TryExpressionType.ThrowingType -> {
            // Non-Result but may throw - infer error type
            val errorType = unifyExceptionTypes(tryType.exceptionTypes)
            validateFunctionReturnType(functionReturnType, tryType.type, errorType)
            tryType.type
        }
        is TryExpressionType.ValueType -> {
            // Pure value - wrap in Result with Throwable error type
            validateFunctionReturnType(functionReturnType, tryType.type, BuiltinTypes.THROWABLE)
            tryType.type
        }
    }
}
```

## Catch Clause Semantics

### Enhanced Catch Handling

Catch clauses now work with both Result errors and thrown exceptions:

```kotlin
try {
    val user = try findUser(id)  // May return Result<User, DatabaseError>
    val file = try File("data.txt").readText()  // May throw IOException
    processUserData(user, file)
} catch {
    case DatabaseError.UserNotFound(id) => handleMissingUser(id)
    case IOException(msg) => handleFileError(msg)  
    case e: Throwable => handleGeneralError(e)
}
```

### Catch Type Inference

Catch clauses must handle the union of all possible error types:

```kotlin
// Error type is: DatabaseError | IOException | Throwable
val errorType = unifyErrorTypes([
    DatabaseError,  // From findUser Result
    IOException,    // From File.readText() exceptions
    Throwable       // Fallback for any other exceptions
])
```

## Updated Examples

### Example 1: Basic Value Wrapping
```kotlin
// All these now work:
fn createOne(): Result<Int, Throwable> => try 1
fn createString(): Result<String, Throwable> => try "hello"
fn createList(): Result<List<Int>, Throwable> => try [1, 2, 3]
```

### Example 2: Function Call Wrapping
```kotlin
// Wrap function results
fn getSystemTime(): Result<Long, Throwable> => {
    try System.currentTimeMillis()  // Wraps Long in Result
}

// Chain with Result-returning functions
fn processUser(id: String): Result<String, DatabaseError> => {
    val user = try database.findUser(id)  // Result<User, DatabaseError> -> User
    val name = try user.getName()  // String -> String (wrapped)
    Ok(name.uppercase())
}
```

### Example 3: Block with Exception Handling
```kotlin
fn readAndProcess(filename: String): Result<ProcessedData, IOException> => {
    try {
        val content = File(filename).readText()  // May throw IOException
        val lines = content.split("\n")
        ProcessedData(lines.size, content.length)
    } catch {
        case IOException(msg) => Error(IOException("Failed to read $filename: $msg"))
    }
}
```

### Example 4: Mixed Result and Value Operations
```kotlin
fn complexOperation(): Result<String, AppError> => {
    val config = try loadConfig()  // Result<Config, ConfigError> -> Config
    val timestamp = try System.currentTimeMillis()  // Long -> Long
    val data = try {
        val raw = fetchData(config.url)  // May throw NetworkException
        processRawData(raw, timestamp)
    } catch {
        case NetworkException(e) => throw AppError.NetworkFailure(e)
        case ProcessingException(e) => throw AppError.ProcessingFailure(e)
    }
    Ok("$config.name-$timestamp-${data.hash}")
}
```

### Example 5: Function Without Explicit Return Type
```kotlin
// Return type inferred as Result<Int, Throwable>
fn compute() => {
    val x = try calculateBase()  // calculateBase() returns Int
    val y = try calculateMultiplier()  // Returns Int
    try x * y  // Final result: Int, wrapped in Ok()
}
```

## Implementation Strategy

### Phase 1: Type System Updates

1. **Remove Result-only restriction** from try expression validation
2. **Add expression type analysis** to determine wrapping strategy
3. **Update function return type inference** for try-containing functions
4. **Enhance error type unification** for mixed error sources

### Phase 2: Bytecode Generation Updates

1. **Value wrapping logic**: Generate `Ok(value)` for non-Result expressions
2. **Exception catching**: Wrap try blocks with exception handlers
3. **Result unwrapping**: Extract values from Result types with error propagation
4. **Mixed error handling**: Support both Result errors and caught exceptions

### Phase 3: Runtime Support

1. **Enhanced TaylorResult**: Support for mixed error types
2. **Exception conversion**: Automatic Throwable to Result conversion
3. **Performance optimization**: Minimize overhead for simple cases

## Breaking Changes and Migration

### What Changes

1. **Try expressions now work everywhere** (instead of Result-only functions)
2. **Auto-wrapping behavior** for non-Result expressions  
3. **Relaxed function return type requirements**

### Migration Path

Existing code continues to work unchanged. New capabilities are additive:

```kotlin
// Old way (still works)
fn oldStyle(): Result<String, DatabaseError> => {
    database.findUser(id) match {
        case Ok(user) => Ok(user.name)
        case Error(e) => Error(e)
    }
}

// New way (now possible)
fn newStyle(): Result<String, DatabaseError> => {
    val user = try database.findUser(id)
    Ok(user.name)
}
```

## Benefits of the New Design

1. **Eliminates Bootstrapping Problem**: Can use try to create Result-returning functions
2. **Improved Ergonomics**: Natural wrapping of values and exceptions
3. **Type Safety Maintained**: All error cases properly typed and handled
4. **Backwards Compatible**: Existing code continues to work
5. **Flexible Error Handling**: Supports both functional and exception-based patterns

This redesign transforms try expressions from a limited Result-unwrapping mechanism into a comprehensive error handling tool that makes Taylor's Result type system actually usable in practice.
