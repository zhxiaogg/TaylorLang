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

## Variable Binding

```rust
fn analyzeList<T>(list: List<T>) => match list {
  case [] => "Empty"
  case [x] as singleton => "Single item list: ${singleton}"
  case [first, ...rest] as fullList if rest.length > 5 => 
    "Long list starting with ${first}, total length: ${fullList.length}"
  case items => "Regular list with ${items.length} items"
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
- Use variable binding to capture and reuse matched values
- Keep patterns readable and focused