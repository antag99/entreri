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
import java.util.HashMap;
import java.util.Map;

/**
 * ValueMapProperty
 * ====================
 *
 * ValueMapProperty is a Property implementation for storing maps of a particular element key type `K` and
 * value type `V` with value semantics.  As a value type for a Map, its default value is an empty map based on
 * the map implementation determined by {@link com.lhkbob.entreri.property.Collection} (HashMap by default).
 * Value semantics are implemented by copying input maps into an internal map that is only ever exposed as an
 * unmodifiable map. Although it itself cannot store null maps as a value type, the maps it maintains are
 * allowed to have null references as keys and values. Value semantics are not enforced upon the elements,
 * they are stored and treated as references.
 *
 * ## Supported method patterns
 *
 * ValueMapProperty supports the standard `get(int) -> Map<T>` and `set(int, Map<T>) -> void` methods used
 * for the Java Bean patterns. Additionally it provides `put(int, K, V) -> boolean` to put a key-value pair in
 * the current map reference; `remove(int, K) -> boolean` to remove an key from the current map reference; and
 * `contains(int, K) -> boolean` to query if a key is in the current map reference. These methods are used by
 * the collections method patterns to allow for more options in Component definitions.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the key and value types of its
 * maps.
 *
 * @author Michael Ludwig
 */
public class ValueMapProperty<K, V>
        implements Property<ValueMapProperty<K, V>>, Property.ValueSemantics, Property.Generic<Map<K, V>> {
    private final Constructor<? extends Map<K, V>> newMapCtor;
    private final boolean clone;
    private Map<K, V>[] data;
    private Map<K, V>[] readOnlyData;

    /**
     * Create a new ValueMapProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param mapClass   The backing Map implementation used for each component
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ValueMapProperty(Class<? extends Map> mapClass, boolean cloneValue) {
        try {
            newMapCtor = (Constructor<? extends Map<K, V>>) mapClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentDefinitionException("Requested backing Map implementation does not have a public default constructor: " +
                                                          mapClass);
        }
        clone = cloneValue;
        data = new Map[1];
        readOnlyData = new Map[1];

        // must initialize it so that the 0 index obeys value semantics
        setDefaultValue(0);
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ValueMapProperty(Collection collection, DoNotClone clone) {
        this((collection == null ? HashMap.class : collection.mapImpl()), clone == null);
    }

    /**
     * @param index The component index
     * @return The current Map as a read-only view (this mirrors any changes to the map)
     */
    public Map<K, V> get(int index) {
        return readOnlyData[index];
    }

    /**
     * Copy the given map into this property's internal map value.
     *
     * @param index The component index
     * @param map   The new Map reference
     */
    public void set(int index, Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException("Value-semantics map cannot be null");
        }
        data[index].clear();
        data[index].putAll(map);
    }

    /**
     * Put the `key`-`value` pair into the  Map at `index`.This has equivalent behavior to {@link
     * java.util.Map#put(Object, Object)}.
     *
     * @param index The component index
     * @param key   The map key
     * @param value The value to associate with the key
     * @return The previous value for the key, or null
     */
    public V put(int index, K key, V value) {
        return data[index].put(key, value);
    }

    /**
     * Get the `key`'s value from the Map at `index`. This has equivalent behavior to {@link
     * java.util.Map#get(Object)}.
     *
     * @param index The component index
     * @param key   The map key
     * @return The current map's value associated with the key
     */
    public V get(int index, K key) {
        return data[index].get(key);
    }

    /**
     * Remove `key` from the map at `index`. This has equivalent behavior to {@link
     * java.util.Map#remove(Object)}.
     *
     * @param index The component index
     * @param key   The key to remove from the current map
     * @return The element that was removed
     */
    public V remove(int index, K key) {
        return data[index].remove(key);
    }

    /**
     * Check if `key` is used as a key in the map at `index`. This has equivalent behavior to {@link
     * java.util.Map#containsKey(Object)}.
     *
     * @param index The component index
     * @param key   The key to check for
     * @return True if the current map contains the `key`
     */
    public boolean contains(int index, K key) {
        return data[index].containsKey(key);
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
        Map<K, V> t = data[indexA];
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
                data[index] = newMapCtor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error creating default value for map property", e);
            }
            readOnlyData[index] = Collections.unmodifiableMap(data[index]);
        } else {
            data[index].clear();
        }
    }

    @Override
    public void clone(ValueMapProperty<K, V> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
