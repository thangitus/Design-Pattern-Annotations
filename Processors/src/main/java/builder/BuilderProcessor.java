package builder;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

@AutoService(Processor.class)
class BuilderProcessor extends AbstractProcessor {

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
      return false;
   }
}
