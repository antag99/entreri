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

import com.lhkbob.entreri.attr.DefaultEnum;
import com.lhkbob.entreri.attr.DoNotClone;

import java.util.Arrays;

/**
 * EnumProperty
 * ============
 *
 * EnumProperty is a specialized generic property for enum values that stores just the ordinal values of a
 * specific enum class in a packed int array.  The type-mapping of component implementation generation
 * automatically uses an EnumProperty for any enum properties unless there's an explicit mapping declared in
 * META-INF.
 *
 * It supports the {@link com.lhkbob.entreri.attr.DefaultEnum} and {@link
 * com.lhkbob.entreri.attr.DoNotClone} attributes. Values will not be cloned if either the source or
 * destination property specify not to clone the value.
 *
 * ## Supported method patterns
 *
 * EnumProperty defines the `get(int) -> T` and `set(int, T) -> void` methods that can be used
 * by a component's Java Bean getters and setters of type the enum type `T`.
 *
 * ## Generic
 *
 * As a generic property, this property supports any type of enum that extends {@link java.lang.Enum}.
 *
 * @author Michael Ludwig
 */
public class EnumProperty<T extends Enum>
        implements Property<EnumProperty<T>>, Property.ValueSemantics, Property.Generic<T> {
    private final T[] values;
    private final T defaultValue;
    private final boolean cloneValue;
    private int[] data;

    /**
     * Create an EnumProperty with the selected default enum value and clone policy. This is a programmer
     * friendly constructor.
     *
     * @param dflt       The default enum value (must not be null)
     * @param cloneValue True if values can be copied during a component clone
     */
    @SuppressWarnings("unchecked")
    public EnumProperty(T dflt, boolean cloneValue) {
        values = (T[]) dflt.getClass().getEnumConstants();
        defaultValue = dflt;
        this.cloneValue = cloneValue;
        data = new int[1];
    }

    /**
     * Create an EnumProperty for the given enum class type, compatible with default constructor conventions.
     *
     * @param enumType The enum class
     */
    public EnumProperty(Class<T> enumType, DefaultEnum dflt, DoNotClone doNotClone) {
        values = enumType.getEnumConstants();
        defaultValue = values[dflt != null ? dflt.ordinal() : 0];
        cloneValue = doNotClone == null;
        data = new int[1];
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than necessary for
     * the number of components in the system. Data can be accessed for a component directly using the
     * component's index. The int values correspond to the ordinals of the enum class stored by this
     * EnumProperty.
     *
     * @return The int data for all packed properties that this property has been packed with
     */
    public int[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param index The component's index
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public T get(int index) {
        return values[data[index]];
    }

    /**
     * Store `value` in this property for the given component index.
     *
     * @param index The index of the component being modified
     * @param value The value to store, can be null
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int index, T value) {
        data[index] = value.ordinal();
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size);
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, defaultValue);
    }

    @Override
    public void clone(EnumProperty<T> src, int srcIndex, int dstIndex) {
        if (!src.cloneValue || !cloneValue) {
            setDefaultValue(dstIndex);
        } else {
            set(dstIndex, src.get(srcIndex));
        }
    }

    @Override
    public void swap(int indexA, int indexB) {
        int ord = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = ord;
    }
}
