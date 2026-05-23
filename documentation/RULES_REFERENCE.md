# Rules Reference Guide

## Overview

This document provides a comprehensive reference for all validation rules available in the validation framework.

### **Sealed Interface Hierarchy**
- `Rule<T>` - Main sealed interface
  - `EdgeRule<T>` - Leaf validation rules (atomic operations)
  - `ComposedRule<T>` - Composite rules (combinators)

---

## **1. Edge Rules (Atomic Validators)**

### **Equality & Comparison**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `Equals` | `Rule.eq(attr, value)` | `attr.eq(value)` | Field equals value |
| `EqualsAttr` | `Rule.eq(left, right)` | N/A | Two attributes equal |
| `NotEq` (Not wrapper) | `Rule.notEq(attr, value)` | `attr.notEq(value)` | Field not equals value |
| `NotEq` (Not wrapper) | `Rule.notEq(left, right)` | N/A | Two attributes not equal |

### **Numeric Comparisons**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `GreaterThan` | `Rule.gt(attr, value)` | `attr.gt(value)` | Field > value |
| `GreaterThanAttr` | `Rule.gt(left, right)` | N/A | left attr > right attr |
| `LessThan` | `Rule.lt(attr, value)` | `attr.lt(value)` | Field < value |
| `LessThanAttr` | `Rule.lt(left, right)` | N/A | left attr < right attr |

### **Null/Empty Checks**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `IsNull` | `Rule.isNull(attr)` | `attr.isNull()` | Field is null |
| `NotNull` | `Rule.notNull(attr)` | `attr.notNull()` | Field is not null |
| `IsEmpty` | `Rule.isEmpty(attr)` | `attr.isEmpty()` | Optional is empty |
| `IsEmptyCollection` | `Rule.isEmptyCollection(attr)` | `attr.isEmptyCollection()` | Collection is empty |

### **Collection Operations**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `ContainsAll` | `Rule.containsAll(superset, subset)` | N/A | Superset contains all subset elements |
| `In` | `Rule.in(attr, collection)` | `attr.in(collection)` | Field value in allowed collection |
| `In` (varargs) | `Rule.in(attr, ...values)` | `attr.in(...values)` | Field value in allowed varargs |
| `IsContainIn` | `Rule.isContainIn(attr, listAttr)` | `attr.isContainIn(listAttr)` | Field contained in another field's list |

### **Custom Predicates** ⚠️ **NOT SERIALIZABLE**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `Predicate` | N/A | `attr.satisfies(predicate, desc)` | Custom predicate on attribute |
| Object-level | `Rule.satisfies(predicate, desc)` | N/A | Custom predicate on whole object |
| Object-level | `Rule.satisfies(predicate)` | N/A | Custom predicate (default desc) |

---

## **2. Composed Rules (Combinators)**

### **Logical Operators**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `And` | `Rule.and(a, b)` | `rule.and(b)` | Both rules must pass |
| `Or` | `Rule.or(a, b)` | `rule.or(b)` | At least one rule must pass |
| `Not` | `Rule.not(rule)` | N/A | Inverts rule result |
| `Any` | `Rule.any(rules...)` | N/A | At least one rule must pass (varargs) |
| `AllOf` | `Rule.allOf(rules...)` | N/A | All rules must pass (varargs) |

### **Control Flow**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `Guard` | `Rule.guard(precondition, rule)` | `precondition.guard(rule)` | Only evaluate `rule` if `precondition` passes |

### **Error Handling**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `WithCode` | N/A | `rule.onInvalid(code)` | Add custom error code (continue validation) |
| `WithCode` | N/A | `rule.onInvalid(code, argsFunc)` | Add custom error code with arguments |
| `WithCode` | N/A | `rule.onInvalidAndAbort(code)` | Add error code and abort validation |
| `WithCode` | N/A | `rule.onInvalidAndAbort(code, argsFunc)` | Add error code with args and abort |

### **Projection**

| Rule | Static Method | Instance Method | Description |
|------|--------------|-----------------|-------------|
| `ContraMap` | N/A | `rule.contramap(projector)` | Transform rule to different context via projection |

---

## **API Availability Matrix**

