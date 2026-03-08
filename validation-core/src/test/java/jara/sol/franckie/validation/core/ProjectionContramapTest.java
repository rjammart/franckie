package jara.sol.franckie.validation.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests demonstrating the projection and contramap pattern:
 * - Define common attributes and rules on a projection
 * - Use contramap to apply projection rules to different validation contexts
 * - Combine common (projected) rules with context-specific rules
 */
class ProjectionContramapTest {

    // ========== Common Projection ==========
    // This represents data shared across multiple validation contexts
    record SessionProjection(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Integer capacity,
            UUID courseId
    ) {}

    // ========== Validation Contexts ==========
    // Different contexts that contain the common projection plus specific data
    record CreateSessionContext(
            SessionProjection projection,
            boolean sessionNameAlreadyExists,
            String status
    ) {}

    record UpdateSessionContext(
            SessionProjection projection,
            SessionProjection existingSession,
            String status
    ) {}

    // ========== Projection Attributes ==========
    // Define attributes on the projection
    private static final Attr<SessionProjection, String> NAME = Attr.of("name", SessionProjection::name);
    private static final Attr<SessionProjection, LocalDate> START_DATE = Attr.of("startDate", SessionProjection::startDate);
    private static final Attr<SessionProjection, LocalDate> END_DATE = Attr.of("endDate", SessionProjection::endDate);
    private static final Attr<SessionProjection, Integer> CAPACITY = Attr.of("capacity", SessionProjection::capacity);
    private static final Attr<SessionProjection, UUID> COURSE_ID = Attr.of("courseId", SessionProjection::courseId);

    // ========== Context-Specific Attributes ==========
    private static final Attr<CreateSessionContext, Boolean> NAME_EXISTS = Attr.of("nameExists", CreateSessionContext::sessionNameAlreadyExists);
    private static final Attr<CreateSessionContext, String> CREATE_STATUS = Attr.of("status", CreateSessionContext::status);
    private static final Attr<UpdateSessionContext, String> UPDATE_STATUS = Attr.of("status", UpdateSessionContext::status);
    private static final Attr<UpdateSessionContext, SessionProjection> EXISTING = Attr.of("existing", UpdateSessionContext::existingSession);

    // Attributes for comparing new vs existing projection dates
    private static final Attr<UpdateSessionContext, LocalDate> NEW_START_DATE = Attr.of("newStartDate", ctx -> ctx.projection().startDate());
    private static final Attr<UpdateSessionContext, LocalDate> EXISTING_START_DATE = Attr.of("existingStartDate", ctx -> ctx.existingSession().startDate());
    private static final Attr<UpdateSessionContext, LocalDate> NEW_END_DATE = Attr.of("newEndDate", ctx -> ctx.projection().endDate());
    private static final Attr<UpdateSessionContext, LocalDate> EXISTING_END_DATE = Attr.of("existingEndDate", ctx -> ctx.existingSession().endDate());

    // ========== Common Projection Rules ==========
    // These rules are defined once on the projection and reused across contexts
    static class CommonSessionRules {
        static final Rule<SessionProjection> NAME_REQUIRED =
                NAME.notNull().onInvalid("session.name.required");

        static final Rule<SessionProjection> START_DATE_REQUIRED =
                START_DATE.notNull().onInvalid("session.start.date.required");

        static final Rule<SessionProjection> END_DATE_AFTER_START =
                Rule.guard(
                        START_DATE.notNull().and(END_DATE.notNull()),
                        START_DATE.lt(END_DATE).onInvalid("session.end.date.must.be.after.start")
                );

        static final Rule<SessionProjection> CAPACITY_IN_RANGE =
                Rule.guard(
                        CAPACITY.notNull(),
                        CAPACITY.gt(0)
                                .and(CAPACITY.lt(500))
                                .onInvalid("session.capacity.must.be.between.1.and.500")
                );

        static final Rule<SessionProjection> COURSE_ID_REQUIRED =
                COURSE_ID.notNull().onInvalid("session.course.id.required");

        // Combine all common rules
        static Rule<SessionProjection> all() {
            return Rule.allOf(
                    NAME_REQUIRED,
                    START_DATE_REQUIRED,
                    END_DATE_AFTER_START,
                    CAPACITY_IN_RANGE,
                    COURSE_ID_REQUIRED
            );
        }
    }

