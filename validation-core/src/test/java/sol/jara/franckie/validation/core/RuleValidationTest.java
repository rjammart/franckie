package sol.jara.franckie.validation.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleValidationTest {

    record Person(String name, int age, String email) {}

    private static final Attr<Person, String> NAME = Attr.of("name", Person::name);
    private static final Attr<Person, Integer> AGE = Attr.of("age", Person::age);
    private static final Attr<Person, String> EMAIL = Attr.of("email", Person::email);

    @Test
    void given__valid_person__when__validate__then__no_violations() {
        var person = new Person("John", 25, "john@example.com");

        Rule<Person> rule = Rule.allOf(
                NAME.notNull(),
                AGE.gt(18),
                EMAIL.notNull()
        );

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    void given__person_with_age_less_than_18__when__validate__then__violation() {
        var person = new Person("John", 15, "john@example.com");

        Rule<Person> rule = AGE.gt(18).onInvalid("age.must.be.greater.than.18");

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("age.must.be.greater.than.18", violations.get(0).translationKey());
    }

    @Test
    void given__failing_rule_without_on_invalid__when__validate__then__default_violation_is_collected() {
        var person = new Person("John", 15, "john@example.com");

        Rule<Person> rule = AGE.gt(18);

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.age.gt", violations.get(0).translationKey());
    }

    @Test
    void given__failing_all_of_without_on_invalid__when__validate__then__default_violation_is_collected() {
        var person = new Person(null, 15, "john@example.com");

        Rule<Person> rule = Rule.allOf(
                NAME.notNull(),
                AGE.gt(18)
        );

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(2, violations.size());
        assertEquals("validation.field.name.notNull", violations.get(0).translationKey());
    }

    @Test
    void given__person_with_null_name__when__validate__then__violation() {
        var person = new Person(null, 25, "john@example.com");

        Rule<Person> rule = NAME.notNull().onInvalid("name.required");

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("name.required", violations.get(0).translationKey());
    }

    @Test
    void given__person_with_multiple_violations__when__validate__then__all_violations_collected() {
        var person = new Person(null, 15, null);

        Rule<Person> rule = Rule.allOf(
                NAME.notNull().onInvalid("name.required"),
                AGE.gt(18).onInvalid("age.must.be.greater.than.18"),
                EMAIL.notNull().onInvalid("email.required")
        );

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(3, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("name.required")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("age.must.be.greater.than.18")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("email.required")));
    }

    @Test
    void given__person_with_age_equals_comparison__when__validate__then__works_correctly() {
        var person = new Person("John", 18, "john@example.com");

        Rule<Person> rule = AGE.eq(18).onInvalid("age.must.equal.18");

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    void given__person_age_in_allowed_range__when__validate__then__no_violation() {
        var person = new Person("John", 25, "john@example.com");

        Rule<Person> rule = AGE.in(18, 25, 30, 40).onInvalid("age.not.in.allowed.values");

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    void given__person_age_not_in_allowed_range__when__validate__then__violation() {
        var person = new Person("John", 22, "john@example.com");

        Rule<Person> rule = AGE.in(18, 25, 30, 40).onInvalid("age.not.in.allowed.values");

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("age.not.in.allowed.values", violations.get(0).translationKey());
    }

    @Test
    void given__or_rule_with_one_valid__when__validate__then__no_violation() {
        var person = new Person("John", 15, "john@example.com");

        // Person can be either over 18 OR have a non-null email
        Rule<Person> rule = Rule.or(
                AGE.gt(18),
                EMAIL.notNull()
        ).onInvalid("must.be.adult.or.have.email");

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty(), "Should have no violations because email is not null");
    }

    @Test
    void given__or_rule_with_all_invalid__when__validate__then__violation() {
        var person = new Person("John", 15, null);

        Rule<Person> rule = Rule.or(
                AGE.gt(18),
                EMAIL.notNull()
        ).onInvalid("must.be.adult.or.have.email");

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("must.be.adult.or.have.email", violations.get(0).translationKey());
    }

    @Test
    void given__abort_on_fail__when__first_violation__then__stop_validation() {
        var person = new Person(null, 15, null);

        Rule<Person> rule = Rule.allOf(
                NAME.notNull().onInvalidAndAbort("name.required"),
                AGE.gt(18).onInvalid("age.must.be.greater.than.18"),
                EMAIL.notNull().onInvalid("email.required")
        );

        List<Violation> violations = Rule.validate(person, rule);

        // Should only have 1 violation because abort stops further validation
        assertEquals(1, violations.size());
        assertEquals("name.required", violations.get(0).translationKey());
    }

    @Test
    void given__valid_person__when__decide_on__then__returns_true() {
        var person = new Person("John", 25, "john@example.com");

        Rule<Person> rule = Rule.allOf(
                NAME.notNull().onInvalid("name.required"),
                AGE.gt(18).onInvalid("age.must.be.greater.than.18")
        );

        List<Violation> violations = Rule.validate(person, rule);
        boolean result = CoreValidation.CoreValidators.decideOn(violations);

        assertTrue(result);
    }

    @Test
    void given__invalid_person__when__decide_on__then__throws_exception() {
        var person = new Person(null, 15, null);

        Rule<Person> rule = Rule.allOf(
                NAME.notNull().onInvalid("name.required"),
                AGE.gt(18).onInvalid("age.must.be.greater.than.18")
        );

        List<Violation> violations = Rule.validate(person, rule);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> CoreValidation.CoreValidators.decideOn(violations)
        );

        assertEquals(2, exception.getViolations().size());
    }

    @Test
    void given__guard_rule_with_precondition_failed__when__validate__then__precondition_violation_propagated() {
        var person = new Person("John", 15, "john@example.com");

        Rule<Person> rule = Rule.guard(
                AGE.gt(18),
                EMAIL.notNull().onInvalid("email.required")
        );

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.age.gt", violations.get(0).translationKey());
    }

    @Test
    void given__guardSilently_rule_with_precondition_failed__when__validate__then__no_violation() {
        var person = new Person("John", 15, "john@example.com");

        Rule<Person> rule = Rule.guardSilently(
                AGE.gt(18),
                EMAIL.notNull().onInvalid("email.required")
        );

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__guard_with_notNull_precondition__when__null__then__notNull_violation_propagated_and_inner_not_evaluated() {
        var person = new Person(null, 25, "john@example.com");

        Rule<Person> rule = NAME.notNull().guard(AGE.gt(18).onInvalid("age.check"));

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.name.notNull", violations.get(0).translationKey());
    }

    @Test
    void given__guard_rule_with_precondition_failed_wrapped_in_on_invalid__when__validate__then__custom_violation() {
        var person = new Person("John", 15, null);

        Rule<Person> rule = Rule.guard(
                AGE.gt(18),
                EMAIL.notNull()
        ).onInvalid("email.required.when.adult");

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("email.required.when.adult", violations.get(0).translationKey());
    }

    @Test
    void given__guard_rule_with_precondition_passed__when__validate__then__guarded_rule_evaluated() {
        var person = new Person("John", 25, null);

        // Only validate email if age > 18
        Rule<Person> rule = Rule.guard(
                AGE.gt(18),
                EMAIL.notNull().onInvalid("email.required")
        );

        List<Violation> violations = Rule.validate(person, rule);

        // Has violation because precondition passed and email is null
        assertEquals(1, violations.size());
        assertEquals("email.required", violations.get(0).translationKey());
    }

    @Test
    void given__comparison_between_two_attributes__when__validate__then__works_correctly() {
        record DateRange(java.time.LocalDate start, java.time.LocalDate end) {}

        var startDate = java.time.LocalDate.of(2024, 1, 1);
        var endDate = java.time.LocalDate.of(2024, 1, 10);
        var range = new DateRange(startDate, endDate);

        Attr<DateRange, java.time.LocalDate> START = Attr.of("start", DateRange::start);
        Attr<DateRange, java.time.LocalDate> END = Attr.of("end", DateRange::end);

        Rule<DateRange> rule = START.lt(END).onInvalid("start.must.be.before.end");

        List<Violation> violations = Rule.validate(range, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__null_field__when__isNull__then__no_violation() {
        var person = new Person(null, 25, "john@example.com");

        Rule<Person> rule = NAME.isNull();

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__non_null_field__when__isNull__then__violation() {
        var person = new Person("John", 25, "john@example.com");

        Rule<Person> rule = NAME.isNull();

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.name.isNull", violations.get(0).translationKey());
    }

    @Test
    void given__null_field__when__notNull_static__then__violation() {
        var person = new Person(null, 25, "john@example.com");

        Rule<Person> rule = Rule.notNull(NAME);

        List<Violation> violations = Rule.validate(person, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.name.notNull", violations.get(0).translationKey());
    }

    @Test
    void given__non_null_field__when__notNull_static__then__no_violation() {
        var person = new Person("John", 25, "john@example.com");

        Rule<Person> rule = Rule.notNull(NAME);

        List<Violation> violations = Rule.validate(person, rule);

        assertTrue(violations.isEmpty());
    }

    record Container(List<String> items) {}

    private static final Attr<Container, List<String>> ITEMS = Attr.of("items", Container::items);

    @Test
    void given__non_empty_collection__when__isNotEmptyCollection__then__no_violation() {
        var container = new Container(List.of("a", "b"));

        Rule<Container> rule = ITEMS.isNotEmptyCollection();

        List<Violation> violations = Rule.validate(container, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__empty_collection__when__isNotEmptyCollection__then__violation() {
        var container = new Container(List.of());

        Rule<Container> rule = ITEMS.isNotEmptyCollection();

        List<Violation> violations = Rule.validate(container, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.items.isNotEmptyCollection", violations.get(0).translationKey());
    }

    @Test
    void given__null_collection__when__isNotEmptyCollection__then__violation() {
        var container = new Container(null);

        Rule<Container> rule = ITEMS.isNotEmptyCollection();

        List<Violation> violations = Rule.validate(container, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.items.isNotEmptyCollection", violations.get(0).translationKey());
    }

    @Test
    void given__non_empty_collection__when__isNotEmptyCollection_with_onInvalid__then__no_violation() {
        var container = new Container(List.of("a"));

        Rule<Container> rule = ITEMS.isNotEmptyCollection().onInvalid("items.required");

        List<Violation> violations = Rule.validate(container, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__empty_collection__when__isNotEmptyCollection_with_onInvalid__then__custom_violation() {
        var container = new Container(List.of());

        Rule<Container> rule = ITEMS.isNotEmptyCollection().onInvalid("items.required");

        List<Violation> violations = Rule.validate(container, rule);

        assertEquals(1, violations.size());
        assertEquals("items.required", violations.get(0).translationKey());
    }

    @Test
    void given__empty_collection__when__isEmptyCollection__then__no_violation() {
        var container = new Container(List.of());

        Rule<Container> rule = ITEMS.isEmptyCollection();

        List<Violation> violations = Rule.validate(container, rule);

        assertTrue(violations.isEmpty());
    }

    @Test
    void given__non_empty_collection__when__isEmptyCollection__then__violation() {
        var container = new Container(List.of("a", "b"));

        Rule<Container> rule = ITEMS.isEmptyCollection();

        List<Violation> violations = Rule.validate(container, rule);

        assertEquals(1, violations.size());
        assertEquals("validation.field.items.isEmptyCollection", violations.get(0).translationKey());
    }
}
