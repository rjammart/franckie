# Custom Predicate Support - Implementation Proposal

**Author:** Raphael Jammart
**Date:** March 9, 2026
**Status:** Proposal
**Related:** Roadmap item "Additional rule types (regex, custom predicates)"

## Executive Summary

This proposal introduces **custom predicate support** to the Franckie validation framework, enabling developers to define arbitrary validation logic using Java `Predicate` functions while maintaining type safety, composability, and integration with the existing fluent API.

## Motivation

While Franckie provides comprehensive built-in rules (`eq`, `gt`, `lt`, `in`, etc.), real-world validation often requires:

1. **Domain-Specific Logic**: Business rules that don't fit standard comparisons
2. **Complex Conditions**: Multi-field logic beyond simple attribute comparisons
3. **External Validation**: Integration with external services or databases
4. **Regex Patterns**: String validation using regular expressions
5. **Custom Algorithms**: Specialized validation logic (e.g., checksum validation, credit card validation)

**Current Workaround:**
```java
// Users must combine multiple built-in rules or create wrapper validation logic
Rule<User> ageVerification = Rule.guard(
    AGE.gte(18),
    EMAIL.notNull() // Limited expressiveness
);
```

**Desired API:**
```java
// Direct predicate-based validation with clear intent
Rule<User> ageVerification = AGE.satisfies(
    age -> age >= 18 && age <= 120,
    "user.age.invalid"
);
```

## Design Goals

1. **Type Safety**: Maintain compile-time type checking
2. **Fluent Integration**: Seamless integration with existing `Attr` API
3. **Composability**: Work with `and()`, `or()`, `contramap()`, etc.
4. **Meaningful Errors**: Provide useful violation messages
5. **Performance**: No reflection overhead
6. **Backward Compatibility**: Don't break existing code
7. **Simplicity**: Easy to learn and use

## Proposed Solution

### Architecture

Add a new `EdgeRule` type for custom predicates:

```java
sealed interface EdgeRule<T> extends Rule<T> {
    // ... existing rules ...

    /**
     * Custom predicate-based validation
     * @param <T> Target type
     * @param <A> Attribute type
     */
    record Predicate<T, A>(
        Attr<T, A> field,
        java.util.function.Predicate<A> predicate,
        String description  // Human-readable description for error messages
    ) implements EdgeRule<T> {}
}
```

### API Design - Three Approaches

#### Option 1: Attr-Based (Recommended)

Add methods to the `Attr` class:

```java
public record Attr<T, A>(String name, Function<T, A> getter) {
    // ... existing methods ...

    /**
     * Validates attribute value using custom predicate
     * @param predicate Validation predicate
     * @param description Human-readable description (used for default error message)
     * @return Rule for this attribute
     */
    public Rule<T> satisfies(
        java.util.function.Predicate<A> predicate,
        String description
    ) {
        return new EdgeRule.Predicate<>(this, predicate, description);
    }

    /**
     * Validates attribute value using custom predicate with default description
     * @param predicate Validation predicate
     * @return Rule for this attribute
     */
    public Rule<T> satisfies(java.util.function.Predicate<A> predicate) {
        return satisfies(predicate, "custom predicate");
    }

    /**
     * Validates string attribute matches regex pattern
     * @param pattern Regular expression pattern
     * @return Rule for this attribute
     */
    public Rule<T> matches(String pattern) {
        @SuppressWarnings("unchecked")
        Attr<T, String> self = (Attr<T, String>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.matches(pattern),
            "matches pattern: " + pattern
        );
    }
}
```

#### Option 2: Rule Static Factory Methods

Add factory methods to `Rule` interface:

```java
public sealed interface Rule<T> permits ComposedRule, EdgeRule, ... {
    // ... existing methods ...

    /**
     * Creates a custom predicate rule for an attribute
     */
    static <T, A> Rule<T> satisfies(
        Attr<T, A> field,
        java.util.function.Predicate<A> predicate,
        String description
    ) {
        return new EdgeRule.Predicate<>(field, predicate, description);
    }

    /**
     * Creates a regex matching rule for string attributes
     */
    static <T> Rule<T> matches(Attr<T, String> field, String pattern) {
        return new EdgeRule.Predicate<>(
            field,
            value -> value != null && value.matches(pattern),
            "matches pattern: " + pattern
        );
    }
}
```

#### Option 3: Hybrid Approach

Combine both for maximum flexibility:
- `Attr` methods for fluent, field-focused usage
- `Rule` static methods for standalone rules

