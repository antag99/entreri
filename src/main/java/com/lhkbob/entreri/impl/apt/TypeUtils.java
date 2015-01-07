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

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TypeUtils
 * =========
 *
 * TypeUtils bundles up the commonly needed utility methods for dealing with complexities of the Java mirror
 * API.
 *
 * @author Michael Ludwig
 */
public class TypeUtils {
    private final ProcessingEnvironment env;

    /**
     * Create a new TypeUtils that uses the provided ProcessingEnvironment.
     *
     * @param env The processing environment of the APT compilation unit processing the component type
     */
    public TypeUtils(ProcessingEnvironment env) {
        this.env = env;
    }

    /**
     * @return The Elements utilities from the processing environment
     */
    public Elements getElements() {
        return env.getElementUtils();
    }

    /**
     * @return The Types utilities from the processing environment
     */
    public Types getTypes() {
        return env.getTypeUtils();
    }

    /**
     * @return The Messager logger from the processing environment
     */
    public Messager getLogger() {
        return env.getMessager();
    }

    /**
     * @return The ProcessingEnvironment for the compilation unit processing this component type
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return env;
    }

    /**
     * Safely convert the given TypeMirror to a TypeElement. This assumes the TypeMirror is one of the kinds
     * that represent a class, interface, array, or primitive.
     *
     * @param type The type to convert to an element
     * @return The TypeElement corresponding to the type mirror
     */
    public TypeElement asElement(TypeMirror type) {
        return findEnclosingTypeElement(getTypes().asElement(type));
    }

    /**
     * Create a TypeMirror from the given Class. The returned mirror will have type variables if the class
     * declared any. It is not the raw type.
     *
     * @param type The class type to convert
     * @return The TypeMirror corresponding to the class
     */
    public TypeMirror fromClass(Class<?> type) {
        return getElements().getTypeElement(type.getCanonicalName()).asType();
    }

    /**
     * Create a TypeMirror from the given Class, filling in any type variables with the given TypeMirror
     * arguments. `arguments` must be as long as the number type variables specified in the class.
     *
     * @param type      The class type to convert
     * @param arguments The types of the variables to be filled in
     * @return The parameterized type as a TypeMirror
     */
    public TypeMirror fromGenericClass(Class<?> type, TypeMirror... arguments) {
        return getTypes().getDeclaredType(getElements().getTypeElement(type.getCanonicalName()), arguments);
    }

    /**
     * Create a TypeMirror from the given Class that represents the raw type of the class.
     *
     * @param type The class type to convert
     * @return The TypeMirror of the class's raw type
     */
    public TypeMirror getRawType(Class<?> type) {
        return getTypes().erasure(fromClass(type));
    }

    /**
     * Complete the parameterized method, as if it were being invoked on the given declared type,
     * `declarer`. This does nothing if the method or type do not define any type parameters. However, if the
     * class declaring the method uses type parameters, the executable element only reports the type variable
     * as its return or argument types. It must be completed with the actual chosen parameters to get the true
     * return or argument types.
     *
     * @param declarer The completed type, with all type variables filled in (e.g. `Set<String>`)
     * @param method   The method that should have its type variables updated based on `declarer`
     * @return The completed method
     */
    public ExecutableType getParameterizedMethod(TypeMirror declarer, ExecutableElement method) {
        return (ExecutableType) getTypes().asMemberOf((DeclaredType) declarer, method);
    }

    /**
     * Get all methods declared in `type` that match the pattern. See {@link
     * #getMatchingMethods(javax.lang.model.type.TypeMirror, java.util.List, java.util.regex.Pattern,
     * javax.lang.model.type.TypeMirror, javax.lang.model.type.TypeMirror...)} for details on how the methods
     * are matched.
     *
     * @param type        The type whose methods are matched against
     * @param namePattern The regex that the method names must match
     * @param returnType  The return type the method must have, or null if the return type can be anything
     * @param params      The parameter types the method must have, or null if the method can have any
     *                    parameters; null elements within the array represent a wildcard
     *                    type for that particular parameter
     * @return All matched elements, in the order they were reported from the type
     */
    public List<ExecutableElement> getMatchingMethods(TypeMirror type, Pattern namePattern,
                                                      TypeMirror returnType, TypeMirror... params) {
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(getElements()
                                                                                    .getAllMembers(asElement(type)));
        return getMatchingMethods(type, methods, namePattern, returnType, params);
    }