    // ========== Context-Specific Rules ==========
    static class CreateSessionRules {
        // Rule specific to creation: name must be unique
        static final Rule<CreateSessionContext> NAME_MUST_BE_UNIQUE =
                NAME_EXISTS.eq(false).onInvalid("session.name.must.be.unique");

        // Rule specific to creation: status must be PENDING
        static final Rule<CreateSessionContext> STATUS_MUST_BE_PENDING =
                CREATE_STATUS.eq("PENDING").onInvalidAndAbort("session.status.must.be.pending");

        // Combine context-specific rules with projection rules using contramap
        static Rule<CreateSessionContext> all() {
            return Rule.allOf(
                    STATUS_MUST_BE_PENDING,
                    NAME_MUST_BE_UNIQUE,
                    // Apply projection rules to context using contramap
                    CommonSessionRules.all().contramap(CreateSessionContext::projection)
            );
        }
    }

    static class UpdateSessionRules {
        // Rule specific to update: status must be CREATED or PUBLISHED
        static final Rule<UpdateSessionContext> STATUS_MUST_BE_CREATED_OR_PUBLISHED =
                UPDATE_STATUS.in("CREATED", "PUBLISHED")
                        .onInvalidAndAbort("session.status.must.be.created.or.published");

        // Rule specific to update: dates cannot be changed if status is PUBLISHED
        static Rule<UpdateSessionContext> datesCannotChangeIfPublished() {
            var statusIsPublished = UPDATE_STATUS.eq("PUBLISHED");

            var startDateUnchanged = NEW_START_DATE.eq(EXISTING_START_DATE)
                    .onInvalid("session.start.date.cannot.change.when.published");

            var endDateUnchanged = NEW_END_DATE.eq(EXISTING_END_DATE)
                    .onInvalid("session.end.date.cannot.change.when.published");

            return Rule.guard(
                    statusIsPublished,
                    startDateUnchanged.and(endDateUnchanged)
            );
        }

        // Combine context-specific rules with projection rules using contramap
        static Rule<UpdateSessionContext> all() {
            return Rule.allOf(
                    STATUS_MUST_BE_CREATED_OR_PUBLISHED,
                    datesCannotChangeIfPublished(),
                    // Apply projection rules to context using contramap
                    CommonSessionRules.all().contramap(UpdateSessionContext::projection)
            );
        }
    }

    // ========== Tests ==========

    @Test
    @DisplayName("given valid create context when validate then no violations")
    void given__valid_create_context__when__validate__then__no_violations() {
        var projection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        var context = new CreateSessionContext(projection, false, "PENDING");

        List<Violation> violations = Rule.validate(context, CreateSessionRules.all());

        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("given create context with invalid projection when validate then projection violations")
    void given__create_context_with_invalid_projection__when__validate__then__projection_violations() {
        // Invalid projection: end date before start date
        var projection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 5),
                LocalDate.of(2024, 2, 1),
                20,
                UUID.randomUUID()
        );
        var context = new CreateSessionContext(projection, false, "PENDING");

        List<Violation> violations = Rule.validate(context, CreateSessionRules.all());

