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
import com.lhkbob.entreri.attr.*;
import com.lhkbob.entreri.impl.Generator;
import com.lhkbob.entreri.impl.MethodDeclaration;
import com.lhkbob.entreri.impl.PropertyDeclaration;
import com.lhkbob.entreri.property.Property;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * BeanSetterPattern
 * =================
 *
 * A method pattern that matches Java bean setter methods. This pattern matches methods that meet the
 * following criteria:
 *
 * * The method has only one parameter
 * * The method's return type is `void` or the Component type declaring the method (in which case the
 * implementation will return `this` for method chaining)
 * * The method's name starts with `set`, and the lower-cased remainder of the name is taken as the declared
 * name of the property
 *
 * If the method, or its single parameter, are annotated with `@Named` that is used to define the property
 * name independently of the method name, although the method name must still start with `set`.
 *
 * @author Michael Ludwig
 */
public class BeanSetterPattern extends AbstractMethodPattern {
    public BeanSetterPattern(Elements eu, Types ty, Messager log) {
        super(eu, ty, log, Arrays.asList("set"),
              Arrays.asList(Validate.class, NotNull.class, Within.class, DontAutoVersion.class));
    }

    @Override
    public Map<String, Class<?>> getDeclaredProperties(Method method) {
        Map<String, Class<?>> props = new HashMap<>();
        String name = getExplicitName(method, 0);
        if (name == null) {
            name = getExplicitName(method);
            if (name == null) {
                name = getDefaultPropertyName(method.getName());
            }
        }
        props.put(name, method.getParameterTypes()[0]);
        return props;
    }

    @Override
    public Map<String, TypeMirror> getDeclaredProperties(ExecutableElement method) {
        Map<String, TypeMirror> props = new HashMap<>();
        String name = getExplicitName(method, 0);
        if (name == null) {
            name = getExplicitName(method);
            if (name == null) {
                name = getDefaultPropertyName(method.getSimpleName().toString());
            }
        }
        props.put(name, method.getParameters().get(0).asType());
        return props;
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(Method method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        // look at method and at 1st param
        Set<Annotation> result = accumulateAttributes(Attribute.Level.PROPERTY, method.getAnnotations(),
                                                      scope);
        result.addAll(accumulateAttributes(Attribute.Level.PROPERTY, method.getParameterAnnotations()[0],
                                           scope));
        return result;
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(ExecutableElement method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        // look at method and at 1st param
        Set<Annotation> result = accumulateAttributes(Attribute.Level.PROPERTY, method, scope);
        result.addAll(accumulateAttributes(Attribute.Level.PROPERTY, method.getParameters().get(0), scope));
        return result;
    }

    @Override
    public MethodDeclaration createMethodDeclaration(Method method, List<Class<? extends Property>> propClass,
                                                     List<? extends PropertyDeclaration> property) {
        Class<?> reqType = getTypeForProperty(method.getParameterTypes()[0], propClass.get(0));
        if (!hasMethod(propClass.get(0), "set", void.class, int.class, reqType)) {
            throw new IllegalComponentDefinitionException("Property type (" + propClass.get(0) +
                                                          ") is incompatible with bean setter for type (" +
                                                          reqType + ")");
        }

        // if it's not void it must be the component type (assuming it passed matches())
        boolean returnComponent =
                !method.getReturnType().equals(void.class) && !method.getReturnType().equals(Void.class);
        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method.getAnnotations(),
                                                     getSupportedAttributes());
        attrs.addAll(accumulateAttributes(Attribute.Level.METHOD, method.getParameterAnnotations()[0],
                                          getSupportedAttributes()));
        return new BeanSetterDeclaration(method, property.get(0), attrs, returnComponent);
    }

    @Override
    public MethodDeclaration createMethodDeclaration(ExecutableElement method, List<TypeMirror> propType,
                                                     List<? extends PropertyDeclaration> property) {
        TypeMirror reqType = getTypeForProperty(method.getParameters().get(0).asType(), propType.get(0));
        if (!hasMethod(propType.get(0), "set", ty.getNoType(TypeKind.VOID), ty.getPrimitiveType(TypeKind.INT),
                       reqType)) {
            throw new IllegalComponentDefinitionException("Property type (" + propType.get(0) +
                                                          ") is incompatible with bean setter for type (" +
                                                          reqType + ")");
        }

        // if it's not void it must be the component type (assuming it passed matches())
        boolean returnComponent = !method.getReturnType().getKind().equals(TypeKind.VOID);
        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method,
                                                     getSupportedAttributes());
        attrs.addAll(accumulateAttributes(Attribute.Level.METHOD, method.getParameters().get(0),
                                          getSupportedAttributes()));
        return new BeanSetterDeclaration(method, property.get(0), attrs, returnComponent);
    }

    @Override
    public boolean matches(Method method) {
        if (!super.matches(method)) {
            return false;
        }

        // must take a single parameter
        if (method.getParameterTypes().length != 1) {
            return false;
        }

        // must return void or the component type
        if (!method.getReturnType().equals(Void.class) && !method.getReturnType().equals(void.class) &&
            !method.getReturnType().equals(method.getDeclaringClass())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean matches(ExecutableElement method) {
        if (!super.matches(method)) {
            return false;
        }

        // must take a single parameter
        if (method.getParameters().size() != 1) {
            return false;
        }

        // must return void or the component type
        TypeMirror declaringType = method.getEnclosingElement().asType();
        if (!method.getReturnType().getKind().equals(TypeKind.VOID) &&
            !ty.isSameType(declaringType, method.getReturnType())) {
            return false;
        }

        return true;
    }

    private static class BeanSetterDeclaration extends AbstractMethodDeclaration {
        private final boolean returnsComponent;
        private final PropertyDeclaration property;

        protected BeanSetterDeclaration(Method method, PropertyDeclaration property, Set<Annotation> attrs,
                                        boolean returnsComponent) {
            super(method, attrs);
            this.returnsComponent = returnsComponent;
            this.property = property;
        }

        protected BeanSetterDeclaration(ExecutableElement method, PropertyDeclaration property,
                                        Set<Annotation> attrs, boolean returnsComponent) {
            super(method, attrs);
            this.returnsComponent = returnsComponent;
            this.property = property;
        }

        @Override
        public void appendMembers(Generator generator) {
            // needs no additional members
        }

        @Override
        public void appendConstructorInitialization(Generator generator) {
            // needs no additional initialization
        }

        @Override
        public void appendMethodBody(Generator generator) {
            // perform any validation
            for (Annotation annot : getAttributes()) {
                if (annot instanceof Validate) {
                    Validations.appendValidation(getParameterNames(), (Validate) annot, generator);
                    break; // @Validate is the only method level attribute this pattern supports
                }
            }

            boolean needsUpdate = true;
            for (Annotation annot : property.getAttributes()) {
                if (annot instanceof NotNull) {
                    Validations.appendNotNull(getParameterNames().get(0), generator);
                } else if (annot instanceof Within) {
                    Validations.appendWithin(getParameterNames().get(0), (Within) annot, generator);
                } else if (annot instanceof DontAutoVersion) {
                    needsUpdate = false;
                }
                // else ignore the unsupported attribute
            }

            String field = generator.getPropertyMemberName(property.getName());
            String index = generator.getComponentIndex();
            generator.appendSyntax(field + ".set(" + index + ", " + getParameterNames().get(0) + ");");

            if (needsUpdate) {
                generator.appendSyntax("updateVersion();");
            }

            if (returnsComponent) {
                generator.appendSyntax("return this;");
            }
        }
    }
}
