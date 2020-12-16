package base;

import javax.lang.model.element.TypeElement;

public abstract class AnnotatedType {
    protected final TypeElement annotatedType;
    protected String typeSimpleName;
    protected String typeQualifiedName;

    public AnnotatedType(TypeElement annotatedType) {
        this.annotatedType = annotatedType;
        typeSimpleName = annotatedType.getSimpleName().toString();
        typeQualifiedName = annotatedType.getQualifiedName().toString();

    }
}
