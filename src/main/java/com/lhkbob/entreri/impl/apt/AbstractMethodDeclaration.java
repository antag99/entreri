/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lhkbob.entreri.impl.apt;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * AbstractMethodDeclaration
 * =========================
 *
 * An abstract base class for {@link com.lhkbob.entreri.impl.apt.MethodDeclaration} to make implementing
 * them easier. Given a method, it will handle the method name, parameters, and return type. All that is left
 * is the actual appending of source code and property validation. It provides protected utility methods for
 * generating the attribute annotation syntax.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractMethodDeclaration implements MethodDeclaration {
    private final ExecutableElement method;

    private final String methodName;
    private final List<String> parameterNames;
    private final List<TypeMirror> parameterTypes;
    private final TypeMirror returnType;

    private final Set<Annotation> attributes;

    /**
     * Create a new declaration wrapping the given method, which is assumed to be of type METHOD. The
     * annotations in `attrs` are assumed to have come from the method or its parameters, be annotated with
     * `@Attribute`, only include the attributes from the requested scope that matched the method initially,
     * and only have an attribute level of METHOD.
     *
     * @param method The method
     * @param attrs  The attributes
     */
    protected AbstractMethodDeclaration(ExecutableElement method, Set<Annotation> attrs) {
        this.method = method;

        methodName = method.getSimpleName().toString();
        returnType = method.getReturnType();
        parameterNames = Collections.unmodifiableList(new ArrayList<>(parameterNames(method)));
        parameterTypes = Collections.unmodifiableList(new ArrayList<>(parameterTypes(method)));

        attributes = attrs;
    }

    /**
     * Generate syntax that uses reflection to retrieve the annotation of type `annotation` from this
     * method. If the method is known to not have the annotation, null is returned.
     *
     * @param context    The context of the generation system
     * @param annotation The type of annotation
     * @return Valid syntax, or null
     */
    protected String getMethodAnnotationReflectionSyntax(Context context, TypeMirror annotation) {
        // make sure this method actually has it
        boolean hasAnnot = false;
        for (AnnotationMirror a : method.getAnnotationMirrors()) {
            if (context.getTypes().isSameType(a.getAnnotationType(), annotation)) {
                hasAnnot = true;
                break;
            }
        }
        if (!hasAnnot) {
            return null;
        }

        return getMethodReflectionSyntax(context.getComponentType()) + ".getAnnotation(" +
               annotation.toString() +
               ".class" + ")";
    }

    /**
     * Generate syntax that uses reflection to retrieve the annotation of type `annotation` from the `param`
     * parameter of this method. If the method parameter does not have the annotation, null is returned. This
     * assumes the syntax will be invoked within an AbstractComponent type.
     *
     * @param context    The context of the generation system
     * @param param      The parameter index of the method to query
     * @param annotation The type of annotation
     * @return Valid syntax, or null
     */
    protected String getMethodParameterAnnotationReflectionSyntax(Context context, int param,
                                                                  TypeMirror annotation) {
        // make sure this method parameter has it
        VariableElement p = method.getParameters().get(param);
        boolean hasAnnot = false;
        for (AnnotationMirror a : p.getAnnotationMirrors()) {
            if (context.getTypes().isSameType(a.getAnnotationType(), annotation)) {
                hasAnnot = true;
                break;
            }
        }
        if (!hasAnnot) {
            return null;
        }

        return "getAnnotation(" + annotation.toString() + ".class, " +
               getMethodReflectionSyntax(context.getComponentType()) + ".getParameterAnnotations()[" + param +
               "])";
    }

    private String getMethodReflectionSyntax(TypeMirror componentType) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentType).append(".class.getMethod(\"").append(methodName).append("\"");
        for (TypeMirror p : parameterTypes) {
            sb.append(", ");
            sb.append(p.toString()).append(".class");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * @return A collection of all the properties defined by this method
     */
    protected abstract Collection<PropertyDeclaration> getProperties();

    @Override
    public ExecutableElement getMethod() {
        return method;
    }

    public Set<Annotation> getAttributes() {
        return attributes;
    }

    @Override
    public int compareTo(MethodDeclaration method) {
        return methodName.compareTo(method.getName());
    }

    @Override
    public String getName() {
        return methodName;
    }

    @Override
    public List<String> getParameterNames() {
        return parameterNames;
    }

    @Override
    public List<TypeMirror> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public TypeMirror getReturnType() {
        return returnType;
    }

    private static List<String> parameterNames(ExecutableElement method) {
        List<String> names = new ArrayList<>();
        for (VariableElement v : method.getParameters()) {
            names.add(v.getSimpleName().toString());
        }
        return names;
    }

    private static List<TypeMirror> parameterTypes(ExecutableElement method) {
        List<TypeMirror> types = new ArrayList<>();
        for (VariableElement v : method.getParameters()) {
            types.add(v.asType());
        }
        return types;
    }

    @Override
    public boolean equals(Object o) {
        if (getClass().isInstance(o)) {
            return false;
        }
        AbstractMethodDeclaration a = (AbstractMethodDeclaration) o;
        return method.equals(a.method);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("(");
        boolean first = true;
        for (TypeMirror t : getParameterTypes()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(t);
        }
        sb.append(") -> ").append(getReturnType()).append("\n\t[declares ");
        first = true;
        for (PropertyDeclaration p : getProperties()) {
            if (first) {
                first = false;
            } else {
                sb.append(",\n\t\t");
            }
            sb.append("(").append(p.getName()).append(" ").append(p.getType()).append(")");
        }
        sb.append("]\n\t[attrs ");
        first = true;
        for (Annotation attr : getAttributes()) {
            if (first) {
                first = false;
            } else {
                sb.append(",\n\t\t");
            }
            sb.append(attr);
        }
        sb.append("]");
        return sb.toString();
    }
}
