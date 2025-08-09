# Pattern Matching

## Basic Pattern Matching

```kotlin
fn handleStatus(status: Status) => match status {
  case Pending => "Waiting to start"
  case InProgress => "Currently running"
  case Completed => "All done!"
  case Failed => "Something went wrong"
}

fn processResult(result) => match result {
  case Ok(value) => println("Success: ${value}")
  case Error(msg) => println("Failed: ${msg}")
}
```

## Destructuring Patterns

```rust
fn handleResponse(response: HttpResponse) => match response {
  case Success(200, body) => println("OK: ${body}")
  case Success(status, body) => println("Success ${status}: ${body}")
  case Redirect(url) => println("Redirecting to: ${url}")
  case ClientError(404, msg) => println("Not found: ${msg}")
  case ClientError(code, msg) => println("Client error ${code}: ${msg}")
  case ServerError(code, details) => println("Server error ${code}: ${details}")
}
```

## Guards and Conditions

```kotlin
fn categorizeNumber(x: Int) => match x {
  case n if n < 0 => "Negative"
  case 0 => "Zero"
  case n if n > 0 && n <= 10 => "Small positive"
  case n => "Large positive: ${n}"
}
```

## List Pattern Matching

```haskell
fn processListPattern<T>(list: List<T>) => match list {
  case [] => "Empty list"
  case [x] => "Single item: ${x}"
  case [x, y] => "Two items: ${x}, ${y}"
  case [first, ...rest] => "First: ${first}, rest has ${rest.length} items"
}
```

## Advanced Pattern Matching

### Partial Field Matching
```kotlin
// Match only subset of fields using ...
fn handleResponse(response: HttpResponse) => match response {
  case Success(body, ...) => println("Success with body: ${body}")
  case ClientError(code, ...) => println("Client error: ${code}")
  case _ => println("Other response")
}
```

### Type Pattern Matching
```kotlin
type HttpResponseBody {
  SimpleTextBody(textBody: String),
  JsonBody(data: Map<String, Any>),
  BinaryBody(bytes: Array<Byte>)
}

fn processResponse(response: HttpResponse) => match response {
  // Type matching
  case Success(body: SimpleTextBody, ...) => println("Text response")
  
  // Type matching with alias
  case Success(body: SimpleTextBody as textBody, ...) => 
    println("Text: ${textBody.textBody}")
  
  // Type deconstruction
  case Success(body: SimpleTextBody(textBody), ...) => 
    println("Direct text access: ${textBody}")
  
  // Combined type deconstruction
  case Success(status, SimpleTextBody(textBody)) => 
    println("Status ${status}: ${textBody}")
}
```

## Exhaustiveness

```kotlin
// Compiler ensures all cases are covered
fn statusMessage(status: Status) => match status {
  case Pending => "Waiting"
  case InProgress => "Running"
  case Completed => "Done"
  case Failed => "Error"
  // All cases covered - compiles successfully
}
```

## Pattern Matching Best Practices

- Always handle all cases for exhaustiveness
- Use guards for complex conditions
- Prefer specific patterns before general ones
- Use type patterns for safe casting and deconstruction
- Leverage partial matching with `...` for cleaner code
- Keep patterns readable and focused