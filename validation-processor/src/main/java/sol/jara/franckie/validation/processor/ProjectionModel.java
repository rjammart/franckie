package sol.jara.franckie.validation.processor;

import java.util.List;

/**
 * Model representing a validation projection and metadata for generating its Attrs class.
 *
 * @param packageName The package of the projection record
 * @param projectionSimpleName The simple name of the projection (e.g., "SessionProjection")
 * @param projectionQualifiedName The fully qualified name of the projection
 * @param generatedPackageName The package for the generated Attrs class
 * @param generatedSimpleName The simple name of the generated Attrs class (e.g., "SessionProjectionAttrs")
 * @param attrs The list of attributes in the projection
 */
public record ProjectionModel(
        String packageName,
        String projectionSimpleName,
        String projectionQualifiedName,
        String generatedPackageName,
        String generatedSimpleName,
        List<ProjectionAttrModel> attrs
) {
}
