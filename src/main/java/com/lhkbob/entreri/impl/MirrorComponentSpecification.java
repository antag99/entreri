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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.Ownable;
import com.lhkbob.entreri.Owner;
import com.lhkbob.entreri.attr.Attribute;
import com.lhkbob.entreri.property.PropertyFactory;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * MirrorComponentSpecification
 * ============================
 *
 * MirrorComponentSpecification is an implementation that extracts a component specification from the mirror
 * API defined in javax.lang.model.  This should only be used in the context of an annotation processor with a
 * valid processing environment.
 *
 * @author Michael Ludwig
 */
public class MirrorComponentSpecification implements ComponentSpecification {
    private final String typeName;
    private final String packageName;
    private final List<MirrorPropertyDeclaration> properties;
    private final List<MethodDeclaration> methods;

    public MirrorComponentSpecification(TypeElement type, List<? extends MethodPattern> patterns, Types tu,
                                        Elements eu, Filer io, Messager log) {
        TypeMirror baseComponentType = eu.getTypeElement(Component.class.getCanonicalName()).asType();
        TypeMirror ownerType = eu.getTypeElement(Owner.class.getCanonicalName()).asType();
        TypeMirror ownableType = eu.getTypeElement(Ownable.class.getCanonicalName()).asType();
        TypeMirror objectType = eu.getTypeElement(Object.class.getCanonicalName()).asType();

        if (!tu.isAssignable(type.asType(), baseComponentType)) {
            throw fail(type.asType(), "Class must extend Component");
        }
        if (!type.getKind().equals(ElementKind.INTERFACE)) {
            throw fail(type.asType(), "Component definition must be an interface");
        }

        List<MirrorPropertyDeclaration> properties = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();

        Set<Class<? extends Annotation>> interested = new HashSet<>();
        interested.add(com.lhkbob.entreri.attr.Factory.class);
        for (MethodPattern pattern : patterns) {
            interested.addAll(pattern.getSupportedAttributes());
        }
        Iterator<Class<? extends Annotation>> it = interested.iterator();
        while (it.hasNext()) {
            Class<? extends Annotation> attr = it.next();
            if (attr.getAnnotation(Attribute.class) == null) {
                it.remove();
                log.printMessage(Diagnostic.Kind.WARNING,
                                 attr + " used as an attribute, but is not annotated with @Attribute");
            }
        }

        // since this is an interface, we're only dealing with public methods
        // so getMethods() returns everything we're interested in plus the methods
        // declared in Component, which we'll have to exclude
        Map<ExecutableElement, MethodPattern> validMethods = new HashMap<>();
        Map<ExecutableElement, List<String>> methodProperties = new HashMap<>();
        Map<String, MirrorPropertyDeclaration> declaredProperties = new HashMap<>();

        for (ExecutableElement m : ElementFilter.methodsIn(eu.getAllMembers(type))) {
            // exclude methods defined in Component, Owner, Ownable, and Object
            TypeMirror declare = findEnclosingTypeElement(m).asType();
            if (tu.isSameType(declare, baseComponentType) ||
                tu.isSameType(declare, ownableType) ||
                tu.isSameType(declare, ownerType) ||
                tu.isSameType(declare, objectType)) {
                continue;
            }

            boolean matched = false;
            for (MethodPattern pattern : patterns) {
                if (pattern.matches(m)) {
                    validMethods.put(m, pattern);
                    matched = true;

                    // determine the set of properties defined by this method and merge it into component state
                    Map<String, TypeMirror> methodDeclaredTypes = pattern.getDeclaredProperties(m);
                    for (Map.Entry<String, TypeMirror> p : methodDeclaredTypes.entrySet()) {
                        if (p.getValue() != null) {
                            // this method explicitly declares a named property and its type
                            MirrorPropertyDeclaration oldProp = declaredProperties.get(p.getKey());
                            if (oldProp != null) {
                                if (!tu.isSameType(oldProp.type, p.getValue())) {
                                    throw fail(type.asType(),
                                               p.getKey() + " has inconsistent type across its methods");
                                }
                                // else type is consistent but some other method already created the prop declaration
                            } else {
                                // make a new property declaration
                                MirrorPropertyDeclaration prop = new MirrorPropertyDeclaration(p.getKey(),
                                                                                               p.getValue());

                                declaredProperties.put(p.getKey(), prop);
                                properties.add(prop);
                            }
                        } else {
                            // this method references a property but doesn't exactly know the type of it yet
                            declaredProperties.put(p.getKey(), null);
                        }
                    }

                    // remember the property names needed for this pattern
                    methodProperties.put(m, new ArrayList<>(methodDeclaredTypes.keySet()));

                    break;
                }
            }

            if (!matched) {
                throw fail(declare, m.getSimpleName().toString() + " is an unsupported property method");
            }
        }

        // make sure all declared properties have a known type
        for (Map.Entry<String, MirrorPropertyDeclaration> p : declaredProperties.entrySet()) {
            if (p.getValue() == null) {
                throw fail(type.asType(),
                           p.getKey() + " is referenced but a concrete type could not be determined");
            }
        }

        // update the list of attribute annotations for all properties
        for (Map.Entry<ExecutableElement, MethodPattern> m : validMethods.entrySet()) {
            for (String property : methodProperties.get(m.getKey())) {
                Set<Annotation> attrs = m.getValue()
                                         .getPropertyLevelAttributes(m.getKey(), property, interested);
                declaredProperties.get(property).addAttributes(attrs);
            }
        }

        // now all attributes are available on the property declaration, the property implementation can be chosen
        for (MirrorPropertyDeclaration prop : declaredProperties.values()) {
            prop.updatePropertyImplementation(tu, eu, io);
        }

        // compute method declarations based on these properties (now complete up to method declarations)
        for (ExecutableElement m : validMethods.keySet()) {
            MethodPattern pattern = validMethods.get(m);
            List<TypeMirror> propTypes = new ArrayList<>();
            List<MirrorPropertyDeclaration> props = new ArrayList<>();
            for (String p : methodProperties.get(m)) {
                MirrorPropertyDeclaration prop = declaredProperties.get(p);
                props.add(prop);
                propTypes.add(prop.propertyType);
            }

            MethodDeclaration method = pattern.createMethodDeclaration(m, propTypes, props);

            // add the method to every property
            for (MirrorPropertyDeclaration prop : props) {
                prop.methods.add(method);
            }
            methods.add(method);
        }

        // order the list of properties by their natural ordering
        Collections.sort(properties);
        Collections.sort(methods);

        String qualifiedName = type.getQualifiedName().toString();
        String packageName = eu.getPackageOf(type).getQualifiedName().toString();
        if (packageName.isEmpty()) {
            typeName = qualifiedName;
            this.packageName = "";
        } else {
            typeName = qualifiedName.substring(packageName.length() + 1);
            this.packageName = packageName;
        }

        this.properties = Collections.unmodifiableList(properties);
        this.methods = Collections.unmodifiableList(methods);
    }

