# Complete Examples

## Web Service

```kotlin
import web.server.{Server, Route}
import web.response.{json, notFound, ok, badRequest}
import services.{User, UserService}

type CreateUserRequest(name: String, email: String)

// Interface definition
interface UserServiceContract {
  async fn createUser(name: String, email: String): Result<User, String>
  fn findUser(id: String): User?
}

// Type definition
type UserService(
  users: Map<String, User>
)

// Implementation
impl UserService {
  fn new(): UserService => UserService(users = {})
}

// Interface implementation
impl UserServiceContract for UserService {
  async fn createUser(name: String, email: String): Result<User, String> => {
    val user = User(id = generateId(), name = name, email = email)
    Ok(user)
  }
  
  fn findUser(id: String): User? => 
    this.users.get(id)
}

val userService = UserService.new()

val routes = [
  Route.get("/users/{id}") { req =>
    val userId = req.params["id"]
    userService.findUser(userId) match {
      case user: User => ok(json(user))
      case null => notFound("User not found")
    }
  },
  
  Route.post("/users") { req =>
    val request = req.body.as<CreateUserRequest>()
    match userService.createUser(request.name, request.email) {
      case Ok(user) => ok(json(user))
      case Error(msg) => badRequest(msg)
    }
  }
]

async fn main() => {
  val server = Server(port = 8080, routes = routes)
  println("Server starting on port 8080...")
  server.start()
}
```

## JSON Parser

```kotlin
type Json = 
  | Null
  | Bool(Boolean)
  | Number(Double)
  | String(String)
  | Array(List<Json>)
  | Object(Map<String, Json>)

fn parseJson(input: String): Result<Json, ParseError> => {
  // Implementation details...
}

fn getValue(json: Json, path: List<String>): Json? => match (json, path) {
  case (value, []) => value
  case (Object(map), [key, ...rest]) => 
    map.get(key) match {
      case nextJson: Json => getValue(nextJson, rest)
      case null => null
    }
  case (Array(items), [indexStr, ...rest]) => 
    parseNumber(indexStr) match {
      case index: Int if index >= 0 && index < items.length =>
        getValue(items[index], rest)
      case _ => null
    }
  case _ => null
}
```

## Functional Data Processing

```haskell
type Sale(id: String, amount: Double, category: String, date: String)

fn analyzesSales(sales: List<Sale>): Map<String, Double> => {
  sales
    .groupBy(sale => sale.category)
    .mapValues(categorySales => 
      categorySales.map(sale => sale.amount).sum()
    )
}

fn topCategories(sales: List<Sale>, n: Int): List<String> => {
  analyzesSales(sales)
    .entries()
    .sortBy((_, total) => -total)  // Sort descending
    .take(n)
    .map((category, _) => category)
}

async fn processReport(sales: List<Sale>) => {
  val totalRevenue = sales.map(sale => sale.amount).sum()
  val topThree = topCategories(sales, 3)
  
  println("Total Revenue: $${totalRevenue}")
  println("Top Categories:")
  topThree.forEach(category => println("  - ${category}"))
}
```

## Domain Modeling Example

```kotlin
// User management domain
type UserId(value: String)
type Email(value: String)

type UserStatus = 
  | Active
  | Inactive
  | Suspended(reason: String, until: Date)

type User(
  id: UserId,
  name: String,
  email: Email,
  status: UserStatus,
  createdAt: Date
)

// Business operations
fn activateUser(user: User): Result<User, String> => match user.status {
  case Suspended(_, until) if Date.now() < until =>
    Error("User still suspended until ${until}")
  case _ => Ok(user.copy(status = Active))
}

async fn sendEmailTo(user: User, subject: String, body: String): Result<Unit, EmailError> => {
  match user.status {
    case Active => await Email.send(user.email, subject, body)
    case _ => Error(UserNotActive)
  }
}
```

These examples demonstrate practical usage patterns combining the language's functional features with real-world application requirements.