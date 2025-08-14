# TaylorLang Examples

This directory contains example TaylorLang programs demonstrating various language features.

## Running Examples

To run any example:

```bash
./gradlew run --args="examples/01_hello_world.taylor"
java Program
```

## Examples Overview

### 01_hello_world.taylor
The simplest TaylorLang program - prints a greeting message.

### 02_arithmetic.taylor
Demonstrates arithmetic operations: addition, subtraction, multiplication, division, and modulo.

### 03_boolean_logic.taylor
Shows boolean operations (AND, OR, NOT) and comparison operators.

### 04_pattern_matching.taylor
Examples of pattern matching with integers and booleans using match expressions.

### 05_if_expressions.taylor
Demonstrates if-else expressions (which return values in TaylorLang).

### 06_strings.taylor
String operations and concatenation examples.

### 07_while_loops.taylor
While loop examples (note: uses mutable variables for demonstration).

### 08_list_operations.taylor
List operations using constructor patterns `Cons(head, tail)` and `Nil()`. Shows the simplified design approach where lists are regular union types without special syntax.

### 09_functions.taylor
Function declarations using the `fn` keyword with both expression (`=>`) and block (`{}`) bodies. Pattern matching within functions.

### 10_constructor_patterns.taylor
Constructor pattern matching with union types like `Ok(value)`, `Some(data)`, and nested patterns. Based on the constructor deconstruction design.

### 11_try_expressions.taylor
Try expressions with error handling using `try` and `catch` blocks. Shows Result type integration and error propagation patterns.

## Language Features

TaylorLang's grammar supports:
- Function declarations with `fn` keyword
- Advanced pattern matching including:
  - Constructor patterns: `Ok(value)`, `Some(data)`, `Cons(head, tail)`
  - Guard patterns: `case x if x > 10`
  - Literal and wildcard patterns
  - Lists as regular union types: `Cons(T, List<T>) | Nil()`
- Try expressions with `try` and `catch` blocks
- Union type declarations: `type Result<T,E> = Ok(T) | Error(E)`
- Lambda expressions: `x => x * 2`
- If-else expressions, while/for loops
- Tuple types and literals
- Basic arithmetic, boolean, and string operations
- Type inference with generic type support

## Implementation Status

**Fully Working**: Top-level expressions, basic pattern matching, arithmetic, strings, if-expressions
**Partially Working**: List operations (built-in functions work, constructor patterns in development)
**In Development**: Function declarations, constructor patterns, try expressions, union type declarations

**Design Philosophy**: Lists are treated as regular union types without special syntax, following the simplified design approach for consistency and maintainability.

The examples show both working syntax and planned features based on the comprehensive language design.