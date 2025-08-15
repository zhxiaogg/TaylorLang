# Architecture Guidelines

## Core Principles

### System Boundaries
- **Module Isolation**: Each module must have clear, well-defined responsibilities
- **Interface Design**: Define explicit contracts between modules through interfaces
- **Dependency Direction**: Dependencies flow inward toward core business logic
- **Boundary Violations**: Flag any cross-boundary coupling or circular dependencies

### Design Patterns
- **Strategy Pattern**: Use for algorithm variations and behavioral switching
- **Factory Pattern**: Use for object creation with complex initialization
- **Observer Pattern**: Use for event-driven communication between modules
- **Command Pattern**: Use for encapsulating operations and supporting undo/redo
- **Adapter Pattern**: Use for integrating external systems without coupling

### Complexity Management
- **Single Responsibility**: Each class/module serves one clear purpose
- **Open/Closed Principle**: Extend behavior through composition, not modification
- **Dependency Inversion**: Depend on abstractions, not concrete implementations
- **Interface Segregation**: Create focused, cohesive interfaces
- **DRY Principle**: Eliminate duplication through proper abstraction

## Architecture Standards

### File Organization
- Maximum 500 lines per file
- Group related functionality in focused modules
- Separate concerns: data, logic, presentation, infrastructure

### Module Structure
```
src/
├── core/           # Business logic and domain models
├── adapters/       # External system interfaces
├── services/       # Application services
├── utils/          # Shared utilities
└── types/          # Type definitions
```

### Dependency Rules
1. Core modules depend on no external modules
2. Service modules depend only on core and other services
3. Adapter modules depend on services and core
4. Utilities are dependency-free and stateless

### Error Handling Architecture
- Use Result/Either types for recoverable errors
- Throw exceptions only for programming errors
- Centralize error logging and monitoring
- Define error boundaries at module interfaces

## Bytecode Generation Architecture

### Component Separation Pattern
**Principle**: Separate bytecode generation concerns into focused, specialized components to prevent VerifyError and improve maintainability.

```kotlin
// PREFERRED: Separated concerns architecture
class PatternBytecodeCompiler(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val expressionGenerator: ExpressionBytecodeGenerator
) {
    // Delegate components for separated concerns
    private val typeConverter = TypeConverter(methodVisitor)
    private val bytecodeUtils = BytecodeGeneratorUtils(methodVisitor, typeConverter)
    private val patternMatcher = PatternMatcher(
        methodVisitor, 
        variableSlotManager, 
        typeConverter, 
        this::generateExpression
    )
}

// AVOID: Monolithic bytecode generator
class MonolithicBytecodeGenerator {
    // Everything mixed together - hard to maintain and debug
}
```

**Benefits**:
- Isolated testing of individual components
- Clear responsibility boundaries
- Easier debugging of bytecode issues
- Reduced complexity per component

### Control Flow Generation Pattern
**Principle**: Generate all conditional tests before visiting failure labels to ensure correct execution order.

```kotlin
// PREFERRED: Delayed failure label placement
fun generatePatternTests(cases: List<MatchCase>) {
    // Phase 1: Generate all pattern tests with forward jumps
    val caseLabels = cases.map { Label() }
    
    for (i in cases.indices) {
        val nextLabel = if (i < cases.size - 1) Label() else Label()
        generatePatternTest(cases[i].pattern, caseLabels[i], nextLabel)
        
        if (i < cases.size - 1) {
            methodVisitor.visitLabel(nextLabel)
        } else {
            // Visit final failure label after ALL tests
            methodVisitor.visitLabel(nextLabel)
            generateDefaultValue()
        }
    }
}
```

**Anti-Pattern**: Premature failure label generation that causes control flow corruption.

### Type System Integration Pattern
**Principle**: Maintain parallel coverage between type inference and code generation phases.

```kotlin
// REQUIRED: Complete expression coverage in both phases
class CompilerPhaseValidator {
    fun validateCoverage() {
        val expressionTypes = getAllExpressionTypes()
        val inferenceHandlers = getTypeInferenceHandlers()
        val codegenHandlers = getCodegenHandlers()
        
        val missingInference = expressionTypes - inferenceHandlers.keys
        val missingCodegen = expressionTypes - codegenHandlers.keys
        
        require(missingInference.isEmpty()) { "Missing type inference for: $missingInference" }
        require(missingCodegen.isEmpty()) { "Missing code generation for: $missingCodegen" }
    }
}
```

## Design Decision Framework

### Trade-off Analysis
When making architectural decisions, evaluate:
- **Performance vs Maintainability**: Favor maintainability unless performance is critical
- **Flexibility vs Simplicity**: Choose simplicity unless flexibility is required
- **Abstraction vs Concreteness**: Abstract only when patterns emerge

### Refactoring Guidelines
1. **Identify Smells**: God objects, feature envy, shotgun surgery
2. **Extract Methods**: Break large functions into focused operations
3. **Extract Classes**: Separate concerns into cohesive units
4. **Introduce Interfaces**: Define contracts for external dependencies
5. **Eliminate Duplication**: Create shared abstractions for common patterns

## Code Quality Metrics

### Complexity Indicators
- Cyclomatic complexity: Maximum 10 per method
- Class coupling: Maximum 7 dependencies per class
- Inheritance depth: Maximum 4 levels
- Method parameters: Maximum 5 parameters

### Architecture Validation
- No circular dependencies between modules
- Clear separation between layers
- Consistent naming conventions across modules
- Proper error handling at all boundaries

### Bytecode Generation Quality
- Stack frame consistency at all merge points
- Proper handling of JVM double/long slot requirements
- Complete expression type coverage in all compiler phases
- Clear separation between pattern matching, type conversion, and control flow

## Review Checklist

### High-Level Assessment
- [ ] Clear module boundaries and responsibilities
- [ ] Appropriate use of design patterns
- [ ] Proper dependency direction
- [ ] Separation of concerns maintained

### Bytecode Generation Review
- [ ] Component responsibilities clearly separated
- [ ] Stack management handles double/long values correctly
- [ ] Control flow labels placed after pattern tests complete
- [ ] Type inference and code generation have parallel coverage
- [ ] Pattern matching handles all pattern types (including wildcards)

### Detailed Review
- [ ] No god objects or large classes
- [ ] Interfaces define clear contracts
- [ ] Error handling is consistent and appropriate
- [ ] Code is organized logically within modules
- [ ] Abstractions are justified and useful

## Escalation Criteria

Escalate architectural decisions when:
- Cross-cutting concerns affect multiple modules
- Performance requirements conflict with maintainability
- New patterns emerge that could benefit the entire codebase
- External system integration requires new architectural approaches
- Bytecode generation patterns could benefit other language features