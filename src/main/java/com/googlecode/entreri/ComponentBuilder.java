/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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
package com.googlecode.entreri;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.entreri.property.AbstractPropertyFactory;
import com.googlecode.entreri.property.Factory;
import com.googlecode.entreri.property.Parameter;
import com.googlecode.entreri.property.Parameters;
import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;
import com.googlecode.entreri.property.Unmanaged;

/**
 * <p>
 * ComponentBuilder is a factory for creating new instances of Components of a
 * specific type. It is capable of using reflection to instantiate Property
 * instances for the Components declared property fields, based on the
 * {@link Factory}, {@link Parameters}, and {@link Parameter} annotations.
 * </p>
 * <p>
 * ComponentBuilder is thread safe, unlike the majority of the library, because
 * a single ComponentBuilder is used for each type, across all systems.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The built component type
 */
final class ComponentBuilder<T extends Component> {
    private final List<PropertyFactory<?>> propertyFactories;
    private final Constructor<T> constructor;
    private final List<Field> fields;

    /**
     * Create a new ComponentBuilder for the given type of Component. This is a
     * slower constructor with lots of reflection so builders should be cached.
     * This constructor also validates the component definition.
     * 
     * @param type The component type created by this builder
     * @throws IllegalArgumentException if the class is not really a component
     *             or if it is abstract
     * @throws IllegalComponentDefinitionException if the class hierarchy of the
     *             component type is invalid by breaking any of the constructor
     *             or field rules for defining a component
     */
    @SuppressWarnings("unchecked")
    public ComponentBuilder(Class<T> type) {
        // Now we actually have to build up a new TypedId - which is sort of slow
        if (!Component.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
        
        // Make sure we don't create TypedIds for abstract Component types 
        // (we don't want to try to allocate these)
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Component class type cannot be abstract: " + type);
        
        // Accumulate all property fields and validate type hierarchy
        fields = new ArrayList<Field>(getFields(type));
        
        Class<? super T> parent = type.getSuperclass();
        while(!Component.class.equals(parent)) {
            if (!Modifier.isAbstract(parent.getModifiers()))
                throw new IllegalComponentDefinitionException(type, "Parent class " + parent + " is not abstract");
            
            // this cast is safe since we're in the while loop
            fields.addAll(getFields((Class<? extends Component>) parent));
            parent = parent.getSuperclass();
        }
        
        constructor = getConstructor(type);
        propertyFactories = getPropertyFactories(fields);
    }

    /**
     * Get the map from Fields of the component type to PropertyFactory
     * implementations that will create valid property objects for each field.
     * The created properties can then form a map valid with
     * {@link #newInstance(EntitySystem, int, Map)}.
     * 
     * @return A map from field to property factory for the type associated with
     *         this builder
     */
    public Map<Field, PropertyFactory<?>> getPropertyFactories() {
        Map<Field, PropertyFactory<?>> props = new HashMap<Field, PropertyFactory<?>>();
        for (int i = 0; i < propertyFactories.size(); i++)
            props.put(fields.get(i), propertyFactories.get(i));
        return Collections.unmodifiableMap(props);
    }

    /**
     * <p>
     * Create a new instance of the Component type created by this builder, for
     * the given system. The component will use the given index initially, but
     * its {@link Component#init()} method is NOT called. The map of properties
     * is used to assign values to the declared property fields of the type.
     * </p>
     * <p>
     * It is assumed that the map was created by the factories returned from
     * {@link #getPropertyFactories()}. Additionally, it is assumed that
     * {@link PropertyFactory#setValue(Property, int)} is invoked by the caller
     * as appropriate (no initialization is performed by the builder).
     * </p>
     * 
     * @param system The owning EntitySystem
     * @param index The index of the new component in the system
     * @param properties The map of properties used to assign field values for
     *            the new component
     * @return A new component of type T
     * @throws RuntimeException if the properties weren't compatible with the
     *             list returned by createProperties()
     */
    public T newInstance(EntitySystem system, int index, Map<Field, Property> properties) {
        try {
            T t = constructor.newInstance(system, index);
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).set(t, properties.get(fields.get(i)));
            }
            
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new Component instance", e);
        }
    }
    
    private static <T extends Component> List<PropertyFactory<?>> getPropertyFactories(List<Field> fields) {
        List<PropertyFactory<?>> factories = new ArrayList<PropertyFactory<?>>();
        for (int i = 0; i < fields.size(); i++) {
            factories.add(createFactory(fields.get(i)));
        }
        return factories;
    }
    
    private static Object parseValue(Class<?> paramType, String paramValue, Class<? extends Component> forCType) {
        try {
            if (String.class.equals(paramType)) {
                return paramValue;
            } else if (Class.class.equals(paramType)) {
                return Class.forName(paramValue);
            } else if (int.class.equals(paramType) || Integer.class.equals(paramType)) {
                return Integer.parseInt(paramValue);
            } else if (float.class.equals(paramType) || Float.class.equals(paramType)) {
                return Float.parseFloat(paramValue);
            } else if (double.class.equals(paramType) || Double.class.equals(paramType)) {
                return Double.parseDouble(paramValue);
            } else if (long.class.equals(paramType) || Long.class.equals(paramType)) {
                return Long.parseLong(paramValue);
            } else if (short.class.equals(paramType) || Short.class.equals(paramType)) {
                return Short.parseShort(paramValue);
            } else if (byte.class.equals(paramType) || Byte.class.equals(paramType)) {
                return Byte.parseByte(paramValue);
            } else if (char.class.equals(paramType) || Character.class.equals(paramType)) {
                return paramValue.charAt(0);
            }
        } catch(Exception e) {
            throw new IllegalComponentDefinitionException(forCType, "Cannot convert parameter value, " + paramValue + ", to type: " + paramType);
        }
        
        throw new IllegalComponentDefinitionException(forCType, "Unsupported parameter value type: " + paramType + ", it must be a String, Class, or (boxed) primitive");
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static PropertyFactory<?> createFactory(Field field) {
        Class<? extends Property> type = (Class<? extends Property>) field.getType();
        Annotation[] annots = field.getAnnotations();
        Class<? extends Component> forCType = (Class<? extends Component>) field.getDeclaringClass();
        
        Parameter[] params = new Parameter[0];
        for (int i = 0; i < annots.length; i++) {
            if (annots[i] instanceof Parameters) {
                // take params array from the @Parameters annotation
                params = ((Parameters) annots[i]).value();
                break;
            } else if (annots[i] instanceof Parameter) {
                // take @Parameter annotation as single-arg constructor
                params = new Parameter[] { (Parameter) annots[i] };
                break;
            } else if (annots[i] instanceof Factory) {
                // use the declared PropertyFactory from the @Factory annotation
                Factory fa = (Factory) annots[i];
                
                // verify that the PropertyFactory actually creates the right type
                Method create;
                try {
                    create = fa.value().getMethod("create");
                } catch (Exception e) {
                    // should not happen
                    throw new RuntimeException("Unable to inspect PropertyFactory create() method", e);
                }
                
                if (!type.isAssignableFrom(create.getReturnType()))
                    throw new IllegalComponentDefinitionException(forCType, "@Factory for " + fa.value() + " creates incorrect Property type for property type: " + type);
                
                PropertyFactory<?> factory;
                try {
                    factory = fa.value().newInstance();
                } catch (Exception e) {
                    throw new IllegalComponentDefinitionException(forCType, "Cannot create PropertyFactory from @Factory annotation: " + fa.value());
                }
                
                return factory;
            } // else unknown annotation so ignore it
        }
        
        // At this point we need to be able to instantiate the Property with reflection
        // so make sure it's not an abstract type (below we'll make sure we have a ctor)
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalComponentDefinitionException(forCType, "Property cannot be instantiated because its type is abstract: " + field);
        
        Class<?>[] ctorTypes = new Class<?>[params.length];
        Object[] paramValues = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            ctorTypes[i] = params[i].type();
            paramValues[i] = parseValue(ctorTypes[i], params[i].value(), forCType);
        }
        
        try {
            Constructor<?> ctor = type.getDeclaredConstructor(ctorTypes);
            ctor.setAccessible(true); // just in case
            return new ReflectionPropertyFactory(ctor, paramValues);
        } catch(NoSuchMethodException e) {
            // parameterized constructor does not exist
            throw new IllegalComponentDefinitionException(forCType, "Property does not have a constructor matching: " + Arrays.toString(ctorTypes) + " for property " + field);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Component> Constructor<T> getConstructor(Class<T> type) {
        // This assumes that type is the concrete type, so it will fail if there
        // are multiple constructors or it's not private with the correct arguments
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length != 1)
            throw new IllegalComponentDefinitionException(type, "Component type must only define a single constructor");
        
        Constructor<T> ctor = (Constructor<T>) ctors[0];
        if (!Modifier.isPrivate(ctor.getModifiers()) && !Modifier.isProtected(ctor.getModifiers()))
            throw new IllegalComponentDefinitionException(type, "Component constructor must be private or protected");
        
        Class<?>[] args = ctor.getParameterTypes();
        if (args.length != 2 || !EntitySystem.class.equals(args[0]) || !int.class.equals(args[1]))
            throw new IllegalComponentDefinitionException(type, "Component constructor does not have proper signature of (ComponentIndex<T>, int, ...)");
        
        // Found it, now make it accessible (which might throw a SecurityException)
        ctor.setAccessible(true);
        return ctor;
    }
    
    private static List<Field> getFields(Class<? extends Component> type) {
        Field[] declared = type.getDeclaredFields();
        List<Field> nonTransientFields = new ArrayList<Field>(declared.length);

        for (int i = 0; i < declared.length; i++) {
            int modifiers = declared[i].getModifiers();
            if (Modifier.isStatic(modifiers))
                continue; // ignore static fields
            
            if (declared[i].isAnnotationPresent(Unmanaged.class))
                continue; // ignore the field
            
            if (!Property.class.isAssignableFrom(declared[i].getType())) {
                throw new IllegalComponentDefinitionException(type, "Component has non-Property field that is not transient: " + declared[i]);
            }
            
            if (!Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers))
                throw new IllegalComponentDefinitionException(type, "Field must be private or protected: " + declared[i]);
            
            nonTransientFields.add(declared[i]);
        }
        
        // Make sure all fields are accessible so we can assign them
        AccessibleObject[] access = new AccessibleObject[nonTransientFields.size()];
        for (int i = 0; i < access.length; i++)
            access[i] = nonTransientFields.get(i);
        Field.setAccessible(access, true);
        return nonTransientFields;
    }
    
    private static class ReflectionPropertyFactory<P extends Property> extends AbstractPropertyFactory<P> {
        private final Constructor<P> ctor;
        private final Object[] values;
        
        public ReflectionPropertyFactory(Constructor<P> ctor, Object[] values) {
            this.ctor = ctor;
            this.values = values;
        }
        
        @Override
        public P create() {
            try {
                return ctor.newInstance(values);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception when creating Property, with constructor " + ctor);
            }
        }
    }
}
