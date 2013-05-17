/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
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

import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 *
 */
public class ReflectionComponentSpecification implements ComponentSpecification {
    private final Class<? extends Component> type;
    private final List<ReflectionPropertyDeclaration> properties;

    public ReflectionComponentSpecification(Class<? extends Component> type) {
        if (!Component.class.isAssignableFrom(type)) {
            throw fail(type, "Class must extend Component");
        }
        if (!type.isInterface()) {
            throw fail(type, "Component definition must be an interface");
        }

        List<ReflectionPropertyDeclaration> properties = new ArrayList<>();

        // since this is an interface, we're only dealing with public methods
        // so getMethods() returns everything we're interested in plus the methods
        // declared in Component, which we'll have to exclude
        Method[] methods = type.getMethods();
        Map<String, Method> getters = new HashMap<>();
        Map<String, Method> setters = new HashMap<>();
        Map<String, Integer> setterParameters = new HashMap<>();

        for (int i = 0; i < methods.length; i++) {
            // exclude methods defined in Component, Owner, Ownable, and Object
            Class<?> md = methods[i].getDeclaringClass();
            if (md.equals(Component.class) || md.equals(Owner.class) ||
                md.equals(Ownable.class) || md.equals(Object.class)) {
                continue;
            }

            if (!Component.class.isAssignableFrom(methods[i].getDeclaringClass())) {
                throw fail(md, methods[i] + ", method is not declared by a component");
            }

            if (methods[i].getName().startsWith("is")) {
                processGetter(methods[i], "is", getters);
            } else if (methods[i].getName().startsWith("has")) {
                processGetter(methods[i], "has", getters);
            } else if (methods[i].getName().startsWith("get")) {
                processGetter(methods[i], "get", getters);
            } else if (methods[i].getName().startsWith("set")) {
                processSetter(methods[i], setters, setterParameters);
            } else {
                throw fail(md, methods[i] + " is an illegal property method");
            }
        }

        for (String property : getters.keySet()) {
            Method getter = getters.get(property);
            Method setter = setters.remove(property);
            Integer param = setterParameters.remove(property);

            if (setter == null) {
                throw fail(type, property + " has no matching setter");
            } else if (!setter.getParameterTypes()[param]
                    .equals(getter.getReturnType())) {
                throw fail(type, property + " has inconsistent type");
            }

            properties.add(new ReflectionPropertyDeclaration(property,
                                                             createFactory(getter),
                                                             getter, setter, param));
        }

        if (!setters.isEmpty()) {
            throw fail(type, setters.keySet() + " have no matching getters");
        }

        // order the list of properties by their natural ordering
        Collections.sort(properties);
        this.type = type;
        this.properties = Collections.unmodifiableList(properties);
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

    private static IllegalComponentDefinitionException fail(Class<?> cls, String msg) {
        return new IllegalComponentDefinitionException(cls.getCanonicalName(), msg);
    }

    /**
     * Implementation of PropertyDeclaration using the setter and getter methods available
     * from reflection.
     */
    private static class ReflectionPropertyDeclaration implements PropertyDeclaration {
        private final String name;
        private final PropertyFactory<?> factory;

        private final Method setter;
        private final int setterParameter;

        private final Method getter;
        private final boolean isSharedInstance;

        private final Class<? extends Property> propertyType;

        @SuppressWarnings("unchecked")
        private ReflectionPropertyDeclaration(String name, PropertyFactory<?> factory,
                                              Method getter, Method setter,
                                              int setterParameter) {
            this.name = name;
            this.factory = factory;
            this.getter = getter;
            this.setter = setter;
            this.setterParameter = setterParameter;
            isSharedInstance = getter.getAnnotation(SharedInstance.class) != null;

            propertyType = getCreatedType(
                    (Class<? extends PropertyFactory<?>>) factory.getClass());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return getter.getReturnType().getCanonicalName();
        }

        @Override
        public String getPropertyImplementation() {
            return propertyType.getCanonicalName();
        }

        @Override
        public String getSetterMethod() {
            return setter.getName();
        }

        @Override
        public String getGetterMethod() {
            return getter.getName();
        }

        @Override
        public int getSetterParameter() {
            return setterParameter;
        }

        @Override
        public boolean getSetterReturnsComponent() {
            return !setter.getReturnType().equals(void.class);
        }

        @Override
        public boolean isShared() {
            return isSharedInstance;
        }

        @Override
        public PropertyFactory<?> getPropertyFactory() {
            return factory;
        }

        @Override
        public int compareTo(PropertyDeclaration o) {
            return name.compareTo(o.getName());
        }
    }

    private static void processSetter(Method m, Map<String, Method> setters,
                                      Map<String, Integer> parameters) {
        if (!m.getReturnType().equals(m.getDeclaringClass()) &&
            !m.getReturnType().equals(void.class)) {
            throw fail(m.getDeclaringClass(), m + " has invalid return type for setter");
        }
        if (m.getParameterTypes().length == 0) {
            throw fail(m.getDeclaringClass(), m + " must have at least one parameter");
        }

        if (m.getParameterTypes().length == 1) {
            String name = getNameFromParameter(m, 0);
            if (name != null) {
                // verify absence of @Named on actual setter
                if (m.getAnnotation(Named.class) != null) {
                    throw fail(m.getDeclaringClass(),
                               m + ", @Named cannot be on both parameter and method");
                }
            } else {
                name = getName(m, "set");
            }

            if (setters.containsKey(name)) {
                throw fail(m.getDeclaringClass(), name + " already declared on a setter");
            }
            setters.put(name, m);
            parameters.put(name, 0);
        } else {
            // verify absence of @Named on actual setter
            if (m.getAnnotation(Named.class) != null) {
                throw fail(m.getDeclaringClass(), m +
                                                  ", @Named cannot be applied to setter method with multiple parameters");
            }

            int numP = m.getParameterTypes().length;
            for (int i = 0; i < numP; i++) {
                String name = getNameFromParameter(m, i);
                if (name == null) {
                    throw fail(m.getDeclaringClass(), m +
                                                      ", @Named must be applied to each parameter for multi-parameter setter methods");
                }

                if (setters.containsKey(name)) {
                    throw fail(m.getDeclaringClass(),
                               name + " already declared on a setter");
                }

                setters.put(name, m);
                parameters.put(name, i);
            }
        }
    }

    private static void processGetter(Method m, String prefix,
                                      Map<String, Method> getters) {
        String name = getName(m, prefix);
        if (getters.containsKey(name)) {
            throw fail(m.getDeclaringClass(), name + " already declared on a getter");
        }
        if (m.getParameterTypes().length != 0) {
            throw fail(m.getDeclaringClass(), m + ", getter must not take arguments");
        }
        if (m.getReturnType().equals(void.class)) {
            throw fail(m.getDeclaringClass(),
                       m + ", getter must have non-void return type");
        }

        getters.put(name, m);
    }

    private static String getNameFromParameter(Method m, int p) {
        Annotation[] annots = m.getParameterAnnotations()[p];
        for (int i = 0; i < annots.length; i++) {
            if (annots[i] instanceof Named) {
                return ((Named) annots[i]).value();
            }
        }
        return null;
    }

    private static String getName(Method m, String prefix) {
        Named n = m.getAnnotation(Named.class);
        if (n != null) {
            return n.value();
        } else {
            return Character.toLowerCase(m.getName().charAt(prefix.length())) +
                   m.getName().substring(prefix.length() + 1);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Property> getCreatedType(
            Class<? extends PropertyFactory<?>> factory) {
        try {
            return (Class<? extends Property>) factory.getMethod("create")
                                                      .getReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot inspect property factory " + factory, e);
        }
    }

    private static PropertyFactory<?> createFactory(Method getter) {
        Class<?> baseType = getter.getReturnType();

        Class<? extends PropertyFactory<?>> factoryType;
        if (getter.getAnnotation(com.lhkbob.entreri.property.Factory.class) != null) {
            // prefer getter specification to allow default overriding
            factoryType = getter.getAnnotation(com.lhkbob.entreri.property.Factory.class)
                                .value();
            validateFactory(getter, factoryType, null);
        } else {
            // try to find a default property type
            Class<? extends Property> mappedType = TypePropertyMapping
                    .getPropertyForType(baseType);
            if (mappedType.getAnnotation(com.lhkbob.entreri.property.Factory.class) ==
                null) {
                throw fail(getter.getDeclaringClass(),
                           mappedType + " has no @Factory annotation");
            } else {
                factoryType = mappedType
                        .getAnnotation(com.lhkbob.entreri.property.Factory.class).value();
                validateFactory(getter, factoryType, mappedType);
            }
        }

        PropertyFactory<?> factory = invokeConstructor(factoryType, new Attributes(
                getter.getAnnotations()));
        if (factory == null) {
            factory = invokeConstructor(factoryType);
        }

        if (factory == null) {
            // unable to create a PropertyFactory
            throw fail(getter.getDeclaringClass(),
                       "Cannot create PropertyFactory for " + getter);
        } else {
            return factory;
        }
    }

    private static void validateFactory(Method getter,
                                        Class<? extends PropertyFactory<?>> factory,
                                        Class<? extends Property> propertyType) {
        boolean isShared = getter.getAnnotation(SharedInstance.class) != null;
        Class<?> baseType = getter.getReturnType();
        Class<? extends Property> createdType = getCreatedType(factory);

        if (propertyType == null) {
            // rely on factory to determine property type
            propertyType = createdType;
        } else {
            // make sure factory returns an assignable type
            if (!propertyType.isAssignableFrom(createdType)) {
                throw fail(getter.getDeclaringClass(), "Factory creates " + createdType +
                                                       ", which is incompatible with expected type " +
                                                       propertyType);
            }
        }

        // verify contract of property
        if (propertyType.equals(ObjectProperty.class)) {
            // special case for ObjectProperty to support more permissive assignments
            // (which to record requires a similar special case in the code generation)
            if (isShared) {
                throw fail(getter.getDeclaringClass(),
                           propertyType + " can't be used with @SharedInstance");
            } else if (baseType.isPrimitive()) {
                throw fail(getter.getDeclaringClass(),
                           "ObjectProperty cannot be used with primitive types");
            }
            // else we know ObjectProperty is defined correctly because its part of the core library
        } else {
            try {
                Method g = propertyType.getMethod("get", int.class);
                if (!g.getReturnType().equals(baseType)) {
                    throw fail(getter.getDeclaringClass(),
                               propertyType + " does not implement " + baseType +
                               " get()");
                }
                // FIXME switch back to int, type method but then we have to update all the property defs
                Method s = propertyType.getMethod("set", baseType, int.class);
                if (!s.getReturnType().equals(void.class)) {
                    throw fail(getter.getDeclaringClass(),
                               propertyType + " does not implement void set(" + baseType +
                               ", int)");
                }
            } catch (NoSuchMethodException e) {
                throw fail(getter.getDeclaringClass(),
                           propertyType + " does not implement " + baseType +
                           " get() or void set(" + baseType + ", int)");
            }

            if (isShared) {
                if (!ShareableProperty.class.isAssignableFrom(propertyType)) {
                    throw fail(getter.getDeclaringClass(),
                               propertyType + " can't be used with @SharedInstance");
                }

                // verify additional shareable property contract
                try {
                    Method sg = propertyType.getMethod("get", int.class, baseType);
                    if (!sg.getReturnType().equals(void.class)) {
                        throw fail(getter.getDeclaringClass(),
                                   propertyType + " does not implement void get(int, " +
                                   baseType + ")");
                    }
                    Method creator = propertyType.getMethod("createShareableInstance");
                    if (!creator.getReturnType().equals(baseType)) {
                        throw fail(getter.getDeclaringClass(),
                                   propertyType + " does not implement " + baseType +
                                   " createShareableInstance()");
                    }
                } catch (NoSuchMethodException e) {
                    throw fail(getter.getDeclaringClass(),
                               propertyType + " does not implement void get(int, " +
                               baseType + ") or " + baseType +
                               " createShareableInstance()");
                }
            }
        }
    }

    private static PropertyFactory<?> invokeConstructor(
            Class<? extends PropertyFactory<?>> type, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }

        try {
            // must use getDeclaredConstructor in case the class type is private
            // or the constructor is not public
            Constructor<?> ctor = type.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return (PropertyFactory<?>) ctor.newInstance(args);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to inspect factory's constructor", e);
        } catch (NoSuchMethodException e) {
            // ignore, fall back to default constructor
            return null;
        } catch (Exception e) {
            // other exceptions should not occur
            throw new RuntimeException("Unexpected exception during factory creation", e);
        }
    }
}
