package builder;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import java.util.LinkedHashSet;
import java.util.Set;

import utils.ProcessingException;

@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {
    private static Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Builder.class.getCanonicalName());
        return annotations;
    }
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Builder.class)) {
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessingException(annotatedElement,
                                                  "Only class can be annotated with @%s",
                                                  Builder.class.getSimpleName());
                }
                Builder builder = annotatedElement.getAnnotation(Builder.class);
                int buildType = builder.buildType();
                TypeElement annotatedType = (TypeElement) annotatedElement;
                BuilderAnnotatedType builderAnnotatedType;
                if (buildType == BuildType.SETTER)
                    builderAnnotatedType = new SetterBuilder(annotatedType, elementUtils);
                else
                    builderAnnotatedType = new ConstructorBuilder(annotatedType, elementUtils);
                builderAnnotatedType.generateCode(filer);
            }
        } catch (ProcessingException e) {
            error(e.getElement(), e.getMessage());
        }

        return false;
    }

    /**
     * Prints an error message
     *
     * @param e
     *         The element which has caused the error. Can be null
     * @param msg
     *         The error message
     */
    public static void error(Element e, String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}
