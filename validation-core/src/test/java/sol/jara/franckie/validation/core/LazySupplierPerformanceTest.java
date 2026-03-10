package sol.jara.franckie.validation.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the performance benefits of using Suppliers in validation contexts.
 * Suppliers enable lazy evaluation - expensive operations only execute if validation reaches them.
 */
class LazySupplierPerformanceTest {

    /**
     * Eager validation context - all values computed at construction
     */
    record EagerValidationContext(
        String status,
        boolean expensiveCheck1,  // Computed eagerly
        boolean expensiveCheck2,  // Computed eagerly
        List<String> expensiveData // Computed eagerly
    ) {}

    /**
     * Lazy validation context - values computed only when accessed
     */
    record LazyValidationContext(
        String status,
        BooleanSupplier expensiveCheck1,  // Lazy
        BooleanSupplier expensiveCheck2,  // Lazy
        Supplier<List<String>> expensiveData // Lazy
    ) {}

    // Attributes for eager context
    private static final Attr<EagerValidationContext, String> EAGER_STATUS =
        Attr.of("status", EagerValidationContext::status);
    private static final Attr<EagerValidationContext, Boolean> EAGER_CHECK1 =
        Attr.of("expensiveCheck1", EagerValidationContext::expensiveCheck1);
    private static final Attr<EagerValidationContext, Boolean> EAGER_CHECK2 =
        Attr.of("expensiveCheck2", EagerValidationContext::expensiveCheck2);
    private static final Attr<EagerValidationContext, List<String>> EAGER_DATA =
        Attr.of("expensiveData", EagerValidationContext::expensiveData);

    // Attributes for lazy context
    private static final Attr<LazyValidationContext, String> LAZY_STATUS =
        Attr.of("status", LazyValidationContext::status);
    private static final Attr<LazyValidationContext, BooleanSupplier> LAZY_CHECK1 =
        Attr.of("expensiveCheck1", LazyValidationContext::expensiveCheck1);
    private static final Attr<LazyValidationContext, BooleanSupplier> LAZY_CHECK2 =
        Attr.of("expensiveCheck2", LazyValidationContext::expensiveCheck2);
    private static final Attr<LazyValidationContext, Supplier<List<String>>> LAZY_DATA =
        Attr.of("expensiveData", LazyValidationContext::expensiveData);

    @Test
    @DisplayName("given eager context when early validation fails then all expensive operations already executed")
    void given__eager_context__when__early_validation_fails__then__all_expensive_operations_already_executed() {
        AtomicInteger check1Calls = new AtomicInteger(0);
        AtomicInteger check2Calls = new AtomicInteger(0);
        AtomicInteger dataCalls = new AtomicInteger(0);

        // Create eager context - ALL operations execute immediately
        var context = new EagerValidationContext(
            "INVALID",  // Will fail first rule
            expensiveCheck(check1Calls),  // ❌ Called even though not needed
            expensiveCheck(check2Calls),  // ❌ Called even though not needed
            expensiveData(dataCalls)      // ❌ Called even though not needed
        );

        // Validation rules with abort
        Rule<EagerValidationContext> rules = Rule.allOf(
            EAGER_STATUS.eq("VALID").onInvalidAndAbort("status.invalid"),
            EAGER_CHECK1.eq(true).onInvalidAndAbort("check1.failed"),
            EAGER_CHECK2.eq(true).onInvalidAndAbort("check2.failed"),
            EAGER_DATA.isNotEmptyCollection()
        );

        List<Violation> violations = Rule.validate(context, rules);

        // Validation failed on status (expected)
        assertEquals(1, violations.size());
        assertEquals("status.invalid", violations.get(0).translationKey());

        // ❌ Problem: All expensive operations were called during context creation
        assertEquals(1, check1Calls.get(), "Check 1 was called unnecessarily");
        assertEquals(1, check2Calls.get(), "Check 2 was called unnecessarily");
        assertEquals(1, dataCalls.get(), "Data loading was called unnecessarily");
    }

    @Test
    @DisplayName("given lazy context when early validation fails then expensive operations never executed")
    void given__lazy_context__when__early_validation_fails__then__expensive_operations_never_executed() {
        AtomicInteger check1Calls = new AtomicInteger(0);
        AtomicInteger check2Calls = new AtomicInteger(0);
        AtomicInteger dataCalls = new AtomicInteger(0);

        // Create lazy context - suppliers created but NOT executed
        var context = new LazyValidationContext(
            "INVALID",  // Will fail first rule
            () -> expensiveCheck(check1Calls),  // ✅ Not called yet
            () -> expensiveCheck(check2Calls),  // ✅ Not called yet
            () -> expensiveData(dataCalls)      // ✅ Not called yet
        );

        // Validation rules with abort
        Rule<LazyValidationContext> rules = Rule.allOf(
            LAZY_STATUS.eq("VALID").onInvalidAndAbort("status.invalid"),
            LAZY_CHECK1.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check1.failed"),
            LAZY_CHECK2.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check2.failed"),
            LAZY_DATA.satisfies(s -> !s.get().isEmpty())
        );

        List<Violation> violations = Rule.validate(context, rules);

        // Validation failed on status (expected)
        assertEquals(1, violations.size());
        assertEquals("status.invalid", violations.get(0).translationKey());

        // ✅ Success: No expensive operations were called!
        assertEquals(0, check1Calls.get(), "Check 1 was NOT called (saved DB query)");
        assertEquals(0, check2Calls.get(), "Check 2 was NOT called (saved service call)");
        assertEquals(0, dataCalls.get(), "Data loading was NOT called (saved expensive operation)");
    }

