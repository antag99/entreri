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
package com.lhkbob.entreri;

import com.lhkbob.entreri.property.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 *
 */
@SuppressWarnings("unchecked")
final class PropertySpecification implements Comparable<PropertySpecification> {
    private final String name;
    private final PropertyFactory<?> factory;

    private final Method setter;
    private final int setterParameter;

    private final Method getter;
    private final boolean isSharedInstance;

    private final Class<? extends Property> propertyType;

    private PropertySpecification(String name, PropertyFactory<?> factory, Method getter,
                                  Method setter, int setterParameter) {
        this.name = name;
        this.factory = factory;
        this.getter = getter;
        this.setter = setter;
        this.setterParameter = setterParameter;
        isSharedInstance = isShared(getter);

        propertyType = getCreatedType(
                (Class<? extends PropertyFactory<?>>) factory.getClass());
    }

    public Class<?> getType() {
        return getter.getReturnType();
    }

    public Class<? extends Property> getPropertyType() {
        return propertyType;
    }

    public boolean isSharedInstance() {
        return isSharedInstance;
    }

    public String getName() {
        return name;
    }

    public PropertyFactory<?> getFactory() {
        return factory;
    }

    public Method getSetterMethod() {
        return setter;
    }

    public int getSetterParameter() {
        return setterParameter;
    }

    public Method getGetterMethod() {
        return getter;
    }

    @Override
    public int compareTo(PropertySpecification o) {
        return name.compareTo(o.name);
    }

    public static List<PropertySpecification> getSpecification(
            Class<? extends Component> componentDefinition) {
        if (!Component.class.isAssignableFrom(componentDefinition)) {
            throw new IllegalArgumentException("Class must extend Component");
        }
        if (!componentDefinition.isInterface()) {
            throw new IllegalComponentDefinitionException(componentDefinition,
                                                          "Component definition must be an interface");
        }

        List<PropertySpecification> properties = new ArrayList<PropertySpecification>();

        // since this is an interface, we're only dealing with public methods
        // so getMethods() returns everything we're interested in plus the methods
        // declared in Component, which we'll have to exclude
        Method[] methods = componentDefinition.getMethods();
        Map<String, Method> getters = new HashMap<String, Method>();
        Map<String, Method> setters = new HashMap<String, Method>();
        Map<String, Integer> setterParameters = new HashMap<String, Integer>();

        for (int i = 0; i < methods.length; i++) {
            // exclude methods defined in Component, Owner, and Ownable
            Class<?> md = methods[i].getDeclaringClass();
            if (md.equals(Component.class) || md.equals(Owner.class) ||
                md.equals(Ownable.class)) {
                continue;
            }

            if (!Component.class.isAssignableFrom(methods[i].getDeclaringClass())) {
                throw new IllegalComponentDefinitionException(componentDefinition,
                                                              "Method is defined in non-Component interface: " +
                                                              md);
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
                throw new IllegalComponentDefinitionException(
                        (Class<? extends Component>) md,
                        "Illegal property method: " + methods[i]);
            }
        }

        for (String property : getters.keySet()) {
            Method getter = getters.get(property);
            Method setter = setters.remove(property);
            Integer param = setterParameters.remove(property);

            if (setter == null) {
                throw new IllegalComponentDefinitionException(componentDefinition,
                                                              "No setter for property: " +
                                                              property);
            } else if (!setter.getParameterTypes()[param]
                    .equals(getter.getReturnType())) {
                throw new IllegalComponentDefinitionException(componentDefinition,
                                                              "Mismatched type between getter and setter for: " +
                                                              property);
            }

            properties.add(new PropertySpecification(property,
                                                     createFactory(getters.get(property),
                                                                   property), getter,
                                                     setter, param));
        }

        if (!setters.isEmpty()) {
            throw new IllegalComponentDefinitionException(componentDefinition,
                                                          "No getter for properties: " +
                                                          setters.keySet());
        }

        Collections.sort(properties);
        return Collections.unmodifiableList(properties);
    }