### ✅ **Both Static and Instance Methods Available:**
- `eq(attr, value)` - Equality
- `notEq(attr, value)` - Not equals  
- `gt(attr, value)` - Greater than
- `lt(attr, value)` - Less than
- `isNull(attr)` - Is null
- `notNull(attr)` - Not null
- `isEmpty(attr)` - Is empty optional
- `isEmptyCollection(attr)` - Is empty collection
- `in(attr, collection)` - Value in collection
- `and(a, b)` - Logical AND
- `or(a, b)` - Logical OR
- `guard(pre, rule)` - Conditional evaluation

### 🔧 **Static Method Only:**
- `eq(left, right)` - Compare two attributes
- `notEq(left, right)` - Two attributes not equal
- `gt(left, right)` - Left attr > right attr
- `lt(left, right)` - Left attr < right attr
- `containsAll(superset, subset)` - Collection containment
- `isContainIn(attr, listAttr)` - Field in another field's list
- `not(rule)` - Negation
- `any(rules...)` - At least one (varargs)
- `allOf(rules...)` - All must pass (varargs)
- `satisfies(predicate)` - Object-level custom predicate

### 🎯 **Instance Method Only:**
- `contramap(projector)` - Projection transformation
- `onInvalid(code)` - Custom error code
- `onInvalid(code, argsFunc)` - Custom error with arguments
- `onInvalidAndAbort(code)` - Custom error and abort
- `onInvalidAndAbort(code, argsFunc)` - Custom error with args and abort
- `satisfies(predicate, desc)` - Attribute-level custom predicate

---

## **Usage Examples**

### Basic Validation
```java
import static UserAttrs.*;

public class UserRules {
    // Field equals value
    public static final Rule<User> AGE_IS_18 = AGE.eq(18);
    
    // Field greater than value
    public static final Rule<User> ADULT = AGE.gt(17);
    
    // Field not null
    public static final Rule<User> NAME_REQUIRED = NAME.notNull();
    
    // Field in collection
    public static final Rule<User> VALID_STATUS = 
        STATUS.in("ACTIVE", "PENDING", "SUSPENDED");
}
```

### Combining Rules
```java
// AND combination
public static final Rule<User> VALID_ADULT = 
    NAME.notNull().and(AGE.gt(17));

// OR combination
public static final Rule<User> NAME_OR_EMAIL = 
    NAME.notNull().or(EMAIL.notNull());

// Static methods
public static final Rule<User> BOTH_REQUIRED = 
    Rule.and(NAME.notNull(), EMAIL.notNull());
```

### Attribute Comparison
```java
// Compare two attributes
public static final Rule<DateRange> VALID_RANGE = 
    Rule.gt(END_DATE, START_DATE);

// Field contained in another field's list
public static final Rule<Request> VALID_SELECTION = 
    SELECTED_ITEM.isContainIn(AVAILABLE_ITEMS);
```

### Conditional Validation (Guard)
```java
// Only validate email format if email is present
public static final Rule<User> EMAIL_FORMAT = 
    EMAIL.notNull().guard(EMAIL.satisfies(e -> e.contains("@"), "valid email"));
```

### Custom Error Codes
```java
public static final Rule<User> AGE_ADULT = 
    AGE.gt(17).onInvalid("user.age.must.be.adult");

// Abort on failure
public static final Rule<User> CRITICAL_CHECK = 
    STATUS.eq("ACTIVE").onInvalidAndAbort("user.must.be.active");
```

### Projection (Contramap)
```java
// Define rules on projection
public class SessionRules {
    public static final Rule<SessionProjection> NAME_REQUIRED = 
        NAME.notNull().onInvalid("session.name.required");
}

// Reuse in different contexts
public class CreateSessionRules {
    public static final Rule<CreateSessionCommand> NAME_REQUIRED = 
        SessionRules.NAME_REQUIRED.contramap(CreateSessionCommand::toProjection);
}
```

### Custom Predicates ⚠️ NOT SERIALIZABLE
```java
// Attribute-level predicate
public static final Rule<User> EMAIL_VALID = 
    EMAIL.satisfies(
        e -> e.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
        "valid email format"
    );

// Object-level predicate
public static final Rule<DateRange> DURATION_VALID = 
    Rule.satisfies(
        dr -> dr.getDurationDays() == 
              ChronoUnit.DAYS.between(dr.getStartDate(), dr.getEndDate()),
        "duration matches dates"
    );
```