    @Test
    @DisplayName("given lazy context when first expensive check fails then second expensive check not executed")
    void given__lazy_context__when__first_expensive_check_fails__then__second_expensive_check_not_executed() {
        AtomicInteger check1Calls = new AtomicInteger(0);
        AtomicInteger check2Calls = new AtomicInteger(0);
        AtomicInteger dataCalls = new AtomicInteger(0);

        // Create lazy context
        var context = new LazyValidationContext(
            "VALID",  // Passes first rule
            () -> {
                check1Calls.incrementAndGet();
                return false;  // This will fail
            },
            () -> {
                check2Calls.incrementAndGet();
                return true;
            },
            () -> {
                dataCalls.incrementAndGet();
                return List.of("data");
            }
        );

        // Validation rules with abort
        Rule<LazyValidationContext> rules = Rule.allOf(
            LAZY_STATUS.eq("VALID").onInvalidAndAbort("status.invalid"),
            LAZY_CHECK1.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check1.failed"),
            LAZY_CHECK2.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check2.failed"),
            LAZY_DATA.satisfies(s -> !s.get().isEmpty())
        );

        List<Violation> violations = Rule.validate(context, rules);

        // Validation failed on check1 (expected)
        assertEquals(1, violations.size());
        assertEquals("check1.failed", violations.get(0).translationKey());

        // Check execution pattern
        assertEquals(1, check1Calls.get(), "Check 1 was called (needed for validation)");
        assertEquals(0, check2Calls.get(), "Check 2 was NOT called (abort prevented it)");
        assertEquals(0, dataCalls.get(), "Data loading was NOT called (abort prevented it)");
    }

    @Test
    @DisplayName("given lazy context when all validations pass then all operations executed in order")
    void given__lazy_context__when__all_validations_pass__then__all_operations_executed_in_order() {
        AtomicInteger check1Calls = new AtomicInteger(0);
        AtomicInteger check2Calls = new AtomicInteger(0);
        AtomicInteger dataCalls = new AtomicInteger(0);

        // Create lazy context - all operations will succeed
        var context = new LazyValidationContext(
            "VALID",
            () -> {
                check1Calls.incrementAndGet();
                return true;
            },
            () -> {
                check2Calls.incrementAndGet();
                return true;
            },
            () -> {
                dataCalls.incrementAndGet();
                return List.of("data");
            }
        );

        // Validation rules
        Rule<LazyValidationContext> rules = Rule.allOf(
            LAZY_STATUS.eq("VALID").onInvalidAndAbort("status.invalid"),
            LAZY_CHECK1.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check1.failed"),
            LAZY_CHECK2.satisfies(s -> s.getAsBoolean()).onInvalidAndAbort("check2.failed"),
            LAZY_DATA.satisfies(s -> !s.get().isEmpty())
        );

        List<Violation> violations = Rule.validate(context, rules);

        // All validations passed
        assertTrue(violations.isEmpty(), "Should have no violations");

        // All operations were called (as needed)
        assertEquals(1, check1Calls.get(), "Check 1 was called");
        assertEquals(1, check2Calls.get(), "Check 2 was called");
        assertEquals(1, dataCalls.get(), "Data loading was called");
    }

    @Test
    @DisplayName("given lazy context without abort when validation fails then all operations still executed")
    void given__lazy_context_without_abort__when__validation_fails__then__all_operations_still_executed() {
        AtomicInteger check1Calls = new AtomicInteger(0);
        AtomicInteger check2Calls = new AtomicInteger(0);
        AtomicInteger dataCalls = new AtomicInteger(0);

        // Create lazy context
        var context = new LazyValidationContext(
            "INVALID",  // Will fail
            () -> {
                check1Calls.incrementAndGet();
                return false;
            },
            () -> {
                check2Calls.incrementAndGet();
                return false;
            },
            () -> {
                dataCalls.incrementAndGet();
                return List.of();
            }
        );

        // Validation rules WITHOUT abort
        Rule<LazyValidationContext> rules = Rule.allOf(
            LAZY_STATUS.eq("VALID").onInvalid("status.invalid"),  // No abort!
            LAZY_CHECK1.satisfies(s -> s.getAsBoolean()).onInvalid("check1.failed"),
            LAZY_CHECK2.satisfies(s -> s.getAsBoolean()).onInvalid("check2.failed"),
            LAZY_DATA.satisfies(s -> !s.get().isEmpty()).onInvalid("data.empty")
        );

        List<Violation> violations = Rule.validate(context, rules);

        // All validations failed
        assertEquals(4, violations.size());

        // All operations were called (because no abort)
        assertEquals(1, check1Calls.get(), "Check 1 was called");
        assertEquals(1, check2Calls.get(), "Check 2 was called");
        assertEquals(1, dataCalls.get(), "Data loading was called");
    }

    // ========== Helper Methods ==========

    /**
     * Simulates an expensive boolean check (e.g., database query)
     */
    private boolean expensiveCheck(AtomicInteger counter) {
        counter.incrementAndGet();
        // Simulate expensive operation
        return true;
    }

    /**
     * Simulates expensive data loading (e.g., service call)
     */
    private List<String> expensiveData(AtomicInteger counter) {
        counter.incrementAndGet();
        // Simulate expensive operation
        return List.of("item1", "item2");
    }
}
