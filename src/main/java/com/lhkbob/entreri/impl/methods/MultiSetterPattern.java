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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * MultiSetterPattern
 * ==================
 *
 * A method pattern that matches setter methods that take multiple arguments. This is useful when validation
 * must be performed on the new values between multiple properties before either is modified (such as
 * preserving an ordering constraint). This pattern matches methods that meet the following criteria:
 *
 * * The method has more than one parameter
 * * Every parameter is annotated with `@Named` specifying the property the parameter corresponds to
 * * The method's return type is `void` or the Component type declaring the method (in which case the
 * implementation will return `this` for method chaining)
 * * The method's name starts with `set`
 *
 * @author Michael Ludwig
 */
public class MultiSetterPattern extends AbstractMethodPattern {
    public MultiSetterPattern(Elements eu, Types ty, Messager log) {
        super(eu, ty, log, Arrays.asList("set"),
              Arrays.asList(Validate.class, NotNull.class, Within.class, DontAutoVersion.class));
    }

    @Override
    public Map<String, Class<?>> getDeclaredProperties(Method method) {
        Map<String, Class<?>> props = new HashMap<>();
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            props.put(getExplicitName(method, i), params[i]);
        }
        return props;
    }

    @Override
    public Map<String, TypeMirror> getDeclaredProperties(ExecutableElement method) {
        Map<String, TypeMirror> props = new HashMap<>();
        for (int i = 0; i < method.getParameters().size(); i++) {
            props.put(getExplicitName(method, i), method.getParameters().get(i).asType());
        }
        return props;
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(Method method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        Set<Annotation> result = accumulateAttributes(Attribute.Level.PROPERTY, method.getAnnotations(),
                                                      scope);
        int numParams = method.getParameterTypes().length;
        for (int i = 0; i < numParams; i++) {
            if (getExplicitName(method, i).equals(property)) {
                result.addAll(accumulateAttributes(Attribute.Level.PROPERTY,
                                                   method.getParameterAnnotations()[i], scope));
            }
        }
        return result;
    }

    @Override
    public Set<Annotation> getPropertyLevelAttributes(ExecutableElement method, String property,
                                                      Set<Class<? extends Annotation>> scope) {
        Set<Annotation> result = accumulateAttributes(Attribute.Level.PROPERTY, method, scope);
        int numParams = method.getParameters().size();
        for (int i = 0; i < numParams; i++) {
            if (getExplicitName(method, i).equals(property)) {
                result.addAll(accumulateAttributes(Attribute.Level.PROPERTY, method.getParameters().get(i),
                                                   scope));
            }
        }
        return result;
    }

    @Override
    public MethodDeclaration createMethodDeclaration(Method method, List<Class<? extends Property>> propClass,
                                                     List<? extends PropertyDeclaration> property) {
        List<PropertyDeclaration> ordered = new ArrayList<>();
        int numParams = method.getParameterTypes().length;
        for (int i = 0; i < numParams; i++) {
            int paramIndex = indexOf(property, getExplicitName(method, i));
            validateProperty(method, i, propClass.get(paramIndex));
            ordered.add(property.get(i));
        }

        // if it's not void it must be the component type (assuming it passed matches())
        boolean returnComponent =
                !method.getReturnType().equals(void.class) && !method.getReturnType().equals(Void.class);
        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method.getAnnotations(),
                                                     getSupportedAttributes());
        for (Annotation[] paramAnnot : method.getParameterAnnotations()) {
            attrs.addAll(accumulateAttributes(Attribute.Level.METHOD, paramAnnot, getSupportedAttributes()));
        }
        return new MultiSetterDeclaration(method, ordered, attrs, returnComponent);
    }

    @Override
    public MethodDeclaration createMethodDeclaration(ExecutableElement method, List<TypeMirror> propType,
                                                     List<? extends PropertyDeclaration> property) {
        List<PropertyDeclaration> ordered = new ArrayList<>();
        int numParams = method.getParameters().size();
        for (int i = 0; i < numParams; i++) {
            int paramIndex = indexOf(property, getExplicitName(method, i));
            validateProperty(method, i, propType.get(paramIndex));
            ordered.add(property.get(i));
        }

        // if it's not void it must be the component type (assuming it passed matches())
        boolean returnComponent = !method.getReturnType().getKind().equals(TypeKind.VOID);
        Set<Annotation> attrs = accumulateAttributes(Attribute.Level.METHOD, method,
                                                     getSupportedAttributes());
        for (VariableElement param : method.getParameters()) {
            attrs.addAll(accumulateAttributes(Attribute.Level.METHOD, param, getSupportedAttributes()));
        }
        return new MultiSetterDeclaration(method, ordered, attrs, returnComponent);
    }

    private int indexOf(List<? extends PropertyDeclaration> properties, String name) {
        for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private void validateProperty(Method method, int param, Class<? extends Property> property) {
        Class<?> reqType = getTypeForProperty(method.getParameterTypes()[param], property);
        if (!hasMethod(property, "set", void.class, int.class, reqType)) {
            throw new IllegalComponentDefinitionException("Property type (" + property +
                                                          ") is incompatible with bean setter for type (" +
                                                          reqType + ")");
        }
    }

    private void validateProperty(ExecutableElement method, int param, TypeMirror property) {
        TypeMirror reqType = getTypeForProperty(method.getParameters().get(param).asType(), property);
        if (!hasMethod(property, "set", ty.getNoType(TypeKind.VOID), ty.getPrimitiveType(TypeKind.INT),
                       reqType)) {
            throw new IllegalComponentDefinitionException("Property type (" + property +
                                                          ") is incompatible with bean setter for type (" +
                                                          reqType + ")");
        }
    }

    @Override
    public boolean matches(Method method) {
        if (!super.matches(method)) {
            return false;
        }

        // must return void or the component type
        if (!method.getReturnType().equals(void.class) &&
            !method.getReturnType().equals(method.getDeclaringClass())) {
            return false;
        }

        // must have more than one parameter, all annotated with @Named
        if (method.getParameterTypes().length <= 1) {
            return false;
        }
        int numParams = method.getParameterTypes().length;
        for (int i = 0; i < numParams; i++) {
            if (getExplicitName(method, i) == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matches(ExecutableElement method) {
        if (!super.matches(method)) {
            return false;
        }

        // must return void or the component type
        TypeMirror declaringType = method.getEnclosingElement().asType();
        if (!method.getReturnType().getKind().equals(TypeKind.VOID) &&
            !ty.isSameType(declaringType, method.getReturnType())) {
            return false;
        }

        // must have more than one parameter, all annotated with @Named
        if (method.getParameters().size() <= 1) {
            return false;
        }
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (getExplicitName(method, i) == null) {
                return false;
            }
        }

        return true;
    }

    private static class MultiSetterDeclaration extends AbstractMethodDeclaration {
        // expected to be in parameter order
        private final List<PropertyDeclaration> properties;
        private final boolean returnsComponent;

        protected MultiSetterDeclaration(Method method, List<PropertyDeclaration> properties,
                                         Set<Annotation> attrs, boolean returnsComponent) {
            super(method, attrs);
            this.properties = properties;
            this.returnsComponent = returnsComponent;
        }

        protected MultiSetterDeclaration(ExecutableElement method, List<PropertyDeclaration> properties,
                                         Set<Annotation> attrs, boolean returnsComponent) {
            super(method, attrs);
            this.properties = properties;
            this.returnsComponent = returnsComponent;
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
            // do method level validation first
            for (Annotation attr : getAttributes()) {
                if (attr instanceof Validate) {
                    Validations.appendValidation(getParameterNames(), (Validate) attr, generator);
                }
            }

            int updateCount = 0;
            int property = 0;
            for (PropertyDeclaration prop : properties) {
                boolean needsUpdate = true;
                for (Annotation attr : prop.getAttributes()) {
                    if (attr instanceof NotNull) {
                        Validations.appendNotNull(getParameterNames().get(property), generator);
                    } else if (attr instanceof Within) {
                        Validations.appendWithin(getParameterNames().get(property), (Within) attr, generator);
                    } else if (attr instanceof DontAutoVersion) {
                        needsUpdate = false;
                    }
                    // else ignore the unsupported attribute
                }

                if (needsUpdate) {
                    updateCount++;
                }
                property++;
            }

            String index = generator.getComponentIndex();
            for (int i = 0; i < getParameterNames().size(); i++) {
                String field = generator.getPropertyMemberName(properties.get(i).getName());
                generator.appendSyntax(field + ".set(" + index + ", " + getParameterNames().get(i) + ");");
            }

            if (updateCount > 0) {
                generator.appendSyntax("updateVersion();");
            }

            if (returnsComponent) {
                generator.appendSyntax("return this;");
            }
        }
    }
}
