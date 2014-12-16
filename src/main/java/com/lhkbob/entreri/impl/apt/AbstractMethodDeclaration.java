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

import com.lhkbob.entreri.IllegalComponentDefinitionException;

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
 * An abstract base class for {@link com.lhkbob.entreri.impl.apt.MethodDeclaration} to make implementing them
 * easier. Given a method (reflection or mirror API-based), it will handle the method name, parameters, and
 * return type. All that is left is the actual appending of source code.
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

    protected AbstractMethodDeclaration(ExecutableElement method, Set<Annotation> attrs) {
        this.method = method;

        methodName = method.getSimpleName().toString();
        returnType = method.getReturnType();
        parameterNames = Collections.unmodifiableList(new ArrayList<>(parameterNames(method)));
        parameterTypes = Collections.unmodifiableList(new ArrayList<>(parameterTypes(method)));

        // add annotations, but make sure that annotations of the same type are not added
        // if the values are equal then ignore the duplicates; if they disagree in parameters then
        // a conflict exists and the component is invalid
        Set<Annotation> checked = new HashSet<>();
        for (Annotation a : attrs) {
            if (!checked.contains(a)) {
                for (Annotation o : checked) {
                    if (a.getClass().equals(o.getClass())) {
                        // a is of the same type as o, but they are not equal
                        throw new IllegalComponentDefinitionException("Conflicting applications of " +
                                                                      a.getClass() + " on method " +
                                                                      methodName);
                    }
                }
                // not of any prior type
                checked.add(a);
            } // ignore duplicates
        }

        attributes = Collections.unmodifiableSet(checked);
    }

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

    protected String getMethodReflectionSyntax(TypeMirror componentType) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentType).append(".class.getMethod(\"").append(methodName).append("\"");
        for (TypeMirror p : parameterTypes) {
            sb.append(", ");
            sb.append(p.toString()).append(".class");
        }
        sb.append(")");
        return sb.toString();
    }

    protected abstract Collection<PropertyDeclaration> getProperties();

    @Override
    public ExecutableElement getMethod() {
        return method;
    }

    @Override
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
        sb.append("Method ").append(getReturnType()).append(" ").append(getName()).append("(");
        boolean first = true;
        for (TypeMirror t : getParameterTypes()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(t);
        }
        sb.append(") [declares ");
        first = true;
        for (PropertyDeclaration p : getProperties()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("(").append(p.getName()).append(" ").append(p.getType()).append(")");
        }
        sb.append("] [attrs ").append(getAttributes()).append("]");
        return sb.toString();
    }
}
