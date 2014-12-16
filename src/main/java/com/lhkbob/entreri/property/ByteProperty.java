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

import com.lhkbob.entreri.attr.DefaultByte;
import com.lhkbob.entreri.attr.DoNotClone;

import java.util.Arrays;

/**
 * ByteProperty
 * ============
 *
 * ByteProperty is an implementation of Property that stores a single byte value, obviously with value
 * semantics. It supports the {@link com.lhkbob.entreri.attr.DefaultByte} and {@link
 * com.lhkbob.entreri.attr.DoNotClone} attributes. Values will not be cloned if either the source or
 * destination property specify not to clone the value.
 *
 * ## Supported method patterns
 *
 * ByteProperty defines the `get(int) -> byte` and `set(int, byte) -> void` methods that can be used
 * by a component's Java Bean getters and setters of type `byte`.
 *
 * @author Michael Ludwig
 */
public final class ByteProperty implements Property<ByteProperty>, Property.ValueSemantics {
    private final byte defaultValue;
    private final boolean cloneValue;
    private byte[] data;

    /**
     * Create a ByteProperty with a programmer friendly signature.
     *
     * @param defaultValue The default byte value when components are initialized
     * @param cloneValue   True if the value is cloned, or false if clones just use the default
     */
    public ByteProperty(byte defaultValue, boolean cloneValue) {
        this.defaultValue = defaultValue;
        this.cloneValue = cloneValue;
        data = new byte[1];
    }

    /**
     * Create a ByteProperty using the constructor satisfying the default annotation conventions.
     */
    public ByteProperty(DefaultByte dflt, DoNotClone clonePolicy) {
        this((dflt != null ? dflt.value() : 0), clonePolicy == null);
    }

    /**
     * Return the backing byte array of this property. The array may be longer than necessary for the number
     * of components in the system. Data can be accessed for a component directly using the component's index.
     *
     * @return The byte data for all packed properties that this property has been packed with
     */
    public byte[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public byte get(int componentIndex) {
        return data[componentIndex];
    }

    /**
     * Store `val` in this property for the given component index.
     *
     * @param componentIndex The index of the component being modified
     * @param val            The value to store
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int componentIndex, byte val) {
        data[componentIndex] = val;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, defaultValue);
    }

    @Override
    public void clone(ByteProperty src, int srcIndex, int dstIndex) {
        if (!src.cloneValue || !cloneValue) {
            setDefaultValue(dstIndex);
        } else {
            set(dstIndex, src.get(srcIndex));
        }
    }

    @Override
    public void swap(int a, int b) {
        byte t = data[a];
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
