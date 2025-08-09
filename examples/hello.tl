// Simple "Hello World" program
fn main() => {
    println("Hello, World!")
}

// Function with parameters and type inference
fn add(x: Int, y: Int): Int => x + y

// Union type example
type Result<T, E> = Ok(T) | Error(E)

fn divide(x: Int, y: Int): Result<Int, String> => {
    if (y == 0) {
        Error("Division by zero")
    } else {
        Ok(x / y)
    }
}

// Pattern matching example
fn handleResult(result: Result<Int, String>): String => match result {
    case Ok(value) => "Result: ${value}"
    case Error(msg) => "Error: ${msg}"
}