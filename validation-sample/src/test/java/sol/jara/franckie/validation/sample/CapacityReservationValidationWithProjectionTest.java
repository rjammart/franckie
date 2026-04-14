package sol.jara.franckie.validation.sample;

import sol.jara.franckie.validation.core.Attr;
import sol.jara.franckie.validation.core.CollectionValidator;
import sol.jara.franckie.validation.core.Command;
import sol.jara.franckie.validation.core.Rule;
import sol.jara.franckie.validation.core.Violation;
import sol.jara.franckie.validation.core.annotation.ValidationProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// Import generated Attrs classes (will be available after compilation)
import static sol.jara.franckie.validation.sample.generated.CapacityReservationAttrs.*;
import static sol.jara.franckie.validation.sample.generated.AddCapacityReservationsCommandAttrs.COMMAND_ID;
import static sol.jara.franckie.validation.sample.generated.AddCapacityReservationsCommandAttrs.NEW_RESERVATIONS;

// For DROP_ID, we'll use qualified names to avoid ambiguity with DropAttrs.DROP_ID
import sol.jara.franckie.validation.sample.generated.AddCapacityReservationsCommandAttrs;

/**
 * Second version of CapacityReservationValidationTest demonstrating the use of
 * @ValidationProjection annotation generator.
 *
 * This version shows how to:
 * - Annotate model records with @ValidationProjection
 * - Use generated *Attrs classes instead of manually defining Attr instances
 * - Maintain the same validation logic with cleaner attribute definitions
 */
public class CapacityReservationValidationWithProjectionTest {

    // ========== Test Models with @ValidationProjection ==========

    /**
     * Represents a capacity reservation for a drop.
     * The @ValidationProjection annotation will generate CapacityReservationAttrs
     * with static fields: TYPE, TARGET_ID, PARACHUTE_TYPE, CAPACITY
     */
    @ValidationProjection
    public record CapacityReservation(
        String type,           // "session", "activity", or "all-unit"
        UUID targetId,         // Required for session/activity, null for all-unit
        String parachuteType,  // Type of parachute
        Integer capacity       // Number of jumpers
    ) {}

    /**
     * Represents a drop with capacity reservations.
     * The @ValidationProjection annotation will generate DropAttrs
     * with static fields: DROP_ID, CAPACITY_RESERVATIONS
     */
    @ValidationProjection
    public record Drop(
        UUID dropId,
        List<CapacityReservation> capacityReservations
    ) {}

    /**
     * Command to add new capacity reservations to an existing drop.
     * The @ValidationProjection annotation will generate AddCapacityReservationsCommandAttrs
     * with static fields: COMMAND_ID, DROP_ID, NEW_RESERVATIONS
     */
    @ValidationProjection
    public record AddCapacityReservationsCommand(
        UUID commandId,
        UUID dropId,
        List<CapacityReservation> newReservations
    ) implements Command {
        @Override
        public UUID getCommandId() {
            return commandId;
        }
    }

    // ========== Validation Rules ==========
    // Note: We use the GENERATED Attrs classes imported at the top instead of manually defining them!

    /**
     * Common rules for individual capacity reservations.
     */
    static class CapacityReservationRules {

        // Type must be one of the valid values
        public static final Rule<CapacityReservation> TYPE_VALID =
            TYPE.notNull()
                .and(TYPE.in("session", "activity", "all-unit"))
                .onInvalid("cr.type.invalid");

        // Parachute type must be present
        public static final Rule<CapacityReservation> PARACHUTE_TYPE_REQUIRED =
            PARACHUTE_TYPE.notNull()
                .onInvalid("cr.parachuteType.required");

        // Capacity must be positive
        public static final Rule<CapacityReservation> CAPACITY_POSITIVE =
            CAPACITY.notNull()
                .and(CAPACITY.gt(0))
                .onInvalid("cr.capacity.positive");

        // Target ID required when type is "session" or "activity"
        public static final Rule<CapacityReservation> TARGET_ID_REQUIRED_FOR_SESSION_OR_ACTIVITY =
            TYPE.eq("session")
                .or(TYPE.eq("activity"))
                .guard(TARGET_ID.notNull().onInvalid("cr.targetId.required"));

