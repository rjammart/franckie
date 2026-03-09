package sol.jara.franckie.validation.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for custom predicate support in the validation framework.
 * Tests cover:
 * - satisfies() with custom predicates
 * - matches() for regex validation
 * - between() for range validation
 * - hasSize(), hasSizeAtLeast(), hasSizeAtMost() for collection validation
 * - Integration with existing rules and composition
 */
class CustomPredicateTest {

    // ========== Test Records ==========

    record User(String username, String email, Integer age) {}

    record CreditCard(String number, String cvv, LocalDate expirationDate) {}

    record Product(String sku, List<String> tags, Integer stockCount) {}

    record Config(String jsonData, String apiKey) {}

    // ========== Attributes ==========

    private static final Attr<User, String> USERNAME = Attr.of("username", User::username);
    private static final Attr<User, String> EMAIL = Attr.of("email", User::email);
    private static final Attr<User, Integer> AGE = Attr.of("age", User::age);

    private static final Attr<CreditCard, String> CARD_NUMBER = Attr.of("number", CreditCard::number);
    private static final Attr<CreditCard, String> CVV = Attr.of("cvv", CreditCard::cvv);
    private static final Attr<CreditCard, LocalDate> EXPIRATION = Attr.of("expirationDate", CreditCard::expirationDate);

    private static final Attr<Product, String> SKU = Attr.of("sku", Product::sku);
    private static final Attr<Product, List<String>> TAGS = Attr.of("tags", Product::tags);
    private static final Attr<Product, Integer> STOCK_COUNT = Attr.of("stockCount", Product::stockCount);

    private static final Attr<Config, String> JSON_DATA = Attr.of("jsonData", Config::jsonData);
    private static final Attr<Config, String> API_KEY = Attr.of("apiKey", Config::apiKey);

    // ========== satisfies() Tests ==========

    @Test
    @DisplayName("given custom predicate when value satisfies then rule passes")
    void given__custom_predicate__when__value_satisfies__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> ageReasonable = AGE.satisfies(
            age -> age >= 18 && age <= 120,
            "age between 18 and 120"
        );

