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

import com.lhkbob.entreri.IllegalComponentDefinitionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ValueListProperty
 * =================
 *
 * ValueListProperty is a Property implementation for storing lists of a particular element type `T` with
 * value semantics. As a value type for a List, its default value is an empty list based on the list
 * implementation determined by {@link com.lhkbob.entreri.property.Collection} (ArrayList by default). Value
 * semantics are implemented by copying input lists into an internal list that is only ever exposed as an
 * unmodifiable list. Although it itself cannot store null lists as a value type, the lists it maintains are
 * allowed to have null references as elements. Value semantics are not enforced upon the elements, they
 * are stored and treated as references.
 *
 * ## Supported method patterns
 *
 * ValueListProperty supports the standard `get(int) -> List<T>` and `set(int, List<T>) -> void` methods
 * used for the Java Bean patterns. Additionally it provides `add(int, T) -> boolean` to add an element to the
 * current list reference; `remove(int, T) -> boolean` to remove an element from the current list reference;
 * and `contains(int, T) -> boolean` to query if an element is in the current list reference. These methods
 * are used by the collections method patterns to allow for more options in Component definitions.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the element type of its lists.
 *
 * @author Michael Ludwig
 */
public class ValueListProperty<T>
        implements Property<ValueListProperty<T>>, Property.ValueSemantics, Property.Generic<List<T>> {
    private final Constructor<? extends List<T>> newListCtor;
    private final boolean clone;
    private List<T>[] data;
    private List<T>[] readOnlyData;

    /**
     * Create a new ValueListProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param listClass  The backing List implementation used for each component
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ValueListProperty(Class<? extends List> listClass, boolean cloneValue) {
        try {
            newListCtor = (Constructor<? extends List<T>>) listClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentDefinitionException("Requested backing List implementation does not have a public default constructor: " +
                                                          listClass);
        }
        clone = cloneValue;
        data = new List[1];
        readOnlyData = new List[1];

        // must initialize it so that the 0 index obeys value semantics
        setDefaultValue(0);
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ValueListProperty(Collection collection, DoNotClone clone) {
        this((collection == null ? ArrayList.class : collection.listImpl()), clone == null);
    }

    /**
     * @param index The component index
     * @return The current List as a read-only reference (this mirrors any changes to the value)
     */
    public List<T> get(int index) {
        return readOnlyData[index];
    }

    /**
     * Copy the given list into this property's internal list value.
     *
     * @param index The component index
     * @param list  The new List reference
     */
    public void set(int index, List<T> list) {
        if (list == null) {
            throw new NullPointerException("Value-semantics list cannot be null");
        }
        data[index].clear();
        data[index].addAll(list);
    }

    /**
     * Add `element` to the current List reference at `index`. This has equivalent behavior to {@link
     * java.util.List#add(Object)}.
     *
     * @param index   The component index
     * @param element The element to append to the current list
     * @return True if the list was modified (generally always true for lists)
     */
    public boolean add(int index, T element) {
        return data[index].add(element);
    }

    /**
     * Remove `element` from the current list reference at `index`. This has equivalent behavior to {@link
     * java.util.List#remove(Object)}.
     *
     * @param index   The component index
     * @param element The element to remove from the current list
     * @return True if the element was removed
     */
    public boolean remove(int index, T element) {
        return data[index].remove(element);
    }

    /**
     * Check if `element` is contained in the current list reference at `index`. This has equivalent
     * behavior to {@link java.util.List#contains(Object)}.
     *
     * @param index   The component index
     * @param element The element to check for
     * @return True if the current list contains `element`
     */
    public boolean contains(int index, T element) {
        return data[index].contains(element);
    }

    @Override
    public void setCapacity(int size) {
        int oldLength = data.length;

        data = Arrays.copyOf(data, size);
        readOnlyData = Arrays.copyOf(readOnlyData, size);

        // guarantee we have value semantics for new values
        for (int i = oldLength; i < size; i++) {
            setDefaultValue(i);
        }
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void swap(int indexA, int indexB) {
        List<T> t = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = t;

        t = readOnlyData[indexA];
        readOnlyData[indexA] = readOnlyData[indexB];
        readOnlyData[indexB] = t;
    }

    @Override
    public void setDefaultValue(int index) {
        if (data[index] == null) {
            // first time initialization for this slot
            try {
                data[index] = newListCtor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error creating default value for list property", e);
            }
            readOnlyData[index] = Collections.unmodifiableList(data[index]);
        } else {
            data[index].clear();
        }
    }

    @Override
    public void clone(ValueListProperty<T> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
