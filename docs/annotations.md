# Annotations

## Annotation Definition

Annotations provide metadata for types, fields, functions, and other language constructs. They enable compile-time processing, code generation, and runtime reflection.

### Defining Annotations

```kotlin
@Annotation(appliesTo=[Type, Interface, Annotation, Field])
annotation JsonField(alias: String, serializable: Boolean)

@Annotation(appliesTo=[Type])
annotation Entity(tableName: String)

@Annotation(appliesTo=[Function])
annotation Deprecated(message: String, since: String = "")
```

## Annotation Usage

### On Types

```kotlin
@Entity(tableName = "users")
type User(
  @JsonField(alias = "userName", serializable = true)
  name: String,
  @JsonField(alias = "userAge", serializable = true)
  age: Int,
  @JsonField(alias = "email", serializable = false)
  email: String
)
```

### On Functions

```kotlin
@Deprecated(message = "Use calculateAreaV2 instead", since = "1.2.0")
fn calculateArea(radius: Double): Double => 3.14 * radius * radius

fn calculateAreaV2(radius: Double): Double => Math.PI * radius * radius
```

### On Fields and Parameters

```kotlin
type Configuration(
  @Required
  host: String,
  @Default("8080")
  port: Int,
  @Nullable
  description: String?
)
```

## Built-in Annotations

### Validation Annotations

```kotlin
@Annotation(appliesTo=[Field])
annotation Required()

@Annotation(appliesTo=[Field])
annotation Range(min: Int, max: Int)

@Annotation(appliesTo=[Field])
annotation Pattern(regex: String)
```

### Serialization Annotations

```kotlin
@Annotation(appliesTo=[Field, Type])
annotation Serializable(format: String = "json")

@Annotation(appliesTo=[Field])
annotation Exclude()

@Annotation(appliesTo=[Field])
annotation Alias(name: String)
```

## Annotation Processing

Annotations can be processed at compile time to generate code, validate constraints, or provide IDE support.

### Example: JSON Serialization

```kotlin
@Serializable
type Person(
  @Alias("full_name")
  name: String,
  age: Int,
  @Exclude
  internalId: String
)

// Generated serialization methods:
// fn Person.toJson(): String
// fn Person.fromJson(json: String): Person
```

