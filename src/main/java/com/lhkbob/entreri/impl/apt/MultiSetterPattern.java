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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
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
 * * The method's return type is `void` or the Component type declaring the method (in which case the
 * implementation will return `this` for method chaining)
 * * The method's name starts with `set`
 *
 * This pattern defines multiple properties per method, where each property is named after the `@Named`
 * attribute applied to the parameter if present or uses the variable name of the parameter as a fallback.
 * This requires the backing Property of each to define a `void set(int, T)` method which it uses to implement
 * the matched methods.
 *
 * @author Michael Ludwig
 */
public class MultiSetterPattern extends AbstractMethodPattern {
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(set).+");

    public MultiSetterPattern() {
        super(Arrays.asList(Validate.class, DoNotAutoVersion.class));
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
                prop.getAttributes().addAll(getPropertyAttributes(context, p));
                methodAttrs.addAll(getMethodAttributes(p));
                fromArgs.add(prop);
            }

            methodAttrs.addAll(getMethodAttributes(m));
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

            Context ctx = generator.getContext();
            int property = 0;
            for (PropertyDeclaration prop : properties) {
                String name = getParameterNames().get(property);
                for (AnnotationMirror annot : prop.getAttributes()) {
                    if (generator.getContext().isAnnotationType(annot, Reference.class)) {
                        Validations
                                .appendReference(name, ctx.asAnnotation(annot, Reference.class), generator);
                    } else if (generator.getContext().isAnnotationType(annot, Within.class)) {
                        Validations.appendWithin(name, ctx.asAnnotation(annot, Within.class), generator);
                    } else if (generator.getContext().isAnnotationType(annot,
                                                                       com.lhkbob.entreri.property.Collection.class)) {
                        TypeMirror mapRawType = generator.getContext().getRawType(Map.class);
                        if (generator.getContext().getTypes().isAssignable(prop.getType(), mapRawType)) {
                            // must check values and keys separately
                            Validations.appendCollectionNoNullElements(name + ".keySet()",
                                                                       ctx.asAnnotation(annot,
                                                                                        com.lhkbob.entreri.property.Collection.class),
                                                                       generator);
                            Validations.appendCollectionNoNullElements(name + ".values()",
                                                                       ctx.asAnnotation(annot,
                                                                                        com.lhkbob.entreri.property.Collection.class),
                                                                       generator);
                        } else {
                            Validations.appendCollectionNoNullElements(name, ctx.asAnnotation(annot,
                                                                                              com.lhkbob.entreri.property.Collection.class),
                                                                       generator);
                        }
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
