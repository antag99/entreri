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
package com.lhkbob.entreri.impl.methods;

import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.impl.MethodDeclaration;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * AbstractMethodDeclaration
 * =========================
 *
 * An abstract base class for {@link com.lhkbob.entreri.impl.MethodDeclaration} to make implementing them
 * easier. Given a method (reflection or mirror API-based), it will handle the method name, parameters, and
 * return type. All that is left is the actual appending of source code.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractMethodDeclaration implements MethodDeclaration {
    private final String methodName;
    private final List<String> parameterNames;
    private final List<String> parameterTypes;
    private final String returnType;

    private final Set<Annotation> attributes;

    protected AbstractMethodDeclaration(Method method, Set<Annotation> attrs) {
        this(method.getName(), method.getReturnType().getCanonicalName(), parameterNames(method),
             parameterTypes(method), attrs);
    }

    protected AbstractMethodDeclaration(ExecutableElement method, Set<Annotation> attrs) {
        this(method.getSimpleName().toString(), method.getReturnType().toString(), parameterNames(method),
             parameterTypes(method), attrs);
    }

    protected AbstractMethodDeclaration(String methodName, String returnType, List<String> parameterNames,
                                        List<String> parameterTypes, Set<Annotation> attrs) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterNames = Collections.unmodifiableList(new ArrayList<>(parameterNames));
        this.parameterTypes = Collections.unmodifiableList(new ArrayList<>(parameterTypes));

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
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    private static List<String> parameterNames(Method method) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            names.add("param" + i);
        }
        return names;
    }

    private static List<String> parameterTypes(Method method) {
        List<String> types = new ArrayList<>();
        for (Class<?> t : method.getParameterTypes()) {
            types.add(t.getCanonicalName());
        }
        return types;
    }

    private static List<String> parameterNames(ExecutableElement method) {
        List<String> names = new ArrayList<>();
        for (VariableElement v : method.getParameters()) {
            names.add(v.getSimpleName().toString());
        }
        return names;
    }

    private static List<String> parameterTypes(ExecutableElement method) {
        List<String> types = new ArrayList<>();
        for (VariableElement v : method.getParameters()) {
            types.add(v.asType().toString());
        }
        return types;
    }
}
