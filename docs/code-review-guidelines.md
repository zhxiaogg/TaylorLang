# Code Review Guidelines

## File Size Standards
- **Source files**: 500 lines max
- **Test files**: 300 lines max
- **Interfaces**: 200 lines max
- **Data models**: 100 lines max

## Core Principles

### Single Responsibility Principle (SRP)
- Each class has one reason to change
- Separate data models from business logic
- No mixing of abstraction levels

### Package Organization
```
project/
├── core/           # Main business logic
├── errors/         # Error types and handling
├── models/         # Data models and definitions
├── operations/     # Business operations
├── services/       # Service implementations
└── utils/          # Utility functions
```

### Design Patterns

**Visitor Pattern** - For complex object structures needing different operations
```java
interface Visitor<T, R> {
    R visit(ConcreteTypeA element);
    R visit(ConcreteTypeB element);
}
```

**Strategy Pattern** - For multiple algorithms solving same problem
```java
interface ProcessingStrategy<T, R> {
    R process(T input, Context context);
}
```

**Factory Pattern** - For complex object creation with validation

## Review Checklist

### Architecture & Design
- [ ] Single responsibility per class
- [ ] File size under limits
- [ ] Appropriate design patterns
- [ ] Immutable data structures where possible
- [ ] Thread safety (no shared mutable state)
- [ ] Minimal coupling between components

### Code Quality
- [ ] Language-specific best practices
- [ ] Proper null/optional handling
- [ ] Consistent error handling
- [ ] Public APIs documented
- [ ] Descriptive naming
- [ ] No magic numbers
- [ ] No code duplication

### Testing
- [ ] 90% coverage for new code
- [ ] Tests organized by feature
- [ ] Descriptive test names
- [ ] Edge cases covered
- [ ] Tests run independently
- [ ] Performance benchmarks for critical paths

### Domain-Specific
*Adapt based on project type*
- [ ] Business logic correctness
- [ ] Data integrity validation
- [ ] Security considerations
- [ ] Scalability support

## Refactoring Priorities

### BLOCKING (Must fix before merge)
- **Large files** (>500 lines) → Split by responsibility
- **Multiple responsibilities** → Extract to separate classes

### HIGH Priority
- **Duplicate logic** → Implement appropriate patterns
- **Poor organization** → Reorganize packages

### MEDIUM Priority
- **Test organization** → Split by feature
- **Inconsistent errors** → Standardize handling

## Blocking Issues
These MUST be resolved before merge:
1. File size violations
2. SRP violations
3. Missing tests (below coverage threshold)
4. Missing documentation (public APIs)
5. Performance regression
6. Security vulnerabilities

## Anti-Patterns to Reject

### Code Smells
- **God classes** - Too many responsibilities
- **Long parameter lists** - >5 parameters
- **Deep nesting** - >4 levels
- **Magic numbers** - Unexplained constants
- **Dead code** - Unused methods/classes
- **Primitive obsession** - Overusing primitives vs domain objects

### Architecture Smells
- **Circular dependencies**
- **Feature envy** - Classes manipulating other classes' data
- **Data clumps** - Same parameters passed everywhere
- **Long switch statements** - Use polymorphism
- **Leaky abstractions** - Implementation details exposed

## Success Metrics
- **File organization**: Average <300 lines, zero over-limit files
- **Test quality**: >90% new code coverage, descriptive names
- **Design patterns**: Used appropriately, not over-engineered
- **Performance**: No regression, consistent error handling

## Exceptions
Allow larger files for:
- Generated code (excluded from limits)
- Data models with many fields (max 150 lines)
- Complex algorithms (max 600 lines with justification)

Require:
- Clear justification in PR
- Refactoring plan if applicable
- Architecture review approval

Following these guidelines improves maintainability, development velocity, and team productivity.