# Validation Framework Review: Violation Collection Issue

## Problem Summary

The original design only added violations to the list when rules were explicitly wrapped with `.onInvalid()` (creating a `WithCode` wrapper). This meant:

```java
// This would NOT add violations:
Rule<Session> rule = NAME.notNull().and(START_DATE.notNull());
List<Violation> violations = Rule.validate(session, rule);
// violations would be empty even if validation failed!

// You had to do this:
Rule<Session> rule = NAME.notNull().onInvalid("name.required")
    .and(START_DATE.notNull().onInvalid("date.required"));
```

## Current Solution Issues

Your fix adds `Optional<ComposedRule.WithCode<T>> lastWithCode` parameter and checks it in every edge rule:

```java
if(!res && lastWithCode.isEmpty()){
    violations.add(Violation.of(
        "validation.rule.eq.failed",
        Rule.defaultArguments().apply(result),
        result
    ));
}
```

**Problems with this approach:**

1. **Massive Code Duplication** - This pattern is repeated in ~13 edge rule cases
2. **Parameter Threading** - `lastWithCode` must be passed through all method signatures
3. **Fragile** - Easy to forget when adding new edge rules
4. **Generic Messages** - Default codes like "validation.rule.eq.failed" aren't descriptive
5. **Commented Code** - Lines 665, 673, 675 suggest uncertainty about the approach
6. **Complexity** - The Guard case creates a fake `WithCode` wrapper (line 693) which is a smell

## Cleaner Alternatives

### Option 1: Always Add Violations (Recommended - Simplest)

**Principle:** Edge rules always add violations when they fail. The `WithCode` wrapper replaces the violation instead of being the only way to add one.

**Changes needed:**

1. Remove `lastWithCode` parameter entirely
2. Add violations directly in `interpret()` for all failures
3. Modify `WithCode` handling to remove the default violation and add the custom one

**Implementation sketch:**

```java
// In interpret() - no lastWithCode parameter
case EdgeRule.Equals<T, ?> eq -> {
    var attr = eq.field();
    var value = eq.value();
    Object found = attr.getter().apply(target);
    boolean res = Objects.equals(found, value);

    var result = new Result(
        res,
        "%s=%s".formatted(attr.name(), value),
        "%s=%s".formatted(attr.name(), found),
        value != null ? Optional.of(value.toString()) : Optional.empty(),
        found != null ? Optional.of(found.toString()) : Optional.empty()
    );

    // ALWAYS add violation on failure with default message
    if(!res) {
        violations.add(Violation.of(
            "validation.%s.eq".formatted(attr.name()), // More specific default
            Rule.defaultArguments().apply(result),
            result
        ));
    }
    yield result;
}

// In collect() for WithCode
case ComposedRule.WithCode<T> wc -> {
    int sizeBefore = violations.size();
    Result inner = collect(target, wc.rule(), violations);

    if (inner.failed()) {
        // Remove default violations added by inner rules
        int sizeAfter = violations.size();
        if (sizeAfter > sizeBefore) {
            violations.subList(sizeBefore, sizeAfter).clear();
        }

        // Add custom violation
        var args = wc.argumentsFunction().apply(inner);
        violations.add(Violation.of(
            wc.messageCode(),
            args,
            inner
        ));

        if (wc.abortOnFail()) {
            throw new AbortValidation();
        }
    }
    yield inner;
}
```

**Pros:**
- No parameter threading
- No duplication
- Impossible to forget - failures always create violations
- `WithCode` becomes an override mechanism, not a requirement
- Better default messages possible (include field name)

**Cons:**
- Removing violations from list is a bit awkward
- May have performance overhead for large rule trees with many WithCode wrappers

---

### Option 2: Centralized Violation Helper (Middle Ground)

Extract violation creation to a single helper method:

```java
private static <T> Result interpretWithViolation(
    EdgeRule<T> rule,
    Result result,
    List<Violation> violations,
    Optional<ComposedRule.WithCode<T>> lastWithCode
) {
    if (result.failed() && lastWithCode.isEmpty()) {
        String code = generateDefaultCode(rule);  // Smart code generation
        violations.add(Violation.of(
            code,
            Rule.defaultArguments().apply(result),
            result
        ));
    }
    return result;
}

// Then each edge rule becomes:
case EdgeRule.Equals<T, ?> eq -> {
    // ... evaluation logic ...
    var result = new Result(...);
    yield interpretWithViolation(eq, result, violations, lastWithCode);
}
```

**Pros:**
- Single place for violation creation logic
- Less duplication than current approach
- Can generate smarter default codes based on rule type

**Cons:**
- Still requires `lastWithCode` threading
- Extra method call overhead
- Doesn't fundamentally solve the design issue

---

### Option 3: Two-Phase Evaluation (Most Flexible)

Separate evaluation from violation collection:

```java
// Phase 1: Evaluate and collect all results
record EvaluationResult<T>(Result result, Rule<T> rule, T target) {}

static <T> List<EvaluationResult<T>> evaluate(T target, Rule<T> rule) {
    // Collect all results without creating violations
}

// Phase 2: Convert to violations
static <T> List<Violation> createViolations(List<EvaluationResult<T>> results) {
    // Post-process: apply WithCode overrides, filter, etc.
}

// Public API remains the same
static <T> List<Violation> validate(T target, Rule<T> rule) {
    List<EvaluationResult<T>> results = evaluate(target, rule);
    return createViolations(results);
}
```

**Pros:**
- Clean separation of concerns
- Enables advanced scenarios (custom violation strategies, filtering, logging)
- No duplication
- Can test evaluation independently from violation creation

**Cons:**
- Most complex refactoring
- May allocate more intermediate objects
- Overkill if you don't need the flexibility

---

## Recommendation

**Go with Option 1** (Always Add Violations) because:

1. **Simplest** - Removes the `lastWithCode` parameter entirely
2. **Most intuitive** - Failed validations always produce violations
3. **Maintainable** - Can't forget to add violations for new rules
4. **Better DX** - Users don't need `.onInvalid()` for basic cases, only for custom messages

The minor awkwardness of removing default violations when WithCode is used is worth the overall simplification.

## Additional Improvements

While refactoring, consider:

1. **Smarter Default Messages**: Include field names in default codes
   ```java
   "validation.field.%s.notNull".formatted(field.name())
   // vs generic "validation.rule.notNull.failed"
   ```

2. **Remove Commented Code**: Lines 665, 673, 675 should be deleted

3. **Guard Case Cleanup**: The fake WithCode creation on line 693 is a code smell. Guards might need special handling

4. **Consider Result.failed()**: Since you added Status enum, use it consistently instead of `!res`

5. **Null Handling**: The null checks on line 272-274 are good - keep them

## Migration Path

If you choose Option 1:

1. Start by removing `lastWithCode` from `interpret()` signature
2. Update all edge rules to always add violations (remove the `if` check)
3. Modify `WithCode` handling to clear default violations
4. Remove `lastWithCode` from `collect()` signature
5. Update `collectContraMapped()` to not pass `Optional.empty()`
6. Test thoroughly - this changes behavior for existing rules without `.onInvalid()`

Would you like me to implement any of these options?
