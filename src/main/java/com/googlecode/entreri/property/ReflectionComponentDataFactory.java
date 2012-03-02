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
package com.googlecode.entreri.property;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.ComponentDataFactory;
import com.googlecode.entreri.IllegalComponentDefinitionException;
import com.googlecode.entreri.annot.DefaultValue;
import com.googlecode.entreri.annot.ElementSize;
import com.googlecode.entreri.annot.Factory;
import com.googlecode.entreri.annot.Unmanaged;

/**
 * <p>
 * ReflectionComponentDataFactory is a factory for creating new instances of ComponentDatas of a
 * specific type. 
 * FIXME: Move conventions from TypeId.getTypeId() to here
 * 
 * @author Michael Ludwig
 * @param <T> The built component type
 */
public final class ReflectionComponentDataFactory<T extends ComponentData<T>> implements ComponentDataFactory<T> {
    private final Constructor<T> constructor;
    private final Map<String, PropertyFactory<?>> propertyFactories;
    private final Map<String, Field> fields;

    /**
     * Create a new ReflectionComponentDataFactory for the given type of
     * ComponentData. This is a slower constructor with lots of reflection so
     * builders should be cached. This will throw an exception if the
     * ComponentData type does meet the conventions required for this type of
     * factory.
     * 
     * @param type The component type created by this builder
     * @throws IllegalArgumentException if the class is not really a component
     *             or if it is abstract
     * @throws IllegalComponentDefinitionException if the class hierarchy of the
     *             component type is invalid by breaking any of the constructor
     *             or field rules for defining a component
     */
    @SuppressWarnings("unchecked")
    public ReflectionComponentDataFactory(Class<T> type) {
        // Now we actually have to build up a new TypeId - which is sort of slow
        if (!ComponentData.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of ComponentData: " + type);
        
        // Make sure we don't create TypedIds for abstract ComponentData types 
        // (we don't want to try to allocate these)
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("ComponentData class type cannot be abstract: " + type);
        
        // Accumulate all property fields and validate type hierarchy
        Map<String, Field> fields = new HashMap<String, Field>(getFields(type));
        
        Class<? super T> parent = type.getSuperclass();
        while(!ComponentData.class.equals(parent)) {
            if (!Modifier.isAbstract(parent.getModifiers()))
                throw new IllegalComponentDefinitionException(type, "Parent class " + parent + " is not abstract");
            
            // this cast is safe since we're in the while loop
            fields.putAll(getFields((Class<? extends ComponentData<?>>) parent));
            parent = parent.getSuperclass();
        }
        
        constructor = getConstructor(type);
        propertyFactories = Collections.unmodifiableMap(getPropertyFactories(fields));
        this.fields = Collections.unmodifiableMap(fields);
    }

    @Override
    public Map<String, PropertyFactory<?>> getPropertyFactories() {
        return propertyFactories;
    }
    
    @Override
    public T createInstance() {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create ComponentData instance", e);
        }
    }
    
    @Override
    public void setProperty(T instance, String key, Property property) {
        if (instance == null || key == null || property == null)
            throw new NullPointerException("Arguments cannot be null");
        Field f = fields.get(key);
        
        // validate field now
        if (f == null)
            throw new IllegalArgumentException("Key is not in Map returned by getPropertyFactories(): " + key);
        if (!f.getType().isAssignableFrom(property.getClass()))
            throw new IllegalArgumentException("Property was not created by correct PropertyFactory for key: " + key);
        
        try {
            f.set(instance, property);
        } catch (Exception e) {
            throw new RuntimeException("Unable to inject Property", e);
        }
    }

    private static <T extends ComponentData<?>> Map<String, PropertyFactory<?>> getPropertyFactories(Map<String, Field> fields) {
        Map<String, PropertyFactory<?>> factories = new HashMap<String, PropertyFactory<?>>();
        for (Entry<String, Field> f: fields.entrySet()) {
            factories.put(f.getKey(), createFactory(f.getValue()));
        }
        return factories;
    }
    