    /**
     * Get all methods from `methods` that match the given pattern. It is assumed that these methods are a
     * sublist of the methods declared in `type`. Methods are first matched against the name regular
     * expression, `namePattern`. If they pass, then their return type is compared to `returnType`. If
     * `returnType` is null, the return type of the method can be anything; otherwise it must identical. If
     * the return type matches, the method parameters are compared to `params`.
     *
     * When `params` is null, the method may have any number of parameters of any type. If it is not null,
     * the method must have the same number of parameters as the length of `params`. When `params` is not
     * null, each parameter of the method must match the corresponding element of `params`. If the element is
     * null, the method parameter can be of any type; if it is not null the types must be identical.
     *
     * This automatically completes any parameterization of the methods based on the type variable
     * assignment within `type`.
     *
     * @param type        The type whose methods are matched against
     * @param namePattern The regex that the method names must match
     * @param returnType  The return type the method must have, or null if the return type can be anything
     * @param params      The parameter types the method must have, or null if the method can have any
     *                    parameters; null elements within the array represent a wildcard
     *                    type for that particular parameter
     * @return All matched elements, in the order they were reported from the type
     */
    public List<ExecutableElement> getMatchingMethods(TypeMirror type,
                                                      List<? extends ExecutableElement> methods,
                                                      Pattern namePattern, TypeMirror returnType,
                                                      TypeMirror... params) {
        List<ExecutableElement> matches = new ArrayList<>();
        Types ty = getTypes();

        for (ExecutableElement m : methods) {
            if (namePattern.matcher(m.getSimpleName()).matches()) {
                ExecutableType parameterizedM = getParameterizedMethod(type, m);

                // check return type (null return type means it is flexible)
                if (returnType != null && !ty.isSameType(returnType, parameterizedM.getReturnType())) {
                    continue;
                }
                // check parameters, a null array is flexible in its entirety, a null parameter is flexible
                // for that particular parameter
                if (params != null) {
                    List<? extends TypeMirror> realParams = parameterizedM.getParameterTypes();
                    if (params.length != realParams.size()) {
                        continue;
                    }
                    boolean match = true;
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] != null && !ty.isSameType(params[i], realParams.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        matches.add(m);
                    }
                } else {
                    matches.add(m);
                }
            }
        }