        List<Violation> violations = Rule.validate(user, ageReasonable);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given custom predicate when value fails then violations added")
    void given__custom_predicate__when__value_fails__then__violations_added() {
        var user = new User("john_doe", "john@example.com", 150);

        Rule<User> ageReasonable = AGE.satisfies(
            age -> age >= 18 && age <= 120,
            "age between 18 and 120"
        ).onInvalid("user.age.out.of.range");

        List<Violation> violations = Rule.validate(user, ageReasonable);

        assertEquals(1, violations.size());
        assertEquals("user.age.out.of.range", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given custom predicate without description when validate then uses default description")
    void given__custom_predicate_without_description__when__validate__then__uses_default_description() {
        var user = new User("john_doe", "john@example.com", 150);

        Rule<User> ageCheck = AGE.satisfies(age -> age >= 18 && age <= 120);

        List<Violation> violations = Rule.validate(user, ageCheck);

        assertEquals(1, violations.size());
        assertEquals("validation.field.age.predicate", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given predicate with null value when test then handles gracefully")
    void given__predicate_with_null_value__when__test__then__handles_gracefully() {
        var user = new User("john_doe", "john@example.com", null);

        Rule<User> ageNotNull = AGE.satisfies(
            age -> age != null && age > 0,
            "age is positive"
        ).onInvalid("user.age.invalid");

        List<Violation> violations = Rule.validate(user, ageNotNull);

        assertEquals(1, violations.size());
        assertEquals("user.age.invalid", violations.get(0).translationKey());
    }

    // ========== matches() Tests ==========

    @Test
    @DisplayName("given regex matcher when string matches then rule passes")
    void given__regex_matcher__when__string_matches__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> emailValid = EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        List<Violation> violations = Rule.validate(user, emailValid);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given regex matcher when string does not match then violations added")
    void given__regex_matcher__when__string_does_not_match__then__violations_added() {
        var user = new User("john_doe", "invalid-email", 25);

        Rule<User> emailValid = EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
            .onInvalid("user.email.invalid");

        List<Violation> violations = Rule.validate(user, emailValid);

        assertEquals(1, violations.size());
        assertEquals("user.email.invalid", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given username pattern when alphanumeric matches then rule passes")
    void given__username_pattern__when__alphanumeric_matches__then__rule_passes() {
        var user = new User("john123", "john@example.com", 25);

        Rule<User> usernameFormat = USERNAME.matches("^[a-zA-Z0-9_]{3,20}$");

        List<Violation> violations = Rule.validate(user, usernameFormat);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given username pattern when special characters present then violations added")
    void given__username_pattern__when__special_characters_present__then__violations_added() {
        var user = new User("john@doe!", "john@example.com", 25);

        Rule<User> usernameFormat = USERNAME.matches("^[a-zA-Z0-9_]{3,20}$")
            .onInvalid("user.username.invalid.format");

        List<Violation> violations = Rule.validate(user, usernameFormat);

        assertEquals(1, violations.size());
        assertEquals("user.username.invalid.format", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given compiled pattern when string matches then rule passes")
    void given__compiled_pattern__when__string_matches__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 25);

        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        Rule<User> emailValid = EMAIL.matches(emailPattern);

        List<Violation> violations = Rule.validate(user, emailValid);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given regex matcher with null value when validate then fails")
    void given__regex_matcher_with_null_value__when__validate__then__fails() {
        var user = new User("john_doe", null, 25);

        Rule<User> emailValid = EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
            .onInvalid("user.email.required");

        List<Violation> violations = Rule.validate(user, emailValid);

        assertEquals(1, violations.size());
        assertEquals("user.email.required", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given cvv pattern when valid 3 digits then rule passes")
    void given__cvv_pattern__when__valid_3_digits__then__rule_passes() {
        var card = new CreditCard("4111111111111111", "123", LocalDate.now().plusYears(1));

        Rule<CreditCard> cvvFormat = CVV.matches("^[0-9]{3,4}$")
            .onInvalid("creditcard.cvv.invalid");

        List<Violation> violations = Rule.validate(card, cvvFormat);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given cvv pattern when invalid format then violations added")
    void given__cvv_pattern__when__invalid_format__then__violations_added() {
        var card = new CreditCard("4111111111111111", "12", LocalDate.now().plusYears(1));

        Rule<CreditCard> cvvFormat = CVV.matches("^[0-9]{3,4}$")
            .onInvalid("creditcard.cvv.invalid");

        List<Violation> violations = Rule.validate(card, cvvFormat);

        assertEquals(1, violations.size());
        assertEquals("creditcard.cvv.invalid", violations.get(0).translationKey());
    }

    // ========== between() Tests ==========

    @Test
    @DisplayName("given between range when value in range then rule passes")
    void given__between_range__when__value_in_range__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> ageInRange = AGE.between(18, 65);

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given between range when value at minimum boundary then rule passes")
    void given__between_range__when__value_at_minimum_boundary__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 18);

        Rule<User> ageInRange = AGE.between(18, 65);

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given between range when value at maximum boundary then rule passes")
    void given__between_range__when__value_at_maximum_boundary__then__rule_passes() {
        var user = new User("john_doe", "john@example.com", 65);

        Rule<User> ageInRange = AGE.between(18, 65);

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given between range when value below minimum then violations added")
    void given__between_range__when__value_below_minimum__then__violations_added() {
        var user = new User("john_doe", "john@example.com", 15);

        Rule<User> ageInRange = AGE.between(18, 65)
            .onInvalid("user.age.out.of.working.range");

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertEquals(1, violations.size());
        assertEquals("user.age.out.of.working.range", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given between range when value above maximum then violations added")
    void given__between_range__when__value_above_maximum__then__violations_added() {
        var user = new User("john_doe", "john@example.com", 70);

        Rule<User> ageInRange = AGE.between(18, 65)
            .onInvalid("user.age.out.of.working.range");

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertEquals(1, violations.size());
        assertEquals("user.age.out.of.working.range", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given between range with null value when validate then fails")
    void given__between_range_with_null_value__when__validate__then__fails() {
        var user = new User("john_doe", "john@example.com", null);

        Rule<User> ageInRange = AGE.between(18, 65)
            .onInvalid("user.age.required");

        List<Violation> violations = Rule.validate(user, ageInRange);

        assertEquals(1, violations.size());
        assertEquals("user.age.required", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given date between range when date in range then rule passes")
    void given__date_between_range__when__date_in_range__then__rule_passes() {
        LocalDate expirationDate = LocalDate.now().plusMonths(6);
        var card = new CreditCard("4111111111111111", "123", expirationDate);

        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.now().plusYears(1);

        Rule<CreditCard> expirationInRange = EXPIRATION.between(minDate, maxDate);

        List<Violation> violations = Rule.validate(card, expirationInRange);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    // ========== Collection Size Tests ==========

    @Test
    @DisplayName("given hasSize when collection has exact size then rule passes")
    void given__hasSize__when__collection_has_exact_size__then__rule_passes() {
        var product = new Product("SKU123", List.of("electronics", "gadgets", "new"), 100);

        Rule<Product> tagsSize = TAGS.hasSize(3);

        List<Violation> violations = Rule.validate(product, tagsSize);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given hasSize when collection size differs then violations added")
    void given__hasSize__when__collection_size_differs__then__violations_added() {
        var product = new Product("SKU123", List.of("electronics", "gadgets"), 100);

        Rule<Product> tagsSize = TAGS.hasSize(3)
            .onInvalid("product.tags.must.have.exactly.3");

        List<Violation> violations = Rule.validate(product, tagsSize);

        assertEquals(1, violations.size());
        assertEquals("product.tags.must.have.exactly.3", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given hasSizeAtLeast when collection meets minimum then rule passes")
    void given__hasSizeAtLeast__when__collection_meets_minimum__then__rule_passes() {
        var product = new Product("SKU123", List.of("electronics", "gadgets", "new"), 100);

        Rule<Product> tagsMinSize = TAGS.hasSizeAtLeast(2);

        List<Violation> violations = Rule.validate(product, tagsMinSize);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given hasSizeAtLeast when collection at exact minimum then rule passes")
    void given__hasSizeAtLeast__when__collection_at_exact_minimum__then__rule_passes() {
        var product = new Product("SKU123", List.of("electronics", "gadgets"), 100);

        Rule<Product> tagsMinSize = TAGS.hasSizeAtLeast(2);

        List<Violation> violations = Rule.validate(product, tagsMinSize);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given hasSizeAtLeast when collection below minimum then violations added")
    void given__hasSizeAtLeast__when__collection_below_minimum__then__violations_added() {
        var product = new Product("SKU123", List.of("electronics"), 100);

        Rule<Product> tagsMinSize = TAGS.hasSizeAtLeast(2)
            .onInvalid("product.tags.minimum.not.met");

        List<Violation> violations = Rule.validate(product, tagsMinSize);

        assertEquals(1, violations.size());
        assertEquals("product.tags.minimum.not.met", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given hasSizeAtMost when collection within maximum then rule passes")
    void given__hasSizeAtMost__when__collection_within_maximum__then__rule_passes() {
        var product = new Product("SKU123", List.of("electronics", "gadgets"), 100);

        Rule<Product> tagsMaxSize = TAGS.hasSizeAtMost(5);

        List<Violation> violations = Rule.validate(product, tagsMaxSize);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given hasSizeAtMost when collection at exact maximum then rule passes")
    void given__hasSizeAtMost__when__collection_at_exact_maximum__then__rule_passes() {
        var product = new Product("SKU123", List.of("a", "b", "c", "d", "e"), 100);

        Rule<Product> tagsMaxSize = TAGS.hasSizeAtMost(5);

        List<Violation> violations = Rule.validate(product, tagsMaxSize);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given hasSizeAtMost when collection exceeds maximum then violations added")
    void given__hasSizeAtMost__when__collection_exceeds_maximum__then__violations_added() {
        var product = new Product("SKU123", List.of("a", "b", "c", "d", "e", "f"), 100);

        Rule<Product> tagsMaxSize = TAGS.hasSizeAtMost(5)
            .onInvalid("product.tags.maximum.exceeded");

        List<Violation> violations = Rule.validate(product, tagsMaxSize);

        assertEquals(1, violations.size());
        assertEquals("product.tags.maximum.exceeded", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given hasSize with null collection when validate then fails")
    void given__hasSize_with_null_collection__when__validate__then__fails() {
        var product = new Product("SKU123", null, 100);

        Rule<Product> tagsSize = TAGS.hasSize(3)
            .onInvalid("product.tags.invalid");

        List<Violation> violations = Rule.validate(product, tagsSize);

        assertEquals(1, violations.size());
        assertEquals("product.tags.invalid", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given empty collection when hasSize zero then rule passes")
    void given__empty_collection__when__hasSize_zero__then__rule_passes() {
        var product = new Product("SKU123", new ArrayList<>(), 100);

        Rule<Product> tagsEmpty = TAGS.hasSize(0);

        List<Violation> violations = Rule.validate(product, tagsEmpty);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    // ========== Integration and Composition Tests ==========

    @Test
    @DisplayName("given predicate rule when composed with other rules then works correctly")
    void given__predicate_rule__when__composed_with_other_rules__then__works_correctly() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> combined = Rule.allOf(
            AGE.satisfies(age -> age >= 18, "adult"),
            AGE.lt(100),
            USERNAME.notNull(),
            EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        );

        List<Violation> violations = Rule.validate(user, combined);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given combined predicate and standard rules when some fail then violations collected")
    void given__combined_predicate_and_standard_rules__when__some_fail__then__violations_collected() {
        var user = new User("john_doe", "invalid-email", 15);

        Rule<User> combined = Rule.allOf(
            AGE.satisfies(age -> age >= 18, "adult").onInvalid("user.must.be.adult"),
            EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").onInvalid("user.email.invalid")
        );

        List<Violation> violations = Rule.validate(user, combined);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("user.must.be.adult")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("user.email.invalid")));
    }

    @Test
    @DisplayName("given multiple predicates in allOf when all pass then no violations")
    void given__multiple_predicates_in_allOf__when__all_pass__then__no_violations() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> allRules = Rule.allOf(
            USERNAME.matches("^[a-zA-Z0-9_]{3,20}$"),
            EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
            AGE.between(18, 65)
        );

        List<Violation> violations = Rule.validate(user, allRules);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given predicate with and combinator when both pass then no violations")
    void given__predicate_with_and_combinator__when__both_pass__then__no_violations() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> combined = USERNAME.notNull()
            .and(USERNAME.matches("^[a-zA-Z0-9_]{3,20}$"))
            .and(USERNAME.satisfies(name -> !name.contains("admin"), "not admin"));

        List<Violation> violations = Rule.validate(user, combined);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given predicate with or combinator when one passes then no violations")
    void given__predicate_with_or_combinator__when__one_passes__then__no_violations() {
        var user = new User("john_doe", "john@example.com", 15);

        Rule<User> eitherAdultOrHasEmail = AGE.satisfies(age -> age >= 18, "adult")
            .or(EMAIL.notNull())
            .onInvalid("must.be.adult.or.have.email");

        List<Violation> violations = Rule.validate(user, eitherAdultOrHasEmail);

        assertTrue(violations.isEmpty(), "Should have no violations because email is not null");
    }

    @Test
    @DisplayName("given predicate used with guard when precondition passes then predicate evaluated")
    void given__predicate_used_with_guard__when__precondition_passes__then__predicate_evaluated() {
        var card = new CreditCard("4111111111111111", "123", LocalDate.now().minusDays(1));

        // Only validate expiration date is in future if card number is present
        Rule<CreditCard> expirationValid = Rule.guard(
            CARD_NUMBER.notNull(),
            EXPIRATION.satisfies(
                date -> date.isAfter(LocalDate.now()),
                "not expired"
            ).onInvalid("creditcard.expired")
        );

        List<Violation> violations = Rule.validate(card, expirationValid);

        assertEquals(1, violations.size());
        assertEquals("creditcard.expired", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given complex business logic in predicate when validates then works correctly")
    void given__complex_business_logic_in_predicate__when__validates__then__works_correctly() {
        var card = new CreditCard("4111111111111111", "123", LocalDate.now().plusYears(1));

        // Luhn algorithm check
        Rule<CreditCard> luhnValid = CARD_NUMBER.satisfies(
            this::isValidLuhn,
            "valid credit card number (Luhn algorithm)"
        ).onInvalid("creditcard.number.invalid");

        List<Violation> violations = Rule.validate(card, luhnValid);

        assertTrue(violations.isEmpty(), "Should have no violations - valid card number");
    }

    @Test
    @DisplayName("given invalid luhn checksum when validate then violations added")
    void given__invalid_luhn_checksum__when__validate__then__violations_added() {
        var card = new CreditCard("1234567890123456", "123", LocalDate.now().plusYears(1));

        Rule<CreditCard> luhnValid = CARD_NUMBER.satisfies(
            this::isValidLuhn,
            "valid credit card number (Luhn algorithm)"
        ).onInvalid("creditcard.number.invalid");

        List<Violation> violations = Rule.validate(card, luhnValid);

        assertEquals(1, violations.size());
        assertEquals("creditcard.number.invalid", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given comprehensive credit card validation when all valid then no violations")
    void given__comprehensive_credit_card_validation__when__all_valid__then__no_violations() {
        var card = new CreditCard("4111111111111111", "123", LocalDate.now().plusYears(1));

        Rule<CreditCard> allRules = Rule.allOf(
            CARD_NUMBER.satisfies(this::isValidLuhn, "valid Luhn checksum"),
            CVV.matches("^[0-9]{3,4}$"),
            EXPIRATION.satisfies(date -> date.isAfter(LocalDate.now()), "not expired")
        );

        List<Violation> violations = Rule.validate(card, allRules);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given comprehensive user validation when all valid then no violations")
    void given__comprehensive_user_validation__when__all_valid__then__no_violations() {
        var user = new User("john_doe", "john@example.com", 25);

        Rule<User> allRules = Rule.allOf(
            USERNAME.matches("^[a-zA-Z0-9_]{3,20}$").onInvalid("user.username.format.invalid"),
            EMAIL.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").onInvalid("user.email.invalid"),
            AGE.between(18, 120).onInvalid("user.age.out.of.range")
        );

        List<Violation> violations = Rule.validate(user, allRules);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given product with size constraints when all valid then no violations")
    void given__product_with_size_constraints__when__all_valid__then__no_violations() {
        var product = new Product("SKU123", List.of("electronics", "gadgets"), 50);

        Rule<Product> allRules = Rule.allOf(
            SKU.matches("^SKU[0-9]{3}$"),
            TAGS.hasSizeAtLeast(1).onInvalid("product.tags.minimum.required"),
            TAGS.hasSizeAtMost(5).onInvalid("product.tags.maximum.exceeded"),
            STOCK_COUNT.between(0, 1000).onInvalid("product.stock.out.of.range")
        );

        List<Violation> violations = Rule.validate(product, allRules);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    // ========== Object-Level Predicate Tests ==========

    record DateRange(LocalDate startDate, LocalDate endDate, Integer durationDays) {}

    private static final Attr<DateRange, LocalDate> RANGE_START = Attr.of("startDate", DateRange::startDate);
    private static final Attr<DateRange, LocalDate> RANGE_END = Attr.of("endDate", DateRange::endDate);
    private static final Attr<DateRange, Integer> DURATION = Attr.of("durationDays", DateRange::durationDays);

    @Test
    @DisplayName("given object-level predicate when object satisfies then rule passes")
    void given__object_level_predicate__when__object_satisfies__then__rule_passes() {
        var range = new DateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 8),
            7
        );

        Rule<DateRange> durationConsistent = Rule.satisfies(
            (DateRange r) -> {
                long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                    r.startDate(),
                    r.endDate()
                );
                return actualDays == r.durationDays();
            },
            "duration matches actual date difference"
        );

        List<Violation> violations = Rule.validate(range, durationConsistent);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given object-level predicate when object fails then violations added")
    void given__object_level_predicate__when__object_fails__then__violations_added() {
        var range = new DateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 8),
            10  // Incorrect - should be 7
        );

        Rule<DateRange> durationConsistent = Rule.satisfies(
            (DateRange r) -> {
                long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                    r.startDate(),
                    r.endDate()
                );
                return actualDays == r.durationDays();
            },
            "duration matches actual date difference"
        ).onInvalid("daterange.duration.mismatch");

        List<Violation> violations = Rule.validate(range, durationConsistent);

        assertEquals(1, violations.size());
        assertEquals("daterange.duration.mismatch", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given object-level predicate without description when validate then uses default")
    void given__object_level_predicate_without_description__when__validate__then__uses_default() {
        var range = new DateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 8),
            10
        );

        Rule<DateRange> check = Rule.satisfies((DateRange r) ->
            java.time.temporal.ChronoUnit.DAYS.between(r.startDate(), r.endDate()) == r.durationDays()
        );

        List<Violation> violations = Rule.validate(range, check);

        assertEquals(1, violations.size());
        assertEquals("validation.field.object.predicate", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given object-level predicate combined with field rules when all valid then no violations")
    void given__object_level_predicate_combined_with_field_rules__when__all_valid__then__no_violations() {
        var range = new DateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 8),
            7
        );

        Rule<DateRange> allRules = Rule.allOf(
            RANGE_START.notNull(),
            RANGE_END.notNull(),
            RANGE_START.lt(RANGE_END),
            DURATION.notNull(),
            DURATION.gt(0),
            Rule.satisfies(
                (DateRange r) -> {
                    long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                        r.startDate(),
                        r.endDate()
                    );
                    return actualDays == r.durationDays();
                },
                "duration matches date difference"
            )
        );

        List<Violation> violations = Rule.validate(range, allRules);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given multi-field validation when fields consistent then rule passes")
    void given__multi_field_validation__when__fields_consistent__then__rule_passes() {
        var user = new User("john_doe", "john_doe@example.com", 25);

        // Email should contain username
        Rule<User> emailContainsUsername = Rule.satisfies(
            (User u) -> u.email() != null && u.email().startsWith(u.username()),
            "email starts with username"
        );

        List<Violation> violations = Rule.validate(user, emailContainsUsername);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given multi-field validation when fields inconsistent then violations added")
    void given__multi_field_validation__when__fields_inconsistent__then__violations_added() {
        var user = new User("john_doe", "different@example.com", 25);

        Rule<User> emailContainsUsername = Rule.satisfies(
            (User u) -> u.email() != null && u.email().startsWith(u.username()),
            "email starts with username"
        ).onInvalid("user.email.must.match.username");

        List<Violation> violations = Rule.validate(user, emailContainsUsername);

        assertEquals(1, violations.size());
        assertEquals("user.email.must.match.username", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given complex business rule using object predicate when valid then no violations")
    void given__complex_business_rule_using_object_predicate__when__valid__then__no_violations() {
        var product = new Product("SKU123", List.of("electronics", "featured"), 50);

        // Business rule: Featured products must have at least 10 items in stock
        Rule<Product> featuredStockRule = Rule.satisfies(
            (Product p) -> {
                boolean isFeatured = p.tags() != null && p.tags().contains("featured");
                if (isFeatured) {
                    return p.stockCount() != null && p.stockCount() >= 10;
                }
                return true;  // Rule doesn't apply to non-featured products
            },
            "featured products must have at least 10 items in stock"
        );

        List<Violation> violations = Rule.validate(product, featuredStockRule);

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given complex business rule using object predicate when invalid then violations added")
    void given__complex_business_rule_using_object_predicate__when__invalid__then__violations_added() {
        var product = new Product("SKU123", List.of("electronics", "featured"), 5);

        Rule<Product> featuredStockRule = Rule.satisfies(
            (Product p) -> {
                boolean isFeatured = p.tags() != null && p.tags().contains("featured");
                if (isFeatured) {
                    return p.stockCount() != null && p.stockCount() >= 10;
                }
                return true;
            },
            "featured products must have at least 10 items in stock"
        ).onInvalid("product.featured.insufficient.stock");

        List<Violation> violations = Rule.validate(product, featuredStockRule);

        assertEquals(1, violations.size());
        assertEquals("product.featured.insufficient.stock", violations.get(0).translationKey());
    }

    // ========== Helper Methods ==========

    /**
     * Validates credit card number using Luhn algorithm
     */
    private boolean isValidLuhn(String cardNumber) {
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
}
