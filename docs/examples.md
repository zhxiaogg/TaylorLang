# Complete Examples

## Web Service

```typescript
import { Server, Route } from "web/server"
import { json, notFound, ok } from "web/response"
import { User, UserService } from "./services"

data class CreateUserRequest(name: String, email: String)

val userService = UserService()

val routes = [
  Route.get("/users/{id}") { req =>
    val userId = req.params["id"]
    userService.findUser(userId) match {
      case Some(user) => ok(json(user))
      case None => notFound("User not found")
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

fn main(): IO<Unit> => {
  val server = Server(port = 8080, routes = routes)
  println("Server starting on port 8080...")
  server.start()
}
```

## JSON Parser

```kotlin
enum Json {
  Null,
  Bool(Boolean),
  Number(Double),
  String(String),
  Array(List<Json>),
  Object(Map<String, Json>)
}

fn parseJson(input: String): Result<Json, ParseError> => {
  // Implementation details...
}

fn getValue(json: Json, path: List<String>): Option<Json> => match (json, path) {
  case (value, []) => Some(value)
  case (Object(map), [key, ...rest]) => 
    map.get(key).flatMap(nextJson => getValue(nextJson, rest))
  case (Array(items), [indexStr, ...rest]) => 
    parseNumber(indexStr).toOption()
      .filter(index => index >= 0 && index < items.length)
      .flatMap(index => getValue(items[index], rest))
  case _ => None
}
```

## Functional Data Processing

```haskell
data class Sale(id: String, amount: Double, category: String, date: String)

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

fn processReport(sales: List<Sale>): IO<Unit> => {
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
data class UserId(value: String)
data class Email(value: String)

enum UserStatus {
  Active,
  Inactive,
  Suspended(reason: String, until: Date)
}

data class User(
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

fn sendEmailTo(user: User, subject: String, body: String): IO<Result<Unit, EmailError>> => {
  match user.status {
    case Active => Email.send(user.email, subject, body)
    case _ => IO.pure(Error(UserNotActive))
  }
}
```

These examples demonstrate practical usage patterns combining the language's functional features with real-world application requirements.