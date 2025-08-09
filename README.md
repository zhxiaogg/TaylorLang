# TaylorLang

A minimal, pure functional, and practical programming language for the JVM with immutability by default, Hindley-Milner type inference, and seamless Java interoperability.

## Features

- **Union Type System**: Algebraic data types with `type A = B | C` syntax
- **Null Safety**: Explicit nullable types with `?` syntax and safe navigation
- **Pattern Matching**: Exhaustive `match` expressions for safe control flow
- **Type Inference**: Local type inference with minimal annotations required
- **Immutability by Default**: `val` for immutable, `var` for mutable variables
- **Functional Programming**: Higher-order functions, lambdas, and pure functions
- **JVM Integration**: Seamless Java interoperability and existing ecosystem access

## Current Implementation Status

### ✅ Completed
- [x] Project structure with Kotlin + Gradle
- [x] ANTLR 4 grammar for core language syntax
- [x] AST definitions with immutable data structures
- [x] Parser integration with comprehensive AST builder
- [x] Basic type checker for primitive types and expressions
- [x] Comprehensive test suites for parser and type checker
- [x] Development guidelines and coding standards

### 🚧 In Progress
- [ ] Union type implementation and pattern matching type checking
- [ ] Advanced type inference engine
- [ ] Standard library with immutable collections

### 📋 Planned
- [ ] JVM bytecode generation with ASM
- [ ] Java interoperability layer
- [ ] Language Server Protocol (LSP) implementation
- [ ] VS Code extension
- [ ] Gradle plugin for build integration

## Quick Start

### Prerequisites

- Java 17 or later
- Gradle 8.0 or later

### Building

```bash
git clone <repository-url>
cd TaylorLang
./gradlew build
```

### Running the Compiler

```bash
./gradlew run --args="examples/hello.tl"
```

### Running Tests

```bash
./gradlew test
```

## Example Programs

### Simple Function

```kotlin
fn add(x: Int, y: Int): Int => x + y
```

### Union Types and Pattern Matching

```kotlin
type Result<T, E> = Ok(T) | Error(E)

fn divide(x: Int, y: Int): Result<Int, String> => {
    if (y == 0) {
        Error("Division by zero")
    } else {
        Ok(x / y)
    }
}

fn handleResult(result: Result<Int, String>): String => match result {
    case Ok(value) => "Result: ${value}"
    case Error(msg) => "Error: ${msg}"
}
```

### Higher-Order Functions

```kotlin
val numbers = [1, 2, 3, 4, 5]
val doubled = numbers.map(x => x * 2)
val evens = numbers.filter(x => x % 2 == 0)
```

## Architecture

### Technology Stack
- **Implementation**: Kotlin with functional programming patterns
- **Parser**: ANTLR 4 for grammar definition and parsing
- **AST**: Immutable data structures using Kotlin data classes
- **Type System**: Structural typing with union types and inference
- **Collections**: Persistent data structures (Arrow, PCollections)
- **Testing**: Kotest for BDD-style testing

### Project Structure

```
src/
├── main/
│   ├── antlr/                 # ANTLR grammar files
│   └── kotlin/
│       ├── ast/               # AST node definitions
│       ├── parser/            # Parser and AST builder
│       ├── typechecker/       # Type system implementation
│       ├── codegen/           # Bytecode generation (planned)
│       └── compiler/          # Main compiler entry point
└── test/
    └── kotlin/                # Test suites
```

## Development

### Code Style
- Use immutable data structures by default
- Prefer functional programming patterns
- Follow Kotlin idioms and conventions
- Use sealed classes for union types
- Leverage Arrow's functional data types for error handling

### Testing
- Comprehensive test coverage for all components
- Property-based testing for complex scenarios
- BDD-style tests with Kotest

### Contributing

1. Follow the development guidelines in `DEVELOPMENT.md`
2. Ensure all tests pass: `./gradlew test`
3. Follow functional programming principles
4. Add tests for new functionality

## Documentation

- [Language Documentation](docs/overview.md) - Complete language specification
- [Development Guidelines](DEVELOPMENT.md) - Coding standards and setup
- [Implementation Plan](docs/implementation-plan.md) - Detailed roadmap
- [Feature Gaps](docs/feature-gaps.md) - Missing features and priorities

## Implementation Approach

TaylorLang follows proven JVM language implementation patterns:

1. **ANTLR + ASM**: Same approach used by Scala and other modern JVM languages
2. **Sealed Classes**: Java 17+ sealed classes for efficient union type representation
3. **Type Inference**: Constraint-based inference with unification
4. **Immutable Collections**: Efficient persistent data structures
5. **Functional Design**: Pure functions and immutable data throughout

## License

[License details to be added]

## Roadmap

See [Implementation Plan](docs/implementation-plan.md) for detailed timeline and milestones.

- **Phase 1 (Months 1-2)**: Core language foundation ✅
- **Phase 2 (Months 3-4)**: Union types and pattern matching
- **Phase 3 (Months 5-6)**: Type inference and generics  
- **Phase 4 (Months 7-8)**: Standard library and Java interop
- **Phase 5 (Months 9-10)**: Developer tooling (LSP, VS Code)
- **Phase 6 (Months 11-12)**: Build integration and MVP release