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
- **Coordinator Pattern**: Use for orchestrating multiple specialized components

### Complexity Management
- **Single Responsibility**: Each class/module serves one clear purpose
- **Open/Closed Principle**: Extend behavior through composition, not modification
- **Dependency Inversion**: Depend on abstractions, not concrete implementations
- **Interface Segregation**: Create focused, cohesive interfaces
- **DRY Principle**: Eliminate duplication through proper abstraction

## Architecture Standards

### File Organization
- **MANDATORY**: Maximum 500 lines per file (zero tolerance policy)
- Group related functionality in focused modules
- Separate concerns: data, logic, presentation, infrastructure
- **File Size Validation**: Use automated checks to prevent violations

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

### Coordinator Pattern for Complex Generators
**Principle**: Transform monolithic generators into coordinating classes that delegate to specialized components.

```kotlin
// PREFERRED: Coordinator with specialized delegates
class ExpressionBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager
) {
    // Specialized components (each under 500 lines)
    private val typeHelper = TypeInferenceBytecodeHelper(methodVisitor, variableSlotManager)
    private val literalGenerator = LiteralBytecodeGenerator(methodVisitor)
    private val arithmeticGenerator = ArithmeticBytecodeGenerator(...)
    private val comparisonGenerator = ComparisonBytecodeGenerator(...)
    private val functionCallGenerator = FunctionCallBytecodeGenerator(...)
    private val constructorCallGenerator = ConstructorCallBytecodeGenerator(...)
    private val variableAccessGenerator = VariableAccessBytecodeGenerator(...)
    
    fun generateExpression(expr: TypedExpression) {
        when (val expression = expr.expression) {
            is Literal -> literalGenerator.generateLiteral(expression)
            is BinaryOp -> {
                when (expression.operator) {
                    BinaryOperator.LESS_THAN, BinaryOperator.EQUAL -> 
                        comparisonGenerator.generateComparison(expression, determineOperandType(expression))
                    else -> arithmeticGenerator.generateBinaryOperation(expression, expr.type)
                }
            }
            is FunctionCall -> functionCallGenerator.generateFunctionCall(expression, expr.type)
            is ConstructorCall -> constructorCallGenerator.generateConstructorCall(expression, expr.type)
            is Identifier -> variableAccessGenerator.generateVariableLoad(expression)
            // ... other delegations
        }
    }
}

// AVOID: Monolithic generator over 500 lines
class MonolithicExpressionGenerator {
    // 1,106 lines of mixed concerns - architectural violation
}
```

**Benefits**:
- **Compliance**: Each component under mandatory 500-line limit
- **Maintainability**: Clear separation enables focused debugging
- **Testability**: Isolated testing of individual generation features
- **Extensibility**: Easy to add new expression types without touching existing generators

### Component Specialization Pattern
**Principle**: Each generator component focuses on one specific type of bytecode generation with clear boundaries.

```kotlin
// PREFERRED: Specialized component with clear responsibility
class ArithmeticBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val typeHelper: TypeInferenceBytecodeHelper,
    private val generateExpression: (TypedExpression) -> Unit
) {
    fun generateBinaryOperation(binaryOp: BinaryOp, resultType: Type) {
        // Focus only on arithmetic operations
        // Delegate type inference to typeHelper
        // Delegate sub-expressions via generateExpression callback
    }
    
    fun generateUnaryOperation(unaryOp: UnaryOp, resultType: Type) {
        // Focus only on unary arithmetic operations
    }
    
    private fun generateStringConcatenation(binaryOp: BinaryOp) {
        // Specialized string concatenation with StringBuilder
    }
}

// AVOID: Mixed responsibilities in single component
class MixedPurposeGenerator {
    // Arithmetic + comparisons + function calls + type inference = complexity violation
}
```

### Type Helper Pattern
**Principle**: Centralize all type-related operations in a dedicated helper to eliminate duplication and ensure consistency.

```kotlin
// PREFERRED: Centralized type operations
class TypeInferenceBytecodeHelper(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager
) {
    fun inferExpressionType(expr: Expression): Type { /* ... */ }
    fun getJvmType(type: Type): String { /* ... */ }
    fun isIntegerType(type: Type): Boolean { /* ... */ }
    fun boxPrimitiveToObject(type: Type) { /* ... */ }
    // All type operations in one place
}

// Multiple generators use the same type helper
val typeHelper = TypeInferenceBytecodeHelper(methodVisitor, variableSlotManager)
val literalGen = LiteralBytecodeGenerator(methodVisitor)
val arithmeticGen = ArithmeticBytecodeGenerator(methodVisitor, typeHelper, generateExpr)

// AVOID: Scattered type operations across multiple classes
class Generator1 {
    fun isIntegerType(type: Type): Boolean { /* duplicated logic */ }
}
class Generator2 {
    fun isIntegerType(type: Type): Boolean { /* duplicated logic */ }
}
```

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

### Generic Type Handling Pattern
**Principle**: Treat Object-returning generic methods as boundary operations requiring explicit stack state management.

