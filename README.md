# Franckie Validation Framework

A type-safe, composable validation framework for Java that leverages annotation processing and functional programming patterns to build elegant, maintainable validation rules.

## Overview

Franckie is a Java validation framework that provides:

- **Type-Safe Validation**: Compile-time checking of attribute types and validation rules
- **Composable Architecture**: Build complex validations from simple, reusable rules
- **Fluent DSL**: Readable, expressive syntax for defining validation logic
- **Code Generation**: Automatic generation of type-safe attribute accessors via annotation processing
- **Customizable Naming**: Full control over generated constant names and logical attribute names
- **Primitive Type Boxing**: Automatic conversion of primitives to boxed types for null-safety
- **Functional Composition**: Reuse validation rules across different contexts using functional patterns
- **Detailed Feedback**: Rich error reporting with expected vs. actual values
- **Internationalization**: Built-in support for translatable validation messages

## Table of Contents

- [Key Features](#key-features)
- [Advanced Features](#advanced-features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Example Usage](#example-usage)
- [Rule Types](#rule-types)
- [API Reference](#api-reference)

## Key Features

### 1. Type-Safe Attribute References

Define validation projections using records and generate type-safe attribute accessors:

```java
@ValidationProjection
public record SessionProjection(
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Integer capacity,
    UUID courseId
) {}
```

The annotation processor generates `SessionProjectionAttrs` with type-safe attribute references:

```java
import static SessionProjectionAttrs.*;

Rule<SessionProjection> rule = NAME.notNull()
    .and(START_DATE.notNull())
    .and(END_DATE.gt(START_DATE));
```

**Generated code:**
```java
public final class SessionProjectionAttrs {
    public static final Attr<SessionProjection, String> NAME =
        new Attr<>("name", SessionProjection::name);

    public static final Attr<SessionProjection, LocalDate> START_DATE =
        new Attr<>("startDate", SessionProjection::startDate);

    public static final Attr<SessionProjection, LocalDate> END_DATE =
        new Attr<>("endDate", SessionProjection::endDate);

    // ... other attributes
}
```

### 2. Composable Rules

Build complex validations by composing simple rules:

```java
public class CommonSessionRules {
    public static final Rule<SessionProjection> NAME_REQUIRED =
        NAME.notNull().onInvalid("session.name.required");

    public static final Rule<SessionProjection> END_DATE_AFTER_START =
        END_DATE.gt(START_DATE).onInvalid("session.date.invalid");

    public static final Rule<SessionProjection> CAPACITY_POSITIVE =
        CAPACITY.notNull()
            .and(CAPACITY.gt(0))
            .onInvalid("session.capacity.positive");

    public static Rule<SessionProjection> all() {
        return Rule.allOf(
            NAME_REQUIRED,
            START_DATE_REQUIRED,
            END_DATE_REQUIRED,
            END_DATE_AFTER_START,
            CAPACITY_POSITIVE
        );
    }
}
```

### 3. Contramap for Rule Reuse

Apply validation rules to different types using contramap:

```java
@ValidationProjection
public record CreateSessionCommand(
    UUID commandId,
    SessionProjection projection,
    boolean sessionNameAlreadyExists,
    String status
) implements Command {}

// Reuse SessionProjection rules for CreateSessionCommand
Rule<CreateSessionCommand> commandRules =
    CommonSessionRules.all().contramap(CreateSessionCommand::projection);
```

### 4. Conditional Validation with Guards

Skip validation based on preconditions:

```java
import static CreateSessionCommandAttrs.*;

Rule<CreateSessionCommand> nameUniquenessCheck =
    Rule.guard(
        cmd -> cmd.sessionNameAlreadyExists(),
        SESSION_NAME_ALREADY_EXISTS.eq(false)
            .onInvalid("session.name.must.be.unique")
    );
```

### 5. Rich Error Reporting

Violations include translation keys, context, and detailed results:

```java
record Violation(String code, Map<String, Object> args, Result result) {}
record Result(Status status, Object expected, Object found, Object left, Object right) {}
```

## Advanced Features

### @AttrName Annotation - Custom Naming Control

The `@AttrName` annotation provides fine-grained control over generated constant names and logical attribute names used in validation messages.

#### Syntax

```java
@interface AttrName {
    /**
     * Logical attribute name used in validation messages.
     * Default: derived from field name (camelCase)
     */
    String value() default "";

    /**
     * Optional generated constant name override.
     * Must be in CONSTANT_CASE format.
     * Default: derived from field name (CONSTANT_CASE)
     */
    String constant() default "";
}
```

#### Three Usage Patterns

**1. Custom Logical Name Only**

Use when you want a custom logical name but keep the auto-generated constant:

```java
@ValidationProjection
public record SessionProjection(
    @AttrName("session.name")  // Logical name for i18n
    String name,

    @AttrName("session.end.date")  // Custom logical name
    LocalDate endDate
) {}
```

**Generated:**
```java
public final class SessionProjectionAttrs {
    // Constant: NAME (auto-generated), Logical: "session.name" (custom)
    public static final Attr<SessionProjection, String> NAME =
        new Attr<>("session.name", SessionProjection::name);

    // Constant: END_DATE (auto-generated), Logical: "session.end.date" (custom)
    public static final Attr<SessionProjection, LocalDate> END_DATE =
        new Attr<>("session.end.date", SessionProjection::endDate);
}
```

**2. Constant-Case Shorthand (Smart Dual-Purpose)**

When the annotation value is in CONSTANT_CASE, it's used as the constant name, and the logical name is auto-derived:

```java
@ValidationProjection
public record CreateSessionCommand(
    UUID commandId,

    @AttrName("CREATE_SESSION_STATUS")  // CONSTANT_CASE shorthand
    String status
) {}
```

**Generated:**
```java
public final class CreateSessionCommandAttrs {
    // Constant: CREATE_SESSION_STATUS (from annotation)
    // Logical: "createSessionStatus" (auto-derived via camelCase conversion)
    public static final Attr<CreateSessionCommand, String> CREATE_SESSION_STATUS =
        new Attr<>("createSessionStatus", CreateSessionCommand::status);
}
```

**Usage:**
```java
import static CreateSessionCommandAttrs.*;

Rule<CreateSessionCommand> statusRule =
    CREATE_SESSION_STATUS.eq("PENDING");  // Use custom constant name
```

**3. Explicit Both (Full Control)**

Specify both the constant name and logical name explicitly:

```java
@ValidationProjection
public record CreateSessionCommand(
    @AttrName(value = "session.status", constant = "CREATE_SESSION_STATUS")
    String status
) {}
```

**Generated:**
```java
public final class CreateSessionCommandAttrs {
    // Constant: CREATE_SESSION_STATUS (explicit)
    // Logical: "session.status" (explicit)
    public static final Attr<CreateSessionCommand, String> CREATE_SESSION_STATUS =
        new Attr<>("session.status", CreateSessionCommand::status);
}
```

#### @AttrName Best Practices

| Use Case | Pattern | Example |
|----------|---------|---------|
| **Domain-specific logical names** | `@AttrName("domain.field")` | `@AttrName("session.end.date")` |
| **Custom constant for clarity** | `@AttrName("CONSTANT_NAME")` | `@AttrName("CREATE_SESSION_STATUS")` |
| **Different naming contexts** | `@AttrName(value = "msg", constant = "CONST")` | `@AttrName(value = "session.status", constant = "STATUS")` |
| **Default behavior** | No annotation | Auto-generates both from field name |

### Primitive Type Boxing

Franckie automatically converts primitive types to their boxed equivalents in generated `Attrs` classes, ensuring null-safety and consistency.

#### Automatic Boxing

```java
@ValidationProjection
public record CreateSessionCommand(
    UUID commandId,
    boolean sessionNameAlreadyExists,  // primitive boolean
    int retries                        // primitive int
) {}
```

**Generated with boxed types:**
```java
public final class CreateSessionCommandAttrs {
    // boolean → Boolean (boxed)
    public static final Attr<CreateSessionCommand, Boolean> SESSION_NAME_ALREADY_EXISTS =
        new Attr<>("sessionNameAlreadyExists", CreateSessionCommand::sessionNameAlreadyExists);

    // int → Integer (boxed)
    public static final Attr<CreateSessionCommand, Integer> RETRIES =
        new Attr<>("retries", CreateSessionCommand::retries);
}
```

#### Supported Primitive Conversions

| Primitive Type | Boxed Type |
|----------------|------------|
| `boolean` | `Boolean` |
| `byte` | `Byte` |
| `char` | `Character` |
| `short` | `Short` |
| `int` | `Integer` |
| `long` | `Long` |
| `float` | `Float` |
| `double` | `Double` |

#### Benefits

1. **Null-Safety**: Boxed types can represent null values for optional fields
2. **Consistency**: All attributes use reference types
3. **Framework Integration**: Works seamlessly with validation rules expecting objects
4. **Auto-Boxing**: Java automatically converts between primitive and boxed types

#### Usage with Validation Rules

```java
import static CreateSessionCommandAttrs.*;

public class CreateSessionRules {
    // Use boxed Boolean type naturally
    public static final Rule<CreateSessionCommand> NAME_MUST_BE_UNIQUE =
        SESSION_NAME_ALREADY_EXISTS.eq(false)
            .onInvalid("session.name.must.be.unique");

    // Integer comparison works seamlessly
    public static final Rule<CreateSessionCommand> MAX_RETRIES =
        RETRIES.lt(5)
            .onInvalid("session.retries.exceeded");
}
```

## Architecture

Franckie is organized as a multi-module Maven project:

### Modules

- **validation-core**: Core validation interfaces, rule definitions, and evaluation engine
  - `Rule<T>`: Sealed interface for all validation rules
  - `Attr<T, A>`: Type-safe attribute accessor with fluent validation API
  - `Violation` & `Result`: Error reporting types
  - `Command`: Interface for validatable commands
  - `@ValidationProjection`: Annotation for code generation
  - `@AttrName`: Annotation for custom attribute naming

- **validation-processor**: Annotation processor for code generation
  - Processes `@ValidationProjection` annotations
  - Generates `*Attrs` classes with static attribute definitions
  - Handles `@AttrName` customizations
  - Performs primitive type boxing
  - Automatic case conversion (camelCase ↔ CONSTANT_CASE)

- **validation-sample**: Example application demonstrating framework usage
  - Sample domain models: `SessionProjection`, `CreateSessionCommand`
  - Validation rules: `CommonSessionRules`, `CreateSessionRules`
  - Integration tests demonstrating all features

### Design Patterns

1. **Sealed Types**: Type-safe rule hierarchy using Java sealed interfaces
2. **Pattern Matching**: Rule evaluation via switch expressions (Java 21)
3. **Fluent API**: Chainable methods for readable rule composition
4. **Functional Composition**: Contramap for rule transformation across types
5. **Annotation Processing**: Compile-time code generation for boilerplate elimination
6. **Builder Pattern**: Complex rule construction through method chaining
7. **Strategy Pattern**: Different rule types (`EdgeRule`, `ComposedRule`) with unified interface

### Rule Hierarchy

```
Rule<T> (sealed interface)
├── EdgeRule<T> (sealed interface)
│   ├── Equals<T, A>
│   ├── GreaterThan<T, A>
│   ├── LessThan<T, A>
│   ├── IsNull<T, A>
│   ├── IsEmpty<T, A>
│   ├── In<T, A>
│   ├── ContainsAll<T, A>
│   └── ... other atomic rules
│
└── ComposedRule<T> (sealed interface)
    ├── And<T>
    ├── Or<T>
    ├── Not<T>
    ├── AllOf<T>
    ├── AnyOf<T>
    ├── Guard<T>
    ├── WithCode<T>
    └── ContraMap<T, U>
```

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.9+

### Building the Project

```bash
# Compile with annotation processing
./mvnw clean compile

# Run tests
./mvnw test

# Install to local Maven repository
./mvnw install
```

### Using in Your Project

Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>jara.sol.franckie</groupId>
        <artifactId>validation-core</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

Configure the annotation processor:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>jara.sol.franckie</groupId>
                        <artifactId>validation-processor</artifactId>
                        <version>0.0.1-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Example Usage

### Basic Example

#### 1. Define a Validation Projection

```java
import jara.sol.franckie.validation.core.ValidationProjection;
import jara.sol.franckie.validation.core.annotation.AttrName;

@ValidationProjection
public record SessionProjection(
    String name,
    LocalDate startDate,
    @AttrName("session.end.date")  // Custom logical name
    LocalDate endDate,
    Integer capacity,
    UUID courseId
) {}
```

#### 2. Create Validation Rules

```java
import static SessionProjectionAttrs.*;

public class SessionRules {
    public static final Rule<SessionProjection> VALID_SESSION =
        Rule.allOf(
            NAME.notNull().onInvalid("session.name.required"),
            START_DATE.notNull().onInvalid("session.startDate.required"),
            END_DATE.notNull().onInvalid("session.endDate.required"),
            END_DATE.gt(START_DATE).onInvalid("session.dates.invalid"),
            CAPACITY.notNull()
                .and(CAPACITY.gt(0))
                .onInvalid("session.capacity.positive"),
            COURSE_ID.notNull().onInvalid("session.courseId.required")
        );
}
```

#### 3. Validate Objects

```java
SessionProjection session = new SessionProjection(
    "Java Advanced",
    LocalDate.of(2024, 1, 15),
    LocalDate.of(2024, 1, 10), // Invalid: end before start
    20,
    UUID.randomUUID()
);

Result result = SessionRules.VALID_SESSION.evaluate(session);

if (result.status() == Status.FAILED) {
    result.violations().forEach(violation ->
        System.out.println("Violation: " + violation.code())
    );
    // Output: Violation: session.dates.invalid
}
```

### Advanced Example - Commands with Contramap

#### 1. Define Command with Custom Attributes

```java
@ValidationProjection
public record CreateSessionCommand(
    UUID commandId,
    SessionProjection projection,
    boolean sessionNameAlreadyExists,
    @AttrName("CREATE_SESSION_STATUS")  // Custom constant name
    String status
) implements Command {
    @Override
    public UUID getCommandId() {
        return commandId;
    }
}
```

#### 2. Compose Validation Rules

```java
import static CreateSessionCommandAttrs.*;

public class CreateSessionRules {
    // Command-specific rules
    public static final Rule<CreateSessionCommand> STATUS_MUST_BE_PENDING =
        CREATE_SESSION_STATUS.eq("PENDING")
            .onInvalid("session.status.must.be.pending");

    public static final Rule<CreateSessionCommand> NAME_MUST_BE_UNIQUE =
        SESSION_NAME_ALREADY_EXISTS.eq(false)
            .onInvalid("session.name.must.be.unique");

    // Reuse common rules via contramap
    public static Rule<CreateSessionCommand> all() {
        return Rule.allOf(
            STATUS_MUST_BE_PENDING,
            NAME_MUST_BE_UNIQUE,
            // Apply SessionProjection rules to the nested projection field
            CommonSessionRules.all().contramap(CreateSessionCommand::projection)
        );
    }
}
```

#### 3. Validate Commands

```java
CreateSessionCommand command = new CreateSessionCommand(
    UUID.randomUUID(),
    new SessionProjection(
        "Existing Course",
        LocalDate.now(),
        LocalDate.now().plusDays(7),
        25,
        UUID.randomUUID()
    ),
    true,  // Name already exists
    "PENDING"
);

Result result = CreateSessionRules.all().evaluate(command);

if (result.status() == Status.FAILED) {
    result.violations().forEach(v -> {
        System.out.printf("Code: %s, Expected: %s, Found: %s%n",
            v.code(), v.result().expected(), v.result().found());
    });
    // Output: Code: session.name.must.be.unique, Expected: false, Found: true
}
```

## Rule Types

### Edge Rules (Atomic Comparisons)

Edge rules represent atomic validation conditions:

#### Value Comparisons
- `eq(value)` / `eq(attr)`: Equality check
- `gt(value)` / `gt(attr)`: Greater than
- `lt(value)` / `lt(attr)`: Less than
- `gte(value)` / `gte(attr)`: Greater than or equal
- `lte(value)` / `lte(attr)`: Less than or equal

#### Null and Emptiness Checks
- `isNull()` / `notNull()`: Null checks
- `isEmpty()` / `notEmpty()`: Collection/String emptiness

#### Collection Operations
- `in(values)`: Membership check
- `containsAll(values)`: Collection contains all specified values

**Examples:**
```java
// Value comparisons
CAPACITY.gt(0)
END_DATE.gt(START_DATE)
STATUS.eq("ACTIVE")

// Null checks
NAME.notNull()
EMAIL.isNull()

// Collection operations
STATUS.in("PENDING", "ACTIVE", "COMPLETED")
TAGS.containsAll("java", "validation")
```

### Composed Rules

Composed rules combine multiple rules into complex validation logic:

#### Logical Operators
- `and(rule)`: Logical AND - both rules must pass
- `or(rule)`: Logical OR - at least one rule must pass
- `not()`: Logical negation - rule must fail

#### Aggregation
- `allOf(rules...)`: All rules must pass (short-circuits on first failure)
- `anyOf(rules...)`: At least one rule must pass

#### Control Flow
- `guard(condition, rule)`: Conditional evaluation - skip rule if condition is false
- `onInvalid(code)`: Set custom violation message code
- `onInvalidAndAbort(code)`: Set code and abort evaluation on failure

#### Transformation
- `contramap(function)`: Transform rule to work on different type

**Examples:**
```java
// Logical AND
NAME.notNull().and(NAME.notEmpty())

// Logical OR
EMAIL.notNull().or(PHONE.notNull())

// Aggregation
Rule.allOf(
    NAME.notNull(),
    EMAIL.notNull(),
    AGE.gte(18)
)

// Guard - only check uniqueness if name is provided
Rule.guard(
    cmd -> cmd.name() != null,
    NAME.eq(existingName).not().onInvalid("name.duplicate")
)

// Contramap - apply SessionProjection rules to nested field
CommonSessionRules.all().contramap(CreateSessionCommand::projection)
```

## API Reference

### Core Classes

#### Attr<T, A>

Type-safe attribute accessor with fluent validation API.

```java
public record Attr<T, A>(String name, Function<T, A> getter) {
    // Factory method
    public static <T, A> Attr<T, A> of(String name, Function<T, A> getter);

    // Value comparison rules
    public Rule<T> eq(A value);
    public Rule<T> eq(Attr<T, A> other);
    public Rule<T> gt(A value);
    public Rule<T> gt(Attr<T, A> other);
    public Rule<T> lt(A value);
    public Rule<T> lt(Attr<T, A> other);
    public Rule<T> gte(A value);
    public Rule<T> lte(A value);

    // Null checks
    public Rule<T> isNull();
    public Rule<T> notNull();

    // Emptiness checks
    public Rule<T> isEmpty();
    public Rule<T> notEmpty();

    // Collection operations
    public Rule<T> in(A... values);
    public Rule<T> containsAll(A... values);
}
```

#### Rule<T>

Base interface for all validation rules.

```java
public sealed interface Rule<T> permits EdgeRule, ComposedRule {
    // Evaluation
    Result evaluate(T value);

    // Logical operators
    Rule<T> and(Rule<T> other);
    Rule<T> or(Rule<T> other);
    Rule<T> not();

    // Error handling
    Rule<T> onInvalid(String code);
    Rule<T> onInvalidAndAbort(String code);

    // Transformation
    <U> Rule<U> contramap(Function<U, T> fn);

    // Factory methods
    static <T> Rule<T> allOf(Rule<T>... rules);
    static <T> Rule<T> anyOf(Rule<T>... rules);
    static <T> Rule<T> guard(Predicate<T> condition, Rule<T> rule);
}
```

#### Result

Validation result with status and violations.

```java
public record Result(
    Status status,
    Object expected,
    Object found,
    Object left,
    Object right,
    List<Violation> violations
) {
    public boolean isValid();
    public boolean isFailed();
    public boolean isSkipped();
}

public enum Status {
    MATCHED,   // Rule passed
    FAILED,    // Rule failed
    SKIPPED    // Rule was skipped (guard condition false)
}
```

#### Violation

Represents a validation failure.

```java
public record Violation(
    String code,              // Translation key for error message
    Map<String, Object> args, // Arguments for message interpolation
    Result result             // Detailed result information
) {}
```

### Annotations

#### @ValidationProjection

Marks a record for code generation.

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ValidationProjection {}
```

#### @AttrName

Customizes generated attribute names.

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.RECORD_COMPONENT)
public @interface AttrName {
    String value() default "";    // Logical name for messages
    String constant() default ""; // Generated constant name
}
```

## Testing

Franckie includes comprehensive tests covering all features:

```bash
# Run all tests
./mvnw test

# Run tests in a specific module
./mvnw test -pl validation-core
./mvnw test -pl validation-processor
./mvnw test -pl validation-sample

# Run with verbose output
./mvnw test -X
```

### Test Coverage

- **RuleValidationTest**: Core rule evaluation logic
- **ProjectionContramapTest**: Contramap transformation testing
- **ValidationProjectionProcessorTest**: Annotation processor tests
  - Basic projection generation
  - Custom logical names
  - CamelCase to CONSTANT_CASE conversion
  - Primitive type boxing
  - Constant-case shorthand
  - Explicit value and constant
- **SessionValidationTest**: Integration tests with sample domain

## Performance Considerations

- **Compile-Time Code Generation**: No runtime reflection overhead
- **Sealed Types**: Efficient switch expressions with pattern matching
- **Short-Circuit Evaluation**: `allOf` stops at first failure
- **Lazy Evaluation**: Guards skip unnecessary rule evaluation
- **Immutable Data Structures**: Thread-safe by default

## Best Practices

1. **Use `@ValidationProjection` on records**: Records provide immutability and concise syntax
2. **Compose rules for reusability**: Create small, focused rules and compose them
3. **Leverage contramap**: Reuse rules across different types
4. **Use guards for conditional logic**: Avoid unnecessary validation
5. **Provide meaningful message codes**: Use i18n keys for error messages
6. **Test validation rules**: Write unit tests for complex rule compositions
7. **Use primitive types in records**: Framework automatically boxes them for null-safety
8. **Customize names when needed**: Use `@AttrName` for domain-specific naming

## Troubleshooting

### Generated classes not found

Ensure annotation processing is enabled:
```xml
<annotationProcessorPaths>
    <path>
        <groupId>jara.sol.franckie</groupId>
        <artifactId>validation-processor</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </path>
</annotationProcessorPaths>
```

### IDE not recognizing generated classes

1. Mark `target/generated-sources/annotations` as a source folder
2. Rebuild project: `./mvnw clean compile`
3. In IntelliJ IDEA: File → Project Structure → Modules → Mark Directory as Sources Root

### Compilation errors with @AttrName

- `constant` parameter must be in CONSTANT_CASE format
- If using shorthand (CONSTANT_CASE value), it must be valid identifier
- Don't mix shorthand with explicit `constant` parameter

## Contributing

Contributions are welcome! Please ensure:

- Code follows existing patterns and conventions
- All tests pass: `./mvnw test`
- New features include appropriate tests
- Documentation is updated for API changes
- Use Java 21 features appropriately (sealed types, pattern matching, records)

## Roadmap

Future enhancements being considered:

- [ ] Additional rule types (regex, custom predicates)
- [ ] Async validation support
- [ ] Integration with Spring Boot validation
- [ ] JSON Schema generation from rules
- [ ] Kotlin support

## License

[Add your license here]

## Authors

- Raphael Jammart

## Acknowledgments

Built with:
- [JavaPoet](https://github.com/square/javapoet) - Code generation
- [Auto Service](https://github.com/google/auto) - Annotation processor registration

Documentation and project setup assisted by [Claude Code](https://claude.com/claude-code) by Anthropic.