**Recommendation:** **Option 1 + Option 3** (Attr methods + Rule factories) provides the best developer experience.

## Implementation Details

### 1. EdgeRule.Predicate Record

```java
sealed interface EdgeRule<T> extends Rule<T> {
    // ... existing rules ...

    /**
     * Custom predicate-based validation rule
     *
     * @param <T> Target object type
     * @param <A> Attribute type being validated
     * @param field The attribute accessor
     * @param predicate The validation predicate
     * @param description Human-readable description of the validation
     */
    record Predicate<T, A>(
        Attr<T, A> field,
        java.util.function.Predicate<A> predicate,
        String description
    ) implements EdgeRule<T> {}
}
```

### 2. Interpretation Logic in Rule.interpret()

Add handling for the new `EdgeRule.Predicate` case:

```java
static <T> Result interpret(T target, EdgeRule<T> rule, List<Violation> violations) {
    // ... existing cases ...

    case EdgeRule.Predicate<T, ?> pred -> {
        var attr = pred.field();
        var predicate = pred.predicate();
        Object found = attr.getter().apply(target);

        @SuppressWarnings("unchecked")
        java.util.function.Predicate<Object> p =
            (java.util.function.Predicate<Object>) predicate;

        boolean res = p.test(found);

        var result = new Result(
            res,
            "%s satisfies: %s".formatted(attr.name(), pred.description()),
            "%s = %s".formatted(attr.name(), found),
            Optional.ofNullable(pred.description()),
            found != null ? Optional.of(found.toString()) : Optional.empty()
        );

        if (!res) {
            violations.add(
                Violation.of(
                    "validation.field.%s.predicate".formatted(attr.name()),
                    Rule.defaultArguments().apply(result),
                    result
                )
            );
        }

        yield result;
    }
}
```

### 3. Updated Attr Class

```java
public record Attr<T, A>(String name, Function<T, A> getter) {
    // ... existing methods ...

    /**
     * Validates using a custom predicate
     */
    public Rule<T> satisfies(
        java.util.function.Predicate<A> predicate,
        String description
    ) {
        return new EdgeRule.Predicate<>(this, predicate, description);
    }

    public Rule<T> satisfies(java.util.function.Predicate<A> predicate) {
        return satisfies(predicate, "custom predicate");
    }

    /**
     * String-specific: validates against regex pattern
     */
    public Rule<T> matches(String pattern) {
        @SuppressWarnings("unchecked")
        Attr<T, String> self = (Attr<T, String>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.matches(pattern),
            "matches /" + pattern + "/"
        );
    }

    /**
     * String-specific: validates using Pattern object (compiled regex)
     */
    public Rule<T> matches(java.util.regex.Pattern pattern) {
        @SuppressWarnings("unchecked")
        Attr<T, String> self = (Attr<T, String>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && pattern.matcher(value).matches(),
            "matches /" + pattern.pattern() + "/"
        );
    }

    /**
     * Numeric-specific: validates value is within range [min, max]
     */
    public <N extends Comparable<N>> Rule<T> between(N min, N max) {
        @SuppressWarnings("unchecked")
        Attr<T, N> self = (Attr<T, N>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null &&
                     value.compareTo(min) >= 0 &&
                     value.compareTo(max) <= 0,
            "between %s and %s".formatted(min, max)
        );
    }

    /**
     * Collection-specific: validates collection size
     */
    public <E> Rule<T> hasSize(int expectedSize) {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.size() == expectedSize,
            "has size " + expectedSize
        );
    }

    /**
     * Collection-specific: validates minimum size
     */
    public <E> Rule<T> hasSizeAtLeast(int minSize) {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.size() >= minSize,
            "has at least %d elements".formatted(minSize)
        );
    }
}
```

## Usage Examples

### Example 1: Simple Predicate Validation

```java
@ValidationProjection
public record User(
    String username,
    String email,
    Integer age
) {}
```

```java
import static UserAttrs.*;

public class UserRules {
    // Regex validation for email
    public static final Rule<User> EMAIL_VALID =
        EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
            .onInvalid("user.email.invalid");

    // Custom age validation
    public static final Rule<User> AGE_REASONABLE =
        AGE.satisfies(
            age -> age >= 18 && age <= 120,
            "age between 18 and 120"
        ).onInvalid("user.age.out.of.range");

    // Username validation (alphanumeric, 3-20 chars)
    public static final Rule<User> USERNAME_FORMAT =
        USERNAME.matches("^[a-zA-Z0-9]{3,20}$")
            .onInvalid("user.username.format.invalid");

    public static Rule<User> all() {
        return Rule.allOf(
            EMAIL_VALID,
            AGE_REASONABLE,
            USERNAME_FORMAT
        );
    }
}
```

