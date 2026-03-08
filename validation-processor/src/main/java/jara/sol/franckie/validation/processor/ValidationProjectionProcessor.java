package jara.sol.franckie.validation.processor;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.JavaFile;
import jara.sol.franckie.validation.core.annotation.ValidationProjection;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

/**
 * Annotation processor for @ValidationProjection.
 * Generates an Attrs class with static Attr fields for each record component.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("jara.sol.franckie.validation.core.annotation.ValidationProjection")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class ValidationProjectionProcessor extends AbstractProcessor {

    private ProjectionModelExtractor extractor;
    private AttrsClassGenerator generator;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.extractor = new ProjectionModelExtractor(
                processingEnv.getElementUtils(),
                processingEnv.getMessager()
        );
        this.generator = new AttrsClassGenerator();
        this.messager = processingEnv.getMessager();

        messager.printMessage(Diagnostic.Kind.NOTE, "ValidationProjectionProcessor initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "ValidationProjectionProcessor processing round");

        for (Element element : roundEnv.getElementsAnnotatedWith(ValidationProjection.class)) {
            try {
                ProjectionModel model = extractor.extract(element);
                JavaFile javaFile = generator.generate(model);

                javaFile.writeTo(processingEnv.getFiler());

                messager.printMessage(
                        Diagnostic.Kind.NOTE,
                        "Generated " + model.generatedSimpleName() + " for " + model.projectionSimpleName()
                );
            } catch (ProjectionModelExtractor.InvalidProjectionException e) {
                // Error already reported by extractor
            } catch (IOException e) {
                error(element, "Failed to generate attrs class: %s", e.getMessage());
            }
        }
        return true;
    }

    private void error(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.format(message, args),
                element
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ValidationProjection.class.getCanonicalName());
    }
}
