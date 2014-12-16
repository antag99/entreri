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

import com.lhkbob.entreri.attr.Attribute;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SharedBeanGetterPattern
 * =======================
 *
 * A method pattern that matches Java bean style getter methods that have been annotated with
 * `@SharedInstance`. To work properly, this pattern must have a higher priority than the regular
 * BeanGetterPattern. This pattern matches methods that meet the following criteria:
 *
 * * The method has a single parameter of the same type as the return type
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
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(get|has|is).+");

    public SharedBeanGetterPattern() {
        super(Collections.<Class<? extends Annotation>>emptyList());
    }

    @Override
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods) {
        // null return type for flexible constraint, and 1 argument
        List<ExecutableElement> getters = context.getMatchingMethods(context.getComponentType(), methods,
                                                                     METHOD_NAME_PATTERN, null,
                                                                     (TypeMirror) null);

        Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = new HashMap<>();
        for (ExecutableElement m : getters) {
            // make sure that the return type is not void
            if (m.getReturnType().getKind() == TypeKind.VOID) {
                continue;
            }
            // make sure the single argument has the same type as the return of the method
            if (!context.getTypes().isSameType(m.getReturnType(), m.getParameters().get(0).asType())) {
                continue;
            }

            String name = getPropertyName(m, 0, false);
            if (name == null) {
                name = getPropertyName(m, "get", "is", "has");
            }

            PropertyDeclaration property = new PropertyDeclaration(context, name, m.getReturnType());
            property.getAttributes()
                    .addAll(getAttributes(Attribute.Level.PROPERTY, m, context.getAttributeScope()));
            property.getAttributes().addAll(getAttributes(Attribute.Level.PROPERTY, m.getParameters().get(0),
                                                          context.getAttributeScope()));

            Set<Annotation> methodAttrs = new HashSet<>();
            methodAttrs.addAll(getAttributes(Attribute.Level.METHOD, m, context.getAttributeScope()));
            methodAttrs.addAll(getAttributes(Attribute.Level.METHOD, m.getParameters().get(0),
                                             context.getAttributeScope()));
            MethodDeclaration method = new SharedBeanGetterDeclaration(m, property, methodAttrs);
            property.getMethods().add(method);

            matches.put(m, Collections.singleton(property));
        }

        return matches;
    }

    private static class SharedBeanGetterDeclaration extends AbstractMethodDeclaration {
        private PropertyDeclaration property;

        public SharedBeanGetterDeclaration(ExecutableElement method, PropertyDeclaration property,
                                           Set<Annotation> attrs) {
            super(method, attrs);
            this.property = property;
        }

        @Override
        public void replace(PropertyDeclaration original, PropertyDeclaration replaceWith) {
            if (property == original) {
                property = replaceWith;
            }
        }

        @Override
        public boolean arePropertiesValid(Context context) {
            // the property must define a get(int, T) -> void method where T is the type of the property declaration
            return context.hasMethod(property.getPropertyImplementation(), "get",
                                     context.getTypes().getNoType(TypeKind.VOID),
                                     context.getTypes().getPrimitiveType(TypeKind.INT), property.getType());
        }

        @Override
        public String getAnnotationSyntax(Context context, PropertyDeclaration property,
                                          TypeMirror attribute) {
            String fromMethod = getMethodAnnotationReflectionSyntax(context, attribute);
            if (fromMethod == null) {
                return getMethodParameterAnnotationReflectionSyntax(context, 0, attribute);
            } else {
                return fromMethod;
            }
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
            String arg = getParameterNames().get(0);
            Validations.appendNotNull(arg, generator);
            generator.appendSyntax(field + ".get(" + index + ", " + arg + ");",
                                   "return " + getParameterNames().get(0) + ";");
        }

        @Override
        protected Collection<PropertyDeclaration> getProperties() {
            return Collections.singleton(property);
        }
    }
}