### Example 2: Complex Business Logic

```java
@ValidationProjection
public record CreditCard(
    String number,
    String cvv,
    LocalDate expirationDate
) {}
```

```java
import static CreditCardAttrs.*;

public class CreditCardRules {
    // Luhn algorithm for credit card validation
    public static final Rule<CreditCard> NUMBER_VALID_CHECKSUM =
        NUMBER.satisfies(
            CreditCardRules::isValidLuhn,
            "valid credit card number (Luhn algorithm)"
        ).onInvalid("creditcard.number.invalid");

    // CVV format
    public static final Rule<CreditCard> CVV_FORMAT =
        CVV.matches("^[0-9]{3,4}$")
            .onInvalid("creditcard.cvv.invalid");

    // Not expired
    public static final Rule<CreditCard> NOT_EXPIRED =
        EXPIRATION_DATE.satisfies(
            date -> date.isAfter(LocalDate.now()),
            "not expired"
        ).onInvalid("creditcard.expired");

    private static boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d+")) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    public static Rule<CreditCard> all() {
        return Rule.allOf(
            NUMBER_VALID_CHECKSUM,
            CVV_FORMAT,
            NOT_EXPIRED
        );
    }
}
```

### Example 3: Multi-Field Validation

```java
@ValidationProjection
public record DateRange(
    LocalDate startDate,
    LocalDate endDate,
    Integer durationDays
) {}
```

```java
import static DateRangeAttrs.*;

public class DateRangeRules {
    // Validate that durationDays matches actual date difference
    public static final Rule<DateRange> DURATION_MATCHES_DATES =
        DURATION_DAYS.satisfies(
            duration -> {
                // This predicate has access to the whole object through closure
                return true; // Simplified - see proper implementation below
            },
            "duration matches date difference"
        ).onInvalid("daterange.duration.mismatch");

    // Better approach using Rule-level predicate (future enhancement)
    public static final Rule<DateRange> DURATION_CONSISTENT =
        Rule.satisfies(
            range -> {
                long actualDays = ChronoUnit.DAYS.between(
                    range.startDate(),
                    range.endDate()
                );
                return actualDays == range.durationDays();
            },
            "duration matches actual date difference"
        ).onInvalid("daterange.duration.mismatch");
}
```

### Example 4: Integration with Existing Rules

```java
public class SessionRules {
    // Combining predicate rules with standard rules
    public static final Rule<SessionProjection> CAPACITY_VALID =
        CAPACITY.notNull()
            .and(CAPACITY.gt(0))
            .and(CAPACITY.satisfies(
                cap -> cap <= 1000,
                "capacity under maximum"
            ))
            .onInvalid("session.capacity.invalid");

    // Using contramap with predicates
    public static Rule<CreateSessionCommand> commandValidation() {
        Rule<SessionProjection> sessionRules = Rule.allOf(
            NAME.matches("^[A-Za-z0-9 ]+$"),
            CAPACITY_VALID,
            END_DATE.gt(START_DATE)
        );

        return sessionRules.contramap(CreateSessionCommand::projection);
    }
}
```

### Example 5: Specialized Validators

```java
public class SpecializedRules {
    // Phone number validation
    public static final Rule<User> PHONE_VALID =
        PHONE.matches("^\\+?[1-9]\\d{1,14}$")
            .onInvalid("user.phone.invalid");

    // URL validation
    public static final Rule<Website> URL_VALID =
        URL.satisfies(
            url -> {
                try {
                    new java.net.URL(url);
                    return true;
                } catch (MalformedURLException e) {
                    return false;
                }
            },
            "valid URL format"
        ).onInvalid("website.url.malformed");

    // JSON validation
    public static final Rule<Config> JSON_VALID =
        CONFIG_JSON.satisfies(
            json -> {
                try {
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(json);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            },
            "valid JSON"
        ).onInvalid("config.json.invalid");
}
```

## Advanced Features

### 1. Object-Level Predicates (Future Enhancement)

For validation requiring access to the entire object:

```java
public sealed interface Rule<T> permits ... {
    /**
     * Creates a rule based on object-level predicate
     * (not tied to a specific attribute)
     */
    static <T> Rule<T> satisfies(
        java.util.function.Predicate<T> predicate,
        String description
    ) {
        // Implementation uses a synthetic Attr or new EdgeRule variant
        Attr<T, T> identity = Attr.of("object", t -> t);
        return new EdgeRule.Predicate<>(identity, predicate, description);
    }
}
```

