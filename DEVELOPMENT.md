# TaylorLang Development Guidelines

## Technology Stack

- **Implementation Language**: Kotlin (instead of Java as originally planned)
- **Parser**: ANTLR 4 for grammar definition and parsing
- **Bytecode Generation**: ASM library for JVM bytecode emission
- **Union Types**: Java 17+ sealed classes for efficient representation
- **Build System**: Gradle with Kotlin DSL
- **Target JVM**: Java 17+ (for sealed classes and pattern matching)

## Code Guidelines

### General Principles

1. **Functional Programming First**: Use immutable data structures and pure functions by default
2. **Kotlin Idioms**: Leverage Kotlin's functional programming features (sealed classes, data classes, etc.)
3. **Type Safety**: Use Kotlin's null safety and type system to prevent runtime errors
4. **Immutability**: Prefer `val` over `var`, use immutable collections
5. **Composition over Inheritance**: Favor functional composition and sealed class hierarchies

### Code Style

```kotlin
// Use data classes for immutable data
data class ASTNode(
    val type: NodeType,
    val children: List<ASTNode> = emptyList(),
    val sourceLocation: SourceLocation? = null
)

// Use sealed classes for union types
sealed class Type {
    data class Primitive(val name: String) : Type()
    data class Union(val variants: List<Type>) : Type()
    data class Generic(val name: String, val parameters: List<Type>) : Type()
}

// Use functional programming patterns
fun transform(node: ASTNode, transformer: (ASTNode) -> ASTNode): ASTNode =
    transformer(node.copy(children = node.children.map { transform(it, transformer) }))

// Use when expressions for exhaustive matching
fun analyzeType(type: Type): String = when (type) {
    is Type.Primitive -> "primitive: ${type.name}"
    is Type.Union -> "union of ${type.variants.size} types"
    is Type.Generic -> "generic: ${type.name}<${type.parameters.joinToString()}>"
}
```

### Immutable Data Structures

**Use these libraries for persistent/immutable collections:**

```kotlin
// Add to build.gradle.kts
dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.1")
    implementation("org.pcollections:pcollections:4.0.1")
}

// Prefer immutable collections
import arrow.core.*
import org.pcollections.*

// Use Arrow's functional data types
val result: Either<CompilerError, AST> = parseSource(input)
val maybeType: Option<Type> = inferType(expression)

// Use PCollections for efficient persistent data structures
val symbols: PMap<String, Symbol> = HashTreePMap.empty<String, Symbol>()
    .plus("main", FunctionSymbol("main"))
    
val statements: PVector<Statement> = TreePVector.empty<Statement>()
    .plus(statement1)
    .plus(statement2)
```

### Error Handling

```kotlin
// Use sealed classes for typed errors
sealed class CompilerError {
    data class ParseError(val message: String, val location: SourceLocation) : CompilerError()
    data class TypeError(val expected: Type, val actual: Type, val location: SourceLocation) : CompilerError()
    data class SymbolError(val symbol: String, val location: SourceLocation) : CompilerError()
}

// Use Arrow's Either for error handling
fun parseExpression(input: String): Either<CompilerError, Expression> =
    try {
        val ast = antlrParser.parse(input)
        ast.toExpression().right()
    } catch (e: ParseException) {
        CompilerError.ParseError(e.message, e.location).left()
    }

// Chain operations with Either
fun compileProgram(source: String): Either<CompilerError, ByteArray> =
    parseProgram(source)
        .flatMap { ast -> typeCheck(ast) }
        .flatMap { typedAst -> generateBytecode(typedAst) }
```

### Naming Conventions

