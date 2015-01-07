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

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * ObjectProperty
 * ==============
 *
 * ObjectProperty is an implementation of Property with reference semantics that can store any Object type.
 * This Property implementation can only be used when the property declaration site specifies the {@link
 * Reference} attribute.
 *
 * Because it uses reference semantics, its behaviors are slightly different from most other properties that
 * use value semantics. In particular, when a property value is "cloned" when creating a component from a
 * template, the reference is copied and not the actual object.  It supports the {@link
 * DoNotClone} attribute. References will not be cloned if either the source or
 * destination property specify not to clone the reference. The default value is always `null`.
 *
 * ## Supported method patterns
 *
 * ObjectProperty defines the `get(int) -> T` and `set(int, T) -> void` methods that can be used
 * by a component's Java Bean getters and setters of type the Object type `T`.
 *
 * ## Generic
 *
 * As a generic property, this property supports any type that extends {@link java.lang.Object}.
 *
 * @author Michael Ludwig
 */
public final class ObjectProperty<T>
        implements Property<ObjectProperty<T>>, Property.ReferenceSemantics, Property.Generic<T> {
    private final boolean cloneValue;
    private T[] data;

    /**
     * Create an ObjectProperty with the given clone policy. This is the programmer-friendly constructor
     *
     * @param type       The component class type
     * @param cloneValue True if the value should be copied (by reference) during a component clone
     */
    @SuppressWarnings("unchecked")
    public ObjectProperty(Class<T> type, boolean cloneValue) {
        this.cloneValue = cloneValue;
        data = (T[]) Array.newInstance(type, 1);
    }

    /**
     * A constructor meeting the default conventions for automated creation.
     */
    public ObjectProperty(Class<T> type, DoNotClone doNotClone) {
        this(type, doNotClone == null);
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than
     * necessary for the number of components in the system. Data can be accessed for a component directly
     * using the component's index.
     *
     * @return The Object data for all packed properties that this property has been packed with
     */
    public T[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    @SuppressWarnings("unchecked")
    public T get(int componentIndex) {
        return data[componentIndex];
    }

    /**
     * Store `val` in this property for the given component index.
     *
     * @param componentIndex The index of the component being modified
     * @param val            The value to store, can be null
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int componentIndex, T val) {
        data[componentIndex] = val;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, null);
    }

    @Override
    public void clone(ObjectProperty<T> src, int srcIndex, int dstIndex) {
        if (!src.cloneValue || !cloneValue) {
            setDefaultValue(dstIndex);
        } else {
            set(dstIndex, src.get(srcIndex));
        }
    }

    @Override
    public void swap(int a, int b) {
        T t = data[a];
        data[a] = data[b];
        data[b] = t;
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void setCapacity(int size) {
        // this properly preserves the array component type using Array.newInstance()
        data = Arrays.copyOf(data, size);
    }
}
