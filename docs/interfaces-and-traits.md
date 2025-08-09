# Interfaces and Traits

## Interface Definition

Interfaces define contracts that types must implement, similar to Rust traits or Java interfaces.

```kotlin
interface UserServiceContract {
  async fn createUser(name: String, email: String): Result<CreatedUser, String>
  async fn findUser(id: String): Option<User>
  fn validateEmail(email: String): Boolean
}

interface Serializable {
  fn toJson(): String
  fn fromJson(json: String): Self
}
```

## Type Implementation

### Basic Type Definition

```kotlin
type UserService()

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
  // Associated functions (static methods)
  fn new(): UserService => UserService()
  
  // Instance methods
  fn addUser(self, user: User): UserService => {
    // Implementation details
  }
  
  fn getUserCount(self): Int => {
    // Implementation details
  }
}
```

### Interface Implementation

Implement interfaces for types using `impl InterfaceName for TypeName`:

```kotlin
impl UserServiceContract for UserService {
  async fn createUser(self, name: String, email: String): Result<CreatedUser, String> => {
    val user = User(
      id = generateId(),
      name = name,
      email = email,
      createdAt = Date.now()
    )
    
    match self.validateEmail(email) {
      case true => {
        val created = self.addUser(user)
        Ok(CreatedUser(user.id, user.name))
      }
      case false => Error("Invalid email format")
    }
  }
  
  async fn findUser(self, id: String): Option<User> => {
    // Implementation details
  }
  
  fn validateEmail(self, email: String): Boolean => {
    email.contains("@") && email.contains(".")
  }
}

impl Serializable for User {
  fn toJson(self): String => {
    """{"id":"${self.id}","name":"${self.name}","email":"${self.email}"}"""
  }
  
  fn fromJson(json: String): User => {
    // JSON parsing implementation
  }
}
```

## Generic Interfaces

```kotlin
interface Repository<T, ID> {
  fn save(item: T): T
  fn findById(id: ID): Option<T>
  fn findAll(): List<T>
  fn deleteById(id: ID): Boolean
}

impl Repository<User, String> for UserService {
  fn save(self, user: User): User => {
    // Implementation
  }
  
  fn findById(self, id: String): Option<User> => {
    // Implementation  
  }
  
  fn findAll(self): List<User> => {
    // Implementation
  }
  
  fn deleteById(self, id: String): Boolean => {
    // Implementation
  }
}
```

## Pattern Matching with Interfaces

You can pattern match over interface implementations:

```kotlin
fn processService(service: UserServiceContract) => match service {
  case impl UserService => println("Using concrete UserService")
  case impl MockUserService => println("Using mock service")
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
  fn print(self): Unit
  
  // Default implementation
  fn printWithPrefix(self, prefix: String): Unit => {
    println("${prefix}: ")
    self.print()
  }
}

impl Printable for User {
  fn print(self): Unit => {
    println("User(${self.name}, ${self.email})")
  }
  
  // printWithPrefix is automatically available
}
```

## Best Practices

- **Keep interfaces focused**: Each interface should have a single, clear responsibility
- **Use descriptive names**: Interface names should clearly indicate their purpose
- **Prefer composition**: Use multiple small interfaces rather than large monolithic ones
- **Leverage pattern matching**: Use interface pattern matching for polymorphic behavior
- **Generic constraints**: Use interface constraints to make generic code more expressive
- **Default implementations**: Provide sensible defaults to reduce boilerplate