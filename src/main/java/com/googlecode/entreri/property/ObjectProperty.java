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

import com.googlecode.entreri.ComponentData;

/**
 * ObjectProperty is an implementation of Property that stores the property data
 * as a number of packed Object references for each property. Because it is not
 * primitive data, cache locality will suffer compared to the primitive property
 * types, but it will allow you to store arbitrary objects.
 * 
 * @author Michael Ludwig
 */
public final class ObjectProperty<T> implements Property {
    private ObjectDataStore store;
    
    /**
     * Create an ObjectProperty with an element size of 1.
     */
    public ObjectProperty() {
        this(1);
    }
    
    /**
     * Create a new ObjectProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public ObjectProperty(int elementSize) {
        store = new ObjectDataStore(elementSize, new Object[elementSize]);
    }

    /**
     * Return a PropertyFactory that creates ObjectProperties with the given
     * element size. If it is less than 1, the factory's create() method will
     * fail. The default value is null.
     * 
     * @param <T>
     * @param elementSize The element size of the created properties
     * @return A PropertyFactory for ObjectProperty
     */
    public static <T> PropertyFactory<ObjectProperty<T>> factory(final int elementSize) {
        return factory(elementSize, null);
    }

    /**
     * Return a PropertyFactory that creates ObjectProperties with the given
     * element size and default value.
     * 
     * @param <T>
     * @param elementSize The element size of the created properties
     * @param dflt The default value assigned to each component and element
     * @return A PropertyFactory for ObjectProperty
     */
    public static <T> PropertyFactory<ObjectProperty<T>> factory(final int elementSize, final T dflt) {
        return new AbstractPropertyFactory<ObjectProperty<T>>() {
            @Override
            public ObjectProperty<T> create() {
                return new ObjectProperty<T>(elementSize);
            }
          
            @Override
            public void setDefaultValue(ObjectProperty<T> p, int index) {
                for (int i = 0; i < elementSize; i++)
                    p.set(dflt, index, i);
            }
        };
    }
    
    /**
     * Return the backing int array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link ComponentData#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The Object data for all packed properties that this property has
     *         been packed with
     */
    public Object[] getIndexedData() {
        return store.array;
    }

    /**
     * Get the value stored in this property for the given component index, and
     * offset. Offset is measured from 0 to 1 minus the element size the
     * property was originally created with.
     * 
     * @param componentIndex The component's index
     * @param offset The offset into the component's data
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex and offset
     *             would access illegal indices
     */
    @SuppressWarnings("unchecked")
    public T get(int componentIndex, int offset) {
        return (T) store.array[componentIndex * store.elementSize + offset];
    }

    /**
     * Store <tt>val</tt> in this property for the given component index, at the
     * specified offset. The offset is measured from 0 to 1 minus the element
     * size that this property was originally created with.
     * 
     * @param val The value to store, can be null
     * @param componentIndex The index of the component being modified
     * @param offset The offset into the component's data
     * @throws ArrayIndexOutOfBoundsException if the componentIndex and offset
     *             would access illegal indices
     */
    public void set(T val, int componentIndex, int offset) {
        store.array[componentIndex * store.elementSize + offset] = val;
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return store;
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        if (store == null)
            throw new NullPointerException("Store cannot be null");
        if (!(store instanceof ObjectDataStore))
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong type: " + store.getClass());
        
        ObjectDataStore newStore = (ObjectDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }

    private static class ObjectDataStore extends AbstractIndexedDataStore<Object[]> {
        private final Object[] array;
        
        public ObjectDataStore(int elementSize, Object[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        public ObjectDataStore create(int size) {
            return new ObjectDataStore(elementSize, new Object[elementSize * size]);
        }

        @Override
        protected Object[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(Object[] array) {
            return array.length;
        }
    }
}
