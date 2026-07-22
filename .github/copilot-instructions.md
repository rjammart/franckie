# Franckie Validation Library — Copilot Instructions

This repository contains the **Franckie** Java validation library.
Follow these patterns whenever you write or extend validation code.

---

## Core Concepts

### Rule<T>
A sealed interface with two families:
- `EdgeRule<T>` — leaf predicates (NotNull, Equals, GreaterThan, In, IsNotEmptyCollection, Predicate, …)
- `ComposedRule<T>` — structural combinators (And, Or, Not, Guard, WithCode, …)

Always create rules through `Attr` methods or `Rule.*` static factories.
Never instantiate `EdgeRule` or `ComposedRule` subtypes directly in production code.

### Attr<T, A>
Binds a field name to a getter on type `T` returning `A`.
```java
Attr<Person, String> NAME = Attr.of("name", Person::name);
```

### Violation
Produced when a rule fails. Has a `translationKey()` used for i18n.
Default key pattern: `validation.field.<fieldName>.<ruleName>` (e.g. `validation.field.name.notNull`).
Override with `.onInvalid("your.key")`.

---

## Rule API Quick Reference

### Attr methods (produce Rule<T>)
```java
attr.notNull()                         // field != null
attr.isNull()                          // field == null
attr.eq(value)                         // field.equals(value)
attr.eq(otherAttr)                     // field.equals(otherAttr field value)
attr.notEq(otherAttr)                  // field != otherAttr field value
attr.gt(value)                         // field > value  (Comparable)
attr.lt(value)                         // field < value
attr.gt(otherAttr)                     // field > otherAttr field value
attr.lt(otherAttr)                     // field < otherAttr field value
attr.in(v1, v2, v3)                    // field in allowed values
attr.between(min, max)                 // min <= field <= max (inclusive)
attr.isNotEmptyCollection()            // Collection not null and not empty
attr.isEmptyCollection()               // Collection null or empty
attr.isEmpty()                         // Optional.isEmpty()
attr.isNotEmpty()                      // Optional.isPresent()
attr.satisfies(predicate, "desc")      // custom predicate (not serializable)
attr.matches("regex")                  // String matches regex
attr.hasSize(n)                        // Collection size == n
attr.hasSizeAtLeast(n)                 // Collection size >= n
attr.hasSizeAtMost(n)                  // Collection size <= n
attr.containsAll(otherAttr)            // list contains all elements of other list
attr.isContainIn(allowedAttr)          // value is in allowedAttr's list
```

### Rule static factories
```java
Rule.allOf(r1, r2, r3)                 // all rules must pass (collects all violations)
Rule.or(r1, r2)                        // at least one must pass
Rule.not(rule)                         // negation
Rule.guard(precondition, rule)         // if precondition fails → skip silently (no violation)
Rule.andThen(required, rule)           // if required fails → keep its violation, skip rule
Rule.notNull(attr)                     // shorthand for attr.notNull()
Rule.validate(target, rule)            // returns List<Violation>
```

### Rule instance methods
```java
rule.and(other)                        // both must pass (always evaluates both sides)
rule.andThen(other)                    // short-circuit: if left fails, skip right and keep left's violation
rule.or(other)                         // at least one must pass
rule.onInvalid("key")                  // override violation key
rule.onInvalidAndAbort("key")          // override key + stop further validation on failure
rule.guard(rule)                       // this rule is precondition; if fails → silent skip
rule.contramap(fn)                     // lift Rule<B> to Rule<A> via A→B projection
```

---

## guard() vs andThen() (IMPORTANT)

These two operators both protect a right-side rule behind a left-side check, but differ on what happens when the left side fails:

| | Left fails | Left passes |
|---|---|---|
| `guard()` | **silent skip** — no violation | evaluate right normally |
| `andThen()` | **keep left's violation**, skip right | evaluate right normally |

### guard() — optional gate (null is allowed)

Use when the left side failing is acceptable (e.g. the field is optional).

```java
// If startDate is null → skip silently (null is allowed)
// If startDate is not null → must be in the future
START_DATE.notNull().guard(START_DATE.gt(LocalDate.now()).onInvalid("start.must.be.future"))

// targetId required only when type is "session" or "activity"
TYPE.eq("session").or(TYPE.eq("activity"))
    .guard(TARGET_ID.notNull().onInvalid("cr.targetId.required"))
```

### andThen() — required field + dependent constraint

Use when the left side failing must produce a violation AND you want to skip the right side (avoids NPE on null field, avoids redundant violations).

```java
// startDate is required; if not null it must also be in the future
START_DATE.notNull().onInvalid("start.required")
          .andThen(START_DATE.gt(LocalDate.now()).onInvalid("start.must.be.future"))
// startDate null  → "start.required" violation, gt skipped (no NPE)
// startDate past  → "start.must.be.future" violation
// startDate ok    → no violations
```

`and()` always evaluates both sides and will NPE if the right side accesses a null field. Use `andThen()` whenever the right rule depends on the left passing.

---

## Null Safety Contract

`Rule.validate()` requires the **target object** to be non-null.
Field values accessed via `Attr` getters CAN be null — rules must handle null explicitly.

Two patterns for null-safe field validation:

```java
// Field is OPTIONAL (null is allowed, but if present must satisfy constraint)
FIELD.notNull().guard(FIELD.gt(x).onInvalid("field.condition"))

// Field is REQUIRED (must not be null, and if not null must satisfy constraint)
FIELD.notNull().onInvalid("field.required")
     .andThen(FIELD.gt(x).onInvalid("field.condition"))
```

