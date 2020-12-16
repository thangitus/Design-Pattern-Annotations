package builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import utils.ProcessingException;

class ConstructorBuilder extends BuilderAnnotatedType {
    StringBuilder fieldsName;

    public ConstructorBuilder(TypeElement annotatedType, Elements elementUtils) {
        super(annotatedType, elementUtils);
    }

    @Override
    protected void generateFieldsAndMethods() {
        MethodSpec constructor = constructorBuilder.build();
        methodSpecs.add(constructor);

        TypeName builderTypeName = ClassName.get(packageName, className);
        for (Element enclosedElement : annotatedType.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.FIELD)) {

                if (enclosedElement.getAnnotation(Ignore.class) != null)
                    continue;

                String fieldName = enclosedElement.getSimpleName().toString();
                TypeName fieldType = TypeName.get(enclosedElement.asType()); // String
                FieldSpec field = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE).build();
                fieldSpecs.add(field);

                String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName
                        .substring(1);
                MethodSpec method = MethodSpec.methodBuilder(methodName)
                                              .addModifiers(Modifier.PUBLIC)
                                              .returns(builderTypeName)
                                              .addParameter(fieldType, fieldName)
                                              .addStatement("this.$N = $N", fieldName, fieldName)
                                              .addStatement("return this").build();

                methodSpecs.add(method);
            }
        }
    }

    @Override
    protected void checkConstructorAnnotatedType() {
        fieldsName = new StringBuilder();
        try {
            for (Element enclosedElement : annotatedType.getEnclosedElements()) {
                if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                    List<? extends VariableElement> parameters = executableElement.getParameters();

                    if (parameters.size() >= fieldSpecs.size()) {

                        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
                            throw new ProcessingException(annotatedType,
                                                          "Constructor with all params must be public or protected");
                        }
                        for (int i = 0; i < parameters.size(); i++) {
                            VariableElement param = parameters.get(i);
                            if (fieldsName.length() > 0) {
                                fieldsName.append(", ");
                            }
                            String fieldName = param.getSimpleName().toString();

                            if (i >= fieldSpecs.size()) {
                                fieldsName.append("null");
                                continue;
                            }

                            if (!fieldName.equals(fieldSpecs.get(i).name)) {
                                String constructorExpect = getConstructorExpect();
                                throw new ProcessingException(enclosedElement,
                                                              "Constructor is ambiguous, it should be %s",
                                                              constructorExpect);
                            }

                            fieldsName.append(param.getSimpleName());
                        }
                        return;
                    }
                }
            }
            throw new ProcessingException(annotatedType,
                                          "The annotated class must have constructor with all params");
        } catch (ProcessingException e) {
            BuilderProcessor.error(e.getElement(), e.getMessage());
        }

    }

    private String getConstructorExpect() {
        StringBuilder constructorExpect = new StringBuilder();
        constructorExpect.append(annotatedType.getSimpleName());
        constructorExpect.append("(");
        for (int i = 0; i < fieldSpecs.size(); i++) {
            if (i > 0)
                constructorExpect.append(", ");
            FieldSpec fieldSpec = fieldSpecs.get(i);
            ClassName className = (ClassName) fieldSpec.type;
            constructorExpect.append(className.simpleName());
            constructorExpect.append(' ');
            constructorExpect.append(fieldSpec.name.toLowerCase());
        }
        constructorExpect.append(")");
        return constructorExpect.toString();
    }

    @Override
    protected MethodSpec generateBuildMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodBuildName)
                                               .addModifiers(Modifier.PUBLIC)
                                               .returns(ClassName.get(annotatedType.asType()))
                                               .addStatement("return new $N($N)",
                                                             annotatedType.getSimpleName()
                                                                          .toString(),
                                                             fieldsName.toString());
        return builder.build();
    }

}