    @SuppressWarnings("unchecked")
    private static PropertyFactory<?> createFactory(Field field) {
        Class<? extends Property> type = (Class<? extends Property>) field.getType();
        Class<? extends ComponentData<?>> forCType = (Class<? extends ComponentData<?>>) field.getDeclaringClass();
        
        // Check to use the explicit factory
        Factory factoryAnnot = field.getAnnotation(Factory.class);
        if (factoryAnnot != null) {
            // verify that the PropertyFactory actually creates the right type
                try {
                    Method create = factoryAnnot.value().getMethod("create");
                    if (!type.isAssignableFrom(create.getReturnType()))
                        throw new IllegalComponentDefinitionException(forCType, "@Factory for " + factoryAnnot.value() + " creates incorrect Property type for property type: " + type);
                } catch (SecurityException e) {
                    // should not happen
                    throw new RuntimeException("Unable to inspect factory's create method", e);
                } catch (NoSuchMethodException e) {
                    // should not happen
                    throw new RuntimeException("Unable to inspect factory's create method", e);
                }
                
            try {
                return factoryAnnot.value().newInstance();
            } catch (Exception e) {
                throw new IllegalComponentDefinitionException(forCType, "Cannot create PropertyFactory from @Factory annotation: " + factoryAnnot.value());
            }
        }
        
        // we'll fall back to using these annotations (and their defaults) to create a factory
        // if there is a static factory() method present on the Property
        DefaultValue dfltValue = field.getAnnotation(DefaultValue.class);
        ElementSize elementSize = field.getAnnotation(ElementSize.class);
        int actualElementSize = (elementSize == null ? 1 : elementSize.value());
        
        if (dfltValue != null) {
            // look for factory methods of the different primitive types
            try {
                { // boolean
                    Method fm = getFactoryMethod(type, true, boolean.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultBoolean());
                }
                { // char
                    Method fm = getFactoryMethod(type, true, char.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultChar());
                }
                { // byte
                    Method fm = getFactoryMethod(type, true, byte.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultByte());
                }
                { // short
                    Method fm = getFactoryMethod(type, true, short.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultShort());
                }
                { // int
                    Method fm = getFactoryMethod(type, true, int.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultInt());
                }
                { // long
                    Method fm = getFactoryMethod(type, true, long.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultLong());
                }
                { // float
                    Method fm = getFactoryMethod(type, true, float.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultFloat());
                }
                { // double
                    Method fm = getFactoryMethod(type, true, double.class);
                    if (fm != null)
                        return (PropertyFactory<?>) fm.invoke(null, actualElementSize, dfltValue.defaultDouble());
                }
            } catch(Exception e) {
                throw new RuntimeException("Unable to call static factory method on Property type: " + type + " on field " + field);
            }
        } else {
            // we don't have a default type to use, so first look for one that
            // takes an element size
            try {
                Method fm = getFactoryMethod(type, true, null);
                if (fm != null)
                    return (PropertyFactory<?>) fm.invoke(null, actualElementSize);
                // fall back to a factory method with no arguments
                fm = getFactoryMethod(type, false, null);
                if (fm != null)
                    return (PropertyFactory<?>) fm.invoke(null);
            } catch(Exception e) {
                
            }
        }

        // unable to create a PropertyFactory
        throw new IllegalComponentDefinitionException(forCType, "Unable to create PropertyFactory for " + field);
    }
        
    private static Method getFactoryMethod(Class<? extends Property> type, boolean elementSize, Class<?> dfltType) {
        Method[] methods = type.getDeclaredMethods();
        
        for (Method m: methods) {
            // check if it is a static method that creates a PropertyFactory
            if (PropertyFactory.class.isAssignableFrom(m.getReturnType())
                && Modifier.isStatic(m.getModifiers())) {
                // now validate parameter types
                Class<?>[] params = m.getParameterTypes();
                if (!elementSize) {
                    // we assume that dfltType is also null and we accept the method
                    // if it has no arguments
                    if (params.length == 0)
                        return m;
                } else {
                    if (dfltType == null) {
                        // we accept it if the first type is an int
                        if (params.length == 1 && int.class.equals(params[0]))
                            return m;
                    } else {
                        // we accept it if the first type is an int and 
                        // the second type equals dfltType
                        if (params.length == 2 && int.class.equals(params[0]) && dfltType.equals(params[1]))
                            return m;
                    }
                }
            }
        }
        
        // no suitable factory method was found
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends ComponentData<?>> Constructor<T> getConstructor(Class<T> type) {
        // This assumes that type is the concrete type, so it will fail if there
        // are multiple constructors or it's not private with the correct arguments
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length != 1)
            throw new IllegalComponentDefinitionException(type, "ComponentData type must only define a single constructor");
        
        Constructor<T> ctor = (Constructor<T>) ctors[0];
        if (!Modifier.isPrivate(ctor.getModifiers()) && !Modifier.isProtected(ctor.getModifiers()))
            throw new IllegalComponentDefinitionException(type, "ComponentData constructor must be private or protected");
        
        Class<?>[] args = ctor.getParameterTypes();
        if (args.length != 0)
            throw new IllegalComponentDefinitionException(type, "ComponentData constructor does not have a default constructor");
        
        // Found it, now make it accessible (which might throw a SecurityException)
        ctor.setAccessible(true);
        return ctor;
    }
    
    private static Map<String, Field> getFields(Class<? extends ComponentData<?>> type) {
        Field[] declared = type.getDeclaredFields();
        Map<String, Field> nonTransientFields = new HashMap<String, Field>(declared.length);

        for (int i = 0; i < declared.length; i++) {
            int modifiers = declared[i].getModifiers();
            if (Modifier.isStatic(modifiers))
                continue; // ignore static fields
            
            if (declared[i].isAnnotationPresent(Unmanaged.class))
                continue; // ignore the field
            
            if (!Property.class.isAssignableFrom(declared[i].getType())) {
                throw new IllegalComponentDefinitionException(type, "ComponentData has non-Property field that is not unmanaged: " + declared[i]);
            }
            
            if (!Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers))
                throw new IllegalComponentDefinitionException(type, "Field must be private or protected: " + declared[i]);
            
            nonTransientFields.put(declared[i].getName(), declared[i]);
        }
        
        // Make sure all fields are accessible so we can assign them
        Field[] access = new Field[nonTransientFields.size()];
        nonTransientFields.values().toArray(access);
        Field.setAccessible(access, true);
        return nonTransientFields;
    }
}
