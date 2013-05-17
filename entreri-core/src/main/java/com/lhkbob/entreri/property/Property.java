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

/**
 * <p/>
 * Property represents a generic field or property of a Component definition. A component
 * can have multiple properties. A single property instance holds the corresponding values
 * for every component of that type in an EntitySystem. It is effectively an indexed map
 * from component index to property value.
 * <p/>
 * This is an approach to mapped-objects where Components can be mapped onto primitive
 * arrays so that iteration sees optimal cache locality. As an example, there could be two
 * instances of type A, with properties a and b. The two 'a' properties would share the
 * same data store, and the two 'b' properties would share another store.
 * <p/>
 * All property implementations must expose two methods: <code>T get(int)</code> and
 * <code>void set(int, T)</code> to get and set values at a particular index. To support
 * primitives without boxing, they are not part of the interface definition but are
 * required. The exposed get() and set() methods, and potentially a bulk accessor (such as
 * returning the underlying array) are the supported methods for manipulating decorated
 * property values.
 * <p/>
 * Property instances are carefully managed by an EntitySystem. There is ever only one
 * property instance per defined property in a component type for a system. Property
 * instances are created by {@link PropertyFactory PropertyFactories}. Every concrete
 * Property class must be annotated with {@link Factory} to specify the PropertyFactory
 * class that constructs it. That PropertyFactory must expose a no-argument constructor,
 * or a constructor that takes an {@link Attributes} instance as its only argument.
 *
 * @author Michael Ludwig
 */
public interface Property {
    /**
     * Resize the internal storage to support indexed lookups from 0 to <code>size -
     * 1</code>.  If <var>>size</var> is less than the current capacity, all previous
     * values with an index less than <var>size</var> must be preserved, and the remainder
     * are discarded.  If <var>size</var> is greater than the current capacity, all
     * previous indexed values must be preserved and the new values can be undefined.
     * <p/>
     * This is for internal use only to manage, and should not be called on decorated
     * properties returned by {@link com.lhkbob.entreri.EntitySystem#decorate(Class,
     * PropertyFactory)}.
     *
     * @param size The new capacity, will be at least 1
     */
    public void setCapacity(int size);

    /**
     * Get the current capacity of the property. All instances must start with a capacity
     * of at least 1.  The capacity represents the number of component instances the
     * property can handle. If a component property is decomposed into multiple primitives
     * that is still a single value instance when considering the capacity.
     * <p/>
     * This is for internal use only to manage, and should not be called on decorated
     * properties returned by {@link com.lhkbob.entreri.EntitySystem#decorate(Class,
     * PropertyFactory)}.
     *
     * @return The current capacity of the property
     */
    public int getCapacity();

    /**
     * Swap the value at <var>indexA</var> with <var>indexB</var>.
     * <p/>
     * This is for internal use only to manage, and should not be called on decorated
     * properties returned by {@link com.lhkbob.entreri.EntitySystem#decorate(Class,
     * PropertyFactory)}.
     *
     * @param indexA The index of the first value
     * @param indexB The index of the second value
     */
    public void swap(int indexA, int indexB);
}
