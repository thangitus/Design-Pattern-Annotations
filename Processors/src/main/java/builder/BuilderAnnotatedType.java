package builder;

import com.squareup.javapoet.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;

import utils.ProcessingException;

class BuilderAnnotatedType {
    protected String packageName;
    protected List<MethodSpec> methodSpecs = new ArrayList<>();
    protected List<FieldSpec> fieldSpecs = new ArrayList<>();
    protected String methodBuildName;
    protected TypeElement annotatedType;
    StringBuilder params;

    public BuilderAnnotatedType(TypeElement annotatedType, Elements elementUtils) {
        Builder builder = annotatedType.getAnnotation(Builder.class);
        methodBuildName = builder.buildMethodName();
        this.annotatedType = annotatedType;

        String qualifiedClassName = annotatedType.getQualifiedName().toString();
        TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();
        generateFieldsAndMethods();
        checkConstructorAnnotatedType();
        methodSpecs.add(generateBuildMethod());
    }

    protected void checkConstructorAnnotatedType() {
        params = new StringBuilder();
        for (Element enclosedElement : annotatedType.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                List<? extends VariableElement> parameters = executableElement.getParameters();

                if (parameters.size() != fieldSpecs.size())
                    continue;

                for (VariableElement param : parameters) {
                    if (params.length() > 0) {
                        params.append(", ");
                    }
                    params.append(param.getSimpleName());
                }
                break;
            }
        }
    }

    protected void generateFieldsAndMethods() {
        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
        methodSpecs.add(constructor);

        TypeName builderTypeName = ClassName.get(packageName, annotatedType.getSimpleName()+"Builder");
        for (Element enclosedElement : annotatedType.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.FIELD)) {

                if (enclosedElement.getAnnotation(Ignore.class) != null)
                    continue;

                String fieldName = enclosedElement.getSimpleName().toString();
                TypeName fieldType = TypeName.get(enclosedElement.asType()); // String
                FieldSpec field = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE).build();
                fieldSpecs.add(field);

                String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                MethodSpec method =
                        MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC).returns(builderTypeName)
                                  .addParameter(fieldType, fieldName).addStatement("this.$N = $N", fieldName, fieldName)
                                  .addStatement("return this").build();

                methodSpecs.add(method);
            }
        }
    }

    protected MethodSpec generateBuildMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodBuildName).addModifiers(Modifier.PUBLIC)
                                               .returns(ClassName.get(annotatedType.asType()))
                                               .addStatement("return new $N($N)",
                                                             annotatedType.getSimpleName().toString(),
                                                             params.toString());
        return builder.build();
    }

    public void generateCode(Filer filer) {

        TypeSpec.Builder typeSpecBuilder =
                TypeSpec.classBuilder(annotatedType.getSimpleName()+"Builder").addModifiers(Modifier.PUBLIC).addFields(fieldSpecs)
                        .addMethods(methodSpecs);

        try {
            JavaFile.builder(packageName, typeSpecBuilder.build()).build().writeTo(filer);
        } catch (IOException e) {
            BuilderProcessor.error(null, e.getMessage());
        }
    }

}
