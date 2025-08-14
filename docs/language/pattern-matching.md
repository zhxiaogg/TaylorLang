# Pattern Matching

## Basic Pattern Matching

```kotlin
// Expression body with pattern matching
fn handleStatus(status: Status): String => match status {
  case Pending => "Waiting to start"
  case InProgress => "Currently running"
  case Completed => "All done!"
  case Failed => "Something went wrong"
}

// Block body with pattern matching
fn processResult(result): Unit {
  match result {
    case Ok(value) => println("Success: ${value}")
    case Error(msg) => println("Failed: ${msg}")
  }
}
```

## Destructuring Patterns

```kotlin
fn handleResponse(response: HttpResponse): Unit {
  match response {
    case Success(200, body) => println("OK: ${body}")
    case Success(status, body) => println("Success ${status}: ${body}")
    case Redirect(url) => println("Redirecting to: ${url}")
    case ClientError(404, msg) => println("Not found: ${msg}")
    case ClientError(code, msg) => println("Client error ${code}: ${msg}")
    case ServerError(code, details) => println("Server error ${code}: ${details}")
  }
}
```

## Guards and Conditions

```kotlin
// Expression body with guards
fn categorizeNumber(x: Int): String => match x {
  case n if n < 0 => "Negative"
  case 0 => "Zero"
  case n if n > 0 && n <= 10 => "Small positive"
  case n => "Large positive: ${n}"
}
```

## List Pattern Matching

Lists in TaylorLang are represented as union types without special syntax:

```kotlin
// List type definition: List<T> = Cons(T, List<T>) | Nil()

// Expression body with list patterns
fn processListPattern<T>(list: List<T>): String => match list {
  case Nil() => "Empty list"
  case Cons(x, Nil()) => "Single item: ${x}"
  case Cons(x, Cons(y, Nil())) => "Two items: ${x}, ${y}"
  case Cons(first, tail) => "First: ${first}, rest has more items"
}

// More complex list patterns
fn processLongerList<T>(list: List<T>): String => match list {
  case Nil() => "Empty"
  case Cons(a, Cons(b, Cons(c, Nil()))) => "Three items: ${a}, ${b}, ${c}"
  case Cons(head, tail) => "Head: ${head}, has tail"
}
```

## Advanced Pattern Matching

### Partial Field Matching
```kotlin
// Match only subset of fields using ...
fn handleResponse(response: HttpResponse): Unit {
  match response {
    case Success(body, ...) => println("Success with body: ${body}")
    case ClientError(code, ...) => println("Client error: ${code}")
    case _ => println("Other response")
  }
}
```

### Type Pattern Matching
```kotlin
type HttpResponseBody {
  SimpleTextBody(textBody: String),
  JsonBody(data: Map<String, Any>),
  BinaryBody(bytes: Array<Byte>)
}

fn processResponse(response: HttpResponse): Unit {
  match response {
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
}
```

## Exhaustiveness

```kotlin
// Compiler ensures all cases are covered
fn statusMessage(status: Status): String => match status {
  case Pending => "Waiting"
  case InProgress => "Running"
  case Completed => "Done"
  case Failed => "Error"
  // All cases covered - compiles successfully
}
```

