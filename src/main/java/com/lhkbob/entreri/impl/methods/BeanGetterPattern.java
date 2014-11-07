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
import com.lhkbob.entreri.impl.Generator;
import com.lhkbob.entreri.impl.MethodDeclaration;
import com.lhkbob.entreri.impl.PropertyDeclaration;
import com.lhkbob.entreri.property.GenericProperty;
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
 * BeanGetterPattern
 * =================
 *
 * A method pattern that matches Java bean style getter methods. This pattern matches methods that meet the
 * following criteria:
 *
 * * The method has no parameters
 * * The method's return type is not `void`, where the return type is the declared type of the property
 * * The method's name starts with `get`, `is`, or `has`, and the lower-cased remainder of the name is taken
 * as the declared name of the property
 *
 * If the method is annotated with `@Named` that is used to define the property name independently of the
 * method name, although the method name must still start with the defined prefixes.
 *
 * @author Michael Ludwig
 */
public class BeanGetterPattern extends AbstractMethodPattern {
    public BeanGetterPattern(Elements el, Types ty, Messager log) {
        super(el, ty, log, Arrays.asList("get", "is", "has"),
              Collections.<Class<? extends Annotation>>emptyList());
    }

    @Override
    public Map<String, Class<?>> getDeclaredProperties(Method method) {
        Map<String, Class<?>> props = new HashMap<>();
        String name = getExplicitName(method);
        if (name == null) {
            name = getDefaultPropertyName(method.getName());
        }
        props.put(name, method.getReturnType());
        return props;
    }

    @Override
    public Map<String, TypeMirror> getDeclaredProperties(ExecutableElement method) {
        Map<String, TypeMirror> props = new HashMap<>();
        String name = getExplicitName(method);
        if (name == null) {
            name = getDefaultPropertyName(method.getSimpleName().toString());
        }
        props.put(name, method.getReturnType());
        return props;
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(Method method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        return accumulateAttributes(Attribute.Level.PROPERTY, method.getAnnotations(), scope);
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(ExecutableElement method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        return accumulateAttributes(Attribute.Level.PROPERTY, method, scope);
    }

    @Override
    public MethodDeclaration createMethodDeclaration(Method method, List<Class<? extends Property>> propType,
                                                     List<? extends PropertyDeclaration> property) {
        Class<?> reqType = getTypeForProperty(method.getReturnType(), propType.get(0));
        if (!hasMethod(propType.get(0), "get", reqType, int.class)) {
            throw new IllegalComponentDefinitionException("Property implementation (" +
                                                          propType.get(0).getName() +
                                                          ") is incompatible with bean getter for type (" +
                                                          reqType + ")");
        }

        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method.getAnnotations(),
                                                     getSupportedAttributes());
        return new BeanGetterDeclaration(method, property.get(0), attrs,
                                         propType.get(0).getAnnotation(GenericProperty.class) != null);
    }

    @Override
    public MethodDeclaration createMethodDeclaration(ExecutableElement method, List<TypeMirror> propType,
                                                     List<? extends PropertyDeclaration> property) {
        TypeMirror reqType = getTypeForProperty(method.getReturnType(), propType.get(0));
        if (!hasMethod(propType.get(0), "get", reqType, ty.getPrimitiveType(TypeKind.INT))) {
            throw new IllegalComponentDefinitionException("Property implementation (" + propType.toString() +
                                                          ") is incompatible with bean getter for type (" +
                                                          reqType + ")");
        }

        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method,
                                                     getSupportedAttributes());
        boolean isGeneric = ty.asElement(propType.get(0)).getAnnotation(GenericProperty.class) != null;
        return new BeanGetterDeclaration(method, property.get(0), attrs, isGeneric);
    }

    @Override
    public boolean matches(Method method) {
        if (!super.matches(method)) {
            return false;
        }

        // additional logic
        if (method.getParameterTypes().length > 0) {
            // must not take arguments
            return false;
        }
        if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class)) {
            // must not return void
            return false;
        }

        return true;
    }

    @Override
    public boolean matches(ExecutableElement method) {
        if (!super.matches(method)) {
            return false;
        }

        // additional logic
        if (method.getParameters().size() > 0) {
            // must not take arguments
            return false;
        }
        if (method.getReturnType().getKind().equals(TypeKind.VOID)) {
            // must not return void
            return false;
        }

        return true;
    }

    private static class BeanGetterDeclaration extends AbstractMethodDeclaration {
        private final PropertyDeclaration property;
        private final boolean isGeneric;

        public BeanGetterDeclaration(Method method, PropertyDeclaration property, Set<Annotation> attrs,
                                     boolean isGeneric) {
            super(method, attrs);
            this.property = property;
            this.isGeneric = isGeneric;
        }

        public BeanGetterDeclaration(ExecutableElement method, PropertyDeclaration property,
                                     Set<Annotation> attrs, boolean isGeneric) {
            super(method, attrs);
            this.property = property;
            this.isGeneric = isGeneric;
        }

        @Override
        public void appendMembers(Generator generator) {
            // needs no extra members
        }

        @Override
        public void appendConstructorInitialization(Generator generator) {
            // needs no extra initialization
        }

        @Override
        public void appendMethodBody(Generator generator) {
            String field = generator.getPropertyMemberName(property.getName());
            String index = generator.getComponentIndex();
            if (isGeneric) {
                generator.appendSyntax("return (" + property.getType() + ") " + field + ".get(" + index +
                                       ");");
            } else {
                generator.appendSyntax("return " + field + ".get(" + index + ");");
            }
        }
    }
}
