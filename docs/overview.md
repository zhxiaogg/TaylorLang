# TaylorLang Documentation

A minimal, pure functional, and practical language for the JVM that emphasizes:
- **Developer-friendly syntax** borrowing from modern languages
- **Functional programming** with immutability by default
- **Type safety** with Hindley-Milner type inference
- **Seamless Java interoperability**
- **Practical effects system** for real-world applications

## Documentation Structure

### Language Fundamentals
- [Language Overview](./language-overview.md) - Core concepts and philosophy
- [Basic Syntax](./basic-syntax.md) - Comments, identifiers, and basic constructs
- [Variables and Types](./variables-and-types.md) - Variable declarations, type system, and primitives
- [Functions](./functions.md) - Function definitions, lambdas, and higher-order functions

### Core Features
- [Control Flow](./control-flow.md) - Conditionals, match expressions, and iteration
- [Algebraic Data Types](./algebraic-data-types.md) - Union types, product types, and type aliases
- [Pattern Matching](./pattern-matching.md) - Destructuring and exhaustive matching
- [Collections](./collections.md) - Lists, maps, sets, and tuples

### Advanced Topics
- [String Handling](./string-handling.md) - String literals, interpolation, and operations
- [Annotations](./annotations.md) - Metadata system for types, functions, and fields
- [Interfaces](./interfaces.md) - Contract definition and implementation with impl blocks
- [Types and Nullability](./types-and-nullability.md) - Union types, nullable types, and Result error handling
- [Modules and Imports](./modules-and-imports.md) - Package system with visibility control
- [Effects and I/O](./effects-and-io.md) - Async/await system for side effects

### Examples and Guides
- [Complete Examples](./examples.md) - Real-world usage examples and patterns

## Getting Started

1. Start with [Language Overview](./language-overview.md) to understand the core concepts
2. Review [Basic Syntax](./basic-syntax.md) for fundamental constructs
3. Explore [Variables and Types](./variables-and-types.md) and [Functions](./functions.md) for core programming constructs
4. Learn [Algebraic Data Types](./algebraic-data-types.md) and [Pattern Matching](./pattern-matching.md) for powerful type modeling
5. Check [Complete Examples](./examples.md) for practical usage patterns

## Key Language Features

- **Union Type System**: Use `type A = B | C` for sum types and `type A(...)` for product types
- **Null Safety**: Explicit nullable types with `?` syntax and safe navigation
- **Pattern Matching**: Exhaustive `match` expressions for safe control flow
- **Interface System**: Rust-like interfaces with `impl` blocks and static/instance methods
- **Error Handling**: `Result<T, E>` types with `try` syntax for error propagation
- **Async/Await**: Unified model for I/O operations and asynchronous code
- **Package-based Modules**: Java-like package system with `pub` visibility control
- **Annotation Support**: Rich metadata system with `@Annotation` syntax
- **Immutability by Default**: `val` for immutable, `var` for mutable variables