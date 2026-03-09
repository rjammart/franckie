# Rule Serializability Guide

This document categorizes validation rules by their serializability for persistence and logging.

## ✅ Serializable Rules (Safe for Persistence)

These rules contain only **data** (values, strings, field names) and can be serialized to JSON, stored in databases, or transmitted between services.

### Comparison Rules
- `eq(value)` - Equality check
- `eq(Attr)` - Equality between two attributes
- `notEq(value)` - Inequality check
- `notEq(Attr)` - Inequality between two attributes
- `gt(value)` - Greater than
- `gt(Attr)` - Greater than another attribute
- `lt(value)` - Less than
- `lt(Attr)` - Less than another attribute
- `gte(value)` - Greater than or equal (if implemented)
- `lte(value)` - Less than or equal (if implemented)

### Null/Empty Checks
- `isNull()` - Null check
- `notNull()` - Not null check
- `isEmpty()` - Empty Optional check
- `isNotEmpty()` - Not empty Optional check
- `isEmptyCollection()` - Empty collection check
- `isNotEmptyCollection()` - Not empty collection check

### Collection Rules
- `in(values...)` - Value is in allowed set
- `containsAll(List)` - Collection contains all elements
- `containsAll(Attr)` - Collection contains all from another attribute
- `isContainIn(Attr)` - Value is contained in another attribute's list

### String Pattern Rules (Serializable)
- `matches(String pattern)` - **Serializable** - pattern is stored as String
- `matches(Pattern pattern)` - **Serializable** - pattern.pattern() gives String

### Range Rules (Serializable)
- `between(min, max)` - **Serializable** - min/max are stored values

### Collection Size Rules (Serializable)
- `hasSize(int)` - **Serializable** - size is stored as int
- `hasSizeAtLeast(int)` - **Serializable** - minimum is stored as int
- `hasSizeAtMost(int)` - **Serializable** - maximum is stored as int

### Rule Composition (Serializable if Inner Rules Are)
- `allOf(rules...)` - All rules must pass
- `anyOf(rules...)` - Any rule must pass
- `and(rule)` - Logical AND
- `or(rule)` - Logical OR
- `not(rule)` - Logical negation
- `guard(precondition, rule)` - Conditional validation
- `onInvalid(code)` - Custom error message
- `onInvalidAndAbort(code)` - Custom error with abort

**Note:** Composition rules are serializable if all their inner rules are serializable.

---

## ❌ Non-Serializable Rules (Runtime Only)

These rules contain **functions/lambdas** that cannot be serialized. Use for runtime validation only.

### Custom Predicate Rules
- `satisfies(Predicate, description)` - **NOT serializable** - contains lambda
- `satisfies(Predicate)` - **NOT serializable** - contains lambda
- `Rule.satisfies(Predicate, description)` - **NOT serializable** - object-level lambda
- `Rule.satisfies(Predicate)` - **NOT serializable** - object-level lambda

### Projection Rules
- `contramap(Function)` - **NOT serializable** - contains mapping function

**Why not serializable?**
- Java lambdas and `Predicate`/`Function` interfaces are not inherently serializable
- Even with serializable lambdas, deserializing code is a security risk
- Cannot guarantee the lambda code exists in the deserializing environment

---

## Serialization Strategy

### For Validation Results (Already Implemented)

Validation **results** are always serializable via `LoggedViolation`:
```java
record LoggedViolation(
    String code,              // Translation key
    Map<String, String> arguments,  // Placeholder arguments
    boolean matched,          // Did rule pass?
    String expected,          // Human-readable expectation
    String found,             // Human-readable actual value
    Optional<String> left,    // Left operand (for logging)
    Optional<String> right    // Right operand (for logging)
)
```

**Even custom predicates** produce serializable violations because the `description` field is logged:
```java
AGE.satisfies(age -> age >= 18 && age <= 120, "age between 18 and 120")
// Produces: "age satisfies: age between 18 and 120"
```

### For Rule Definitions (Future Work)

If you need to persist/transmit **rule definitions** themselves:

1. **Use only serializable rules** from the ✅ section above
2. Serialize to a DTO representation (field names + values)
3. Deserialize by resolving field names back to `Attr` constants

Example DTO structure:
```java
sealed interface RuleDTO {
    record EqualsDTO(String fieldName, Object value) {}
    record GreaterThanDTO(String fieldName, Object value) {}
    record RegexDTO(String fieldName, String pattern) {}
    record BetweenDTO(String fieldName, Object min, Object max) {}
    record AllOfDTO(List<RuleDTO> rules) {}
    // ...
}
```

---

## Best Practices

### ✅ Do: Use Built-in Validators for Persistence

```java
// Email validation - serializable
Rule<User> emailValid = EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

// Age range - serializable
Rule<User> ageValid = AGE.between(18, 120);

// Collection size - serializable
Rule<Product> tagsValid = TAGS.hasSizeAtLeast(1).and(TAGS.hasSizeAtMost(5));
```

### ❌ Don't: Use Custom Predicates for Persisted Rules

```java
// Runtime only - cannot persist this rule definition
Rule<User> customCheck = AGE.satisfies(age -> {
    // Complex business logic here
    return someComplexValidation(age);
}, "complex age validation");
```

### ✅ Do: Use Custom Predicates for Complex Runtime Validation

Custom predicates are **perfect for**:
- Complex business logic that changes frequently
- Integration with external services
- Validation requiring non-serializable resources
- Temporary or test-specific validation

```java
// Fine for runtime - just don't try to persist this rule
Rule<CreditCard> luhnValid = CARD_NUMBER.satisfies(
    this::validateLuhnChecksum,
    "valid credit card number"
);
```

### ✅ Do: Separate Persisted Rules from Runtime Rules

```java
public class SessionRules {
    // Persisted rules - serializable
    public static final Rule<SessionProjection> PERSISTED = Rule.allOf(
        NAME.notNull(),
        START_DATE.lt(END_DATE),
        CAPACITY.between(1, 500)
    );

    // Runtime-only rules - may include custom predicates
    public static Rule<SessionProjection> withCustomValidation() {
        return PERSISTED.and(
            NAME.satisfies(name -> !prohibitedNames.contains(name), "allowed name")
        );
    }
}
```

---

## Summary Table

| Rule Type | Serializable? | Example | Use Case |
|-----------|---------------|---------|----------|
| Comparison (`eq`, `gt`, `lt`) | ✅ Yes | `AGE.gt(18)` | Persistent rules |
| Null checks | ✅ Yes | `NAME.notNull()` | Persistent rules |
| In/Contains | ✅ Yes | `STATUS.in("A", "B")` | Persistent rules |
| Regex | ✅ Yes | `EMAIL.matches("...")` | Persistent rules |
| Range | ✅ Yes | `AGE.between(18, 65)` | Persistent rules |
| Collection size | ✅ Yes | `TAGS.hasSize(3)` | Persistent rules |
| Composition | ✅ If inner rules are | `allOf(...), or(...)` | Depends on content |
| Custom predicate | ❌ No | `satisfies(x -> ...)` | Runtime only |
| Contramap | ❌ No | `contramap(Cmd::proj)` | Runtime only |

---

## Current Status

- ✅ Validation **results** are fully serializable (implemented)
- ✅ Most rules are serializable in principle
- ⚠️ Rule **definition** serialization not yet implemented (future work)
- ⚠️ Custom predicates are explicitly not serializable (by design)

## Future Work

1. Implement `RuleDTO` for serializing rule definitions
2. Create serialization/deserialization helpers
3. Consider `@ProjectTo` annotation for serializable contramap alternatives
4. Add validation to detect non-serializable rules at compile time
