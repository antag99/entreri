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
 * PropertySpecification instances hold the required information to implement the accessor
 * and mutator methods for a single property declared in a component type. Instances are
 * not created directly, but are gotten by {@link #getSpecification(Class)} that analyzes
 * and validates a component type.
 *
 * @author Michael Ludwig
 */
@SuppressWarnings("unchecked")
public final class PropertySpecification implements Comparable<PropertySpecification> {
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
        isSharedInstance = getter.getAnnotation(SharedInstance.class) != null;

        propertyType = getCreatedType(
                (Class<? extends PropertyFactory<?>>) factory.getClass());
    }

    /**
     * @return The class type of returned by the getter, required by the setter, and
     *         effective data type of the property.
     */
    public Class<?> getType() {
        return getter.getReturnType();
    }

    /**
     * @return The property implementation class that will store the property data, and is
     *         the type created by the associated property factory
     */
    public Class<? extends Property> getPropertyType() {
        return propertyType;
    }

    /**
     * @return True if the property uses an internal shared instance
     */
    public boolean isSharedInstance() {
        return isSharedInstance;
    }

    /**
     * @return The name of the property
     */
    public String getName() {
        return name;
    }

    /**
     * @return The property factory instance that can create properties for systems that
     *         use this component type
     */
    public PropertyFactory<?> getFactory() {
        return factory;
    }

    /**
     * @return The setter method that mutates the property
     */
    public Method getSetterMethod() {
        return setter;
    }

    /**
     * @return The specific parameter index of the setter's arguments that has the
     *         property's value (will always be 0 for a single-argument setter)
     */
    public int getSetterParameter() {
        return setterParameter;
    }

    /**
     * @return The getter method that accesses the property
     */
    public Method getGetterMethod() {
        return getter;
    }

    @Override
    public int compareTo(PropertySpecification o) {
        return name.compareTo(o.name);
    }

    /**
     * Analyze the given component class and return the property specification. This will
     * validate the class as well and throw an exception if it violates any of the rules
     * specified in {@link com.lhkbob.entreri.Component}. Each PropertySpecification
     * instance in the returned list will have a unique name. The list will be ordered by
     * PropertySpecification's natural ordering and is immutable.
     *
     * @param componentDefinition The component type to analyze
     *
     * @return The ordered set of properties declared in the component
     *
     * @throws com.lhkbob.entreri.IllegalComponentDefinitionException
     *          if the component class or any referenced PropertyFactories or Properties
     *          are invalid
     */
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
                                                     createFactory(getters.get(property)),
                                                     getter, setter, param));
        }

        if (!setters.isEmpty()) {
            throw new IllegalComponentDefinitionException(componentDefinition,
                                                          "No getter for properties: " +
                                                          setters.keySet());
        }

        // order the list of properties by their natural ordering
        Collections.sort(properties);
        return Collections.unmodifiableList(properties);
    }

    /**
     * Validate the method, which is assumed to be a bean mutator method starting with
     * 'set'. . If the method is valid, it will be placed in the <var>setters</var> map by
     * the determined name of the property. The method parameter is placed into
     * <var>parameters</var> with the same key.
     *
     * @param m          The getter method
     * @param setters    The name to mutator map that will be updated
     * @param parameters The name to index map to be updated
     *
     * @throws IllegalComponentDefinitionException
     *          if the method is invalid
     */
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

    /**
     * Validate the method, which is assumed to be a bean accessor method starting with
     * 'get', 'is', or 'has'. The correct prefix must be passed in through
     * <var>prefix</var>. If the method is valid, it will be placed in the
     * <var>getters</var> map by the determined name of the property.
     *
     * @param m       The getter method
     * @param prefix  The prefix of the getter method
     * @param getters The name to accessor map that will be updated
     *
     * @throws IllegalComponentDefinitionException
     *          if the method is invalid
     */
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

    /**
     * Extract the property name from a {@link Named} annotation applied to the
     * <var>p</var>th parameter. Null is returned if the parameter has not been annotated.
     * This is intended for setter methods.
     *
     * @param m The method to inspect
     * @param p The specific parameter whose name should be returned
     *
     * @return The name if it exists
     */
    private static String getNameFromParameter(Method m, int p) {
        Annotation[] annots = m.getParameterAnnotations()[p];
        for (int i = 0; i < annots.length; i++) {
            if (annots[i] instanceof Named) {
                return ((Named) annots[i]).value();
            }
        }
        return null;
    }

    /**
     * Extract the bean name from the method given the prefix, which is assumed to be
     * 'set', 'get', 'is', or 'has'. If the method is annotated with {@link Named}, that
     * overrides the bean name.
     *
     * @param m      The bean method
     * @param prefix The prefix the method name starts with
     *
     * @return The name for the property corresponding to the method
     */
    private static String getName(Method m, String prefix) {
        Named n = m.getAnnotation(Named.class);
        if (n != null) {
            return n.value();
        } else {
            return Character.toLowerCase(m.getName().charAt(prefix.length())) +
                   m.getName().substring(prefix.length() + 1);
        }
    }

    /**
     * Look up the Property class created by the factory class by inspecting its
     * <code>create()</code> method.
     *
     * @param factory The property factory to inspect
     *
     * @return The Property type the factory creates
     */
    private static Class<? extends Property> getCreatedType(
            Class<? extends PropertyFactory<?>> factory) {
        try {
            return (Class<? extends Property>) factory.getMethod("create")
                                                      .getReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot inspect property factory " + factory, e);
        }
    }

    /**
     * Determine and instantiate a PropertyFactory that is capable of handling the data
     * type of the getter method. The getter method is accessor bean method of one of the
     * high-level properties defined in the component type.
     * <p/>
     * If the getter is annotated with {@link Factory}, that factory is used. Otherwise
     * the return type is used with {@link TypePropertyMapping} to find the appropriate
     * Property class. The {@link Factory} annotation on that Property is then used for
     * the factory type.
     * <p/>
     * Validation is performed to ensure the factory creates properties of the expected
     * type, and the property exposes the methods expected by the proxy code generation.
     *
     * @param getter The getter of the property
     *
     * @return The constructed property factory
     *
     * @throws IllegalComponentDefinitionException
     *          if the property factory or getter has an invalid configuration
     */
    private static PropertyFactory<?> createFactory(Method getter) {
        Class<?> baseType = getter.getReturnType();
        Class<? extends Component> cType = (Class<? extends Component>) getter
                .getDeclaringClass();

        Class<? extends PropertyFactory<?>> factoryType;
        if (getter.getAnnotation(Factory.class) != null) {
            // prefer getter specification to allow default overriding
            factoryType = getter.getAnnotation(Factory.class).value();
            validateFactory(getter, factoryType, null);
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
            throw new IllegalComponentDefinitionException(cType,
                                                          "Unable to create PropertyFactory for " +
                                                          getter.getName());
        } else {
            return factory;
        }
    }

    /**
     * Validate the various aspects of the property to make sure they are consistent with
     * the getter method. The factory's create method is checked to make sure it returns
     * instances assignable to <var>propertyType</var>. The property type is checked for
     * the required <code>T get(int)</code> and <code>void set(T, int)</code> methods. If
     * the property should be shared, then <code>void get(int, T)</code> is checked as
     * well.
     *
     * @param getter       The getter method of the property being analyzed
     * @param factory      The factory type to use with the getter
     * @param propertyType The property type to use with the getter
     *
     * @throws IllegalComponentDefinitionException
     *          if the factory or property are invalid given the method and whether or not
     *          it's shared
     */
    private static void validateFactory(Method getter,
                                        Class<? extends PropertyFactory<?>> factory,
                                        Class<? extends Property> propertyType) {
        boolean isShared = getter.getAnnotation(SharedInstance.class) != null;
        Class<?> baseType = getter.getReturnType();
        Class<? extends Property> createdType = getCreatedType(factory);
        Class<? extends Component> forType = (Class<? extends Component>) getter
                .getDeclaringClass();

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

    /**
     * Invoke the constructor defined by <var>type</var> that has arguments identical to
     * the corresponding classes of <var>args</var>. The constructor will be invoked with
     * those arguments if it exists, otherwise null is returned.
     *
     * @param type The property factory class to construct
     * @param args The arguments to pass to the matching constructor
     *
     * @return The created instance
     *
     * @throws RuntimeException if an unexpected exception occurs
     */
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