    @Override
    public String getType() {
        return typeName;
    }

    @Override
    public String getPackage() {
        return packageName;
    }

    @Override
    public List<? extends PropertyDeclaration> getProperties() {
        return properties;
    }

    @Override
    public List<? extends MethodDeclaration> getMethods() {
        return methods;
    }

    private static IllegalComponentDefinitionException fail(TypeMirror type, String msg) {
        return new IllegalComponentDefinitionException(type.toString(), msg);
    }

    private static class MirrorPropertyDeclaration implements PropertyDeclaration {
        private final String name;

        private final TypeMirror type;
        private TypeMirror propertyType; // cannot be determined prior to construction since @Factory could be on any referencing method

        private final Set<Annotation> annotations;
        private final List<MethodDeclaration> methods;

        public MirrorPropertyDeclaration(String name, TypeMirror type) {
            this.name = name;
            this.type = type;

            annotations = new HashSet<>();
            methods = new ArrayList<>();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type.toString();
        }

        @Override
        public String getPropertyImplementation() {
            return propertyType.toString();
        }

        @Override
        public List<MethodDeclaration> getMethods() {
            return Collections.unmodifiableList(methods);
        }

        @Override
        public Set<Annotation> getAttributes() {
            return Collections.unmodifiableSet(annotations);
        }

