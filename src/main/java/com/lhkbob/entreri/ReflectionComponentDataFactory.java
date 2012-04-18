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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * ReflectionComponentDataFactory is a factory for creating new instances of
 * ComponentDatas of a specific type. ReflectionComponentDataFactory has a
 * number of requirements for the way that a ComponentData subclass is defined
 * in order to be processed correctly. If these are too restrictive for your
 * needs, you can implement your own {@link ComponentDataFactory} and annotate
 * the ComponentData type with {@link DefaultFactory}.
 * </p>
 * <p>
 * ReflectionComponentDataFactory has the following requirements:
 * <ol>
 * <li>If the class is not a direct subclass of ComponentData, its parent must
 * be a assignable to ComponentData and be declared abstract. The parent's
 * declared fields must also follow the rules below.</li>
 * <li>A concrete subclass of ComponentData must have only one constructor; it
 * must be private or protected and take zero arguments.</li>
 * <li>All non-static fields that are not annotated with {@link Unmanaged}
 * defined in the ComponentData type, or its abstract parents, must be
 * Properties and be private or protected.</li>
 * <li>The Property field must be annotated with {@link Factory} or the Property
 * class must be annotated with {@link Factory}.</li>
 * <li>The specified factory must have a default constructor, or a constructor
 * that takes a single {@link Attributes} instance.</li>
 * </ol>
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The built component type
 */
public final class ReflectionComponentDataFactory<T extends ComponentData<T>> implements ComponentDataFactory<T> {
    private final Constructor<T> constructor;
    private final Map<Field, PropertyFactory<?>> propertyFactories;

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
        List<Field> fields = new ArrayList<Field>(getFields(type));
        
        Class<? super T> parent = type.getSuperclass();
        while(!ComponentData.class.equals(parent)) {
            if (!Modifier.isAbstract(parent.getModifiers()))
                throw new IllegalComponentDefinitionException(type, "Parent class " + parent + " is not abstract");
            
            // this cast is safe since we're in the while loop
            fields.addAll(getFields((Class<? extends ComponentData<?>>) parent));
            parent = parent.getSuperclass();
        }
        
        constructor = getConstructor(type);
        propertyFactories = Collections.unmodifiableMap(getPropertyFactories(fields));
    }

    @Override
    public Map<Field, PropertyFactory<?>> getPropertyFactories() {
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
    public void setProperty(T instance, Object key, Property property) {
        if (instance == null || key == null || property == null)
            throw new NullPointerException("Arguments cannot be null");
        Field f = (Field) key;
        
        if (!f.getType().isAssignableFrom(property.getClass()))
            throw new IllegalArgumentException("Property was not created by correct PropertyFactory for key: " + key);
        
        try {
            f.set(instance, property);
        } catch (Exception e) {
            throw new RuntimeException("Unable to inject Property", e);
        }
    }

    private static <T extends ComponentData<?>> Map<Field, PropertyFactory<?>> getPropertyFactories(List<Field> fields) {
        Map<Field, PropertyFactory<?>> factories = new HashMap<Field, PropertyFactory<?>>();
        for (Field f: fields) {
            factories.put(f, createFactory(f));
        }
        return factories;
    }
    
    @SuppressWarnings("unchecked")
    private static PropertyFactory<?> createFactory(Field field) {
        Class<? extends Property> type = (Class<? extends Property>) field.getType();
        Class<? extends ComponentData<?>> forCType = (Class<? extends ComponentData<?>>) field.getDeclaringClass();
        
        // Check for the @Factory on the field and the type
        Class<? extends PropertyFactory<?>> factoryType;
        if (field.getAnnotation(Factory.class) != null) {
            // prefer field declaration
            factoryType = field.getAnnotation(Factory.class).value();
        } else if (type.getAnnotation(Factory.class) != null) {
            // fall back to type declaration
            factoryType = type.getAnnotation(Factory.class).value();
        } else {
            throw new IllegalComponentDefinitionException(forCType, "Cannot create PropertyFactory for " + type + ", no @Factory annotation on field or type");
        }

        // verify that the PropertyFactory actually creates the right type
        try {
            Method create = factoryType.getMethod("create");
            if (!type.isAssignableFrom(create.getReturnType()))
                throw new IllegalComponentDefinitionException(forCType, "@Factory(" + factoryType + ") creates incorrect Property type: " + create.getReturnType() + ", required type: " + type);
        } catch (SecurityException e) {
            // should not happen
            throw new RuntimeException("Unable to inspect factory's create method", e);
        } catch (NoSuchMethodException e) {
            // should not happen
            throw new RuntimeException("Unable to inspect factory's create method", e);
        }
        
        PropertyFactory<?> factory = invokeConstructor(factoryType, new Attributes(field));
        if (factory == null)
            factory = invokeConstructor(factoryType);

        if (factory == null) {
            // unable to create a PropertyFactory
            throw new IllegalComponentDefinitionException(forCType, "Unable to create PropertyFactory for " + field);
        } else {
            return factory;
        }
    }
    
    private static PropertyFactory<?> invokeConstructor(Class<? extends PropertyFactory<?>> type, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++)
            paramTypes[i] = args[i].getClass();
        
        try {
            Constructor<?> ctor = type.getConstructor(paramTypes);
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
    
    private static List<Field> getFields(Class<? extends ComponentData<?>> type) {
        Field[] declared = type.getDeclaredFields();
        List<Field> nonTransientFields = new ArrayList<Field>(declared.length);

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
            
            nonTransientFields.add(declared[i]);
        }
        
        // Make sure all fields are accessible so we can assign them
        Field[] access = new Field[nonTransientFields.size()];
        nonTransientFields.toArray(access);
        Field.setAccessible(access, true);
        return nonTransientFields;
    }
}
