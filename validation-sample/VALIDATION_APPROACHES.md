# Validation Approaches for Collections in Franckie

This document compares three approaches for validating commands that contain collections of items (e.g., `List<CapacityReservation>`).

## The Problem

When validating a command with a collection of items:
```java
record AddCapacityReservationsCommand(
    UUID commandId,
    UUID dropId,
    List<CapacityReservation> newReservations  // How to validate each item?
) implements Command
```

We want to:
1. Validate each `CapacityReservation` individually with detailed violations
2. Validate command-level concerns (conflicts, authorization)
3. Keep the command clean (no embedded context)

## Approach 1: Single-Pass with `.satisfies()` ⚠️

**Pattern:**
```java
public static Rule<AddCapacityReservationsCommand> allSinglePass(
    Drop existingDrop,
    Supplier<List<UUID>> allowedActivityIds
) {
    return basicValidation()
        .and(eachReservationValidSimple())  // ⚠️ Loses detail
        .and(noConflictWithExisting(existingDrop))
        .and(activitiesAreAllowed(allowedActivityIds));
}

private static Rule<Command> eachReservationValidSimple() {
    return NEW_RESERVATIONS.satisfies(
        reservations -> reservations.stream()
            .allMatch(cr -> Rule.validate(cr, CRRules.all()).isEmpty()),
        "One or more reservations are invalid"
    ).onInvalid("command.newReservations.containsInvalidItems");
}
```

**Pros:**
- ✅ Simple, declarative
- ✅ All validation in one place
- ✅ Single API call

**Cons:**
- ❌ Only ONE violation if any item fails (no detail on which item or which field)
- ❌ Cannot use contramap for collection items
- ❌ User gets generic "some reservation is invalid" message

**Use when:**
- You don't need detailed per-item error messages
- Simple yes/no validation is sufficient

---

## Approach 2: Two-Pass Validation (Recommended) ✅

**Pattern:**
```java
// Rule factory validates ONLY command-level concerns
public static Rule<Command> allContextOnly(
    Drop existingDrop,
    Supplier<List<UUID>> allowedActivityIds
) {
    return basicValidation()
        .and(noConflictWithExisting(existingDrop))
        .and(activitiesAreAllowed(allowedActivityIds));
}

// Helper for two-pass validation
public static List<Violation> validateTwoPass(
    AddCapacityReservationsCommand command,
    Drop existingDrop,
    Supplier<List<UUID>> allowedActivityIds
) {
    List<Violation> allViolations = new ArrayList<>();

    // Pass 1: Validate each CR individually
    for (CapacityReservation cr : command.newReservations()) {
        allViolations.addAll(Rule.validate(cr, CRRules.all()));
    }

    // Pass 2: Validate command context
    allViolations.addAll(Rule.validate(
        command,
        allContextOnly(existingDrop, allowedActivityIds)
    ));

    return allViolations;
}
```

**Usage in application:**
```java
// Option A: Use the helper
List<Violation> violations = AddCRRules.validateTwoPass(
    command,
    existingDrop,
    () -> activityService.getAllowedActivities(dropId)
);

// Option B: Manual orchestration
List<Violation> violations = new ArrayList<>();

// Pass 1: Individual items
for (CR cr : command.newReservations()) {
    violations.addAll(Rule.validate(cr, CRRules.all()));
}

// Pass 2: Command context (can short-circuit if desired)
if (violations.isEmpty()) {
    violations.addAll(Rule.validate(command, AddCRRules.all(drop, allowedActivities)));
}

CoreValidation.decideOn(violations);
```

**Pros:**
- ✅ Detailed violations per item (e.g., "CR[0].type.invalid", "CR[1].targetId.required")
- ✅ Clear separation: item validation vs. command validation
- ✅ Can short-circuit after Pass 1 if desired
- ✅ Reusable CR rules
- ✅ Full control over violation collection

**Cons:**
- ⚠️ Requires explicit orchestration (two API calls)
- ⚠️ Not purely declarative (imperative loop)

**Use when:**
- You need detailed error messages (production-quality UX)
- Collection size is dynamic
- You want to reuse item-level rules

---

## Approach 3: Contramap (Limitations) ⚠️

**Why it doesn't work well:**

Contramap is designed for **single projections**, not collections:

```java
// ✅ Works - single projection
CommonSessionRules.all().contramap(CreateSessionCommand::projection)

// ❌ Doesn't work - collection
CRRules.all().contramap(AddCRCommand::newReservations)  // Returns List<CR>!
```

**Workaround for fixed-size collections:**
```java
// Only works if you know the exact size
Rule<Command> validateFirstCR = CRRules.all().contramap(cmd -> cmd.newReservations().get(0));
Rule<Command> validateSecondCR = CRRules.all().contramap(cmd -> cmd.newReservations().get(1));

return validateFirstCR.and(validateSecondCR);
```

**Why this is impractical:**
- ❌ Requires knowing collection size at compile time
- ❌ Doesn't work for dynamic lists
- ❌ Verbose for large collections

**Verdict:**
Contramap is not the right tool for collection validation in Franckie.

---

## Comparison Matrix

| Aspect | Single-Pass | Two-Pass | Contramap |
|--------|-------------|----------|-----------|
| **Detailed violations** | ❌ One generic | ✅ Per-item detail | ❌ Not applicable |
| **Declarative style** | ✅ Yes | ⚠️ Partial | ✅ Yes |
| **Code simplicity** | ✅ Simple | ⚠️ Orchestration needed | ❌ Verbose |
| **Dynamic collections** | ✅ Yes | ✅ Yes | ❌ No |
| **Reusable item rules** | ✅ Yes | ✅ Yes | ⚠️ Limited |
| **User experience** | ⚠️ Generic errors | ✅ Specific errors | N/A |
| **Performance** | ✅ One pass | ⚠️ Two passes | N/A |

---

## Recommendation

**Use Two-Pass Validation** for production code:

```java
public class CapacityReservationService {

    public void addReservations(AddCRCommand command) {
        // Fetch context
        Drop existingDrop = dropRepository.findById(command.dropId());
        Supplier<List<UUID>> allowedActivities =
            () -> activityService.getAllowedActivities(command.dropId());

        // Validate (two-pass)
        List<Violation> violations = AddCRRules.validateTwoPass(
            command,
            existingDrop,
            allowedActivities
        );

        // Decide
        CoreValidation.decideOn(violations);

        // Execute if valid
        // ...
    }
}
```

**Benefits:**
1. Users get specific error messages: "Reservation #2: Target ID is required for session type"
2. Clear separation of concerns: item validation vs. contextual validation
3. Reusable rules across different commands
4. Type-safe and testable

---

## Future Enhancement Idea

If Franckie adds native collection validation support, it could look like:

```java
// Hypothetical future API
Rule<Command> allItemsValid = NEW_RESERVATIONS.forEach(
    CapacityReservationRules.all(),
    "newReservations[%d]"  // Prefix for violations
);

// Would generate violations like:
// - "newReservations[0].type.invalid"
// - "newReservations[1].targetId.required"
```

Until then, two-pass validation is the best approach for detailed, user-friendly error messages.

---

## Test Examples

See `CapacityReservationValidationTest.java` for complete examples:

- **Single-pass:** `given__command_with_invalid_cr_single_pass__when__validate__then__one_violation`
- **Two-pass (helper):** `given__command_with_invalid_cr_two_pass__when__validate__then__detailed_violations`
- **Two-pass (manual):** `given__command_with_invalid_cr_manual_two_pass__when__validate__then__detailed_violations`

Run tests: `mvn test -Dtest=CapacityReservationValidationTest`
