package builder;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import base.AnnotatedType;

abstract class BuilderAnnotatedType extends AnnotatedType {
    protected final String PREFIX = "Builder";
    protected String packageName;
    protected String className;
    protected List<MethodSpec> methodSpecs = new ArrayList<>();
    protected List<FieldSpec> fieldSpecs = new ArrayList<>();
    protected String methodBuildName;
    protected MethodSpec.Builder constructorBuilder;

    public BuilderAnnotatedType(TypeElement annotatedType) {
        super(annotatedType);
        Builder builder = annotatedType.getAnnotation(Builder.class);
        className = builder.builderClassName();
        if (className.isEmpty()) {
            className = annotatedType.getSimpleName().toString() + PREFIX;
        }
        methodBuildName = builder.buildMethodName();
    }

    public BuilderAnnotatedType(TypeElement annotatedType, Elements elementUtils) {
        this(annotatedType);
        String qualifiedClassName = annotatedType.getQualifiedName().toString();
        TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();
        createCtorBuilder();
        generateFieldsAndMethods();
        checkConstructorAnnotatedType();
        methodSpecs.add(generateBuildMethod());
    }

    protected abstract void checkConstructorAnnotatedType();

    protected void createCtorBuilder() {
        constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    }

    protected abstract void generateFieldsAndMethods();

    protected abstract MethodSpec generateBuildMethod();

    public void generateCode(Filer filer) {

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
                                                   .addModifiers(Modifier.PUBLIC)
                                                   .addFields(fieldSpecs).addMethods(methodSpecs);

        // Write file
        try {
            JavaFile.builder(packageName, typeSpecBuilder.build()).build().writeTo(filer);
        } catch (IOException e) {
            BuilderProcessor.error(null, e.getMessage());
        }
    }

}
