package jara.sol.franckie.validation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows specifying a custom logical name for a record component in validation rules.
 * By default, the component name is used as the logical attribute name.
 *
 * Example:
 * <pre>
 * {@code
 * @ValidationProjection
 * public record SessionProjection(
 *     @AttrName("session.name")
 *     String name,
 *     LocalDate startDate
 * ) {}
 * }
 * </pre>
 *
 * In validation error messages, "name" will be referred to as "session.name".
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.SOURCE)
public @interface AttrName {
    String value();
}
