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
- [Algebraic Data Types](./algebraic-data-types.md) - Sum types, product types, and ADTs
- [Pattern Matching](./pattern-matching.md) - Destructuring and exhaustive matching
- [Collections](./collections.md) - Lists, maps, sets, and tuples

### Advanced Topics
- [String Handling](./string-handling.md) - String literals, interpolation, and operations
- [Annotations](./annotations.md) - Metadata system for types, functions, and fields
- [Modules and Imports](./modules-and-imports.md) - Package system with visibility control
- [Effects and I/O](./effects-and-io.md) - Effect system for managing side effects

### Examples and Guides
- [Complete Examples](./examples.md) - Real-world usage examples and patterns

## Getting Started

1. Start with [Language Overview](./language-overview.md) to understand the core concepts
2. Review [Basic Syntax](./basic-syntax.md) for fundamental constructs
3. Explore [Variables and Types](./variables-and-types.md) and [Functions](./functions.md) for core programming constructs
4. Learn [Algebraic Data Types](./algebraic-data-types.md) and [Pattern Matching](./pattern-matching.md) for powerful type modeling
5. Check [Complete Examples](./examples.md) for practical usage patterns

## Key Language Features

- **Unified Type System**: Use `type` for both sum types (enums) and product types (records)
- **Pattern Matching**: Exhaustive `match` expressions for safe control flow
- **Package-based Modules**: Java-like package system with `pub` visibility control
- **Annotation Support**: Rich metadata system with `@Annotation` syntax
- **Immutability by Default**: `val` for immutable, `var` for mutable variables
- **Functional Programming**: Higher-order functions, lambdas, and functional collections