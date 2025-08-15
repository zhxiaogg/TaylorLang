# Error Handling

## Overview

TaylorLang provides a unified error handling system that directly leverages JVM's native exception hierarchy through Result types, eliminating unnecessary wrapper types while providing type-safe error management.

## Core Design Philosophy

TaylorLang's error handling is built on two fundamental principles:

1. **Direct JVM Integration**: Use `Throwable` and its subclasses directly without wrapper types
2. **Type-Safe Results**: Combine native exceptions with Result types for explicit error handling

## Result Type System

### Result Type Definition

```kotlin
// Standard library Result type (provided by stdlib)
type Result<T, E extends Throwable> = Ok(T) | Error(E)
```

The Result type uses Java's Throwable hierarchy directly, enabling seamless integration with existing JVM libraries and eliminating the need for custom error wrapper types.

### Basic Result Usage

```kotlin
// Direct use of standard JVM exceptions
fn readFile(path: String): Result<String, IOException> => {
  try File(path).readText()
}

fn parseNumber(input: String): Result<Int, NumberFormatException> => {
  try Integer.parseInt(input)
}

fn validateEmail(email: String): Result<String, IllegalArgumentException> => {
  if (email.contains("@")) {
    Ok(email)
  } else {
    Error(IllegalArgumentException("Invalid email format: ${email}"))
  }
}
```

### Working with Exception Hierarchies

```kotlin
// Using specific exception types
fn processFile(path: String): Result<FileData, IOException> => {
  val content = try readFile(path)  // Result<String, IOException>
  val parsed = try parseFileContent(content)  // May throw IOException
  Ok(FileData(parsed))
}

// Using broader exception types for multiple failure modes
fn complexOperation(): Result<Data, Exception> => {
  val config = try loadConfig()  // May throw IOException, IllegalArgumentException, etc.
  val connection = try establishConnection(config)  // May throw SQLException, NetworkException
  val data = try fetchData(connection)  // May throw various exceptions
  Ok(data)
}
```

## Try Expression Semantics

### Universal Exception Wrapping

Try expressions work with any expression type and automatically wrap exceptions in Result types:

```kotlin
// Basic value wrapping
fn createData(): Result<String, Throwable> => {
  try "hello world"  // Wraps literal in Ok("hello world")
}

// Function call wrapping
fn getTimestamp(): Result<Long, Throwable> => {
  try System.currentTimeMillis()  // Wraps result or catches exceptions
}

// Exception handling
fn riskyOperation(): Result<String, RuntimeException> => {
  try {
    val result = performRiskyTask()  // May throw RuntimeException
    result.toString()
  }
}
```

### Result Type Pass-Through

When expressions return Result types, try expressions unwrap success values and propagate errors:

```kotlin
fn chainOperations(): Result<ProcessedData, IOException> => {
  val content = try readFile("data.txt")      // Result<String, IOException> -> String
  val parsed = try parseContent(content)      // Result<ParsedData, IOException> -> ParsedData
  val processed = try processData(parsed)     // Result<ProcessedData, IOException> -> ProcessedData
  Ok(processed)
}
```

### Type Inference and Compatibility

```kotlin
// Return type inferred as Result<String, IOException>
fn processFiles() => {
  val file1 = try readFile("file1.txt")
  val file2 = try readFile("file2.txt")
  try combineFiles(file1, file2)
}

// Explicit compatible error types
fn multiStepProcess(): Result<Data, Exception> => {
  val config = try loadConfig()        // IOException -> Exception
  val conn = try openConnection(config) // SQLException -> Exception
  val data = try fetchData(conn)       // NetworkException -> Exception
  Ok(data)
}
```

## Exception Propagation Strategy

### Dual Error Handling Model

TaylorLang supports both Result-based and traditional exception-based error handling:

#### Result-Based Error Handling (Recommended)

Use Results for expected, recoverable errors that callers should handle explicitly:

```kotlin
fn validateUser(data: UserData): Result<User, IllegalArgumentException> => {
  if (data.email.isEmpty()) {
    Error(IllegalArgumentException("Email is required"))
  } else if (!isValidEmail(data.email)) {
    Error(IllegalArgumentException("Invalid email format: ${data.email}"))
  } else {
    Ok(User(data.email, data.name))
  }
}

fn processUserRegistration(data: UserData): Result<RegisteredUser, Exception> => {
  val user = try validateUser(data)           // IllegalArgumentException -> Exception
  val id = try generateUserId()               // May throw various exceptions
  val saved = try database.saveUser(user, id) // SQLException -> Exception
  Ok(RegisteredUser(saved))
}
```

#### Exception-Based Error Handling

Use exceptions for programming errors and unrecoverable conditions:

```kotlin
fn allocateBuffer(size: Int): ByteArray => {
  if (size < 0) {
    throw IllegalArgumentException("Size cannot be negative: ${size}")
  }
  if (size > MAX_MEMORY) {
    throw OutOfMemoryError("Requested size ${size} exceeds limit ${MAX_MEMORY}")
  }
  ByteArray(size)
}

// Exceptions propagate normally through Result-returning functions
fn processLargeData(data: RawData): Result<ProcessedData, IOException> => {
  val buffer = allocateBuffer(data.size)  // May throw - propagates as exception
  val processed = try processData(data, buffer)  // IOException handled as Result
  Ok(processed)
}
```

### Exception Propagation Rules

1. **Unhandled Exceptions**: Always propagate through the call stack, regardless of function return type
2. **Try Expression Boundary**: Exceptions within try expressions are caught and wrapped in Result.Error
3. **Result Error Propagation**: Result errors propagate only through try expressions
4. **Type Compatibility**: Exception types must be compatible with Result error type bounds