    private static void processSetter(Method m, Map<String, Method> setters,
                                      Map<String, Integer> parameters) {
        Class<? extends Component> cType = (Class<? extends Component>) m
                .getDeclaringClass();
        if (!m.getReturnType().equals(m.getDeclaringClass()) &&
            !m.getReturnType().equals(void.class)) {
            throw new IllegalComponentDefinitionException(cType,
                                                          "Setter method must have a return type equal to its declaring class, or return void: " +
                                                          m.getName());
        }
        if (m.getParameterTypes().length == 0) {
            throw new IllegalComponentDefinitionException(cType,
                                                          "Setter method must have at least 1 parameter: " +
                                                          m.getName());
        }

        if (m.getParameterTypes().length == 1) {
            String name = getNameFromParameter(m, 0);
            if (name != null) {
                // verify absence of @Named on actual setter
                if (m.getAnnotation(Named.class) != null) {
                    throw new IllegalComponentDefinitionException(cType,
                                                                  "@Named cannot be on both parameter and method: " +
                                                                  m.getName());
                }
            } else {
                name = getName(m, "set");
            }

            if (setters.containsKey(name)) {
                throw new IllegalComponentDefinitionException(cType,
                                                              "Property name collision: " +
                                                              name);
            }
            setters.put(name, m);
            parameters.put(name, 0);
        } else {
            // verify absence of @Named on actual setter
            if (m.getAnnotation(Named.class) != null) {
                throw new IllegalComponentDefinitionException(cType,
                                                              "@Named cannot be applied to setter method with multiple parameters: " +
                                                              m.getName());
            }

            int numP = m.getGenericParameterTypes().length;
            for (int i = 0; i < numP; i++) {
                String name = getNameFromParameter(m, i);
                if (name == null) {
                    throw new IllegalComponentDefinitionException(cType,
                                                                  "@Named must be applied to each parameter for setter methods with multiple parameters: " +
                                                                  m.getName());
                }

                if (setters.containsKey(name)) {
                    throw new IllegalComponentDefinitionException(cType,
                                                                  "Property name collision: " +
                                                                  name);
                }

                setters.put(name, m);
                parameters.put(name, i);
            }
        }
    }

    private static void processGetter(Method m, String prefix,
                                      Map<String, Method> getters) {
        Class<? extends Component> cType = (Class<? extends Component>) m
                .getDeclaringClass();
        String name = getName(m, prefix);
        if (getters.containsKey(name)) {
            throw new IllegalComponentDefinitionException(cType,
                                                          "Property name collision: " +
                                                          name);
        }
        if (m.getParameterTypes().length != 0) {
            throw new IllegalComponentDefinitionException(cType,
                                                          "Getter method cannot take arguments: " +
                                                          m.getName());
        }
        if (m.getReturnType().equals(void.class)) {
            throw new IllegalComponentDefinitionException(cType,
                                                          "Getter method must have a non-void return type: " +
                                                          m.getName());
        }

        getters.put(name, m);
    }

