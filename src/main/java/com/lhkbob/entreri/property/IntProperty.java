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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * IntProperty is an implementation of Property that stores a single int value.
 *
 * @author Michael Ludwig
 */
@Factory(IntProperty.Factory.class)
public final class IntProperty implements Property {
    private IntDataStore store;

    /**
     * Create an IntProperty.
     */
    public IntProperty() {
        store = new IntDataStore(1, new int[1]);
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The array may be
     * longer than necessary for the number of components in the system. Data can be
     * accessed for a component directly using the component's index.
     *
     * @return The int data for all packed properties that this property has been packed
     *         with
     */
    public int[] getIndexedData() {
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
    public int get(int componentIndex) {
        return store.getArray()[componentIndex];
    }

    /**
     * Store <var>val</var> in this property for the given component index.
     *
     * @param val            The value to store, can be null
     * @param componentIndex The index of the component being modified
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int val, int componentIndex) {
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
        if (!(store instanceof IntDataStore)) {
            throw new IllegalArgumentException(
                    "Store not compatible with IntProperty, wrong type: " +
                    store.getClass());
        }

        IntDataStore newStore = (IntDataStore) store;
        if (newStore.elementSize != this.store.elementSize) {
            throw new IllegalArgumentException(
                    "Store not compatible with IntProperty, wrong element size: " +
                    newStore.elementSize);
        }

        this.store = newStore;
    }

    /**
     * Factory to create IntProperties. Properties annotated with DefaultInt will use that
     * value as the default for all components.
     *
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<IntProperty> {
        private final int defaultValue;

        public Factory(Attributes attrs) {
            super(attrs);

            if (attrs.hasAttribute(DefaultInt.class)) {
                defaultValue = attrs.getAttribute(DefaultInt.class).value();
            } else {
                defaultValue = 0;
            }
        }

        public Factory(int defaultValue) {
            super(null);
            this.defaultValue = defaultValue;
        }

        @Override
        public IntProperty create() {
            return new IntProperty();
        }

        @Override
        public void setDefaultValue(IntProperty property, int index) {
            property.set(defaultValue, index);
        }
    }

    /**
     * Default int attribute for properties.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultInt {
        int value();
    }
}
