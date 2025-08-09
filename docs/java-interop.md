# Java Interoperability

## Importing Java Classes

```java
// Import Java classes
import java.util.ArrayList from Java
import java.time.LocalDateTime from Java
import java.util.concurrent.CompletableFuture from Java

// Import static methods
import java.lang.Math.sqrt from Java
import java.util.Collections.emptyList from Java
```

## Using Java APIs

```kotlin
// Create Java objects
val javaList = ArrayList<String>()
javaList.add("item1")
javaList.add("item2")

// Convert between types
val ourList = javaList.toList()         // Convert to immutable list
val backToJava = ourList.toJavaList()   // Convert back to Java list

// Call Java methods
val now = LocalDateTime.now()
val formatted = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
```

## Null Safety

```kotlin
// Java methods that can return null
fn processJavaString(str: String?): String => {
  str?.trim() ?: "default"
}

// Handling nullable Java APIs
val javaMap = HashMap<String, String>()
val value: String? = javaMap.get("key")
val safeValue = value ?: "default"
```

## Type Mapping

```kotlin
// Automatic conversions
List<T>           <-> java.util.List<T>
Map<K, V>         <-> java.util.Map<K, V>
Set<T>            <-> java.util.Set<T>
Option<T>         <-> T? (nullable reference)
Result<T, E>      <-> CompletableFuture<T> (for async)
String            <-> java.lang.String
```

## Interop Best Practices

- Wrap Java APIs in safe, functional interfaces
- Handle null values explicitly using Option types
- Convert between collection types at boundaries
- Use type-safe wrappers for Java APIs
- Minimize direct Java API usage in core logic
- Leverage automatic type conversions where available