    private static boolean isShared(Method getter) {
        Annotation[] annots = getter.getAnnotations();
        for (int i = 0; i < annots.length; i++) {
            if (annots[i] instanceof SharedInstance) {
                return true;
            }
        }
        return false;
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

    private static Class<? extends Property> getCreatedType(
            Class<? extends PropertyFactory<?>> factory) {
        try {
            return (Class<? extends Property>) factory.getMethod("create")
                                                      .getReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot inspect property factory " + factory, e);
        }
    }

    private static void validateFactory(Method getter, boolean isShared,
                                        Class<? extends PropertyFactory<?>> factory,
                                        Class<? extends Property> propertyType,
                                        Class<? extends Component> forType) {
        Class<?> baseType = getter.getReturnType();
        Class<? extends Property> createdType = getCreatedType(factory);

        if (propertyType == null) {
            // rely on factory to determine property type
            propertyType = createdType;
        } else {
            // make sure factory returns an assignable type
            if (!propertyType.isAssignableFrom(createdType)) {
                throw new RuntimeException("Factory creates " + createdType +
                                           ", which is incompatible with expected type " +
                                           propertyType);
            }
        }

        // verify contract of property
        if (propertyType.equals(ObjectProperty.class)) {
            // special case for ObjectProperty to support more permissive assignments
            // (which to record requires a similar special case in the code generation)
            if (isShared) {
                throw new IllegalComponentDefinitionException(forType,
                                                              "Property cannot be used with @SharedInstance: " +
                                                              propertyType);
            } else if (baseType.isPrimitive()) {
                throw new IllegalComponentDefinitionException(forType,
                                                              "ObjectProperty cannot be used with primitive types");
            }
            // else we know ObjectProperty is defined correctly because its part of the core library
        } else {
            try {
                Method g = propertyType.getMethod("get", int.class);
                if (!g.getReturnType().equals(baseType)) {
                    throw new IllegalComponentDefinitionException(forType, "Property " +
                                                                           propertyType +
                                                                           " does not implement required get() method for type " +
                                                                           baseType);
                }
                // FIXME switch back to int, type method but then we have to update all the property defs
                Method s = propertyType.getMethod("set", baseType, int.class);
                if (!s.getReturnType().equals(void.class)) {
                    throw new IllegalComponentDefinitionException(forType, "Property " +
                                                                           propertyType +
                                                                           " does not implement required set() method for type " +
                                                                           baseType);
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentDefinitionException(forType, "Property " +
                                                                       propertyType +
                                                                       " does not implement required get() or set() method for type " +
                                                                       baseType);
            }

            if (isShared) {
                if (!ShareableProperty.class.isAssignableFrom(propertyType)) {
                    throw new IllegalComponentDefinitionException(forType, "Property " +
                                                                           propertyType +
                                                                           " cannot be used with @SharedInstance: " +
                                                                           propertyType);
                }

                // verify additional shareable property contract
                try {
                    Method sg = propertyType.getMethod("get", int.class, baseType);
                    if (!sg.getReturnType().equals(void.class)) {
                        throw new IllegalComponentDefinitionException(forType,
                                                                      "Property " +
                                                                      propertyType +
                                                                      " does not implement shareable get() for " +
                                                                      baseType);
                    }
                    Method creator = propertyType.getMethod("createShareableInstance");
                    if (!creator.getReturnType().equals(baseType)) {
                        throw new IllegalComponentDefinitionException(forType,
                                                                      "Property " +
                                                                      propertyType +
                                                                      " does not implement createShareableInstance() for " +
                                                                      baseType);
                    }
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(propertyType +
                                               " does not implement required shareable methods for type " +
                                               baseType, e);
                }
            }
        }
    }

    private static PropertyFactory<?> createFactory(Method getter, String name) {
        boolean isShared = isShared(getter);
        Class<?> baseType = getter.getReturnType();
        Class<? extends Component> cType = (Class<? extends Component>) getter
                .getDeclaringClass();

        Class<? extends PropertyFactory<?>> factoryType;
        if (getter.getAnnotation(Factory.class) != null) {
            // prefer getter specification to allow default overriding
            factoryType = getter.getAnnotation(Factory.class).value();
            validateFactory(getter, isShared, factoryType, null, cType);
        } else {
            // try to find a default property type
            Class<? extends Property> mappedType = TypePropertyMapping
                    .getPropertyForType(baseType);
            if (mappedType.getAnnotation(Factory.class) == null) {
                throw new IllegalComponentDefinitionException(cType,
                                                              "Cannot create PropertyFactory for " +
                                                              mappedType +
                                                              ", no @Factory annotation on type");
            } else {
                factoryType = mappedType.getAnnotation(Factory.class).value();
                validateFactory(getter, isShared, factoryType, mappedType, cType);
            }
        }

        PropertyFactory<?> factory = invokeConstructor(factoryType, new Attributes(
                getter.getAnnotations()));
        if (factory == null) {
            factory = invokeConstructor(factoryType);
        }

        if (factory == null) {
            // unable to create a PropertyFactory
            throw new IllegalComponentDefinitionException(cType,
                                                          "Unable to create PropertyFactory for " +
                                                          name);
        } else {
            return factory;
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