        // Target ID must be null when type is "all-unit"
        public static final Rule<CapacityReservation> TARGET_ID_NULL_FOR_ALL_UNIT =
            TYPE.eq("all-unit")
                .guard(TARGET_ID.isNull().onInvalid("cr.targetId.mustBeNull"));

        // Combine all basic rules
        public static Rule<CapacityReservation> all() {
            return TYPE_VALID
                .and(PARACHUTE_TYPE_REQUIRED)
                .and(CAPACITY_POSITIVE)
                .and(TARGET_ID_REQUIRED_FOR_SESSION_OR_ACTIVITY)
                .and(TARGET_ID_NULL_FOR_ALL_UNIT);
        }
    }

    /**
     * Validation rules for AddCapacityReservationsCommand.
     *
     * Uses TWO-PASS VALIDATION PATTERN with generated Attrs.
     */
    static class AddCRRules {

        // Helper record for uniqueness checking
        record CRKey(String type, UUID targetId, String parachuteType) {}

        /**
         * Validates command structure and contextual concerns only.
         * Does NOT validate individual CRs.
         */
        public static Rule<AddCapacityReservationsCommand> contextOnly(
            Drop existingDrop,
            Supplier<List<UUID>> allowedActivityIdsSupplier
        ) {
            return basicValidation()
                .and(noConflictWithExisting(existingDrop))
                .and(activitiesAreAllowed(allowedActivityIdsSupplier));
        }

        /**
         * Complete two-pass validation helper.
         * Validates both individual CRs AND command context, collecting all violations.
         */
        public static List<Violation> validateTwoPass(
            AddCapacityReservationsCommand command,
            Drop existingDrop,
            Supplier<List<UUID>> allowedActivityIdsSupplier
        ) {
            List<Violation> allViolations = new ArrayList<>();

            // Pass 1: Validate each CR individually using CollectionValidator
            allViolations.addAll(
                CollectionValidator.validateEach(
                    command.newReservations(),
                    CapacityReservationRules.all()
                )
            );

            // Pass 2: Validate command-level context
            allViolations.addAll(
                Rule.validate(
                    command,
                    contextOnly(existingDrop, allowedActivityIdsSupplier)
                )
            );

            return allViolations;
        }

        /**
         * Single-pass validation with GLOBAL item validation.
         * Uses CollectionValidator.validateListAttr() for cleaner composition.
         */
        public static Rule<AddCapacityReservationsCommand> allWithGlobalItemValidation(
            Drop existingDrop,
            Supplier<List<UUID>> allowedActivityIdsSupplier
        ) {
            return basicValidation()
                .and(CollectionValidator.validateListAttr(
                    NEW_RESERVATIONS,  // Using generated Attr!
                    CapacityReservationRules.all(),
                    "command.newReservations.invalid"
                ))
                .and(noConflictWithExisting(existingDrop))
                .and(activitiesAreAllowed(allowedActivityIdsSupplier));
        }

        /**
         * Basic command-level validation.
         */
        private static Rule<AddCapacityReservationsCommand> basicValidation() {
            return COMMAND_ID.notNull().onInvalid("command.id.required")  // Using generated Attr!
                .and(AddCapacityReservationsCommandAttrs.DROP_ID.notNull().onInvalid("command.dropId.required"))  // Using qualified name to avoid ambiguity
                .and(NEW_RESERVATIONS.notNull().onInvalid("command.newReservations.required"));  // Using generated Attr!
        }

        /**
         * No conflict with existing reservations on the drop.
         */
        private static Rule<AddCapacityReservationsCommand> noConflictWithExisting(Drop existingDrop) {
            return NEW_RESERVATIONS.satisfies(  // Using generated Attr!
                newCRs -> {
                    if (newCRs == null) return true;

                    // Combine existing and new reservations
                    List<CapacityReservation> combined = new ArrayList<>();
                    if (existingDrop.capacityReservations() != null) {
                        combined.addAll(existingDrop.capacityReservations());
                    }
                    combined.addAll(newCRs);

                    // Check uniqueness
                    long uniqueCount = combined.stream()
                        .map(cr -> new CRKey(cr.type(), cr.targetId(), cr.parachuteType()))
                        .distinct()
                        .count();

                    return uniqueCount == combined.size();
                },
                "New reservations conflict with existing ones on the drop"
            ).onInvalid("command.reservations.conflict");
        }

