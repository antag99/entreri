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
import com.lhkbob.entreri.attr.Attribute;
import com.lhkbob.entreri.attr.Named;
import com.lhkbob.entreri.impl.MethodPattern;
import com.lhkbob.entreri.property.GenericProperty;
import com.lhkbob.entreri.property.Property;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AbstractMethodPattern
 * =====================
 *
 * An abstract base class for {@link com.lhkbob.entreri.impl.MethodPattern} implementations that provides
 * useful utility methods for filtering annotations, or extracting names from methods.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractMethodPattern implements MethodPattern {
    private final Set<Class<? extends Annotation>> attributes;

    protected final Elements eu;
    protected final Types ty;
    protected final Messager log;

    protected final String[] methodPrefixes;

    protected AbstractMethodPattern(Elements eu, Types ty, Messager log, List<String> prefixes,
                                    List<Class<? extends Annotation>> supportedAttributes) {
        this.eu = eu;
        this.ty = ty;
        this.log = log;
        methodPrefixes = prefixes.toArray(new String[prefixes.size()]);

        Set<Class<? extends Annotation>> annots = new HashSet<>();
        for (Class<? extends Annotation> a : supportedAttributes) {
            if (a.getAnnotation(Attribute.class) != null) {
                annots.add(a);
            }
        }
        annots.add(Named.class);
        attributes = Collections.unmodifiableSet(annots);
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAttributes() {
        return attributes;
    }

    @Override
    public boolean matches(Method method) {
        String methodName = method.getName();
        for (String prefix : methodPrefixes) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matches(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        for (String prefix : methodPrefixes) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String getPropertyName(String methodName, String prefix) {
        return Character.toLowerCase(methodName.charAt(prefix.length())) +
               methodName.substring(prefix.length() + 1);
    }

    protected String getDefaultPropertyName(String methodName) {
        for (String prefix : methodPrefixes) {
            if (methodName.startsWith(prefix)) {
                return getPropertyName(methodName, prefix);
            }
        }

        throw new UnsupportedOperationException("Method name does not match accepted pattern: " + methodName);
    }

    protected Set<Annotation> accumulateAttributes(Attribute.Level level, Annotation[] annotations,
                                                   Set<Class<? extends Annotation>> scope) {
        Set<Annotation> result = new HashSet<>();

        for (Annotation a : annotations) {
            if (scope.contains(a.annotationType()) &&
                a.annotationType().getAnnotation(Attribute.class).value() == level) {
                result.add(a);
            }
        }

        return result;
    }

    protected Set<Annotation> accumulateAttributes(Attribute.Level level, Element annotSource,
                                                   Set<Class<? extends Annotation>> scope) {
        Set<Annotation> result = new HashSet<>();

        for (Class<? extends Annotation> aType : scope) {
            if (aType.getAnnotation(Attribute.class).value() == level) {
                Annotation a = annotSource.getAnnotation(aType);
                if (a != null) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    protected String getExplicitName(Method method) {
        Named name = method.getAnnotation(Named.class);
        if (name != null) {
            return name.value();
        } else {
            return null;
        }
    }

    protected String getExplicitName(Method method, int param) {
        Annotation[] forParam = method.getParameterAnnotations()[param];
        for (Annotation a : forParam) {
            if (a instanceof Named) {
                return ((Named) a).value();
            }
        }
        return null;
    }

    protected String getExplicitName(ExecutableElement method) {
        Named name = method.getAnnotation(Named.class);
        if (name != null) {
            return name.value();
        } else {
            return null;
        }
    }

    protected String getExplicitName(ExecutableElement method, int param) {
        VariableElement p = method.getParameters().get(param);
        Named name = p.getAnnotation(Named.class);
        if (name != null) {
            return name.value();
        } else {
            return null;
        }
    }

    protected boolean hasMethod(Class<?> targetType, String name, Class<?> returnType, Class<?>... params) {
        for (Method m : targetType.getMethods()) {
            if (m.getName().equals(name) && m.getReturnType().equals(returnType)) {
                Class<?>[] realParams = m.getParameterTypes();
                if (realParams.length == params.length) {
                    boolean found = true;
                    for (int i = 0; i < params.length; i++) {
                        if (!params[i].equals(realParams[i])) {
                            found = false;
                            break;
                        }
                    }

                    if (found) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected boolean hasMethod(TypeMirror targetType, String name, TypeMirror returnType,
                                TypeMirror... params) {
        List<? extends ExecutableElement> methods = ElementFilter
                                                            .methodsIn(eu.getAllMembers((TypeElement) ty.asElement(targetType)));

        for (ExecutableElement m : methods) {
            if (m.getSimpleName().contentEquals(name) && ty.isSameType(returnType, m.getReturnType())) {
                // now check parameters
                List<? extends VariableElement> realParams = m.getParameters();
                if (params.length == realParams.size()) {
                    boolean found = true;
                    for (int i = 0; i < params.length; i++) {
                        if (!ty.isSameType(params[i], realParams.get(i).asType())) {
                            found = false;
                            break;
                        }
                    }

                    if (found) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected Class<?> getTypeForProperty(Class<?> type, Class<? extends Property> propClass) {
        GenericProperty generic = propClass.getAnnotation(GenericProperty.class);
        if (generic != null) {
            Class<?> genType = generic.superClass();
            if (!genType.isAssignableFrom(type)) {
                throw new IllegalComponentDefinitionException("Generic property super type (" + genType +
                                                              ") cannot be used with type (" + type + ")");
            }
            return genType;
        } else {
            return type;
        }
    }

    protected TypeMirror getTypeForProperty(TypeMirror type, TypeMirror propType) {
        GenericProperty generic = ty.asElement(propType).getAnnotation(GenericProperty.class);
        if (generic == null) {
            return type;
        } else {
            try {
                generic.superClass(); // will throw an exception
                return null;
            } catch (MirroredTypeException e) {
                TypeMirror genType = e.getTypeMirror();
                // test if first arg (type) is assignable to second arg (genType)
                if (!ty.isAssignable(type, genType)) {
                    throw new IllegalComponentDefinitionException("Generic property super type (" + genType +
                                                                  ") cannot be used with type (" + type +
                                                                  ")");
                }
                return genType;
            }
        }
    }
}
