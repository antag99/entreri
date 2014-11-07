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
import com.lhkbob.entreri.attr.SharedInstance;
import com.lhkbob.entreri.impl.Generator;
import com.lhkbob.entreri.impl.MethodDeclaration;
import com.lhkbob.entreri.impl.PropertyDeclaration;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.ShareableProperty;

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
 * SharedBeanGetterPattern
 * =======================
 *
 * A method pattern that matches Java bean style getter methods that have been annotated with
 * `@SharedInstance`. To work properly, this pattern must have a higher priority than the regular
 * BeanGetterPattern. This pattern matches methods that meet the following criteria:
 *
 * * The method has no parameters
 * * The method is annotated with `@SharedInstance`
 * * The method's return type is not `void`, where the return type is the declared type of the property
 * * The method's name starts with `get`, `is`, or `has`, and the lower-cased remainder of the name is taken
 * as the declared name of the property
 *
 * If the method is annotated with `@Named` that is used to define the property name independently of the
 * method name, although the method name must still start with the defined prefixes.
 *
 * @author Michael Ludwig
 */
public class SharedBeanGetterPattern extends AbstractMethodPattern {
    public SharedBeanGetterPattern(Elements eu, Types ty, Messager log) {
        super(eu, ty, log, Arrays.asList("get", "is", "has"),
              Arrays.<Class<? extends Annotation>>asList(SharedInstance.class));
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
    public MethodDeclaration createMethodDeclaration(Method method, List<Class<? extends Property>> propClass,
                                                     List<? extends PropertyDeclaration> property) {
        if (!ShareableProperty.class.isAssignableFrom(propClass.get(0))) {
            // property must be a shareable property
            throw new IllegalComponentDefinitionException("Property implementation (" + propClass.get(0) +
                                                          ") does not extend ShareableProperty, is incompatible with @SharedInstance applied to " +
                                                          method.getName());
        }

        // note that this implicitly prevents generic properties from working with shared types if they
        // do not exactly equal the requested type (which is what we want, since inheritance would not work
        // with a shared instance of the super type).
        if (!hasMethod(propClass.get(0), "get", void.class, int.class, method.getReturnType()) &&
            !hasMethod(propClass.get(0), "createShareableInstance", method.getReturnType())) {
            throw new IllegalComponentDefinitionException("Property implementation (" + propClass.get(0) +
                                                          ") is incompatible with shared instance getter for type (" +
                                                          method.getReturnType() + ")");
        }

        return new SharedBeanGetterDeclaration(method, property.get(0),
                                               accumulateAttributes(Attribute.Level.METHOD,
                                                                    method.getAnnotations(),
                                                                    getSupportedAttributes()));
    }

    @Override
    public MethodDeclaration createMethodDeclaration(ExecutableElement method, List<TypeMirror> propType,
                                                     List<? extends PropertyDeclaration> property) {
        TypeMirror shareablePropertyType = ty.erasure(eu.getTypeElement(ShareableProperty.class.getName())
                                                        .asType());
        if (!ty.isAssignable(ty.erasure(propType.get(0)), shareablePropertyType)) {
            // property must be a shareable property
            throw new IllegalComponentDefinitionException("Property implementation (" + propType.get(0) +
                                                          ") does not extend ShareableProperty, is incompatible with @SharedInstance applied to " +
                                                          method.getSimpleName().toString());
        }

        // note that this implicitly prevents generic properties from working with shared types if they
        // do not exactly equal the requested type (which is what we want, since inheritance would not work
        // with a shared instance of the super type).
        if (!hasMethod(propType.get(0), "get", ty.getNoType(TypeKind.VOID), ty.getPrimitiveType(TypeKind.INT),
                       method.getReturnType()) &&
            !hasMethod(propType.get(0), "createShareableInstance", method.getReturnType())) {
            throw new IllegalComponentDefinitionException("Property implementation (" + propType.get(0) +
                                                          ") is incompatible with shared instance getter for type (" +
                                                          method.getReturnType() + ")");
        }

        return new SharedBeanGetterDeclaration(method, property.get(0),
                                               accumulateAttributes(Attribute.Level.METHOD, method,
                                                                    getSupportedAttributes()));
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
        if (method.getReturnType().equals(void.class)) {
            // must not return void
            return false;
        }
        if (method.getAnnotation(SharedInstance.class) == null) {
            // must be annotated with @SharedInstance
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
        if (method.getAnnotation(SharedInstance.class) == null) {
            // must be annotated with @SharedInstance
            return false;
        }

        return true;
    }

    private static class SharedBeanGetterDeclaration extends AbstractMethodDeclaration {
        private final PropertyDeclaration property;
        private final String sharedKey;

        public SharedBeanGetterDeclaration(Method method, PropertyDeclaration property,
                                           Set<Annotation> attrs) {
            super(method, attrs);
            this.property = property;
            sharedKey = "sharedInstance_" + property.getName();
        }

        public SharedBeanGetterDeclaration(ExecutableElement method, PropertyDeclaration property,
                                           Set<Annotation> attrs) {
            super(method, attrs);
            this.property = property;
            sharedKey = "sharedInstance_" + property.getName();
        }

        @Override
        public void appendMembers(Generator generator) {
            String member = generator.getMemberName(sharedKey, this);
            generator.appendSyntax("private final " + property.getType() + " " + member + ";");
        }

        @Override
        public void appendConstructorInitialization(Generator generator) {
            String member = generator.getMemberName(sharedKey, this);
            String propertyMember = generator.getPropertyMemberName(property.getName());

            generator.appendSyntax(member + " = " + propertyMember + ".createShareableInstance();");
        }

        @Override
        public void appendMethodBody(Generator generator) {
            String member = generator.getMemberName(sharedKey, this);
            String propertyMember = generator.getPropertyMemberName(property.getName());
            String index = generator.getComponentIndex();

            generator.appendSyntax(propertyMember + ".get(" + index + ", " + member + ");",
                                   "return " + member + ";");
        }
    }
}
