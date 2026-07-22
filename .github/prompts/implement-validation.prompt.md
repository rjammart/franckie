---
mode: agent
description: Implement validation for a new use case using the Franckie library
---

Implement backend validation for the use case described below using the Franckie validation library.

Follow these steps in order:

## Step 1 — Define the projection and command records

Create a `<Entity>Projection` record annotated with `@ValidationProjection` for the shared fields.
Create a `<UseCase>Command` record annotated with `@ValidationProjection` for the command-level fields (status flags, boolean context flags, etc.).
Both records must use only non-null types in their contexts; use `Optional<T>` for genuinely absent values.

```java
@ValidationProjection
public record MyEntityProjection(
    String name,
    LocalDate startDate,
    // …
) {}

@ValidationProjection
public record CreateMyEntityCommand(
    UUID commandId,
    MyEntityProjection projection,
    boolean nameAlreadyExists,
    String status
) implements Command {
    @Override public UUID getCommandId() { return commandId; }
}
```

## Step 2 — Define common projection rules

Create a `<Entity>Rules` class with `static final Rule<MyEntityProjection>` fields and an `all()` factory.
Use generated `*Attrs` constants after the annotation processor runs.

```java
import static generated.MyEntityProjectionAttrs.*;

public final class MyEntityRules {
    public static final Rule<MyEntityProjection> NAME_REQUIRED =
        NAME.notNull().onInvalid("entity.name.required");

    public static final Rule<MyEntityProjection> END_DATE_AFTER_START =
        END_DATE.gt(START_DATE).onInvalid("entity.endDate.must.be.after.start");

    public static Rule<MyEntityProjection> all() {
        return Rule.allOf(NAME_REQUIRED, END_DATE_AFTER_START /*, … */);
    }
}
```

## Step 3 — Define use-case-specific rules

Create a `<UseCase>Rules` class that combines context-specific rules with common rules via `contramap`.

```java
public final class CreateMyEntityRules {
    static final Rule<CreateMyEntityCommand> STATUS_CHECK =
        CreateMyEntityCommandAttrs.STATUS.eq("PENDING")
            .onInvalidAndAbort("entity.status.must.be.pending");

    static final Rule<CreateMyEntityCommand> NAME_UNIQUE =
        CreateMyEntityCommandAttrs.NAME_ALREADY_EXISTS.eq(false)
            .onInvalid("entity.name.must.be.unique");

    public static Rule<CreateMyEntityCommand> all() {
        return Rule.allOf(
            STATUS_CHECK,
            NAME_UNIQUE,
            MyEntityRules.all().contramap(CreateMyEntityCommand::projection)
        );
    }
}
```

## Step 4 — Handle conditional and null-safe rules

Choose the right operator based on whether the left-side failure is acceptable:

**`guard()`** — left failing is OK (field is optional, null is allowed):
```java
// targetId required only when type is "session" or "activity"; otherwise silently skipped
TYPE.eq("session").or(TYPE.eq("activity"))
    .guard(TARGET_ID.notNull().onInvalid("entity.targetId.required"))

// if capacity is absent, skip; if present, must be positive
CAPACITY.notNull().guard(CAPACITY.gt(0).onInvalid("entity.capacity.positive"))
```

**`andThen()`** — left failing is a violation AND right depends on left passing (avoids NPE):
```java
// capacity is required; if present it must also be positive
CAPACITY.notNull().onInvalid("entity.capacity.required")
        .andThen(CAPACITY.gt(0).onInvalid("entity.capacity.positive"))

// startDate is required; if present it must be before endDate
START_DATE.notNull().onInvalid("entity.startDate.required")
          .andThen(START_DATE.lt(END_DATE).onInvalid("entity.startDate.before.endDate"))
```

Never use `and()` when the right side would access a potentially null field — use `andThen()` instead.

## Step 5 — Handle collections with two-pass validation

If the command contains a list of items:

```java
List<Violation> all = new ArrayList<>();
// Pass 1: validate each item
all.addAll(CollectionValidator.validateEach(command.items(), ItemRules.all()));
// Pass 2: validate command context
all.addAll(Rule.validate(command, contextOnly(existingData, () -> externalService.fetch())));
CoreValidation.CoreValidators.decideOn(all);
```

## Step 6 — Inject external context lazily via Supplier

For rules that need DB or service data, pass a `Supplier<T>`:

```java
public static Rule<MyCommand> itemsAllowed(Supplier<List<UUID>> allowedIdsSupplier) {
    return ITEMS_ATTR.satisfies(
        items -> {
            if (items == null) return true;
            List<UUID> allowed = allowedIdsSupplier.get();
            return items.stream().map(Item::id).allMatch(allowed::contains);
        },
        "All items must be allowed"
    ).onInvalid("command.items.notAllowed");
}
```

## Step 7 — Write tests

Test file naming: `<UseCase>ValidationTest.java`
Method naming: `given__<context>__when__validate__then__<outcome>()`

Cover:
- Happy path (no violations)
- Each violation type individually
- Abort propagation (`onInvalidAndAbort`)
- `guard()` skip: optional field absent → no violation
- `andThen()` short-circuit: required field null → left violation only, right skipped
- Two-pass collection validation if applicable

---

**Use case to implement:**

${input:useCaseDescription:Describe the entity, the use case (create/update/delete), the fields, and the business rules (required fields, uniqueness, status constraints, conditional rules, collection constraints…)}
