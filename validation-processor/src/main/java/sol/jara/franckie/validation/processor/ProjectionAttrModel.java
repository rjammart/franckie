package sol.jara.franckie.validation.processor;

import javax.lang.model.type.TypeMirror;

/**
 * Model representing a single attribute in a validation projection.
 *
 * @param componentName The name of the record component
 * @param constantName The generated constant name (e.g., "START_DATE" for "startDate")
 * @param logicalAttrName The logical name used in validation messages (from @AttrName or component name)
 * @param typeMirror The type of the attribute
 */
public record ProjectionAttrModel(
        String componentName,
        String constantName,
        String logicalAttrName,
        TypeMirror typeMirror
) {
}
