package jara.sol.franckie.validation.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Tests for the ValidationProjectionProcessor using Google's compile-testing library.
 */
class ValidationProjectionProcessorTest {

    @Test
    @DisplayName("given record with @ValidationProjection when processed then generates Attrs class")
    void given__record_with_validation_projection__when__processed__then__generates_attrs_class() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString(
                "test.SessionProjection",
                """
                package test;

                import jara.sol.franckie.validation.core.annotation.ValidationProjection;
                import java.time.LocalDate;

                @ValidationProjection
                public record SessionProjection(
                    String name,
                    LocalDate startDate,
                    LocalDate endDate,
                    Integer capacity
                ) {}
                """
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new ValidationProjectionProcessor())
                .compile(sourceFile);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.generated.SessionProjectionAttrs");
    }

    @Test
    @DisplayName("given record with @AttrName when processed then uses custom logical name")
    void given__record_with_attr_name__when__processed__then__uses_custom_logical_name() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString(
                "test.SessionProjection",
                """
                package test;

                import jara.sol.franckie.validation.core.annotation.ValidationProjection;
                import jara.sol.franckie.validation.core.annotation.AttrName;
                import java.time.LocalDate;

                @ValidationProjection
                public record SessionProjection(
                    String name,
                    @AttrName("session.end.date")
                    LocalDate endDate
                ) {}
                """
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new ValidationProjectionProcessor())
                .compile(sourceFile);

        assertThat(compilation).succeeded();

        // Verify the generated file contains the custom attribute name
        assertThat(compilation)
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .contentsAsUtf8String()
                .contains("\"session.end.date\"");
    }

    @Test
    @DisplayName("given record with camelCase field when processed then generates CONSTANT_CASE name")
    void given__record_with_camel_case_field__when__processed__then__generates_constant_case_name() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString(
                "test.TestProjection",
                """
                package test;

                import jara.sol.franckie.validation.core.annotation.ValidationProjection;

                @ValidationProjection
                public record TestProjection(
                    String startDate,
                    Integer maxCapacity
                ) {}
                """
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new ValidationProjectionProcessor())
                .compile(sourceFile);

        assertThat(compilation).succeeded();

        String generatedSource = compilation
                .generatedSourceFile("test.generated.TestProjectionAttrs")
                .map(jfo -> {
                    try {
                        return jfo.getCharContent(false).toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse("");

        // Verify constant names
        assertThat(compilation)
                .generatedSourceFile("test.generated.TestProjectionAttrs")
                .contentsAsUtf8String()
                .contains("START_DATE");

        assertThat(compilation)
                .generatedSourceFile("test.generated.TestProjectionAttrs")
                .contentsAsUtf8String()
                .contains("MAX_CAPACITY");
    }

    @Test
    @DisplayName("given class with @ValidationProjection when processed then fails with error")
    void given__class_with_validation_projection__when__processed__then__fails_with_error() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString(
                "test.InvalidProjection",
                """
                package test;

                import jara.sol.franckie.validation.core.annotation.ValidationProjection;

                @ValidationProjection
                public class InvalidProjection {
                    private String name;
                }
                """
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new ValidationProjectionProcessor())
                .compile(sourceFile);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("can only be used on records");
    }

    @Test
    @DisplayName("given generated Attrs class then contains correct Attr field types")
    void given__generated_attrs_class__then__contains_correct_attr_field_types() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString(
                "test.SessionProjection",
                """
                package test;

                import jara.sol.franckie.validation.core.annotation.ValidationProjection;
                import java.time.LocalDate;
                import java.util.UUID;

                @ValidationProjection
                public record SessionProjection(
                    String name,
                    LocalDate startDate,
                    Integer capacity,
                    UUID courseId
                ) {}
                """
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new ValidationProjectionProcessor())
                .compile(sourceFile);

        assertThat(compilation).succeeded();

        String generated = compilation
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .map(jfo -> {
                    try {
                        return jfo.getCharContent(false).toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse("");

        // Verify field types in generated code
        assertThat(compilation)
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .contentsAsUtf8String()
                .contains("Attr<SessionProjection, String> NAME");

        assertThat(compilation)
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .contentsAsUtf8String()
                .contains("Attr<SessionProjection, LocalDate> START_DATE");

        assertThat(compilation)
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .contentsAsUtf8String()
                .contains("Attr<SessionProjection, Integer> CAPACITY");

        assertThat(compilation)
                .generatedSourceFile("test.generated.SessionProjectionAttrs")
                .contentsAsUtf8String()
                .contains("Attr<SessionProjection, UUID> COURSE_ID");
    }
}