---

## **Key Design Patterns**

### 1. **Fluent API**
Chain instance methods for readable validation rules:
```java
Rule<User> VALID_USER = NAME.notNull()
    .and(AGE.gt(17))
    .and(EMAIL.notNull())
    .onInvalid("user.invalid");
```

### 2. **Contramap Pattern**
Reuse projection rules across different contexts:
```java
// Define once on projection
Rule<SessionProjection> commonRule = NAME.notNull();

// Reuse in multiple contexts
Rule<CreateCmd> createRule = commonRule.contramap(CreateCmd::toProjection);
Rule<UpdateCmd> updateRule = commonRule.contramap(UpdateCmd::toProjection);
```

### 3. **Guard Pattern**
Conditional validation (only validate if precondition met):
```java
// Only check email format if email exists
Rule<User> EMAIL_FORMAT = EMAIL.notNull()
    .guard(EMAIL.satisfies(e -> e.contains("@"), "valid format"));
```

### 4. **Early Abort**
Stop validation on first critical failure:
```java
Rule<User> CRITICAL = STATUS.eq("ACTIVE")
    .onInvalidAndAbort("user.must.be.active");
```

### 5. **Custom Error Codes**
Override default codes with domain-specific messages:
```java
Rule<User> AGE_CHECK = AGE.gt(17)
    .onInvalid("user.age.too.young");
```

---

## **Serialization Warning ⚠️**

The `Predicate` rule type (created via `satisfies()` methods) is **NOT serializable** because it contains lambda functions. 

**Use only for runtime validation.**

For persistent/serializable rules, use built-in validators like:
- `eq()`, `notEq()`
- `gt()`, `lt()`
- `in()`
- `notNull()`, `isNull()`
- `isEmpty()`, `isEmptyCollection()`
- `containsAll()`, `isContainIn()`

---

## **Default Error Codes**

Each edge rule generates a default error code if not overridden:

| Rule Type | Default Code Pattern | Example |
|-----------|---------------------|---------|
| Equals | `validation.field.{name}.eq` | `validation.field.age.eq` |
| EqualsAttr | `validation.field.{left}.eq.{right}` | `validation.field.startDate.eq.endDate` |
| GreaterThan | `validation.field.{name}.gt` | `validation.field.age.gt` |
| GreaterThanAttr | `validation.field.{left}.gt.{right}` | `validation.field.endDate.gt.startDate` |
| LessThan | `validation.field.{name}.lt` | `validation.field.price.lt` |
| LessThanAttr | `validation.field.{left}.lt.{right}` | `validation.field.startDate.lt.endDate` |
| IsNull | `validation.field.{name}.isNull` | `validation.field.email.isNull` |
| NotNull | `validation.field.{name}.notNull` | `validation.field.name.notNull` |
| IsEmpty | `validation.field.{name}.isEmpty` | `validation.field.optional.isEmpty` |
| IsEmptyCollection | `validation.field.{name}.isEmptyCollection` | `validation.field.items.isEmptyCollection` |
| In | `validation.field.{name}.in` | `validation.field.status.in` |
| IsContainIn | `validation.field.{name}.in.{allowed}` | `validation.field.item.in.allowedItems` |
| ContainsAll | `validation.field.{superset}.containsAll.{subset}` | `validation.field.permissions.containsAll.required` |
| Predicate | `validation.field.{name}.predicate` | `validation.field.email.predicate` |

---

## **Best Practices**

1. **Prefer built-in validators** over custom predicates for serializability
2. **Use meaningful error codes** with `onInvalid()` for better user feedback
3. **Leverage contramap** to reuse common validation rules across contexts
4. **Use guards** to avoid unnecessary validation when preconditions aren't met
5. **Combine static and instance methods** based on readability and context
6. **Document custom predicates** clearly since they're not serializable
7. **Use early abort** (`onInvalidAndAbort`) for critical validations that should stop the entire process

---

## **Testing**

See test files for comprehensive examples:
- `RuleValidationTest.java` - Basic rule validation
- `ProjectionContramapTest.java` - Projection and contramap patterns
- `CustomPredicateTest.java` - Custom predicate usage
- `SessionValidationTest.java` - Real-world validation scenarios