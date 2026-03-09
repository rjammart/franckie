package sol.jara.franckie.validation.sample;

import sol.jara.franckie.validation.core.Attr;
import sol.jara.franckie.validation.core.Rule;

import java.util.UUID;

import static sol.jara.franckie.validation.sample.generated.SessionProjectionAttrs.*;


/**
 * Common validation rules for SessionProjection.
 * These rules can be reused across different contexts using contramap.
 */
public final class CommonSessionRules {

    //Used to show a mixed of generate and user defined Attr
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
