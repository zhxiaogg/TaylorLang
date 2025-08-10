# TaylorLang Documentation

A minimal, pure functional, and practical language for the JVM with immutability by default, Hindley-Milner type inference, and seamless Java interoperability.

## Documentation Structure

### Language Fundamentals
- [Basic Syntax](./basic-syntax.md) - Comments, identifiers, and basic constructs
- [Variables and Types](./variables-and-types.md) - Variable declarations, type system, and primitives
- [Functions](./functions.md) - Function definitions, lambdas, and higher-order functions

### Core Features
- [Control Flow](./control-flow.md) - Conditionals, match expressions, and iteration
- [Pattern Matching](./pattern-matching.md) - Destructuring and exhaustive matching
- [Collections](./collections.md) - Lists, maps, sets, and tuples

### Advanced Topics
- [String Handling](./string-handling.md) - String literals, interpolation, and operations
- [Annotations](./annotations.md) - Metadata system for types, functions, and fields
- [Interfaces](./interfaces.md) - Contract definition and implementation with impl blocks
- [Types and Nullability](./types-and-nullability.md) - Comprehensive type system guide
- [Modules and Imports](./modules-and-imports.md) - Package system with visibility control
- [Effects and I/O](./effects-and-io.md) - Async/await system for side effects

### Examples and Guides
- [Complete Examples](./examples.md) - Real-world usage examples and patterns

### Development and Planning
- [Feature Gaps](./feature-gaps.md) - Missing features and implementation roadmap
- [Implementation Plan](./implementation-plan.md) - Detailed plan for building TaylorLang iteratively

## Getting Started

1. Start with [Basic Syntax](./basic-syntax.md) for fundamental language constructs
2. Learn [Variables and Types](./variables-and-types.md) and [Functions](./functions.md) for core programming concepts
3. Explore [Types and Nullability](./types-and-nullability.md) and [Pattern Matching](./pattern-matching.md) for type modeling
4. Review [Complete Examples](./examples.md) for practical usage patterns

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