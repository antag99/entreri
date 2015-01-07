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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ValueSetProperty
 * ================
 *
 * ValueSetProperty is a Property implementation for storing sets of a particular element type `T` with
 * value semantics. As a value type for a Set, its default value is an empty set based on the set
 * implementation determined by {@link Collection} (HashSet by default). Value semantics are implemented by
 * copying input sets into an internal set that is only ever exposed as an unmodifiable set. Although it
 * itself cannot store null sets as a value type, the sets it maintains are allowed to have null references as
 * elements. Value semantics are not enforced upon the elements, they are stored and treated as references.
 *
 * ## Supported method patterns
 *
 * ValueSetProperty supports the standard `get(int) -> Set<T>` and `set(int, Set<T>) -> void` methods used
 * for the Java Bean patterns. Additionally it provides `add(int, T) -> boolean` to add an element to the
 * current list reference; `remove(int, T) -> boolean` to remove an element from the current list reference;
 * and `contains(int, T) -> boolean` to query if an element is in the current list reference. These methods
 * are used by the collections method patterns to allow for more options in Component definitions.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the element type of its sets.
 *
 * @author Michael Ludwig
 */
public class ValueSetProperty<T>
        implements Property<ValueSetProperty<T>>, Property.ValueSemantics, Property.Generic<Set<T>> {
    private final Constructor<? extends Set<T>> newSetCtor;
    private final boolean clone;
    private Set<T>[] data;
    private Set<T>[] readOnlyData;

    /**
     * Create a new ValueSetProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param setClass   The backing Set implementation used for each component
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ValueSetProperty(Class<? extends Set> setClass, boolean cloneValue) {
        try {
            newSetCtor = (Constructor<? extends Set<T>>) setClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentDefinitionException("Requested backing Set implementation does not have a public default constructor: " +
                                                          setClass);
        }
        clone = cloneValue;
        data = new Set[1];
        readOnlyData = new Set[1];

        // must initialize it so that the 0 index obeys value semantics
        setDefaultValue(0);
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ValueSetProperty(Collection collection, DoNotClone clone) {
        this((collection == null ? HashSet.class : collection.setImpl()), clone == null);
    }

    /**
     * @param index The component index
     * @return The current Set as a read-only reference (this mirrors any changes to the value)
     */
    public Set<T> get(int index) {
        return readOnlyData[index];
    }

    /**
     * Copy the given set into this property's internal set value.
     *
     * @param index The component index
     * @param set   The new Set reference
     */
    public void set(int index, Set<T> set) {
        if (set == null) {
            throw new NullPointerException("Value-semantics set cannot be null");
        }
        data[index].clear();
        data[index].addAll(set);
    }

    /**
     * Add `element` to the current Set reference at `index`. This has equivalent behavior to {@link
     * java.util.Set#add(Object)}.
     *
     * @param index   The component index
     * @param element The element to append to the current set
     * @return True if the set was modified (e.g. the set did not contain the element before)
     */
    public boolean add(int index, T element) {
        return data[index].add(element);
    }

    /**
     * Remove `element` from the current Set reference at `index`. This has equivalent behavior to {@link
     * java.util.Set#remove(Object)}.
     *
     * @param index   The component index
     * @param element The element to remove from the current set
     * @return True if the element was removed
     */
    public boolean remove(int index, T element) {
        return data[index].remove(element);
    }

    /**
     * Check if `element` is contained in the current set reference at `index`. This has equivalent behavior
     * to {@link java.util.Set#contains(Object)}.
     *
     * @param index   The component index
     * @param element The element to check for
     * @return True if the current set contains `element`
     */
    public boolean contains(int index, T element) {
        return data[index].contains(element);
    }

    @Override
    public void setCapacity(int size) {
        int oldLength = data.length;
        data = Arrays.copyOf(data, size);
        readOnlyData = Arrays.copyOf(readOnlyData, size);

        // guarantee we have value semantics for new elements
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
        Set<T> t = data[indexA];
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
                data[index] = newSetCtor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error creating default value for set property", e);
            }
            readOnlyData[index] = Collections.unmodifiableSet(data[index]);
        } else {
            data[index].clear();
        }
    }

    @Override
    public void clone(ValueSetProperty<T> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
