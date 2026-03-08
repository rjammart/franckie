package jara.sol.franckie.validation.sample;

import jara.sol.franckie.validation.core.Attr;
import jara.sol.franckie.validation.core.Rule;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Common validation rules for SessionProjection.
 * These rules can be reused across different contexts using contramap.
 */
public final class CommonSessionRules {

    // Note: After annotation processing, you can use generated SessionProjectionAttrs class
    // For now, we define Attr instances manually
    private static final Attr<SessionProjection, String> NAME =
            Attr.of("name", SessionProjection::name);

    private static final Attr<SessionProjection, LocalDate> START_DATE =
            Attr.of("startDate", SessionProjection::startDate);

    private static final Attr<SessionProjection, LocalDate> END_DATE =
            Attr.of("endDate", SessionProjection::endDate);

    private static final Attr<SessionProjection, Integer> CAPACITY =
            Attr.of("capacity", SessionProjection::capacity);

    private static final Attr<SessionProjection, UUID> COURSE_ID =
            Attr.of("courseId", SessionProjection::courseId);

    public static final Rule<SessionProjection> NAME_REQUIRED =
            NAME.notNull();

    public static final Rule<SessionProjection> START_DATE_REQUIRED =
            START_DATE.notNull();

    public static final Rule<SessionProjection> END_DATE_REQUIRED =
            END_DATE.notNull();

    public static final Rule<SessionProjection> END_DATE_AFTER_START =
            END_DATE.gt(START_DATE);

    public static final Rule<SessionProjection> CAPACITY_POSITIVE =
            CAPACITY.notNull().and(CAPACITY.gt(0));

    public static final Rule<SessionProjection> COURSE_ID_REQUIRED =
            COURSE_ID.notNull();

    public static Rule<SessionProjection> all() {
        return Rule.allOf(
                NAME_REQUIRED,
                START_DATE_REQUIRED,
                END_DATE_REQUIRED,
                END_DATE_AFTER_START,
                CAPACITY_POSITIVE,
                COURSE_ID_REQUIRED
        );
    }

    private CommonSessionRules() {
        // Utility class
    }
}
