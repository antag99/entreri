/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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

import java.util.Arrays;

/**
 * ShortProperty
 * =============
 *
 * ShortProperty is an implementation of Property that stores a single short value, obviously with value
 * semantics. It supports the {@link DefaultShort} and {@link
 * DoNotClone} attributes. Values will not be cloned if either the source or
 * destination property specify not to clone the value.
 *
 * ## Supported method patterns
 *
 * ShortProperty defines the `get(int) -> short` and `set(int, short) -> void` methods that can be used
 * by a component's Java Bean getters and setters of type `short`.
 *
 * @author Michael Ludwig
 */
public final class ShortProperty implements Property<ShortProperty>, Property.ValueSemantics {
    private final short defaultValue;
    private final boolean cloneValue;
    private short[] data;

    /**
     * Create a ShortProperty with a programmer friendly signature.
     *
     * @param defaultValue The default short value when components are initialized
     * @param cloneValue   True if the value is cloned, or false if clones just use the default
     */
    public ShortProperty(short defaultValue, boolean cloneValue) {
        this.defaultValue = defaultValue;
        this.cloneValue = cloneValue;
        data = new short[1];
    }

    /**
     * Create a ShortProperty using the constructor satisfying the default annotation conventions.
     */
    public ShortProperty(DefaultShort dflt, DoNotClone clonePolicy) {
        this((dflt != null ? dflt.value() : 0), clonePolicy == null);
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The array may be longer than
     * necessary for the number of components in the system. Data can be accessed for a component directly
     * using the component's index.
     *
     * @return The short data for all packed properties that this property has been packed with
     */
    public short[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public short get(int componentIndex) {
        return data[componentIndex];
    }

    /**
     * Store <var>val</var> in this property for the given component index.
     *
     * @param componentIndex The index of the component being modified
     * @param val            The value to store, can be null
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int componentIndex, short val) {
        data[componentIndex] = val;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, defaultValue);
    }

    @Override
    public void clone(ShortProperty src, int srcIndex, int dstIndex) {
        if (!src.cloneValue || !cloneValue) {
            setDefaultValue(dstIndex);
        } else {
            set(dstIndex, src.get(srcIndex));
        }
    }

    @Override
    public void swap(int a, int b) {
        short t = data[a];
        data[a] = data[b];
        data[b] = t;
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size);
    }
}