        /**
         * All activities referenced in new reservations must be in the allowed list.
         */
        private static Rule<AddCapacityReservationsCommand> activitiesAreAllowed(
            Supplier<List<UUID>> allowedActivityIdsSupplier
        ) {
            return NEW_RESERVATIONS.satisfies(  // Using generated Attr!
                newCRs -> {
                    if (newCRs == null) return true;

                    List<UUID> allowedIds = allowedActivityIdsSupplier.get();

                    return newCRs.stream()
                        .filter(cr -> "activity".equals(cr.type()))
                        .map(CapacityReservation::targetId)
                        .allMatch(allowedIds::contains);
                },
                "Some activities are not allowed for this drop"
            ).onInvalid("command.activity.notAllowed");
        }
    }

    // ========== Individual CR Validation Tests ==========

    @Test
    @DisplayName("given valid session CR when validate then no violations")
    void given__valid_session_cr__when__validate__then__no_violations() {
        // Given
        CapacityReservation cr = new CapacityReservation(
            "session",
            UUID.randomUUID(),
            "tandem",
            10
        );

        // When
        List<Violation> violations = Rule.validate(cr, CapacityReservationRules.all());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("given invalid type when validate then violation")
    void given__invalid_type__when__validate__then__violation() {
        // Given
        CapacityReservation cr = new CapacityReservation(
            "invalid-type",
            UUID.randomUUID(),
            "tandem",
            10
        );

        // When
        List<Violation> violations = Rule.validate(cr, CapacityReservationRules.all());

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("cr.type.invalid")));
    }

    @Test
    @DisplayName("given missing target ID for session when validate then violation")
    void given__missing_target_id_for_session__when__validate__then__violation() {
        // Given
        CapacityReservation cr = new CapacityReservation(
            "session",
            null,  // Should have target ID for session
            "tandem",
            10
        );

        // When
        List<Violation> violations = Rule.validate(cr, CapacityReservationRules.all());

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("cr.targetId.required")));
    }

    // ========== Command Validation Tests with Context ==========

    @Test
    @DisplayName("given valid command with no conflicts when validate then no violations")
    void given__valid_command_with_no_conflicts__when__validate__then__no_violations() {
        // Given - Existing drop with one reservation
        UUID sessionId = UUID.randomUUID();
        Drop existingDrop = new Drop(
            UUID.randomUUID(),
            List.of(
                new CapacityReservation("session", sessionId, "tandem", 10)
            )
        );

        // Command to add different reservations (no conflict)
        UUID activityId = UUID.randomUUID();
        AddCapacityReservationsCommand command = new AddCapacityReservationsCommand(
            UUID.randomUUID(),
            existingDrop.dropId(),
            List.of(
                new CapacityReservation("session", sessionId, "sport", 5),  // Same session, different parachute
                new CapacityReservation("activity", activityId, "tandem", 8)  // Different type
            )
        );

        // Context - allowed activities
        Supplier<List<UUID>> allowedActivities = () -> List.of(activityId);

        // When - Using two-pass validation
        List<Violation> violations = AddCRRules.validateTwoPass(
            command,
            existingDrop,
            allowedActivities
        );

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("given command with conflicting reservation when validate then violation")
    void given__command_with_conflicting_reservation__when__validate__then__violation() {
        // Given - Existing drop with session + tandem reservation
        UUID sessionId = UUID.randomUUID();
        Drop existingDrop = new Drop(
            UUID.randomUUID(),
            List.of(
                new CapacityReservation("session", sessionId, "tandem", 10)
            )
        );

        // Command tries to add SAME combination (session + tandem)
        AddCapacityReservationsCommand command = new AddCapacityReservationsCommand(
            UUID.randomUUID(),
            existingDrop.dropId(),
            List.of(
                new CapacityReservation("session", sessionId, "tandem", 15)  // CONFLICT!
            )
        );

        // Context
        Supplier<List<UUID>> allowedActivities = () -> List.of();

        // When
        List<Violation> violations = AddCRRules.validateTwoPass(
            command,
            existingDrop,
            allowedActivities
        );

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("command.reservations.conflict")));
    }

    @Test
    @DisplayName("given command with invalid CR using two-pass when validate then detailed violations")
    void given__command_with_invalid_cr_two_pass__when__validate__then__detailed_violations() {
        // Given
        Drop existingDrop = new Drop(UUID.randomUUID(), List.of());

        AddCapacityReservationsCommand command = new AddCapacityReservationsCommand(
            UUID.randomUUID(),
            existingDrop.dropId(),
            List.of(
                new CapacityReservation("invalid-type", UUID.randomUUID(), "tandem", 10),
                new CapacityReservation("session", null, "sport", 0)  // Missing target ID, zero capacity
            )
        );

        Supplier<List<UUID>> allowedActivities = () -> List.of();

        // When - using two-pass helper
        List<Violation> violations = AddCRRules.validateTwoPass(
            command,
            existingDrop,
            allowedActivities
        );

        // Then - we get DETAILED violations from each CR
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 3, "Should have multiple violations from both CRs");

        // First CR violations
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("cr.type.invalid")));

        // Second CR violations
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("cr.targetId.required")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.translationKey().equals("cr.capacity.positive")));
    }

    // ========== Single-Pass Validation Tests (Option 3: Global Item Validation) ==========

    @Test
    @DisplayName("given valid command using single-pass global validation when validate then no violations")
    void given__valid_command_single_pass_global__when__validate__then__no_violations() {
        // Given
        Drop existingDrop = new Drop(UUID.randomUUID(), List.of());

        UUID activityId = UUID.randomUUID();
        AddCapacityReservationsCommand command = new AddCapacityReservationsCommand(
            UUID.randomUUID(),
            existingDrop.dropId(),
            List.of(
                new CapacityReservation("session", UUID.randomUUID(), "tandem", 10),
                new CapacityReservation("activity", activityId, "sport", 5)
            )
        );

        Supplier<List<UUID>> allowedActivities = () -> List.of(activityId);

        // When - Single pass with global item validation
        List<Violation> violations = Rule.validate(
            command,
            AddCRRules.allWithGlobalItemValidation(existingDrop, allowedActivities)
        );

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("given command with invalid CRs using single-pass global when validate then ONE global violation")
    void given__invalid_crs_single_pass_global__when__validate__then__one_global_violation() {
        // Given - Command with multiple invalid CRs
        Drop existingDrop = new Drop(UUID.randomUUID(), List.of());

        AddCapacityReservationsCommand command = new AddCapacityReservationsCommand(
            UUID.randomUUID(),
            existingDrop.dropId(),
            List.of(
                new CapacityReservation("invalid-type", UUID.randomUUID(), "tandem", 10),  // Invalid type
                new CapacityReservation("session", null, "sport", 0)  // Missing target ID, zero capacity
            )
        );

        Supplier<List<UUID>> allowedActivities = () -> List.of();

        // When - Single pass with global item validation
        List<Violation> violations = Rule.validate(
            command,
            AddCRRules.allWithGlobalItemValidation(existingDrop, allowedActivities)
        );

        // Then - We get ONE global violation (not detailed per-item)
        assertFalse(violations.isEmpty());

        // Should have exactly ONE violation for invalid items
        long itemValidationViolations = violations.stream()
            .filter(v -> v.translationKey().equals("command.newReservations.invalid"))
            .count();

        assertEquals(1, itemValidationViolations,
            "Should have exactly ONE global violation for invalid items (not detailed per-item violations)");
    }

    @Test
    @DisplayName("given collection validator when validate list then all violations collected")
    void given__collection_validator__when__validate_list__then__all_violations_collected() {
        // Given - List with multiple invalid CRs
        List<CapacityReservation> crs = List.of(
            new CapacityReservation("invalid-type", UUID.randomUUID(), "tandem", 10),
            new CapacityReservation("session", null, "sport", 5),  // Missing target ID
            new CapacityReservation("all-unit", UUID.randomUUID(), "student", 0)  // Has target ID, zero capacity
        );

        // When - Using CollectionValidator utility
        List<Violation> violations = CollectionValidator.validateEach(
            crs,
            CapacityReservationRules.all()
        );

        // Then - All violations from all items collected
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 3, "Should have multiple violations from different CRs");

        // Verify specific violations
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("cr.type.invalid")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("cr.targetId.required")));
        assertTrue(violations.stream().anyMatch(v -> v.translationKey().equals("cr.targetId.mustBeNull")));
    }
}
