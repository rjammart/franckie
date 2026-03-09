package sol.jara.franckie.validation.sample;

import sol.jara.franckie.validation.core.annotation.ValidationProjection;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Projection for session-related validation.
 * The @ValidationProjection annotation will generate a SessionProjectionAttrs class
 * with static Attr fields for each component.
 */
@ValidationProjection
public record SessionProjection(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Integer capacity,
        UUID courseId
) {
}
