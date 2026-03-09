package sol.jara.franckie.validation.processor;

import sol.jara.franckie.validation.core.annotation.AttrName;
import sol.jara.franckie.validation.core.annotation.ValidationProjection;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts projection metadata from annotated record elements.
 */
public final class ProjectionModelExtractor {
    private static final Pattern CONSTANT_CASE = Pattern.compile("[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*");

    private final Elements elementUtils;
    private final Messager messager;

    public ProjectionModelExtractor(Elements elementUtils, Messager messager) {
        this.elementUtils = elementUtils;
        this.messager = messager;
    }

    public ProjectionModel extract(Element element) {
        validateProjectionElement(element);

        TypeElement typeElement = (TypeElement) element;
        PackageElement packageElement = elementUtils.getPackageOf(typeElement);

        String packageName = packageElement.getQualifiedName().toString();
        String projectionSimpleName = typeElement.getSimpleName().toString();
        String projectionQualifiedName = typeElement.getQualifiedName().toString();
        String generatedPackageName = packageName + ".generated";
        String generatedSimpleName = projectionSimpleName + "Attrs";

        List<ProjectionAttrModel> attrs = extractAttrs(typeElement);

        return new ProjectionModel(
                packageName,
                projectionSimpleName,
                projectionQualifiedName,
                generatedPackageName,
                generatedSimpleName,
                attrs
        );
    }

    private void validateProjectionElement(Element element) {
        if (!element.getKind().name().equals("RECORD")) {
            error(element, "@%s can only be used on records",
                    ValidationProjection.class.getSimpleName());
            throw new InvalidProjectionException();
        }

        if (!(element instanceof TypeElement)) {
            error(element, "Annotated element is not a TypeElement");
            throw new InvalidProjectionException();
        }
    }

    private List<ProjectionAttrModel> extractAttrs(TypeElement typeElement) {
        List<? extends RecordComponentElement> components = typeElement.getRecordComponents();
        List<ProjectionAttrModel> result = new ArrayList<>();
        Set<String> constantNames = new HashSet<>();

        for (RecordComponentElement component : components) {
            String componentName = component.getSimpleName().toString();
            String constantName = extractConstantName(component, componentName);
            String logicalAttrName = extractLogicalAttrName(component, componentName, constantName);
            TypeMirror typeMirror = component.asType();

            if (!constantNames.add(constantName)) {
                error(component,
                        "Duplicate generated constant name '%s' in projection '%s'",
                        constantName,
                        typeElement.getQualifiedName());
                throw new InvalidProjectionException();
            }

            result.add(new ProjectionAttrModel(
                    componentName,
                    constantName,
                    logicalAttrName,
                    typeMirror
            ));
        }

        return result;
    }

    private String extractConstantName(RecordComponentElement component, String componentName) {
        AttrName attrName = component.getAnnotation(AttrName.class);
        String defaultConstantName = toConstantName(componentName);

        if (attrName == null) {
            return defaultConstantName;
        }

        String explicitConstant = attrName.constant();
        if (explicitConstant != null && !explicitConstant.isBlank()) {
            validateConstantName(component, explicitConstant);
            return explicitConstant;
        }

        String value = attrName.value();
        if (value != null && !value.isBlank() && isConstantCase(value)) {
            return value;
        }

        return defaultConstantName;
    }

    private String extractLogicalAttrName(
            RecordComponentElement component,
            String componentName,
            String constantName
    ) {
        AttrName attrName = component.getAnnotation(AttrName.class);

        if (attrName == null) {
            return componentName;
        }

        String value = attrName.value();
        String constant = attrName.constant();

        if ((value == null || value.isBlank()) && (constant == null || constant.isBlank())) {
            error(component, "@%s value must not be blank", AttrName.class.getSimpleName());
            throw new InvalidProjectionException();
        }

        if (value == null || value.isBlank()) {
            return toLowerCamelCase(constantName);
        }

        if ((constant == null || constant.isBlank()) && isConstantCase(value)) {
            return toLowerCamelCase(value);
        }

        return value;
    }

    private void validateConstantName(RecordComponentElement component, String constantName) {
        if (!isConstantCase(constantName)) {
            error(component,
                    "@%s constant must be CONSTANT_CASE (letters, digits, underscore): '%s'",
                    AttrName.class.getSimpleName(),
                    constantName);
            throw new InvalidProjectionException();
        }
    }

    private static boolean isConstantCase(String input) {
        return input != null && CONSTANT_CASE.matcher(input).matches();
    }

    static String toLowerCamelCase(String constantCase) {
        if (constantCase == null || constantCase.isBlank()) {
            return constantCase;
        }

        String[] parts = constantCase.split("_+");
        if (parts.length == 0) {
            return constantCase;
        }

        StringBuilder result = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].toLowerCase(Locale.ROOT);
            if (part.isEmpty()) {
                continue;
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }

    /**
     * Converts camelCase to CONSTANT_CASE.
     * Examples: startDate -> START_DATE, myValue -> MY_VALUE
     */
    static String toConstantName(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];

            if (Character.isUpperCase(current) && i > 0 && chars[i - 1] != '_') {
                result.append('_');
            }

            result.append(Character.toUpperCase(current));
        }

        return result.toString().replace('-', '_');
    }

    private void error(Element element, String message, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(Locale.ROOT, message, args),
                element
        );
    }

    public static final class InvalidProjectionException extends RuntimeException {
    }
}
