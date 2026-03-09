package jara.sol.franckie.validation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows customizing generated Attr metadata for a record component.
 *
 * <p>By default:
 * <ul>
 *   <li>Logical name = component name (used in validation messages)</li>
 *   <li>Generated constant = CONSTANT_CASE(component name)</li>
 * </ul>
 *
 * <p>Usage patterns:
 * <ul>
 *   <li>{@code @AttrName("session.status")} -> custom logical name only</li>
 *   <li>{@code @AttrName("CREATE_SESSION_STATUS")} -> shorthand: custom constant and derived logical name
 *       ({@code createSessionStatus})</li>
 *   <li>{@code @AttrName(value = "session.status", constant = "CREATE_SESSION_STATUS")} -> explicit both</li>
 * </ul>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.SOURCE)
public @interface AttrName {
    /**
     * Logical attribute name used in validation messages.
     */
    String value() default "";

    /**
     * Optional generated constant name override.
     */
    String constant() default "";
}