## Advanced Error Handling Patterns

### Pattern 1: Pure Result Chain

```kotlin
fn fileProcessingWorkflow(path: String): Result<ProcessedData, IOException> => {
  val content = try readFile(path)
  val validated = try validateFileContent(content)
  val parsed = try parseFileData(validated)
  val processed = try processFileData(parsed)
  Ok(processed)
}
```

### Pattern 2: Mixed Exception and Result Handling

```kotlin
fn robustDataOperation(): Result<Data, Exception> => {
  try {
    val config = loadSystemConfig()  // May throw IOException - propagates as exception
    val connection = try establishConnection(config)  // Result<Connection, SQLException>
    val data = try fetchData(connection)  // Result<Data, NetworkException>
    Ok(data)
  } catch {
    case IOException(e) => Error(e)  // Convert exception to Result.Error
    case e: RuntimeException => throw e  // Re-throw unexpected runtime errors
  }
}
```

### Pattern 3: Error Recovery with Fallbacks

```kotlin
fn resilientDataFetch(urls: List<String>): Result<Data, Exception> => {
  for (url in urls) {
    match try fetchFromUrl(url) {
      case Ok(data) => return Ok(data)
      case Error(err) => continue  // Try next URL
    }
  }
  Error(RuntimeException("All data sources failed"))
}
```

### Pattern 4: Exception Type Narrowing

```kotlin
// Narrow exception types for specific error handling
fn parseConfigFile(path: String): Result<Config, Exception> => {
  try {
    val content = try readFile(path)  // IOException
    val json = try parseJson(content)  // JsonParseException
    val config = try validateConfig(json)  // IllegalArgumentException
    Ok(config)
  } catch {
    case IOException(e) => Error(IOException("Failed to read config file: ${e.message}"))
    case JsonParseException(e) => Error(IllegalArgumentException("Invalid config format: ${e.message}"))
    case IllegalArgumentException(e) => Error(e)  // Pass through validation errors
  }
}
```

## Integration with JVM Libraries

### Seamless Java Interoperability

```kotlin
// Direct use of Java exceptions
fn connectToDatabase(url: String): Result<Connection, SQLException> => {
  try DriverManager.getConnection(url)
}

fn httpRequest(url: String): Result<HttpResponse, IOException> => {
  try {
    val connection = URL(url).openConnection()
    val response = connection.getInputStream().readAllBytes()
    Ok(HttpResponse(200, String(response)))
  }
}

// Working with existing Java libraries
fn processJsonData(json: String): Result<DataObject, Exception> => {
  try {
    val mapper = ObjectMapper()
    val data = mapper.readValue(json, DataObject::class.java)
    Ok(data)
  }
}
```

### Exception Type Hierarchies

```kotlin
// Leverage Java's exception hierarchy
fn networkOperation(): Result<Data, IOException> => {
  try {
    val socket = Socket("localhost", 8080)  // May throw IOException subtypes
    val data = readFromSocket(socket)       // SocketTimeoutException, etc.
    Ok(data)
  }
}

// Handle specific exception subtypes
fn fileOperations(): Result<String, IOException> => {
  try {
    val content = Files.readString(Path.of("data.txt"))
    Ok(content)
  } catch {
    case FileNotFoundException(e) => Error(FileNotFoundException("Required file not found: ${e.message}"))
    case AccessDeniedException(e) => Error(IOException("Permission denied: ${e.message}"))
    case e: IOException => Error(e)  // Handle other IO exceptions
  }
}
```

## Implementation Guidelines

### Type System Requirements

1. **Exception Bounds**: Result error types must extend Throwable
2. **Type Inference**: Infer Result types when try expressions are used
3. **Compatibility Checking**: Verify exception type compatibility in try expressions
4. **Automatic Wrapping**: Wrap non-Result expressions in appropriate Result types

### Runtime Behavior

1. **Exception Interception**: Try expressions catch and wrap exceptions
2. **Performance Optimization**: Minimize overhead for exception handling
3. **Stack Trace Preservation**: Maintain original exception stack traces
4. **JVM Compatibility**: Preserve standard JVM exception semantics

### Code Generation

1. **Efficient Try-Catch**: Generate optimized try-catch blocks for try expressions
2. **Result Type Operations**: Optimize Result type handling and unwrapping
3. **Exception Flow**: Ensure correct exception propagation through the call stack
4. **Type Erasure Handling**: Handle generic Result types correctly in generated bytecode

## Best Practices

### 1. Exception Type Selection

- Use specific exception types when callers need to handle different errors differently
- Use broader exception types (Exception, RuntimeException) for simplified error handling
- Leverage Java's existing exception hierarchy instead of creating custom types

### 2. Result vs Exception Guidelines

- **Use Results for**: Business logic errors, validation failures, expected error conditions
- **Use Exceptions for**: Programming errors, system failures, unrecoverable conditions
- **Consider the caller**: Use Results when callers should explicitly handle errors

### 3. Error Handling Strategy

- Choose Result-based or exception-based handling based on error recoverability
- Use try expressions consistently within Result-returning functions
- Handle both Result errors and exceptions appropriately in mixed scenarios

### 4. JVM Integration

- Prefer standard JVM exception types over custom error types
- Maintain compatibility with existing Java libraries and frameworks
- Preserve exception semantics and stack traces for debugging

This simplified approach eliminates the complexity of custom error wrapper types while providing type-safe error handling through direct integration with JVM's robust exception system.