        assertEquals(1, violations.size());
        assertEquals("session.end.date.must.be.after.start", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given create context with existing name when validate then name uniqueness violation")
    void given__create_context_with_existing_name__when__validate__then__name_uniqueness_violation() {
        var projection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        // Name already exists
        var context = new CreateSessionContext(projection, true, "PENDING");

        List<Violation> violations = Rule.validate(context, CreateSessionRules.all());

        assertEquals(1, violations.size());
        assertEquals("session.name.must.be.unique", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given create context with wrong status when validate then status violation and abort")
    void given__create_context_with_wrong_status__when__validate__then__status_violation_and_abort() {
        // Invalid projection data but should not be evaluated due to abort
        var projection = new SessionProjection(
                null, // null name
                LocalDate.of(2024, 2, 5),
                LocalDate.of(2024, 2, 1), // end before start
                -10, // invalid capacity
                null // null course ID
        );
        var context = new CreateSessionContext(projection, true, "CREATED"); // Wrong status

        List<Violation> violations = Rule.validate(context, CreateSessionRules.all());

        // Only status violation because abort stops further validation
        assertEquals(1, violations.size());
        assertEquals("session.status.must.be.pending", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given valid update context with CREATED status when validate then no violations")
    void given__valid_update_context_with_created_status__when__validate__then__no_violations() {
        var existingProjection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        var newProjection = new SessionProjection(
                "Java Training Updated",
                LocalDate.of(2024, 3, 1), // Changed date
                LocalDate.of(2024, 3, 5), // Changed date
                25, // Changed capacity
                existingProjection.courseId()
        );
        var context = new UpdateSessionContext(newProjection, existingProjection, "CREATED");

        List<Violation> violations = Rule.validate(context, UpdateSessionRules.all());

        assertTrue(violations.isEmpty(), "Should have no violations - dates can change when CREATED");
    }

    @Test
    @DisplayName("given update context with PUBLISHED status and changed dates when validate then date change violation")
    void given__update_context_with_published_status_and_changed_dates__when__validate__then__date_change_violation() {
        var existingProjection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        var newProjection = new SessionProjection(
                "Java Training Updated",
                LocalDate.of(2024, 3, 1), // Changed date - not allowed when PUBLISHED
                LocalDate.of(2024, 3, 5), // Changed date - not allowed when PUBLISHED
                25,
                existingProjection.courseId()
        );
        var context = new UpdateSessionContext(newProjection, existingProjection, "PUBLISHED");

        List<Violation> violations = Rule.validate(context, UpdateSessionRules.all());

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
            v.translationKey().equals("session.start.date.cannot.change.when.published")));
        assertTrue(violations.stream().anyMatch(v ->
            v.translationKey().equals("session.end.date.cannot.change.when.published")));
    }

    @Test
    @DisplayName("given update context with PUBLISHED status and unchanged dates when validate then no violations")
    void given__update_context_with_published_status_and_unchanged_dates__when__validate__then__no_violations() {
        var existingProjection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        var newProjection = new SessionProjection(
                "Java Training Updated",
                LocalDate.of(2024, 2, 1), // Unchanged
                LocalDate.of(2024, 2, 5), // Unchanged
                25, // Capacity can change
                existingProjection.courseId()
        );
        var context = new UpdateSessionContext(newProjection, existingProjection, "PUBLISHED");

        List<Violation> violations = Rule.validate(context, UpdateSessionRules.all());

        assertTrue(violations.isEmpty(), "Should have no violations - dates unchanged");
    }

    @Test
    @DisplayName("given update context with wrong status when validate then status violation and abort")
    void given__update_context_with_wrong_status__when__validate__then__status_violation_and_abort() {
        var existingProjection = new SessionProjection(
                "Java Training",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        // Invalid projection data but should not be evaluated due to abort
        var newProjection = new SessionProjection(
                null, // null name
                null, // null start date
                null, // null end date
                null, // null capacity
                null  // null course ID
        );
        var context = new UpdateSessionContext(newProjection, existingProjection, "PENDING"); // Wrong status

        List<Violation> violations = Rule.validate(context, UpdateSessionRules.all());

        // Only status violation because abort stops further validation
        assertEquals(1, violations.size());
        assertEquals("session.status.must.be.created.or.published", violations.get(0).translationKey());
    }

    @Test
    @DisplayName("given context with multiple projection violations when validate then all collected")
    void given__context_with_multiple_projection_violations__when__validate__then__all_collected() {
        // Multiple invalid fields in projection
        var projection = new SessionProjection(
                null, // null name
                LocalDate.of(2024, 2, 5),
                LocalDate.of(2024, 2, 1), // end before start
                -10, // invalid capacity
                null  // null course ID
        );
        var context = new CreateSessionContext(projection, false, "PENDING");

        List<Violation> violations = Rule.validate(context, CreateSessionRules.all());

        // Should have violations for: name, dates, capacity, courseId
        assertTrue(violations.size() >= 4, "Should have at least 4 violations");
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("session.name.required")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("session.end.date.must.be.after.start")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("session.capacity.must.be.between.1.and.500")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("session.course.id.required")));
    }

    @Test
    @DisplayName("given projection rules when applied via contramap to context then rules work correctly")
    void given__projection_rules__when__applied_via_contramap_to_context__then__rules_work_correctly() {
        // Test contramap directly
        var projection = new SessionProjection(
                null, // null name - should trigger violation
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 5),
                20,
                UUID.randomUUID()
        );
        var context = new CreateSessionContext(projection, false, "PENDING");

        // Apply projection rule directly to context via contramap
        Rule<CreateSessionContext> projectionRuleOnContext =
                CommonSessionRules.NAME_REQUIRED.contramap(CreateSessionContext::projection);

        List<Violation> violations = Rule.validate(context, projectionRuleOnContext);

        assertEquals(1, violations.size());
        assertEquals("session.name.required", violations.get(0).translationKey());
    }
}
