# Modules and Imports

## Package Structure and Visibility

TaylorLang uses a package-based module system similar to Java. Code is organized into packages using folder structures, and visibility is controlled using access modifiers.

### Package Declaration

```kotlin
// file: math/geometry.lang
package math.geometry

// Private by default - accessible within package
val PRECISION = 0.0001

// Public - accessible from other packages
pub val PI = 3.14159

pub fn area(shape: Shape): Double => match shape {
  case Circle(radius) => PI * radius * radius
  case Rectangle(width, height) => width * height
  case Triangle(a, b, c) => {
    val s = (a + b + c) / 2.0
    Math.sqrt(s * (s - a) * (s - b) * (s - c))
  }
}

pub type Point(x: Double, y: Double)

pub type Shape {
  Circle(radius: Double),
  Rectangle(width: Double, height: Double),
  Triangle(a: Double, b: Double, c: Double)
}
```

## Import Statements

```kotlin
// Import specific items from package
import math.geometry.PI
import math.geometry.{PI, area, Point}
import std.list.{map, filter, reduce}

// Import entire package with alias
import std.json as JSON
import std.http

// Import all public items from package
import math.geometry.* as Geometry
```

## Package Usage

```kotlin
// Using imported items
val circle = Circle(radius = 5.0)
val circleArea = area(circle)

val point = Point(10.0, 20.0)

// Using aliased package
val jsonString = JSON.stringify(data)
val response = http.get("/api/users")
```

## Visibility Rules

### Default (Package-Private)
- Items without visibility modifiers are accessible within the same package
- Cannot be accessed from other packages

### Public (`pub`)
- Items marked with `pub` are accessible from other packages
- Must be explicitly imported to be used

```kotlin
// math/internal.lang
package math.internal

val privateConstant = 42        // Only accessible within math.internal
pub val publicConstant = 3.14   // Accessible from other packages

fn privateFunction() => "hidden"     // Package-private
pub fn publicFunction() => "visible"  // Public
```