Never use `and()` when the right side would NPE on a null field — use `andThen()` instead.

For truly optional fields (e.g. `Integer capacity` that may not be provided), also consider wrapping in `Optional<Integer>` on the validation context record to make absence explicit at the type level.

---

## Annotation Processor — @ValidationProjection

Annotate a `record` with `@ValidationProjection` to auto-generate an `*Attrs` class:
```java
@ValidationProjection
public record SessionProjection(
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Integer capacity,
    UUID courseId
) {}
// → generates: SessionProjectionAttrs with static fields NAME, START_DATE, END_DATE, CAPACITY, COURSE_ID
```

Use `@AttrName("CUSTOM_NAME")` on a component to override the generated constant name.

Import the generated class and use directly:
```java
import static sol.jara.franckie.validation.sample.generated.SessionProjectionAttrs.*;

Rule<SessionProjection> rule = NAME.notNull().and(END_DATE.gt(START_DATE));
```

---

## Contramap Pattern

Reuse rules defined on a projection across different contexts (Create, Update, etc.):
```java
// Rules on the shared projection
public static Rule<SessionProjection> all() {
    return Rule.allOf(NAME.notNull(), START_DATE.notNull(), END_DATE.gt(START_DATE));
}

// Lift to command context via contramap
public static Rule<CreateSessionCommand> all() {
    return Rule.allOf(
        STATUS_MUST_BE_PENDING,
        NAME_MUST_BE_UNIQUE,
        CommonSessionRules.all().contramap(CreateSessionCommand::projection)
    );
}
```

---

## Violation Key Conventions

- Default keys: `validation.field.<name>.<ruleName>` — rely on defaults during development
- Override with `.onInvalid("domain.entity.constraint")` for production i18n keys
- Use `.onInvalidAndAbort("key")` when subsequent rules are meaningless if this one fails (e.g., status check that gates all further validation)

Example:
```java
STATUS.eq("PENDING").onInvalidAndAbort("session.status.must.be.pending")
// → only ONE violation returned; all remaining rules are skipped
```

---

## Two-Pass Validation for Collections

When validating a command that contains a list of items:

**Pass 1** — validate each item individually (detailed per-item violations):
```java
List<Violation> itemViolations = CollectionValidator.validateEach(
    command.newReservations(),
    CapacityReservationRules.all()
);
```

**Pass 2** — validate command-level context (conflicts, authorization):
```java
List<Violation> contextViolations = Rule.validate(
    command,
    AddCRRules.contextOnly(existingDrop, allowedActivitiesSupplier)
);
```

Combine (full validation):
```java
List<Violation> all = new ArrayList<>();
all.addAll(CollectionValidator.validateEach(command.items(), ItemRules.all()));
all.addAll(Rule.validate(command, contextOnly(existingData, supplier)));
CoreValidation.CoreValidators.decideOn(all); // throws ValidationException if non-empty
```

Fast-fail variant (stop at Pass 1 if any item invalid):
```java
List<Violation> itemViolations = CollectionValidator.validateEach(command.items(), ItemRules.all());
if (!itemViolations.isEmpty()) return itemViolations;
return Rule.validate(command, contextOnly(existingData, supplier));
```

Single-pass global variant (one violation if any item invalid, not detailed):
```java
CollectionValidator.validateListAttr(ITEMS_ATTR, ItemRules.all(), "command.items.invalid")
```

---

## Lazy Context with Supplier

Inject external data (DB, cache) lazily to avoid loading it when validation short-circuits:
```java
public static Rule<AddCRsCommand> activitiesAreAllowed(
    Supplier<List<UUID>> allowedIdsSupplier
) {
    return NEW_RESERVATIONS.satisfies(
        newCRs -> {
            if (newCRs == null) return true;
            List<UUID> allowed = allowedIdsSupplier.get(); // called only when needed
            return newCRs.stream()
                .filter(cr -> "activity".equals(cr.type()))
                .map(CR::targetId)
                .allMatch(allowed::contains);
        },
        "All activities must be allowed for this drop"
    ).onInvalid("command.activity.notAllowed");
}
```

---

## Deciding on Violations

```java
List<Violation> violations = Rule.validate(target, rules);
CoreValidation.CoreValidators.decideOn(violations);
// → returns true if empty
// → throws ValidationException (with violations list) if non-empty
```

Catch in your application layer:
```java
try {
    CoreValidation.CoreValidators.decideOn(violations);
} catch (ValidationException e) {
    List<Violation> v = e.getViolations();
    // map to API response, log, etc.
}
```

---

## File Structure Convention

```
<module>/
  src/main/java/.../
    <Entity>Projection.java           // @ValidationProjection record — shared fields
    <Command>.java                    // @ValidationProjection record — implements Command
    <Entity>Rules.java                // common projection rules (static finals + all())
    <UseCase>Rules.java               // context-specific rules using contramap
  src/test/java/.../
    <UseCase>ValidationTest.java      // given__<context>__when__validate__then__<outcome>
```

---

## Test Pattern

```java
@Test
@DisplayName("given <context> when validate then <outcome>")
void given__<context>__when__validate__then__<outcome>() {
    // Given
    var subject = new MyRecord(...);

    // When
    List<Violation> violations = Rule.validate(subject, MyRules.all());

    // Then
    assertTrue(violations.isEmpty());
    // or
    assertEquals(1, violations.size());
    assertEquals("expected.key", violations.get(0).translationKey());
    // or
    assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("some.key")));
}
```
