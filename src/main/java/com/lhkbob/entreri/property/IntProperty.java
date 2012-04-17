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
package com.lhkbob.entreri.property;

import com.lhkbob.entreri.Attributes;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.IndexedDataStore;
import com.lhkbob.entreri.Property;
import com.lhkbob.entreri.PropertyFactory;
import com.lhkbob.entreri.annot.DefaultValue;
import com.lhkbob.entreri.annot.ElementSize;
import com.lhkbob.entreri.annot.Factory;

/**
 * IntProperty is an implementation of Property that stores the property data
 * as a number of packed ints for each property.
 * 
 * @author Michael Ludwig
 */
@Factory(IntProperty.IntPropertyFactory.class)
public final class IntProperty implements Property {
    private IntDataStore store;
    
    /**
     * Create an IntProperty with an element size of 1.
     */
    public IntProperty() {
        this(1);
    }
    
    /**
     * Create a new IntProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public IntProperty(int elementSize) {
        store = new IntDataStore(elementSize, new int[elementSize]);
    }

    /**
     * Return a PropertyFactory that creates IntProperties with the given
     * element size. If it is less than 1, the factory's create() method will
     * fail. The default value is 0.
     * 
     * @param elementSize The element size of the created properties
     * @return A PropertyFactory for IntProperty
     */
    public static PropertyFactory<IntProperty> factory(int elementSize) {
        return factory(elementSize, 0);
    }

    /**
     * Return a PropertyFactory that creates IntProperties with the given
     * element size and default value.
     * 
     * @param elementSize The element size of the created properties
     * @param dflt The default value assigned to each component and element
     * @return A PropertyFactory for IntProperty
     */
    public static PropertyFactory<IntProperty> factory(int elementSize, final int dflt) {
        return new IntPropertyFactory(elementSize, dflt);
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link ComponentData#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The int data for all packed properties that this property has
     *         been packed with
     */
    public int[] getIndexedData() {
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
    public int get(int componentIndex, int offset) {
        return store.array[componentIndex * store.elementSize + offset];
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
    public void set(int val, int componentIndex, int offset) {
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
        if (!(store instanceof IntDataStore))
            throw new IllegalArgumentException("Store not compatible with IntProperty, wrong type: " + store.getClass());
        
        IntDataStore newStore = (IntDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with IntProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }
    
    private static class IntPropertyFactory extends AbstractPropertyFactory<IntProperty> {
        private final int elementSize;
        private final int defaultValue;
        
        public IntPropertyFactory(Attributes attrs) {
            super(attrs);
            
            if (attrs.hasAttribute(DefaultValue.class))
                defaultValue = attrs.getAttribute(DefaultValue.class).defaultInt();
            else
                defaultValue = 0;
            
            if (attrs.hasAttribute(ElementSize.class))
                elementSize = attrs.getAttribute(ElementSize.class).value();
            else
                elementSize = 1;
        }
        
        public IntPropertyFactory(int elementSize, int defaultValue) {
            super(null);
            this.elementSize = elementSize;
            this.defaultValue = defaultValue;
        }

        @Override
        public IntProperty create() {
            return new IntProperty(elementSize);
        }

        @Override
        public void setDefaultValue(IntProperty property, int index) {
            for (int i = 0; i < elementSize; i++)
                property.set(defaultValue, index, i);
        }
    }

    private static class IntDataStore extends AbstractIndexedDataStore<int[]> {
        private final int[] array;
        
        public IntDataStore(int elementSize, int[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        public long memory() {
            return 4 * array.length;
        }
        
        @Override
        public IntDataStore create(int size) {
            return new IntDataStore(elementSize, new int[elementSize * size]);
        }

        @Override
        protected int[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(int[] array) {
            return array.length;
        }
    }
}
