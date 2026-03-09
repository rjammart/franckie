package sol.jara.franckie.validation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a record as a validation projection.
 * An annotation processor will generate an Attrs class with static Attr fields
 * for each record component, making it easy to reference attributes in validation rules.
 *
 * Example:
 * <pre>
 * {@code
 * @ValidationProjection
 * public record SessionProjection(
 *     String name,
 *     LocalDate startDate,
 *     LocalDate endDate
 * ) {}
 * }
 * </pre>
 *
 * This will generate a class {@code SessionProjectionAttrs} with static fields:
 * <pre>
 * {@code
 * public final class SessionProjectionAttrs {
 *     public static final Attr<SessionProjection, String> NAME = ...;
 *     public static final Attr<SessionProjection, LocalDate> START_DATE = ...;
 *     public static final Attr<SessionProjection, LocalDate> END_DATE = ...;
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ValidationProjection {
}