**Usage:**
```java
Rule<DateRange> consistentDates = Rule.satisfies(
    range -> ChronoUnit.DAYS.between(
        range.startDate(),
        range.endDate()
    ) == range.durationDays(),
    "date range duration is consistent"
).onInvalid("daterange.inconsistent");
```

### 2. Predicate Combinators

```java
public class PredicateUtils {
    public static <T> java.util.function.Predicate<T> and(
        java.util.function.Predicate<T>... predicates
    ) {
        return value -> Arrays.stream(predicates).allMatch(p -> p.test(value));
    }

    public static <T> java.util.function.Predicate<T> or(
        java.util.function.Predicate<T>... predicates
    ) {
        return value -> Arrays.stream(predicates).anyMatch(p -> p.test(value));
    }
}
```

### 3. Reusable Predicate Library

```java
public class CommonPredicates {
    // Email validation
    public static final java.util.function.Predicate<String> EMAIL =
        s -> s != null && s.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // Non-blank string
    public static final java.util.function.Predicate<String> NON_BLANK =
        s -> s != null && !s.isBlank();

    // Positive number
    public static final java.util.function.Predicate<Integer> POSITIVE =
        n -> n != null && n > 0;

    // Future date
    public static final java.util.function.Predicate<LocalDate> FUTURE =
        d -> d != null && d.isAfter(LocalDate.now());
}
```

**Usage:**
```java
Rule<User> emailValid = EMAIL.satisfies(
    CommonPredicates.EMAIL,
    "valid email format"
).onInvalid("user.email.invalid");
```

## Testing Strategy

### Unit Tests

```java
@Test
@DisplayName("given custom predicate when value satisfies then rule passes")
void givenCustomPredicate_whenValueSatisfies_thenRulePasses() {
    @ValidationProjection
    record TestRecord(Integer value) {}

    Attr<TestRecord, Integer> VALUE = Attr.of("value", TestRecord::value);

    Rule<TestRecord> evenNumber = VALUE.satisfies(
        v -> v % 2 == 0,
        "even number"
    );

    TestRecord record = new TestRecord(4);
    Result result = evenNumber.evaluate(record);

    assertTrue(result.isValid());
    assertTrue(result.violations().isEmpty());
}

@Test
@DisplayName("given custom predicate when value fails then violations added")
void givenCustomPredicate_whenValueFails_thenViolationsAdded() {
    @ValidationProjection
    record TestRecord(Integer value) {}

    Attr<TestRecord, Integer> VALUE = Attr.of("value", TestRecord::value);

    Rule<TestRecord> evenNumber = VALUE.satisfies(
        v -> v % 2 == 0,
        "even number"
    ).onInvalid("value.not.even");

    TestRecord record = new TestRecord(5);
    Result result = evenNumber.evaluate(record);

    assertFalse(result.isValid());
    assertEquals(1, result.violations().size());
    assertEquals("value.not.even", result.violations().get(0).code());
}

@Test
@DisplayName("given regex matcher when string matches then rule passes")
void givenRegexMatcher_whenStringMatches_thenRulePasses() {
    @ValidationProjection
    record TestRecord(String email) {}

    Attr<TestRecord, String> EMAIL = Attr.of("email", TestRecord::email);

    Rule<TestRecord> emailValid = EMAIL.matches("^\\w+@\\w+\\.\\w+$");

    TestRecord record = new TestRecord("test@example.com");
    Result result = emailValid.evaluate(record);

    assertTrue(result.isValid());
}

@Test
@DisplayName("given predicate rule when composed with other rules then works correctly")
void givenPredicateRule_whenComposedWithOtherRules_thenWorksCorrectly() {
    @ValidationProjection
    record TestRecord(Integer age, String name) {}

    Attr<TestRecord, Integer> AGE = Attr.of("age", TestRecord::age);
    Attr<TestRecord, String> NAME = Attr.of("name", TestRecord::name);

    Rule<TestRecord> combined = Rule.allOf(
        AGE.satisfies(a -> a >= 18, "adult"),
        AGE.lt(100),
        NAME.notNull(),
        NAME.matches("^[A-Z][a-z]+$")
    );

    TestRecord valid = new TestRecord(25, "John");
    assertTrue(combined.evaluate(valid).isValid());

    TestRecord invalid = new TestRecord(15, "john");
    assertFalse(combined.evaluate(invalid).isValid());
}
```

### Integration Tests

