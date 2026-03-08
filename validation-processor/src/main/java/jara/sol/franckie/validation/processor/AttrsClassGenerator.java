package jara.sol.franckie.validation.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

/**
 * Generates an Attrs class with static Attr fields for a validation projection.
 *
 * Example output for a SessionProjection:
 * <pre>
 * {@code
 * package com.example.generated;
 *
 * import jara.sol.franckie.validation.core.Attr;
 * import com.example.SessionProjection;
 * import java.time.LocalDate;
 *
 * public final class SessionProjectionAttrs {
 *     public static final Attr<SessionProjection, String> NAME =
 *         new Attr<>("name", SessionProjection::name);
 *     public static final Attr<SessionProjection, LocalDate> START_DATE =
 *         new Attr<>("startDate", SessionProjection::startDate);
 *     public static final Attr<SessionProjection, LocalDate> END_DATE =
 *         new Attr<>("endDate", SessionProjection::endDate);
 *
 *     private SessionProjectionAttrs() {}
 * }
 * }
 * </pre>
 */
public final class AttrsClassGenerator {

    public JavaFile generate(ProjectionModel model) {

        ClassName projectionClass = ClassName.bestGuess(model.projectionQualifiedName());
        ClassName attrClass = ClassName.get("jara.sol.franckie.validation.core", "Attr");

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(model.generatedSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (ProjectionAttrModel attr : model.attrs()) {
            TypeName componentType = TypeName.get(attr.typeMirror());

            ParameterizedTypeName attrType = ParameterizedTypeName.get(
                    attrClass,
                    projectionClass,
                    componentType
            );

            FieldSpec field = FieldSpec.builder(attrType, attr.constantName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(CodeBlock.of(
                            "new $T<>($S, $T::$L)",
                            attrClass,
                            attr.logicalAttrName(),
                            projectionClass,
                            attr.componentName()
                    ))
                    .build();

            typeBuilder.addField(field);
        }

        typeBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );

        return JavaFile.builder(model.generatedPackageName(), typeBuilder.build())
                .build();
    }
}
