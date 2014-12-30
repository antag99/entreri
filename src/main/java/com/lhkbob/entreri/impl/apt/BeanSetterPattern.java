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

import com.lhkbob.entreri.DoNotAutoVersion;
import com.lhkbob.entreri.Validate;
import com.lhkbob.entreri.property.Reference;
import com.lhkbob.entreri.property.Within;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

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
 * name independently of the method name, although the method name must still start with `set`. It requires
 * the backing Property to provide a `void set(int, T)` method which it uses to implement the matched methods.
 *
 * @author Michael Ludwig
 */
public class BeanSetterPattern extends AbstractMethodPattern {
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(set).+");

    public BeanSetterPattern() {
        super(Arrays.asList(Validate.class, DoNotAutoVersion.class));
    }

    @Override
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods) {
        // null return type for flexible constraint, and 1 null argument for flexible constraint
        List<ExecutableElement> setters = context.getMatchingMethods(context.getComponentType(), methods,
                                                                     METHOD_NAME_PATTERN, null,
                                                                     (TypeMirror) null);

        Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = new HashMap<>();
        for (ExecutableElement m : setters) {
            // make sure that the return type is void or equal to the component type
            if (m.getReturnType().getKind() != TypeKind.VOID &&
                !context.getTypes().isSameType(context.getComponentType(), m.getReturnType())) {
                continue;
            }

            String name = getPropertyName(m, 0, false);
            if (name == null) {
                name = getPropertyName(m, "set");
            }
            PropertyDeclaration property = new PropertyDeclaration(context, name,
                                                                   m.getParameters().get(0).asType());
            property.getAttributes().addAll(getPropertyAttributes(context, m));
            property.getAttributes().addAll(getPropertyAttributes(context, m.getParameters().get(0)));

            Set<Annotation> methodAttrs = new HashSet<>();
            methodAttrs.addAll(getMethodAttributes(m));
            methodAttrs.addAll(getMethodAttributes(m.getParameters().get(0)));
            MethodDeclaration method = new BeanSetterDeclaration(m, property, methodAttrs,
                                                                 m.getReturnType().getKind() !=
                                                                 TypeKind.VOID);
            property.getMethods().add(method);

            matches.put(m, Collections.singleton(property));
        }

        return matches;
    }

    private static class BeanSetterDeclaration extends AbstractMethodDeclaration {
        private final boolean returnsComponent;
        private PropertyDeclaration property;

        protected BeanSetterDeclaration(ExecutableElement method, PropertyDeclaration property,
                                        Set<Annotation> attrs, boolean returnsComponent) {
            super(method, attrs);
            this.returnsComponent = returnsComponent;
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
            // must have a set(int, T) -> void method where T is the property type
            return context.hasMethod(property.getPropertyImplementation(), "set",
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
            // needs no additional members
        }

        @Override
        public void appendConstructorInitialization(Generator generator) {
            // needs no additional initialization
        }

        @Override
        public void appendMethodBody(Generator generator) {
            // perform any validation
            boolean needsUpdate = true;
            for (Annotation annot : getAttributes()) {
                if (annot instanceof Validate) {
                    Validations.appendValidation(getParameterNames(), (Validate) annot, generator);
                } else if (annot instanceof DoNotAutoVersion) {
                    needsUpdate = false;
                }
            }

            Context ctx = generator.getContext();
            String name = getParameterNames().get(0);
            for (AnnotationMirror annot : property.getAttributes()) {
                if (generator.getContext().isAnnotationType(annot, Reference.class)) {
                    Validations.appendReference(name, ctx.asAnnotation(annot, Reference.class), generator);
                } else if (generator.getContext().isAnnotationType(annot, Within.class)) {
                    Validations.appendWithin(name, ctx.asAnnotation(annot, Within.class), generator);
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

        @Override
        protected Collection<PropertyDeclaration> getProperties() {
            return Collections.singleton(property);
        }
    }
}