        return matches;
    }

    /**
     * A convenience method to determine if the type declares the exact method specified. This relies on
     * `getMatchingMethods` under the hood, so the handling of null values for `returnType` and `params`
     * is identical. The method name pattern is created so that it must identically match `methodName`.
     *
     * @param targetType The type whose methods are searched through
     * @param methodName The method name that must be found
     * @param returnType The return type the method must have, or null if the return type can be anything
     * @param params     The parameter types the method must have, or null if the method can have any
     *                   parameters; null elements within the array represent a wildcard
     *                   type for that particular parameter
     */
    public boolean hasMethod(TypeMirror targetType, String methodName, TypeMirror returnType,
                             TypeMirror... params) {
        return !getMatchingMethods(targetType, Pattern.compile(methodName), returnType, params).isEmpty();
    }

    /**
     * Utility method to determine the TypeElement that encloses the given element, regardless of the exact
     * type of element `e` is.
     *
     * @param e The element in question
     * @return The TypeElement enclosing `e`.
     */
    public static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }

    /**
     * Return whether or not the given annotation mirror instance's annotation type is equal to the provided
     * annotation class.
     *
     * @param mirror    The annotation mirror in question
     * @param annotType The annotation class type loaded into the JVM
     * @return True if the mirror is compatible with `annotType`
     */
    public boolean isAnnotationType(AnnotationMirror mirror, Class<? extends Annotation> annotType) {
        return mirror.getAnnotationType().toString().equals(annotType.getCanonicalName());
    }

    /**
     * Convert the given annotation mirror into the runtime-available annotation class type. If the mirror
     * is not of the same annotation type as the specified class, then null is returned. Not every method
     * is supported or guaranteed to be implemented up to specification, e.g.
     *
     * * `equals()` only supports being called with other annotations returned by this method.
     * * `hashCode()` may not be compatible with other annotation implementations but is consistent within the
     * instances returned by this method.
     * * Annotation methods that return arrays are not supported currently.
     * * Annotation methods that return a class type throw a {@link
     * javax.lang.model.type.MirroredTypeException} that reports the value as a {@link TypeMirror} instead.
     *
     * All other constant values are reported properly.
     *
     * @param annot     The annotation mirror to convert to an actual instance
     * @param annotType The runtime class of the annotation type
     * @return The annotation mirror as an actual annotation proxy
     */
    public <T extends Annotation> T asAnnotation(final AnnotationMirror annot, final Class<T> annotType) {
        if (!isAnnotationType(annot, annotType)) {
            return null;
        } else {
            return annotType.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                                                         new Class[] { annotType },
                                                         new AnnotationMirrorProxy(annot, annotType)));
        }
    }

    private class AnnotationMirrorProxy implements InvocationHandler {
        private final AnnotationMirror mirror;
        private final Class<?> annotType;
        private final Map<? extends ExecutableElement, ? extends AnnotationValue> allValues;

        private AnnotationMirrorProxy(AnnotationMirror mirror, Class<?> annotType) {
            this.mirror = mirror;
            this.annotType = annotType;
            allValues = getElements().getElementValuesWithDefaults(mirror);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("annotationType") && method.getReturnType().equals(Class.class) &&
                method.getParameterTypes().length == 0) {
                return annotType;
            } else if (method.getName().equals("equals") &&
                       method.getReturnType().equals(boolean.class) &&
                       method.getParameterTypes().length == 1 &&
                       method.getParameterTypes()[0].equals(Object.class)) {
                if (!annotType.isInstance(args[0])) {
                    return false;
                }
                if (!Proxy.isProxyClass(args[0].getClass())) {
                    throw new UnsupportedOperationException("Equals is only implemented between other proxies made by the same context");
                }
                InvocationHandler h = Proxy.getInvocationHandler(args[0]);
                if (!(h instanceof AnnotationMirrorProxy)) {
                    throw new UnsupportedOperationException("Equals is only implemented between other proxies made by the same context");
                }
                return ((AnnotationMirrorProxy) h).mirror.equals(mirror);
            } else if (method.getName().equals("hashCode") && method.getReturnType().equals(int.class) &&
                       method.getParameterTypes().length == 0) {
                // this may not be the proper hashCode for the annotation spec but it is consistent with equality
                // within annotation mirrors and that should be good enough for most purposes
                return mirror.hashCode();
            } else if (method.getName().equals("toString") && method.getReturnType().equals(String.class) &&
                       method.getParameterTypes().length == 0) {
                return mirror.toString();
            } else {
                for (ExecutableElement e : allValues.keySet()) {
                    if (e.getSimpleName().toString().equals(method.getName())) {
                        Object value = allValues.get(e).getValue();
                        if (value instanceof VariableElement) {
                            // enum
                            VariableElement v = (VariableElement) value;
                            String enumName = v.getSimpleName().toString();
                            for (Object enumVal : method.getReturnType().getEnumConstants()) {
                                if (((Enum) enumVal).name().equals(enumName)) {
                                    return enumVal;
                                }
                            }
                            throw new RuntimeException("Could not find enum constant for " + v + " within " +
                                                       mirror);
                        } else if (value instanceof AnnotationMirror) {
                            return Proxy.newProxyInstance(getClass().getClassLoader(),
                                                          new Class[] { method.getReturnType() },
                                                          new AnnotationMirrorProxy((AnnotationMirror) value,
                                                                                    method.getReturnType()));
                        } else if (value instanceof TypeMirror) {
                            throw new MirroredTypeException((TypeMirror) value);
                        } else if (value instanceof List) {
                            throw new UnsupportedOperationException("Arrays aren't supported yet'");
                        } else {
                            // is a number or string and does not need conversion
                            return value;
                        }
                    }
                }
                throw new RuntimeException("Could not find annotation value for " + method);
            }
        }
    }
}
