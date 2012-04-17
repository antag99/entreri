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
 * BooleanProperty is an implementation of Property that stores the property data
 * as a number of packed booleans for each property. An example would be a
 * three-dimensional vector, which would have an element size of 3.
 * 
 * @author Michael Ludwig
 */
@Factory(BooleanProperty.BooleanPropertyFactory.class)
public final class BooleanProperty implements Property {
    private BooleanDataStore store;
    
    /**
     * Create a FloatProperty with an element size of 1.
     */
    public BooleanProperty() {
        this(1);
    }

    /**
     * Create a new FloatProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public BooleanProperty(int elementSize) {
        store = new BooleanDataStore(elementSize, new boolean[elementSize]);
    }

    /**
     * Return a PropertyFactory that creates BooleanProperties with the given
     * element size. If it is less than 1, the factory's create() method will
     * fail. The default value is false.
     * 
     * @param elementSize The element size of the created properties
     * @return A PropertyFactory for BooleanProperty
     */
    public static PropertyFactory<BooleanProperty> factory(int elementSize) {
        return factory(elementSize, false);
    }

    /**
     * Return a PropertyFactory that creates IntProperties with the given
     * element size and default value.
     * 
     * @param elementSize The element size of the created properties
     * @param dflt The default value assigned to each component and element
     * @return A PropertyFactory for IntProperty
     */
    public static PropertyFactory<BooleanProperty> factory(int elementSize, boolean dflt) {
        return new BooleanPropertyFactory(elementSize, dflt);
    }

    /**
     * Return the backing boolean array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link ComponentData#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The boolean data for all packed properties that this property has
     *         been packed with
     */
    public boolean[] getIndexedData() {
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
    public boolean get(int componentIndex, int offset) {
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
    public void set(boolean val, int componentIndex, int offset) {
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
        if (!(store instanceof BooleanDataStore))
            throw new IllegalArgumentException("Store not compatible with FloatProperty, wrong type: " + store.getClass());
        
        BooleanDataStore newStore = (BooleanDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with FloatProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }
    
    private static class BooleanPropertyFactory extends AbstractPropertyFactory<BooleanProperty> {
        private final int elementSize;
        private final boolean defaultValue;
        
        public BooleanPropertyFactory(Attributes attrs) {
            super(attrs);
            
            if (attrs.hasAttribute(DefaultValue.class))
                defaultValue = attrs.getAttribute(DefaultValue.class).defaultBoolean();
            else
                defaultValue = false;
            
            if (attrs.hasAttribute(ElementSize.class))
                elementSize = attrs.getAttribute(ElementSize.class).value();
            else
                elementSize = 1;
        }
        
        public BooleanPropertyFactory(int elementSize, boolean defaultValue) {
            super(null);
            this.elementSize = elementSize;
            this.defaultValue = defaultValue;
        }

        @Override
        public BooleanProperty create() {
            return new BooleanProperty(elementSize);
        }

        @Override
        public void setDefaultValue(BooleanProperty property, int index) {
            for (int i = 0; i < elementSize; i++)
                property.set(defaultValue, index, i);
        }
    }

    private static class BooleanDataStore extends AbstractIndexedDataStore<boolean[]> {
        private final boolean[] array;
        
        public BooleanDataStore(int elementSize, boolean[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        public long memory() {
            return array.length;
        }
        
        @Override
        public BooleanDataStore create(int size) {
            return new BooleanDataStore(elementSize, new boolean[elementSize * size]);
        }

        @Override
        protected boolean[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(boolean[] array) {
            return array.length;
        }
    }
}