        @Override
        public PropertyFactory<?> getPropertyFactory() {
            throw new UnsupportedOperationException("Cannot create PropertyFactory with mirror API");
        }

        @Override
        public int compareTo(PropertyDeclaration o) {
            return name.compareTo(o.getName());
        }

        private void addAttributes(Set<Annotation> annotations) {
            // add annotations, but make sure that annotations of the same type are not added
            // if the values are equal then ignore the duplicates; if they disagree in parameters then
            // a conflict exists and the component is invalid
            for (Annotation a : annotations) {
                if (!this.annotations.contains(a)) {
                    for (Annotation o : this.annotations) {
                        if (a.getClass().equals(o.getClass())) {
                            // a is of the same type as o, but they are not equal
                            throw new IllegalComponentDefinitionException("Conflicting applications of " +
                                                                          a.getClass() + " on property " +
                                                                          name);
                        }
                    }
                    // not of any prior type
                    this.annotations.add(a);
                } // ignore duplicates
            }
        }

        private void updatePropertyImplementation(Types tu, Elements eu, Filer io) {
            TypeMirror propFactoryType = null;

            for (Annotation a : annotations) {
                if (a instanceof com.lhkbob.entreri.attr.Factory) {
                    try {
                        ((com.lhkbob.entreri.attr.Factory) a).value();
                    } catch (MirroredTypeException te) {
                        propFactoryType = te.getTypeMirror();
                    }
                    break;
                }
            }

            if (propFactoryType == null) {
                // look up from the file mapping
                propFactoryType = TypePropertyMapping.getPropertyFactory(type, tu, eu, io);
            }

            if (propFactoryType != null) {
                validateFactoryConstructor(propFactoryType, tu, eu);

                TypeMirror propertyType = null;
                List<? extends ExecutableElement> factoryMethods = ElementFilter
                                                                           .methodsIn(eu.getAllMembers((TypeElement) tu.asElement(propFactoryType)));
                for (ExecutableElement m : factoryMethods) {
                    if (m.getSimpleName().contentEquals("create")) {
                        propertyType = m.getReturnType();
                        break;
                    }
                }
                if (propertyType == null) {
                    throw new RuntimeException(propFactoryType + " is missing create() method");
                }

                this.propertyType = propertyType;
            } else {
                throw new RuntimeException("Unable to determine PropertyFactory for property " + name);
            }
        }

        private void validateFactoryConstructor(TypeMirror factoryType, Types tu, Elements eu) {
            List<? extends ExecutableElement> ctors = ElementFilter
                                                              .constructorsIn(eu.getAllMembers((TypeElement) tu.asElement(factoryType)));
            boolean foundValid = false;
            for (ExecutableElement ctor : ctors) {
                if (isValidConstructor(ctor, tu, eu)) {
                    foundValid = true;
                    break;
                }
            }

            if (!foundValid) {
                throw new IllegalComponentDefinitionException("PropertyFactory referenced by property " +
                                                              name +
                                                              " has no valid constructor to invoke by reflection");
            }
        }

        private boolean isValidConstructor(ExecutableElement ctor, Types tu, Elements eu) {
            List<? extends TypeParameterElement> args = ctor.getTypeParameters();
            if (args.size() == 0) {
                return true;
            }

            int startIndex = 0;
            if (tu.isSameType(args.get(0).asType(),
                              eu.getTypeElement(Class.class.getCanonicalName()).asType())) {
                startIndex = 1;
            }

            TypeMirror annotClass = eu.getTypeElement(Annotation.class.getCanonicalName()).asType();
            for (int i = startIndex; i < args.size(); i++) {
                if (!tu.isAssignable(args.get(i).asType(), annotClass) ||
                    args.get(i).getAnnotation(Attribute.class) == null) {
                    return false;
                }
            }

            return true;
        }
    }

    private static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }
}