```java
@Test
@DisplayName("given complex validation scenario with predicates")
void givenComplexValidationScenario_withPredicates() {
    CreateSessionCommand command = new CreateSessionCommand(
        UUID.randomUUID(),
        new SessionProjection(
            "Advanced Java",
            LocalDate.now(),
            LocalDate.now().plusDays(7),
            25,
            UUID.randomUUID()
        ),
        false,
        "PENDING"
    );

    Rule<CreateSessionCommand> rules = Rule.allOf(
        // Predicate-based rules
        CREATE_SESSION_STATUS.matches("^(PENDING|ACTIVE|COMPLETED)$"),

        // Composed rules
        SESSION_NAME_ALREADY_EXISTS.eq(false),

        // Contramap with predicates
        CommonSessionRules.all().contramap(CreateSessionCommand::projection)
    );

    Result result = rules.evaluate(command);
    assertTrue(result.isValid());
}
```

## Migration Path

### Phase 1: Core Implementation
1. Add `EdgeRule.Predicate` record
2. Implement `interpret()` case for predicates
3. Add `satisfies()` methods to `Attr`
4. Update tests

### Phase 2: Convenience Methods
1. Add `matches()` for regex
2. Add `between()` for ranges
3. Add collection size validators
4. Create `CommonPredicates` utility class

### Phase 3: Advanced Features
1. Add object-level `Rule.satisfies()`
2. Create predicate combinator utilities
3. Enhanced error message formatting
4. Performance optimizations

## Performance Considerations

1. **No Reflection**: Predicates are lambda expressions, compiled to bytecode
2. **Short-Circuit Evaluation**: Works naturally with `allOf()` and `anyOf()`
3. **Minimal Overhead**: Single predicate test per evaluation
4. **Pattern Compilation**: For `matches()`, consider caching compiled `Pattern` objects

**Optimization for regex:**
```java
// Instead of compiling pattern on every evaluation
public Rule<T> matches(String pattern) {
    java.util.regex.Pattern compiled = java.util.regex.Pattern.compile(pattern);
    return matches(compiled);
}

public Rule<T> matches(java.util.regex.Pattern pattern) {
    @SuppressWarnings("unchecked")
    Attr<T, String> self = (Attr<T, String>) this;
    return new EdgeRule.Predicate<>(
        self,
        value -> value != null && pattern.matcher(value).matches(),
        "matches /" + pattern.pattern() + "/"
    );
}
```

## Backward Compatibility

✅ **Fully backward compatible**
- No changes to existing API
- All existing rules continue to work
- New functionality is additive only

## Documentation Updates

1. Update README.md with predicate examples
2. Add CUSTOM_PREDICATES_GUIDE.md with comprehensive examples
3. Update API reference section
4. Add to "Advanced Features" section

## Alternative Approaches Considered

### 1. Function-Based Instead of Predicate
**Rejected**: `Predicate` is more semantically appropriate for validation

### 2. Separate PredicateRule class
**Rejected**: Increases API surface, less fluent

### 3. Annotation-based (@ValidateWith)
**Rejected**: Adds reflection, less flexible, not aligned with framework philosophy

## Open Questions

1. **Should we support async predicates?**
   - Concern: Complicates API, adds CompletableFuture overhead
   - Decision: Defer to future async validation feature

2. **Should predicates have access to validation context?**
   - Use case: Predicates that need external state
   - Decision: Keep simple for now, use closure for context

3. **Should we provide predicate negation?**
   ```java
   AGE.doesNotSatisfy(predicate)  // or
   AGE.satisfies(predicate).not()  // Already works
   ```
   - Decision: Use `.not()` on rules for consistency

## Recommendation

**Proceed with implementation using Option 1 (Attr-based) + Option 3 (Hybrid)**

**Priority order:**
1. Core `EdgeRule.Predicate` + `interpret()` handling
2. `Attr.satisfies()` methods
3. `Attr.matches()` for regex
4. `Attr.between()` for ranges
5. Collection validators (`hasSize`, etc.)
6. `CommonPredicates` utility class
7. Object-level `Rule.satisfies()` (Phase 2)

## Conclusion

Custom predicate support is a natural and necessary extension of the Franckie validation framework. The proposed design:

- ✅ Maintains type safety
- ✅ Integrates seamlessly with existing API
- ✅ Provides excellent developer experience
- ✅ Enables powerful custom validation logic
- ✅ Preserves backward compatibility
- ✅ Follows framework design principles

This feature will significantly enhance Franckie's expressiveness while staying true to its core philosophy of type-safe, composable validation.
