package sol.jara.franckie.validation.sample;

import sol.jara.franckie.validation.core.Rule;
import sol.jara.franckie.validation.core.Violation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the Franckie validation framework with session validation examples.
 */
class SessionValidationTest {

    @Test
    @DisplayName("given valid session projection when validate then no violations")
    void given__valid_session_projection__when__validate__then__no_violations() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );

        // When
        List<Violation> violations = Rule.validate(projection, CommonSessionRules.all());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("given missing name when validate then violation")
    void given__missing_name__when__validate__then__violation() {
        // Given
        SessionProjection projection = new SessionProjection(
                null,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );

        // When
        List<Violation> violations = Rule.validate(projection, CommonSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> !v.result().matched()));
    }

    @Test
    @DisplayName("given end date before start date when validate then violation")
    void given__end_date_before_start_date__when__validate__then__violation() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 5),
                LocalDate.of(2026, 4, 1),
                20,
                UUID.randomUUID()
        );

        // When
        List<Violation> violations = Rule.validate(projection, CommonSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> !v.result().matched()));
    }

    @Test
    @DisplayName("given zero capacity when validate then violation")
    void given__zero_capacity__when__validate__then__violation() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                0,
                UUID.randomUUID()
        );

        // When
        List<Violation> violations = Rule.validate(projection, CommonSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("given valid create session command when validate then no violations")
    void given__valid_create_session_command__when__validate__then__no_violations() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );
        CreateSessionCommand command = new CreateSessionCommand(
                UUID.randomUUID(),
                projection,
                false,
                "PENDING"
        );

        // When
        List<Violation> violations = Rule.validate(command, CreateSessionRules.all());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("given duplicate session name when validate then violation")
    void given__duplicate_session_name__when__validate__then__violation() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );
        CreateSessionCommand command = new CreateSessionCommand(
                UUID.randomUUID(),
                projection,
                true, // Session name already exists
                "PENDING"
        );

        // When
        List<Violation> violations = Rule.validate(command, CreateSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("given non-pending status when validate then violation")
    void given__non_pending_status__when__validate__then__violation() {
        // Given
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );
        CreateSessionCommand command = new CreateSessionCommand(
                UUID.randomUUID(),
                projection,
                false,
                "ACTIVE" // Invalid status for creation
        );

        // When
        List<Violation> violations = Rule.validate(command, CreateSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("given invalid projection in command when validate then violations include projection errors")
    void given__invalid_projection_in_command__when__validate__then__violations_include_projection_errors() {
        // Given
        SessionProjection invalidProjection = new SessionProjection(
                null, // Missing name
                LocalDate.of(2026, 4, 5),
                LocalDate.of(2026, 4, 1), // End before start
                -5, // Negative capacity
                null // Missing course ID
        );
        CreateSessionCommand command = new CreateSessionCommand(
                UUID.randomUUID(),
                invalidProjection,
                true,
                "PENDING"
        );

        // When
        List<Violation> violations = Rule.validate(command, CreateSessionRules.all());

        // Then
        assertFalse(violations.isEmpty());
        // Should have multiple violations from the projection
        assertTrue(violations.size() == 5);
    }

    @Test
    @DisplayName("given contramap pattern when validate then common rules apply to nested projection")
    void given__contramap_pattern__when__validate__then__common_rules_apply_to_nested_projection() {
        // Given - This test demonstrates the contramap pattern
        SessionProjection projection = new SessionProjection(
                "Spring Boot Training",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                20,
                UUID.randomUUID()
        );

        // Direct validation of projection
        List<Violation> directViolations = Rule.validate(projection, CommonSessionRules.all());

        // Validation via contramap in command context
        CreateSessionCommand command = new CreateSessionCommand(
                UUID.randomUUID(),
                projection,
                false,
                "PENDING"
        );
        List<Violation> contramapViolations = Rule.validate(command, CreateSessionRules.all());

        // Then - Both should pass (no violations)
        assertTrue(directViolations.isEmpty());
        assertTrue(contramapViolations.isEmpty());
    }
}
