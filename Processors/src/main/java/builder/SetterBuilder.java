package builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import utils.ProcessingException;

class SetterBuilder extends BuilderAnnotatedType {
    private final String fieldName = "target";

    public SetterBuilder(TypeElement annotatedType, Elements elementUtils) {
        super(annotatedType, elementUtils);
    }

    @Override
    protected void generateFieldsAndMethods() {
        TypeName targetType = ClassName.get(annotatedType.asType());
        FieldSpec target = FieldSpec.builder(targetType, fieldName).addModifiers(Modifier.PRIVATE)
                                    .build();
        fieldSpecs.add(target);

        constructorBuilder.addStatement("this.$N = new $N()", fieldName, typeSimpleName);
        methodSpecs.add(constructorBuilder.build());

        TypeName builderTypeName = ClassName.get(packageName, className);
        try {
            for (Element enclosedElement : annotatedType.getEnclosedElements()) {
                if (enclosedElement.getKind().equals(ElementKind.FIELD)) {

                    if (enclosedElement.getAnnotation(Ignore.class) != null)
                        continue;

                    String fieldName = enclosedElement.getSimpleName().toString();
                    TypeName fieldType = TypeName.get(enclosedElement.asType());

                    String methodName = "set" + Character
                            .toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                                                                 .addModifiers(Modifier.PUBLIC)
                                                                 .returns(builderTypeName)
                                                                 .addParameter(fieldType,
                                                                               fieldName);
                    Set<Modifier> modifierSet = enclosedElement.getModifiers();
                    boolean addSetStatement = false;
                    for (Element executableElement : annotatedType.getEnclosedElements()) {
                        if (executableElement.getKind()
                                             .equals(ElementKind.METHOD) && !executableElement
                                .getModifiers().contains(Modifier.PRIVATE) && executableElement
                                .getSimpleName().toString().equals(methodName)) {
                            methodBuilder.addStatement("target.$N($N)", methodName, fieldName);
                            addSetStatement = true;
                            break;
                        }
                    }
                    if (!modifierSet.contains(Modifier.PRIVATE) && !addSetStatement) {
                        methodBuilder.addStatement("target.$N = $N", fieldName, fieldName);
                    }
                    if (!addSetStatement) {
                        ClassName className = (ClassName) fieldType;
                        throw new ProcessingException(enclosedElement,
                                                      "The %s field should not private or have a setter method like %s(%s %s)",
                                                      fieldName, methodName, className.simpleName(),
                                                      fieldName);

                    }
                    methodBuilder.addStatement("return this").build();
                    methodSpecs.add(methodBuilder.build());
                }
            }
        } catch (ProcessingException e) {
            BuilderProcessor.error(e.getElement(), e.getMessage());
        }
    }

    @Override
    protected void checkConstructorAnnotatedType() {
        try {
            for (Element enclosedElement : annotatedType.getEnclosedElements()) {
                if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                    int numberParam = executableElement.getParameters().size();
                    if (numberParam == 0)
                        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
                            throw new ProcessingException(annotatedType,
                                                          "Empty constructor must be public or protected");
                        } else
                            return;
                }
            }
            throw new ProcessingException(annotatedType,
                                          "The annotated class must have empty constructor");

        } catch (ProcessingException e) {
            BuilderProcessor.error(e.getElement(), e.getMessage());
        }

    }

    @Override
    protected MethodSpec generateBuildMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodBuildName)
                                               .addModifiers(Modifier.PUBLIC)
                                               .returns(ClassName.get(annotatedType.asType()))
                                               .addStatement("return $N", fieldName);
        return builder.build();
    }

}
