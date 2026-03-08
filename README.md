# Franckie Validation Framework

A type-safe, composable validation framework for Java that leverages annotation processing and functional programming patterns to build elegant, maintainable validation rules.

## Overview

Franckie is a Java validation framework that provides:

- **Type-Safe Validation**: Compile-time checking of attribute types and validation rules
- **Composable Architecture**: Build complex validations from simple, reusable rules
- **Fluent DSL**: Readable, expressive syntax for defining validation logic
- **Code Generation**: Automatic generation of type-safe attribute accessors via annotation processing
- **Functional Composition**: Reuse validation rules across different contexts using functional patterns
- **Detailed Feedback**: Rich error reporting with expected vs. actual values
- **Internationalization**: Built-in support for translatable validation messages

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

### 2. Composable Rules

Build complex validations by composing simple rules:

```java
public class CommonSessionRules {
    public static final Rule<SessionProjection> NAME_REQUIRED =
        NAME.notNull().onInvalid("session.name.required");

    public static final Rule<SessionProjection> END_DATE_AFTER_START =
        END_DATE.gt(START_DATE).onInvalid("session.date.invalid");

    public static final Rule<SessionProjection> CAPACITY_POSITIVE =
        CAPACITY.gt(0).onInvalid("session.capacity.positive");

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
// Reuse SessionProjection rules for CreateSessionCommand
Rule<CreateSessionCommand> commandRules =
    CommonSessionRules.all().contramap(CreateSessionCommand::projection);
```

### 4. Conditional Validation with Guards

Skip validation based on preconditions:

```java
Rule<CreateSessionCommand> nameUniquenessCheck =
    Rule.guard(
        cmd -> cmd.sessionNameAlreadyExists(),
        NAME.eq(null).onInvalid("session.name.duplicate")
    );
```

### 5. Rich Error Reporting

Violations include translation keys, context, and detailed results:

```java
record Violation(String code, Map<String, Object> args, Result result) {}
record Result(Status status, Object expected, Object found, Object left, Object right) {}
```

## Architecture

Franckie is organized as a multi-module Maven project:

### Modules

- **validation-core**: Core validation interfaces, rule definitions, and evaluation engine
  - `Rule<T>`: Sealed interface for all validation rules
  - `Attr<T, A>`: Type-safe attribute accessor
  - `Violation` & `Result`: Error reporting types

- **validation-processor**: Annotation processor for code generation
  - Processes `@ValidationProjection` annotations
  - Generates `*Attrs` classes with static attribute definitions

- **validation-sample**: Example application demonstrating framework usage
  - Sample domain models and validation rules
  - Integration tests

### Design Patterns

1. **Sealed Types**: Type-safe rule hierarchy using sealed interfaces
2. **Pattern Matching**: Rule evaluation via switch expressions
3. **Fluent API**: Chainable methods for readable rule composition
4. **Functional Composition**: Contramap for rule transformation
5. **Annotation Processing**: Compile-time code generation for boilerplate elimination

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

### 1. Define a Validation Projection

```java
import jara.sol.franckie.validation.core.ValidationProjection;

@ValidationProjection
public record SessionProjection(
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Integer capacity
) {}
```

### 2. Create Validation Rules

```java
import static SessionProjectionAttrs.*;

public class SessionRules {
    public static final Rule<SessionProjection> VALID_SESSION =
        Rule.allOf(
            NAME.notNull().onInvalid("session.name.required"),
            START_DATE.notNull().onInvalid("session.startDate.required"),
            END_DATE.notNull().onInvalid("session.endDate.required"),
            END_DATE.gt(START_DATE).onInvalid("session.dates.invalid"),
            CAPACITY.gt(0).onInvalid("session.capacity.positive")
        );
}
```

### 3. Validate Objects

```java
SessionProjection session = new SessionProjection(
    "Java Advanced",
    LocalDate.of(2024, 1, 15),
    LocalDate.of(2024, 1, 10), // Invalid: end before start
    20
);

Result result = SessionRules.VALID_SESSION.evaluate(session);
List<Violation> violations = result.violations();

violations.forEach(v ->
    System.out.println("Violation: " + v.code())
);
```

## Rule Types

### Edge Rules (Atomic Comparisons)

- `eq(value)` / `eq(attr)`: Equality check
- `gt(value)` / `gt(attr)`: Greater than
- `lt(value)` / `lt(attr)`: Less than
- `gte(value)` / `gte(attr)`: Greater than or equal
- `lte(value)` / `lte(attr)`: Less than or equal
- `isNull()` / `notNull()`: Null checks
- `isEmpty()` / `notEmpty()`: Collection/String emptiness
- `in(values)`: Membership check
- `containsAll(values)`: Collection contains all

### Composed Rules

- `and(rule)`: Logical AND
- `or(rule)`: Logical OR
- `not()`: Logical negation
- `allOf(rules...)`: All rules must pass
- `anyOf(rules...)`: At least one rule must pass
- `guard(condition, rule)`: Conditional evaluation
- `contramap(function)`: Transform rule to different type
- `onInvalid(code)`: Set violation message code
- `onInvalidAndAbort(code)`: Set code and abort on failure

## Contributing

Contributions are welcome! Please ensure:

- Code follows existing patterns and conventions
- All tests pass: `./mvnw test`
- New features include appropriate tests
- Documentation is updated for API changes

## License

[Add your license here]

## Authors

- Raphael Jammart

## Acknowledgments

Built with:
- [JavaPoet](https://github.com/square/javapoet) - Code generation
- [Auto Service](https://github.com/google/auto) - Annotation processor registration

Documentation and project setup assisted by [Claude Code](https://claude.com/claude-code) by Anthropic.
