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
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * ReflectionComponentSpecification
 * ================================
 *
 * ReflectionComponentSpecification is an implementation that extracts the component specification from a
 * {@link Class} object using the reflection APIs defined by Java. This can only be used after the referenced
 * classes have been compiled and are capable of being loaded, e.g. the opposite scenario from
 * MirrorComponentSpecification.
 *
 * @author Michael Ludwig
 */
public class ReflectionComponentSpecification implements ComponentSpecification {
    private final Class<? extends Component> type;
    private final List<ReflectionPropertyDeclaration> properties;
    private final List<MethodDeclaration> methods;

    public ReflectionComponentSpecification(Class<? extends Component> type,
                                            List<? extends MethodPattern> patterns) {
        if (!Component.class.isAssignableFrom(type)) {
            throw fail(type, "Class must extend Component");
        }
        if (!type.isInterface()) {
            throw fail(type, "Component definition must be an interface");
        }

        List<ReflectionPropertyDeclaration> properties = new ArrayList<>();
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
            }
        }

        // since this is an interface, we're only dealing with public methods
        // so getMethods() returns everything we're interested in plus the methods
        // declared in Component, which we'll have to exclude
        Map<Method, MethodPattern> validMethods = new HashMap<>();
        Map<Method, List<String>> methodProperties = new HashMap<>();
        Map<String, ReflectionPropertyDeclaration> declaredProperties = new HashMap<>();

        for (Method m : type.getMethods()) {
            // exclude methods defined in Component, Owner, Ownable, and Object
            Class<?> declare = m.getDeclaringClass();
            if (declare.equals(Component.class) || declare.equals(Ownable.class) ||
                declare.equals(Owner.class) || declare.equals(Object.class)) {
                continue;
            }

            boolean matched = false;
            for (MethodPattern pattern : patterns) {
                if (pattern.matches(m)) {
                    validMethods.put(m, pattern);
                    matched = true;

                    // determine the set of properties defined by this method and merge it into component state
                    Map<String, Class<?>> methodDeclaredTypes = pattern.getDeclaredProperties(m);
                    for (Map.Entry<String, Class<?>> p : methodDeclaredTypes.entrySet()) {
                        if (p.getValue() != null) {
                            // this method explicitly declares a named property and its type
                            ReflectionPropertyDeclaration oldProp = declaredProperties.get(p.getKey());
                            if (oldProp != null) {
                                if (!oldProp.type.equals(p.getValue())) {
                                    throw fail(type,
                                               p.getKey() + " has inconsistent type across its methods");
                                }
                                // else type is consistent but some other method already created the prop declaration
                            } else {
                                // make a new property declaration
                                ReflectionPropertyDeclaration prop = new ReflectionPropertyDeclaration(p.getKey(),
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
                throw fail(declare, m.getName() + " is an unsupported property method");
            }
        }

        // make sure all declared properties have a known type
        for (Map.Entry<String, ReflectionPropertyDeclaration> p : declaredProperties.entrySet()) {
            if (p.getValue() == null) {
                throw fail(type, p.getKey() + " is referenced but a concrete type could not be determined");
            }
        }

        // update the list of property attribute annotations for all properties
        for (Map.Entry<Method, MethodPattern> m : validMethods.entrySet()) {
            for (String property : methodProperties.get(m.getKey())) {
                ReflectionPropertyDeclaration prop = declaredProperties.get(property);
                Set<Annotation> attrs = m.getValue()
                                         .getPropertyLevelAttributes(m.getKey(), property, interested);
                prop.addAttributes(attrs);
            }
        }

        // now all attributes are available on the property declaration, the property implementation can be chosen
        for (ReflectionPropertyDeclaration prop : declaredProperties.values()) {
            prop.updatePropertyImplementation(validMethods, methodProperties);
        }

        // compute method declarations based on these properties (now complete up to method declarations)
        for (Method m : validMethods.keySet()) {
            MethodPattern pattern = validMethods.get(m);
            List<Class<? extends Property>> propTypes = new ArrayList<>();
            List<ReflectionPropertyDeclaration> props = new ArrayList<>();
            for (String p : methodProperties.get(m)) {
                ReflectionPropertyDeclaration prop = declaredProperties.get(p);
                props.add(prop);
                propTypes.add(prop.propertyType);
            }

            MethodDeclaration method = pattern.createMethodDeclaration(m, propTypes, props);

            // add the method to every property
            for (ReflectionPropertyDeclaration prop : props) {
                prop.methods.add(method);
            }
            methods.add(method);
        }

        // order the list of properties by their natural ordering
        Collections.sort(properties);
        Collections.sort(methods);

        this.type = type;
        this.properties = Collections.unmodifiableList(properties);
        this.methods = Collections.unmodifiableList(methods);
    }

    @Override
    public String getType() {
        String canonicalName = type.getCanonicalName();
        String packageName = type.getPackage().getName();
        if (packageName.isEmpty()) {
            return canonicalName;
        } else {
            // strip off package
            return canonicalName.substring(getPackage().length() + 1);
        }
    }

    @Override
    public String getPackage() {
        return type.getPackage().getName();
    }

    @Override
    public List<? extends PropertyDeclaration> getProperties() {
        return properties;
    }

    @Override
    public List<? extends MethodDeclaration> getMethods() {
        return methods;
    }

    private static IllegalComponentDefinitionException fail(Class<?> cls, String msg) {
        return new IllegalComponentDefinitionException(cls.getCanonicalName(), msg);
    }

    /**
     * Implementation of PropertyDeclaration using the setter and getter methods available from reflection.
     */
    private static class ReflectionPropertyDeclaration implements PropertyDeclaration {
        private final String name;
        private PropertyFactory<?> factory;
        private Class<? extends Property> propertyType;

        private final Class<?> type;
        private final Set<Annotation> annotations;
        private final List<MethodDeclaration> methods;

        @SuppressWarnings("unchecked")
        private ReflectionPropertyDeclaration(String name, Class<?> type) {
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
            return type.getCanonicalName();
        }

        @Override
        public String getPropertyImplementation() {
            return propertyType.getCanonicalName();
        }

        @Override
        public Set<Annotation> getAttributes() {
            return annotations;
        }

        @Override
        public List<MethodDeclaration> getMethods() {
            return methods;
        }

        @Override
        public PropertyFactory<?> getPropertyFactory() {
            return factory;
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

        @SuppressWarnings("unchecked")
        private void updatePropertyImplementation(Map<Method, MethodPattern> methods,
                                                  Map<Method, List<String>> methodProperties) {
            Class<? extends PropertyFactory<?>> propFactoryType = null;

            for (Annotation a : annotations) {
                if (a instanceof com.lhkbob.entreri.attr.Factory) {
                    propFactoryType = ((com.lhkbob.entreri.attr.Factory) a).value();
                    break;
                }
            }

            if (propFactoryType == null) {
                // look up from the file mapping
                propFactoryType = TypePropertyMapping.getPropertyFactory(type);
            }

            if (propFactoryType != null) {
                try {
                    propertyType = (Class<? extends Property>) propFactoryType.getMethod("create")
                                                                              .getReturnType();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Cannot inspect PropertyFactory for create() method");
                }

                factory = createFactory(propFactoryType, methods, methodProperties);
            } else {
                throw new RuntimeException("Unable to determine PropertyFactory for property " + name);
            }
        }

        @SuppressWarnings("unchecked")
        private PropertyFactory<?> createFactory(Class<? extends PropertyFactory<?>> factoryType,
                                                 Map<Method, MethodPattern> methods,
                                                 Map<Method, List<String>> methodProperties) {
            Constructor<?> bestCtor = null;
            for (Constructor<?> ctor : factoryType.getDeclaredConstructors()) {
                if (isValidConstructor(ctor) && (bestCtor == null || ctor.getParameterTypes().length >
                                                                     bestCtor.getParameterTypes().length)) {
                    bestCtor = ctor;
                }
            }

            if (bestCtor == null) {
                throw new IllegalComponentDefinitionException("No suitable constructor for PropertyFactory (" +
                                                              factoryType + ") used by property " + name);
            }

            // collect all attribute types relevant to this constructor
            Set<Class<? extends Annotation>> interested = new HashSet<>();
            for (Class<?> arg : bestCtor.getParameterTypes()) {
                if (arg.isAnnotation()) {
                    // at this point the only annotation args will be attributes
                    interested.add((Class<? extends Annotation>) arg);
                }
            }
            // accumulate the actual attribute values on the property
            for (Map.Entry<Method, MethodPattern> m : methods.entrySet()) {
                if (methodProperties.get(m.getKey()).contains(name)) {
                    // this method produced this property so query its properties
                    Set<Annotation> attrs = m.getValue()
                                             .getPropertyLevelAttributes(m.getKey(), name, interested);
                    addAttributes(attrs);
                }
            }

            try {
                bestCtor.setAccessible(true);
                return (PropertyFactory<?>) bestCtor.newInstance(createArgumentValues(bestCtor));
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to create PropertyFactory via reflection", e);
            }
        }

        private Object[] createArgumentValues(Constructor<?> ctor) {
            Class<?>[] argTypes = ctor.getParameterTypes();
            Object[] args = new Object[argTypes.length];

            if (args.length == 0) {
                return args;
            }

            int startIndex = 0;
            if (argTypes[0].equals(Class.class)) {
                args[0] = type;
                startIndex = 1;
            }

            for (int i = startIndex; i < args.length; i++) {
                Annotation v = null;
                for (Annotation a : annotations) {
                    if (argTypes[i].isInstance(a)) {
                        v = a;
                        break;
                    }
                }

                args[i] = v;
            }

            return args;
        }

        private boolean isValidConstructor(Constructor<?> ctor) {
            Class<?>[] args = ctor.getParameterTypes();
            if (args.length == 0) {
                return true;
            }

            int startIndex = 0;
            if (args[0].equals(Class.class)) {
                startIndex = 1;
            }
            for (int i = startIndex; i < args.length; i++) {
                if (!args[i].isAnnotation() || args[i].getAnnotation(Attribute.class) == null) {
                    // one of the remaining arguments is not an attribute annotation
                    return false;
                }
            }
            return true;
        }
    }
}
