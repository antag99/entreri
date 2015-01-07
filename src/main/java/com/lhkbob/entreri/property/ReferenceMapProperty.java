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
import java.util.Map;

/**
 * ReferenceMapProperty
 * ====================
 *
 * ReferenceMapProperty is a Property implementation for storing maps of a particular element key type `K`
 * and value type `V` with reference semantics. This means the Property can store null maps, and its default
 * is a null map. As it stores references, the Map implementation that may be optionally specified with the
 * {@link Collection} attribute is ignored.
 *
 * ## Supported method patterns
 *
 * ReferenceMapProperty supports the standard `get(int) -> Map<T>` and `set(int, Map<T>) -> void` methods
 * used for the Java Bean patterns. Additionally it provides `put(int, K, V) -> boolean` to put a key-value
 * pair in the current map reference; `remove(int, K) -> boolean` to remove an key from the current map
 * reference; and `contains(int, K) -> boolean` to query if a key is in the current map reference. These
 * methods are used by the collections method patterns to allow for more options in Component definitions.
 * They will throw a {@link java.lang.NullPointerException} if the current map reference is null.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the key and value types of its
 * maps.
 *
 * @author Michael Ludwig
 */
public class ReferenceMapProperty<K, V>
        implements Property<ReferenceMapProperty<K, V>>, Property.ReferenceSemantics,
                   Property.Generic<Map<K, V>> {
    private final boolean clone;
    private Map<K, V>[] data;

    /**
     * Create a new ReferenceMapProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ReferenceMapProperty(boolean cloneValue) {
        clone = cloneValue;
        data = new Map[1];
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ReferenceMapProperty(DoNotClone clone) {
        this(clone == null);
    }

    /**
     * @param index The component index
     * @return The current List reference
     */
    public Map<K, V> get(int index) {
        return data[index];
    }

    /**
     * Update the current List reference to `list`, which can be null.
     *
     * @param index The component index
     * @param list  The new List reference
     */
    public void set(int index, Map<K, V> list) {
        data[index] = list;
    }

    /**
     * Put the `key`-`value` pair into the current Map reference at `index`. If the current map is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Map#put(Object,
     * Object)}.
     *
     * @param index The component index
     * @param key   The map key
     * @param value The value to associate with the key
     * @return The previous value for the key, or null
     */
    public V put(int index, K key, V value) {
        return get(index).put(key, value);
    }

    /**
     * Get the `key`'s value from the current Map reference at `index`. If the current map is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Map#get(Object)}.
     *
     * @param index The component index
     * @param key   The map key
     * @return The current map's value associated with the key
     */
    public V get(int index, K key) {
        return get(index).get(key);
    }

    /**
     * Remove `key` from the current map reference at `index`. If the current map is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.Map#remove(Object)}.
     *
     * @param index The component index
     * @param key   The key to remove from the current map
     * @return The element that was removed
     */
    public V remove(int index, K key) {
        return get(index).remove(key);
    }

    /**
     * Check if `key` is used as a key in the current map reference at `index`. If the current map is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link
     * java.util.Map#containsKey(Object)}.
     *
     * @param index The component index
     * @param key   The key to check for
     * @return True if the current map contains the `key`
     */
    public boolean contains(int index, K key) {
        return get(index).containsKey(key);
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than necessary
     * for the number of components in the system. Data can be accessed for a component directly using the
     * component's index.
     *
     * @return The Map data for all packed properties that this property has been packed with
     */
    public Map<K, V>[] getIndexedData() {
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
        Map<K, V> t = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = t;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, null);
    }

    @Override
    public void clone(ReferenceMapProperty<K, V> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
