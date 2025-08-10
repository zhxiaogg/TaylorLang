type Result<T, E> = Ok(T) | Error(E)

fn divide(x: Int, y: Int): Result<Int, String> => {
    if (y == 0) {
        Error("Division by zero")
    } else {
        Ok(x / y)
    }
}

fn handleResult(result: Result<Int, String>): String => match result {
    case Ok(value) => "Result: " + value
    case Error(msg) => "Error: " + msg
}