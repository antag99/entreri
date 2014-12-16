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

import com.lhkbob.entreri.attr.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Collection;
import java.util.regex.Pattern;

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
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(set).+");

    public MultiSetterPattern() {
        super(Arrays.asList(Validate.class, Reference.class, Within.class, DoNotAutoVersion.class));
    }

    @Override
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods) {
        List<ExecutableElement> setters = context.getMatchingMethods(context.getComponentType(), methods,
                                                                     METHOD_NAME_PATTERN, null,
                                                                     (TypeMirror[]) null);

        Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = new HashMap<>();
        for (ExecutableElement m : setters) {
            // further filter the methods to have > 1 argument and a return type of void or the component type
            if (m.getParameters().size() <= 1) {
                continue;
            }
            if (m.getReturnType().getKind() != TypeKind.VOID &&
                !context.getTypes().isSameType(context.getComponentType(), m.getReturnType())) {
                continue;
            }

            Set<Annotation> methodAttrs = new HashSet<>();
            List<PropertyDeclaration> fromArgs = new ArrayList<>();
            int index = 0;
            for (VariableElement p : m.getParameters()) {
                String name = getPropertyName(m, index++, true);
                PropertyDeclaration prop = new PropertyDeclaration(context, name, p.asType());
                prop.getAttributes()
                    .addAll(getAttributes(Attribute.Level.PROPERTY, p, context.getAttributeScope()));
                methodAttrs.addAll(getAttributes(Attribute.Level.METHOD, p, context.getAttributeScope()));
                fromArgs.add(prop);
            }

            methodAttrs.addAll(getAttributes(Attribute.Level.METHOD, m, context.getAttributeScope()));
            MethodDeclaration methodDecl = new MultiSetterDeclaration(m, fromArgs, methodAttrs,
                                                                      m.getReturnType().getKind() !=
                                                                      TypeKind.VOID);
            for (PropertyDeclaration p : fromArgs) {
                p.getMethods().add(methodDecl);
            }

            matches.put(m, fromArgs);
        }
        return matches;
    }

    private static class MultiSetterDeclaration extends AbstractMethodDeclaration {
        // expected to be in parameter order
        private final List<PropertyDeclaration> properties;
        private final boolean returnsComponent;

        protected MultiSetterDeclaration(ExecutableElement method, List<PropertyDeclaration> properties,
                                         Set<Annotation> attrs, boolean returnsComponent) {
            super(method, attrs);
            this.properties = properties;
            this.returnsComponent = returnsComponent;
        }

        @Override
        public void replace(PropertyDeclaration original, PropertyDeclaration replaceWith) {
            int index = properties.indexOf(original);
            if (index >= 0) {
                properties.set(index, replaceWith);
            }
        }

        @Override
        public boolean arePropertiesValid(Context context) {
            for (PropertyDeclaration p : properties) {
                if (!context.hasMethod(p.getPropertyImplementation(), "set",
                                       context.getTypes().getNoType(TypeKind.VOID),
                                       context.getTypes().getPrimitiveType(TypeKind.INT), p.getType())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getAnnotationSyntax(Context context, PropertyDeclaration property,
                                          TypeMirror attribute) {
            int index = properties.indexOf(property);
            return getMethodParameterAnnotationReflectionSyntax(context, index, attribute);
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
            boolean needsUpdate = true;
            for (Annotation attr : getAttributes()) {
                if (attr instanceof Validate) {
                    Validations.appendValidation(getParameterNames(), (Validate) attr, generator);
                } else if (attr instanceof DoNotAutoVersion) {
                    needsUpdate = false;
                }
            }

            int property = 0;
            for (PropertyDeclaration prop : properties) {
                for (Annotation attr : prop.getAttributes()) {
                    if (attr instanceof Reference) {
                        Validations.appendReference(getParameterNames().get(property), (Reference) attr,
                                                    generator);
                    }
                    // else ignore the unsupported attribute
                }

                property++;
            }

            String index = generator.getComponentIndex();
            for (int i = 0; i < getParameterNames().size(); i++) {
                String field = generator.getPropertyMemberName(properties.get(i).getName());
                generator.appendSyntax(field + ".set(" + index + ", " + getParameterNames().get(i) + ");");
            }

            if (needsUpdate) {
                generator.appendSyntax("updateVersion();");
            }

            if (returnsComponent) {
                generator.appendSyntax("return this;");
            }
        }

        @Override
        protected Collection<PropertyDeclaration> getProperties() {
            return properties;
        }
    }
}
