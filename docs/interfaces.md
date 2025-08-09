# Interfaces

## Interface Definition

Interfaces define contracts that types must implement, similar to Java interfaces.

```kotlin
interface UserServiceContract {
  async fn createUser(name: String, email: String): Result<CreatedUser, String>
  async fn findUser(id: String): User?
  fn validateEmail(email: String): Boolean
}

interface Serializable {
  fn toJson(): String
  static fn fromJson(json: String): Self
}
```

## Type Implementation

### Basic Type Definition

```kotlin
type UserService(
  users: Map<String, User>
)

type User(
  id: String,
  name: String, 
  email: String,
  createdAt: Date
)
```

### Implementation Blocks

Use `impl` blocks to provide implementations for types:

```kotlin
impl UserService {
  // Static functions - cannot access 'this'
  static fn new(): UserService => UserService(users = {})
  
  // Instance methods - can access 'this'
  fn addUser(user: User): UserService => {
    // Implementation details using 'this'
    this.users.put(user.id, user)
    this
  }
  
  fn getUserCount(): Int => {
    this.users.size()
  }
}
```

### Interface Implementation

Implement interfaces for types using `impl InterfaceName for TypeName`:

```kotlin
impl UserServiceContract for UserService {
  async fn createUser(name: String, email: String): Result<CreatedUser, String> => {
    val user = User(
      id = generateId(),
      name = name,
      email = email,
      createdAt = Date.now()
    )
    
    match this.validateEmail(email) {
      case true => {
        val created = this.addUser(user)
        Ok(CreatedUser(user.id, user.name))
      }
      case false => Error("Invalid email format")
    }
  }
  
  async fn findUser(id: String): User? => {
    this.users.get(id)
  }
  
  fn validateEmail(email: String): Boolean => {
    email.contains("@") && email.contains(".")
  }
}

impl Serializable for User {
  fn toJson(): String => {
    """{"id":"${this.id}","name":"${this.name}","email":"${this.email}"}"""
  }
  
  static fn fromJson(json: String): User => {
    // JSON parsing implementation - static since it creates new instance
    parseUserFromJson(json)
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

impl Repository<User, String> for UserService {
  fn save(user: User): User => {
    this.users.put(user.id, user)
    user
  }
  
  fn findById(id: String): User? => {
    this.users.get(id)
  }
  
  fn findAll(): List<User> => {
    this.users.values().toList()
  }
  
  fn deleteById(id: String): Boolean => {
    this.users.remove(id) != null
  }
}
```

## Pattern Matching with Interfaces

You can pattern match over interface implementations using type patterns:

```kotlin
fn processService(service: UserService) => match service {
  case v1: UserServiceContract => println("Using v1 impl")
  case v2: UserServiceContractV2 => println("Using v2 impl")
  case _ => println("Unknown service implementation")
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
- Called on the type itself: `UserService.new()`
- Used for constructors and utility functions

### Instance Methods  
- Can access `this` reference
- Called on instances: `userService.addUser(user)`
- Used for operations that modify or query instance state

```kotlin
impl UserService {
  // Static - constructor
  static fn new(): UserService => UserService(users = {})
  
  // Static - utility function
  static fn validateId(id: String): Boolean => id.length > 0
  
  // Instance - modifies this
  fn addUser(user: User): Unit => {
    this.users.put(user.id, user)
  }
  
  // Instance - queries this
  fn hasUser(id: String): Boolean => {
    this.users.contains(id)
  }
}

// Usage
val service = UserService.new()      // static call
val isValidId = UserService.validateId("123")  // static call
service.addUser(user)                // instance call
val exists = service.hasUser("123")  // instance call
```

## Best Practices

- **Keep interfaces focused**: Each interface should have a single, clear responsibility
- **Use descriptive names**: Interface names should clearly indicate their purpose
- **Prefer composition**: Use multiple small interfaces rather than large monolithic ones
- **Leverage pattern matching**: Use interface pattern matching for polymorphic behavior
- **Generic constraints**: Use interface constraints to make generic code more expressive
- **Default implementations**: Provide sensible defaults to reduce boilerplate
- **Static vs Instance**: Use static for constructors/utilities, instance for state operations