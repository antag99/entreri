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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

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
 * method name, although the method name must still start with the defined prefixes. It requires the
 * backing Property to provide a `T get(int)` method that it uses to implement the matched methods.
 *
 * @author Michael Ludwig
 */
public class BeanGetterPattern extends AbstractMethodPattern {
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(get|has|is).+");

    public BeanGetterPattern() {
        super(Collections.<Class<? extends Annotation>>emptyList());
    }

    @Override
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods) {
        // null return type for flexible constraint, and 0 arguments
        List<ExecutableElement> getters = context.getMatchingMethods(context.getComponentType(), methods,
                                                                     METHOD_NAME_PATTERN, null);

        Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = new HashMap<>();
        for (ExecutableElement m : getters) {
            // make sure that the return type is not void
            if (m.getReturnType().getKind() == TypeKind.VOID) {
                continue;
            }

            PropertyDeclaration property = new PropertyDeclaration(context,
                                                                   getPropertyName(m, "get", "is", "has"),
                                                                   m.getReturnType());
            property.getAttributes().addAll(getPropertyAttributes(context, m));
            MethodDeclaration method = new BeanGetterDeclaration(m, property, getMethodAttributes(m));
            property.getMethods().add(method);

            matches.put(m, Collections.singleton(property));
        }

        return matches;
    }

    private static class BeanGetterDeclaration extends AbstractMethodDeclaration {
        private PropertyDeclaration property;

        public BeanGetterDeclaration(ExecutableElement method, PropertyDeclaration property,
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
            // the property must define a get(int) -> T method where T is the type of the property declaration
            return context.hasMethod(property.getPropertyImplementation(), "get", property.getType(),
                                     context.getTypes().getPrimitiveType(TypeKind.INT));
        }

        @Override
        public String getAnnotationSyntax(Context context, PropertyDeclaration property,
                                          TypeMirror attribute) {
            return getMethodAnnotationReflectionSyntax(context, attribute);
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
            generator.appendSyntax("return " + field + ".get(" + index + ");");
        }

        @Override
        protected Collection<PropertyDeclaration> getProperties() {
            return Collections.singleton(property);
        }
    }
}