```kotlin
// PREFERRED: Generic type boundary handling
class ConstructorPatternGenerator {
    fun generateFieldAccess(constructor: Constructor, fieldIndex: Int) {
        val returnType = constructor.getFieldReturnType(fieldIndex)
        
        // Generate method call
        generateMethodCall(constructor.getFieldMethod(fieldIndex))
        
        // Handle generic boundary conversion
        if (returnType.isGenericObjectReturn()) {
            handleGenericBoundaryConversion(returnType, expectedType)
        } else {
            // Direct type-safe conversion
            typeConverter.convertType(returnType, expectedType)
        }
    }
    
    private fun handleGenericBoundaryConversion(sourceType: Type, targetType: Type) {
        // Verify stack state before conversion
        stackStateValidator.verifyExpectedState(sourceType)
        
        // Perform conversion with proper stack management
        typeConverter.convertWithStackVerification(sourceType, targetType)
    }
}

// AVOID: Direct generic conversion without boundary handling
fun generateFieldAccessBadPattern(constructor: Constructor, fieldIndex: Int) {
    generateMethodCall(constructor.getFieldMethod(fieldIndex))
    typeConverter.convertType() // May fail on Object->primitive with VerifyError
}
```

**Key Insights**:
- Generic methods returning `Object` create type boundaries requiring special handling
- Stack state verification must occur before Object-to-primitive conversions
- Constructor patterns with generics need explicit boundary management
- Type-safe methods (returning `T` or concrete types) can use direct conversion

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
1. **Identify Smells**: God objects, feature envy, shotgun surgery, file size violations
2. **Extract Methods**: Break large functions into focused operations
3. **Extract Classes**: Separate concerns into cohesive units (coordinator pattern)
4. **Introduce Interfaces**: Define contracts for external dependencies
5. **Eliminate Duplication**: Create shared abstractions for common patterns (type helper pattern)
6. **Validate Compliance**: Ensure all files meet 500-line limit

## Code Quality Metrics

### Complexity Indicators
- Cyclomatic complexity: Maximum 10 per method
- Class coupling: Maximum 7 dependencies per class
- Inheritance depth: Maximum 4 levels
- Method parameters: Maximum 5 parameters
- **File size: MANDATORY maximum 500 lines**

### Architecture Validation
- No circular dependencies between modules
- Clear separation between layers
- Consistent naming conventions across modules
- Proper error handling at all boundaries
- **Automated file size validation in CI/CD**

### Bytecode Generation Quality
- Stack frame consistency at all merge points
- Proper handling of JVM double/long slot requirements
- Complete expression type coverage in all compiler phases
- Clear separation between pattern matching, type conversion, and control flow
- Generic type boundary handling with explicit stack state verification
- **Component specialization with coordinator orchestration**

## Review Checklist

### High-Level Assessment
- [ ] Clear module boundaries and responsibilities
- [ ] Appropriate use of design patterns (coordinator, component specialization)
- [ ] Proper dependency direction
- [ ] Separation of concerns maintained
- [ ] **All files under 500 lines (MANDATORY)**

### Bytecode Generation Review
- [ ] Component responsibilities clearly separated
- [ ] Coordinator pattern used for complex generators
- [ ] Type helper centralizes all type operations
- [ ] Stack management handles double/long values correctly
- [ ] Control flow labels placed after pattern tests complete
- [ ] Type inference and code generation have parallel coverage
- [ ] Pattern matching handles all pattern types (including wildcards)
- [ ] Generic type boundaries handled with proper stack state verification
- [ ] Object-to-primitive conversions include stack state validation

### Detailed Review
- [ ] No god objects or large classes
- [ ] Interfaces define clear contracts
- [ ] Error handling is consistent and appropriate
- [ ] Code is organized logically within modules
- [ ] Abstractions are justified and useful
- [ ] **File size compliance validated**

### ExpressionBytecodeGenerator Decomposition Validation
- [ ] Main coordinator under 500 lines
- [ ] All extracted components under 500 lines
- [ ] TypeInferenceBytecodeHelper centralizes type operations
- [ ] LiteralBytecodeGenerator handles only literal generation
- [ ] ArithmeticBytecodeGenerator focuses on arithmetic/binary operations
- [ ] ComparisonBytecodeGenerator specialized for comparisons
- [ ] FunctionCallBytecodeGenerator handles function calls with boxing
- [ ] ConstructorCallBytecodeGenerator manages union type constructors
- [ ] VariableAccessBytecodeGenerator manages variable operations
- [ ] Clear delegation patterns between coordinator and components

## Escalation Criteria

Escalate architectural decisions when:
- Cross-cutting concerns affect multiple modules
- Performance requirements conflict with maintainability
- New patterns emerge that could benefit the entire codebase
- External system integration requires new architectural approaches
- Bytecode generation patterns could benefit other language features
- **File size violations cannot be resolved through standard refactoring**

## Success Metrics

### Architectural Compliance
- Zero files over 500 lines in codebase
- Component specialization ratio > 80% for complex generators
- Type operation centralization ratio > 90%
- Clear separation of concerns in all bytecode generators

### Maintainability Improvements
- Reduced debugging time for bytecode issues
- Faster code review cycles (< 24 hours for components under 500 lines)
- Increased developer productivity on expression features
- Reduced merge conflicts through clear component boundaries