# Attr Generation Comparison

This document compares two approaches for defining validation attributes in the Franckie validation framework:
1. **Manual Attr Definition** (CapacityReservationValidationTest)
2. **@ValidationProjection Annotation** (CapacityReservationValidationWithProjectionTest)

## Approach 1: Manual Attr Definition

### Model Definition
```java
// Plain record - no annotations
record CapacityReservation(
    String type,
    UUID targetId,
    String parachuteType,
    Integer capacity
) {}
```

### Attr Definition (Manual)
```java
// Must manually define each Attr
static final Attr<CapacityReservation, String> TYPE =
    Attr.of("type", CapacityReservation::type);

static final Attr<CapacityReservation, UUID> TARGET_ID =
    Attr.of("targetId", CapacityReservation::targetId);

static final Attr<CapacityReservation, String> PARACHUTE_TYPE =
    Attr.of("parachuteType", CapacityReservation::parachuteType);

static final Attr<CapacityReservation, Integer> CAPACITY =
    Attr.of("capacity", CapacityReservation::capacity);
```

### Usage in Rules
```java
public static final Rule<CapacityReservation> TYPE_VALID =
    TYPE.notNull()
        .and(TYPE.in("session", "activity", "all-unit"))
        .onInvalid("cr.type.invalid");
```

### Pros
- ✅ Explicit and clear
- ✅ No annotation processor required
- ✅ Works in any context (tests, main code, etc.)
- ✅ Easy to understand for newcomers

### Cons
- ❌ Verbose - must manually write each Attr
- ❌ Easy to make typos in attribute names
- ❌ Duplication between record fields and Attr definitions
- ❌ Maintenance burden when adding/removing fields

---

## Approach 2: @ValidationProjection Annotation

### Model Definition
```java
// Annotated record - processor generates Attrs
@ValidationProjection
public record CapacityReservation(
    String type,
    UUID targetId,
    String parachuteType,
    Integer capacity
) {}
```

### Generated Attrs (Automatic)
```java
// Generated automatically in sol.jara.franckie.validation.sample.generated package
public final class CapacityReservationAttrs {
    public static final Attr<CapacityReservation, String> TYPE =
        new Attr<>("type", CapacityReservation::type);

    public static final Attr<CapacityReservation, UUID> TARGET_ID =
        new Attr<>("targetId", CapacityReservation::targetId);

    public static final Attr<CapacityReservation, String> PARACHUTE_TYPE =
        new Attr<>("parachuteType", CapacityReservation::parachuteType);

    public static final Attr<CapacityReservation, Integer> CAPACITY =
        new Attr<>("capacity", CapacityReservation::capacity);
}
```

### Import and Use
```java
// Static import the generated Attrs
import static sol.jara.franckie.validation.sample.generated.CapacityReservationAttrs.*;

// Use exactly the same as manual approach
public static final Rule<CapacityReservation> TYPE_VALID =
    TYPE.notNull()
        .and(TYPE.in("session", "activity", "all-unit"))
        .onInvalid("cr.type.invalid");
```

### Pros
- ✅ Less boilerplate - just annotate the record
- ✅ No typos - generated from actual record fields
- ✅ Single source of truth (the record definition)
- ✅ Automatic updates when fields change
- ✅ Consistent naming (camelCase → UPPER_SNAKE_CASE)

### Cons
- ❌ Requires annotation processor
- ❌ Generated code not immediately visible in IDE
- ❌ Must handle import conflicts (e.g., DROP_ID in multiple classes)
- ❌ Requires public classes/records when nested in test classes

---

## Key Differences Summary

| Aspect | Manual | @ValidationProjection |
|--------|--------|----------------------|
| **Setup** | Write Attr definitions | Annotate record |
| **Lines of Code** | ~4 lines per field | 1 annotation |
| **Type Safety** | Manual typing required | Fully generated |
| **Refactoring** | Update Attr when field changes | Automatic regeneration |
| **IDE Support** | Direct code navigation | Navigate to generated sources |
| **Learning Curve** | Lower (explicit code) | Higher (generated code) |
| **Best For** | Small models, examples | Production code, large models |

---

## Import Conflict Resolution

When multiple generated Attrs classes have the same field name (e.g., `DROP_ID`):

### Problem
```java
// Both have DROP_ID!
import static ...DropAttrs.*;
import static ...AddCapacityReservationsCommandAttrs.*;

DROP_ID.notNull()  // ❌ Ambiguous!
```

### Solution 1: Selective Imports
```java
import static ...AddCapacityReservationsCommandAttrs.COMMAND_ID;
import static ...AddCapacityReservationsCommandAttrs.NEW_RESERVATIONS;
import sol.jara.franckie.validation.sample.generated.AddCapacityReservationsCommandAttrs;

// Use qualified name for conflicting field
AddCapacityReservationsCommandAttrs.DROP_ID.notNull()  // ✅ Clear
```

### Solution 2: No Static Imports
```java
import sol.jara.franckie.validation.sample.generated.AddCapacityReservationsCommandAttrs;

// Always use qualified names
AddCapacityReservationsCommandAttrs.DROP_ID.notNull()  // ✅ Verbose but clear
```

---

## Test Results

Both approaches produce identical test results:

```
CapacityReservationValidationTest (manual):           23 tests passed ✅
CapacityReservationValidationWithProjectionTest:       9 tests passed ✅
Total:                                                32 tests passed ✅
```

---

## Recommendation

- **Use Manual Definition** for:
  - Proof-of-concept code
  - Small models with few fields
  - Teaching/examples
  - When you want explicit control

- **Use @ValidationProjection** for:
  - Production code
  - Large models with many fields
  - Domain models that change frequently
  - When you want DRY (Don't Repeat Yourself)

Both approaches are valid and can coexist in the same codebase. Choose based on your team's preferences and project needs.
