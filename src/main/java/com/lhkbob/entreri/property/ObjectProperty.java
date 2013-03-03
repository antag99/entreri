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

import com.lhkbob.entreri.*;

import java.lang.reflect.Method;

/**
 * ObjectProperty is an implementation of Property that stores the property data as a
 * number of packed Object references for each property. Because it is not primitive data,
 * cache locality will suffer compared to the primitive property types, but it will allow
 * you to store arbitrary objects.
 *
 * @author Michael Ludwig
 */
@Factory(ObjectProperty.Factory.class)
public final class ObjectProperty<T> implements Property {
    private ObjectDataStore store;

    /**
     * Create an ObjectProperty.
     */
    public ObjectProperty() {
        store = new ObjectDataStore(1, new Object[1]);
    }

    /**
     * Return a PropertyFactory that creates ObjectProperties with the given element size
     * and default value.
     *
     * @param <T>
     * @param dflt The default value assigned to each component and element
     *
     * @return A PropertyFactory for ObjectProperty
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> PropertyFactory<ObjectProperty<T>> factory(T dflt) {
        PropertyFactory superRaw = new Factory(dflt);
        return superRaw;
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The array may be
     * longer than necessary for the number of components in the system. Data can be
     * accessed for a component directly using the component's index.
     *
     * @return The Object data for all packed properties that this property has been
     *         packed with
     */
    public Object[] getIndexedData() {
        return store.getArray();
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     *
     * @return The object at the given offset for the given component
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    @SuppressWarnings("unchecked")
    public T get(int componentIndex) {
        return (T) store.getArray()[componentIndex];
    }

    /**
     * Store <tt>val</tt> in this property for the given component index.
     *
     * @param val            The value to store, can be null
     * @param componentIndex The index of the component being modified
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(T val, int componentIndex) {
        store.getArray()[componentIndex] = val;
    }

    @Override
    public IndexedDataStore getDataStore() {
        return store;
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        if (store == null) {
            throw new NullPointerException("Store cannot be null");
        }
        if (!(store instanceof ObjectDataStore)) {
            throw new IllegalArgumentException(
                    "Store not compatible with ObjectProperty, wrong type: " +
                    store.getClass());
        }

        ObjectDataStore newStore = (ObjectDataStore) store;
        if (newStore.elementSize != this.store.elementSize) {
            throw new IllegalArgumentException(
                    "Store not compatible with ObjectProperty, wrong element size: " +
                    newStore.elementSize);
        }

        this.store = newStore;
    }

    /**
     * Factory to create ObjectProperties.
     *
     * @author Michael Ludwig
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static class Factory extends AbstractPropertyFactory<ObjectProperty> {
        private final Object defaultValue;

        public Factory(Attributes attrs) {
            super(attrs);

            defaultValue = null;
        }

        public Factory(Object defaultValue) {
            super(null);
            this.defaultValue = defaultValue;
        }

        @Override
        public ObjectProperty create() {
            return new ObjectProperty();
        }

        @Override
        public void setDefaultValue(ObjectProperty property, int index) {
            property.set(defaultValue, index);
        }

        @Override
        public void clone(ObjectProperty src, int srcIndex, ObjectProperty dst,
                          int dstIndex) {
            Object orig = src.get(srcIndex);

            if (clonePolicy == Clone.Policy.INVOKE_CLONE && orig instanceof Cloneable) {
                try {
                    // if they implemented Cloneable properly, clone() should
                    // be public and take no arguments
                    Method cloneMethod = orig.getClass().getMethod("clone");
                    Object cloned = cloneMethod.invoke(orig);
                    dst.set(cloned, dstIndex);
                } catch (Exception e) {
                    // if they implement Cloneable, this shouldn't fail
                    // and if it does it's not really our fault
                    throw new RuntimeException(e);
                }
            } else {
                super.clone(src, srcIndex, dst, dstIndex);
            }
        }
    }
}