- **Classes**: PascalCase (`ASTNode`, `TypeChecker`, `BytecodeGenerator`)
- **Functions/Variables**: camelCase (`parseExpression`, `symbolTable`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RECURSION_DEPTH`, `DEFAULT_TARGET_VERSION`)
- **Sealed Classes**: Use nested classes for variants (`Type.Primitive`, `Expression.Binary`)

### File Organization

```
compiler/
├── src/main/kotlin/
│   ├── ast/                    # AST node definitions
│   │   ├── Expression.kt
│   │   ├── Statement.kt
│   │   └── Type.kt
│   ├── parser/                 # ANTLR integration
│   │   ├── TaylorLangParser.kt
│   │   └── ASTBuilder.kt
│   ├── typechecker/           # Type system
│   │   ├── TypeChecker.kt
│   │   ├── TypeInference.kt
│   │   └── Unification.kt
│   ├── codegen/               # Bytecode generation
│   │   ├── BytecodeGenerator.kt
│   │   └── ClassWriter.kt
│   └── main/                  # Compiler entry point
│       └── Compiler.kt
└── src/test/kotlin/           # Tests mirror main structure
    ├── ast/
    ├── parser/
    ├── typechecker/
    └── codegen/
```

## Testing Guidelines

### Test Structure

```kotlin
// Use Kotest for BDD-style testing
class TypeCheckerTest : StringSpec({
    "should infer Int type for numeric literals" {
        val expression = IntLiteral(42)
        val result = TypeChecker().inferType(expression)
        
        result.shouldBeRight()
        result.getOrNull() shouldBe Type.Primitive("Int")
    }
    
    "should reject mismatched types in binary operations" {
        val expression = BinaryOp(
            left = IntLiteral(42),
            operator = Plus,
            right = StringLiteral("hello")
        )
        
        val result = TypeChecker().inferType(expression)
        result.shouldBeLeft()
        result.leftOrNull() shouldBe instanceOf<CompilerError.TypeError>()
    }
})

// Use property-based testing for complex scenarios
class ParserPropertyTest : StringSpec({
    "parsing then pretty-printing should be idempotent" {
        checkAll(Gen.validTaylorLangProgram()) { program ->
            val parsed = Parser.parse(program)
            val prettyPrinted = PrettyPrinter.print(parsed.getOrThrow())
            val reparsed = Parser.parse(prettyPrinted)
            
            parsed shouldBe reparsed
        }
    }
})
```

### Testing Dependencies

```kotlin
// Add to build.gradle.kts
dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

## Performance Guidelines

### Immutable Collections Performance

```kotlin
// Use PCollections for performance-critical paths
class SymbolTable private constructor(
    private val symbols: PMap<String, Symbol> = HashTreePMap.empty()
) {
    fun define(name: String, symbol: Symbol): SymbolTable =
        SymbolTable(symbols.plus(name, symbol))
    
    fun lookup(name: String): Symbol? = symbols[name]
    
    // Use lazy evaluation for expensive computations
    val allSymbols: List<Symbol> by lazy { symbols.values.toList() }
}

// Prefer sequences for lazy evaluation
fun collectAllTypes(ast: ASTNode): Sequence<Type> = sequence {
    when (ast) {
        is Expression -> ast.type?.let { yield(it) }
        is Statement -> yieldAll(ast.children.asSequence().flatMap { collectAllTypes(it) })
    }
}
```

### Memory-Conscious Code

```kotlin
// Use object pooling for frequently created objects
object TypePool {
    val intType: Type.Primitive by lazy { Type.Primitive("Int") }
    val stringType: Type.Primitive by lazy { Type.Primitive("String") }
    val booleanType: Type.Primitive by lazy { Type.Primitive("Boolean") }
}

// Avoid creating unnecessary intermediate collections
fun transformExpressions(expressions: List<Expression>): List<Expression> =
    expressions.asSequence()
        .filter { it.isValid() }
        .map { transform(it) }
        .toList()
```

## Build Configuration

### Gradle Setup (build.gradle.kts)

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    antlr
    application
}

kotlin {
    jvmToolchain(17)
    
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {
    // Core dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")
    
    // ANTLR
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    
    // Bytecode generation
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    
    // Functional programming
    implementation("io.arrow-kt:arrow-core:1.2.1")
    implementation("org.pcollections:pcollections:4.0.1")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

tasks.generateGrammarSource {
    outputDirectory = file("src/main/generated/antlr")
    arguments = arguments + listOf("-visitor", "-long-messages")
}

tasks.test {
    useJUnitPlatform()
}
```

## Development Workflow

### Git Workflow

1. **Feature branches**: Create branches from `main` for each feature
2. **Commit messages**: Use conventional commits (feat:, fix:, refactor:, etc.)
3. **Pull requests**: All changes go through PR review
4. **CI/CD**: Automated testing and building on every PR

### Development Setup

```bash
# 1. Clone and setup
git clone <repository>
cd TaylorLang

# 2. Build project
./gradlew build

# 3. Run tests
./gradlew test

# 4. Generate ANTLR sources
./gradlew generateGrammarSource

# 5. Run compiler on example
./gradlew run --args="examples/hello.tl"
```

### Code Review Checklist

- [ ] Code follows Kotlin idioms and functional programming principles
- [ ] Uses immutable data structures where appropriate
- [ ] Includes comprehensive tests
- [ ] Error handling uses Either/Result types
- [ ] Performance considerations addressed
- [ ] Documentation updated if needed

This development approach leverages Kotlin's strengths for building a robust, maintainable compiler while staying true to functional programming principles.