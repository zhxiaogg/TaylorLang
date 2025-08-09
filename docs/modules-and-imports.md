# Modules and Imports

## Module Definition

```javascript
// file: math/geometry.lang

export val PI = 3.14159

export fn area(shape: Shape): Double => match shape {
  case Circle(radius) => PI * radius * radius
  case Rectangle(width, height) => width * height
  case Triangle(a, b, c) => {
    val s = (a + b + c) / 2.0
    Math.sqrt(s * (s - a) * (s - b) * (s - c))
  }
}

export data class Point(x: Double, y: Double)

export enum Shape {
  Circle(radius: Double),
  Rectangle(width: Double, height: Double),
  Triangle(a: Double, b: Double, c: Double)
}
```

## Import Statements

```javascript
// Import specific items
import { PI, area, Point } from "math/geometry"
import { map, filter, reduce } from "std/list"

// Import with alias
import json from "std/json" as JSON
import http from "std/http"

// Import everything
import * from "math/geometry" as Geometry

// Relative imports
import { User, UserService } from "./models"
import { validate } from "../utils/validation"
```

## Module Usage

```kotlin
// Using imported items
val circle = Circle(radius = 5.0)
val circleArea = area(circle)

val point = Point(10.0, 20.0)

val jsonString = JSON.stringify(data)
val response = http.get("/api/users")
```

## Module Best Practices

- Organize related functionality into modules
- Use descriptive module names that reflect their purpose
- Export only what needs to be public
- Prefer specific imports over wildcard imports
- Use aliases to avoid naming conflicts
- Keep module dependencies minimal and clear