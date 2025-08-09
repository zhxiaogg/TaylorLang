# Interfaces

## Interface Definition

Interfaces define contracts that types implement, similar to Java interfaces.

```kotlin
interface Calculator {
  fn add(a: Int, b: Int): Int
  fn multiply(a: Int, b: Int): Int
}

interface Serializable {
  fn toJson(): String
  static fn fromJson(json: String): Self
}
```

## Type Implementation

### Basic Type Definition

```kotlin
type BasicCalculator(name: String)

type Person(name: String, age: Int)
```

### Implementation Blocks

Use `impl` blocks to provide implementations for types:

```kotlin
impl BasicCalculator {
  // Static functions - cannot access 'this'
  static fn new(name: String): BasicCalculator => BasicCalculator(name)
  
  // Instance methods - can access 'this'
  fn getName(): String => {
    this.name
  }
}
```

### Interface Implementation

Implement interfaces for types using `impl InterfaceName for TypeName`:

```kotlin
impl Calculator for BasicCalculator {
  fn add(a: Int, b: Int): Int => {
    a + b
  }
  
  fn multiply(a: Int, b: Int): Int => {
    a * b
  }
}

impl Serializable for Person {
  fn toJson(): String => {
    """{"name":"${this.name}","age":${this.age}}"""
  }
  
  static fn fromJson(json: String): Person => {
    parsePersonFromJson(json)
  }
}
```

## Generic Interfaces

```kotlin
interface Repository<T, ID> {
  fn save(item: T): T
  fn findById(id: ID): T?
  fn findAll(): List<T>
  fn deleteById(id: ID): Boolean
}

impl Repository<Person, String> for PersonDatabase {
  fn save(person: Person): Person => {
    this.data.put(person.name, person)
    person
  }
  
  fn findById(name: String): Person? => {
    this.data.get(name)
  }
  
  fn findAll(): List<Person> => {
    this.data.values().toList()
  }
  
  fn deleteById(name: String): Boolean => {
    this.data.remove(name) != null
  }
}
```

## Pattern Matching with Interfaces

You can pattern match over interface implementations using type patterns:

```kotlin
fn processCalculator(calc: BasicCalculator) => match calc {
  case c: Calculator => println("Standard calculator")
  case _ => println("Unknown calculator type")
}

fn handleSerializable<T: Serializable>(item: T) => {
  val json = item.toJson()
  println("Serialized: ${json}")
}
```

## Interface Constraints

Use interfaces as type constraints in generic functions:

```kotlin
fn serialize<T: Serializable>(items: List<T>): List<String> => {
  items.map(item => item.toJson())
}

fn createAndSave<T, ID>(
  repository: Repository<T, ID>, 
  factory: () => T
): T => {
  val item = factory()
  repository.save(item)
}
```

## Default Implementations

Interfaces can provide default implementations:

```kotlin
interface Printable {
  fn print(): Unit
  
  // Default implementation
  fn printWithPrefix(prefix: String): Unit => {
    println("${prefix}: ")
    this.print()
  }
}

impl Printable for User {
  fn print(): Unit => {
    println("User(${this.name}, ${this.email})")
  }
  
  // printWithPrefix is automatically available
}
```

## Static vs Instance Methods

### Static Methods
- Marked with `static` keyword
- Cannot access `this` reference
- Called on the type itself: `BasicCalculator.new()`
- Used for constructors and utility functions

### Instance Methods  
- Can access `this` reference
- Called on instances: `calc.add(2, 3)`
- Used for operations that modify or query instance state

```kotlin
impl BasicCalculator {
  // Static - constructor
  static fn new(name: String): BasicCalculator => BasicCalculator(name)
  
  // Static - utility function
  static fn isValidOperation(op: String): Boolean => op.length > 0
  
  // Instance - queries this
  fn getName(): String => {
    this.name
  }
}

// Usage
val calc = BasicCalculator.new("MyCalc")        // static call
val isValid = BasicCalculator.isValidOperation("+")  // static call
val name = calc.getName()                       // instance call
```

