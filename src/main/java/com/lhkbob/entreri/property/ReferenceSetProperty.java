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
import java.util.Set;

/**
 * ReferenceSetProperty
 * ====================
 *
 * ReferenceSetProperty is a Property implementation for storing sets of a particular element type `T` with
 * reference semantics. This means the Property can store null sets, and its default is a null set. As it
 * stores references, the Set implementation that may be optionally specified with the {@link
 * com.lhkbob.entreri.property.Collection} attribute is ignored.
 *
 * ## Supported method patterns
 *
 * ReferenceSetProperty supports the standard `get(int) -> Set<T>` and `set(int, Set<T>) -> void` methods
 * used for the Java Bean patterns. Additionally it provides `add(int, T) -> boolean` to add an element to the
 * current set reference; `remove(int, T) -> boolean` to remove an element from the current set reference; and
 * `contains(int, T) -> boolean` to query if an element is in the current set reference. These methods are
 * used by the collections method patterns to allow for more options in Component definitions. They will throw
 * a {@link NullPointerException} if the current set reference is null.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the element type of its sets.
 *
 * @author Michael Ludwig
 */
public class ReferenceSetProperty<T>
        implements Property<ReferenceSetProperty<T>>, Property.ReferenceSemantics, Property.Generic<Set<T>> {
    private final boolean clone;
    private Set<T>[] data;

    /**
     * Create a new ReferenceSetProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ReferenceSetProperty(boolean cloneValue) {
        clone = cloneValue;
        data = new Set[1];
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ReferenceSetProperty(DoNotClone clone) {
        this(clone == null);
    }

    /**
     * @param index The component index
     * @return The current Set reference
     */
    public Set<T> get(int index) {
        return data[index];
    }

    /**
     * Update the current Set reference to `list`, which can be null.
     *
     * @param index The component index
     * @param set   The new Set reference
     */
    public void set(int index, Set<T> set) {
        data[index] = set;
    }

    /**
     * Add `element` to the current Set reference at `index`. If the current set is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Set#add(Object)}.
     *
     * @param index   The component index
     * @param element The element to append to the current set
     * @return True if the set was modified (e.g. the set did not contain the element before)
     */
    public boolean add(int index, T element) {
        return get(index).add(element);
    }

    /**
     * Remove `element` from the current Set reference at `index`. If the current set is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Set#remove(Object)}.
     *
     * @param index   The component index
     * @param element The element to remove from the current set
     * @return True if the element was removed
     */
    public boolean remove(int index, T element) {
        return get(index).remove(element);
    }

    /**
     * Check if `element` is contained in the current Set reference at `index`. If the current set is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Set#contains(Object)}.
     *
     * @param index   The component index
     * @param element The element to check for
     * @return True if the current set contains `element`
     */
    public boolean contains(int index, T element) {
        return get(index).contains(element);
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than necessary
     * for the number of components in the system. Data can be accessed for a component directly using the
     * component's index.
     *
     * @return The Set data for all packed properties that this property has been packed with
     */
    public Set<T>[] getIndexedData() {
        return data;
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
    public void swap(int indexA, int indexB) {
        Set<T> t = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = t;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, null);
    }

    @Override
    public void clone(ReferenceSetProperty<T> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
