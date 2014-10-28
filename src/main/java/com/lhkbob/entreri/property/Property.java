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

/**
 * Property
 * ========
 *
 * Property represents a generic field or property of a {@link com.lhkbob.entreri.Component} definition. A
 * component can have multiple properties. A single Property instance holds the corresponding values for every
 * component of that type in an EntitySystem. It is effectively an indexed map from component index to
 * property value.
 *
 * This is an approach to mapped-objects where Components can be mapped onto primitive arrays so that
 * iteration sees optimal cache locality. As an example, there could be two instances of type A, with
 * properties a and b. The two 'a' references would share the same data store, and the two 'b' references
 * would share another store.
 *
 * All property implementations must expose two methods:
 *
 * 1. `T get(int)` - which returns the value of type `T` at the component index.
 * 2. `void set(int, T)` - which stores the value of type `T` at the particular component index.
 *
 * To support primitives without boxing, `T` need not be a subclass of Object are not part of the interface
 * definition that would require generics.  They are are required and any attempts to use a Property
 * implementation without them will throw an exception. The exposed get() and set() methods, and potentially a
 * bulk accessor (such as returning the underlying array) are the supported methods for manipulating decorated
 * property values.
 *
 * Property instances are carefully managed by an EntitySystem. There is ever only one property instance per
 * defined property in a component type for a system. Property instances are created by {@link PropertyFactory
 * PropertyFactories}. Every concrete Property class must be annotated with {@link Factory} to specify the
 * PropertyFactory class that constructs it. That PropertyFactory must expose a no-argument constructor, or a
 * constructor that takes an {@link Attributes} instance as its only argument.
 *
 * @author Michael Ludwig
 */
public interface Property {
    /**
     * Resize the internal storage to support indexed lookups from 0 to <code>size - 1</code>.  If
     * `size` is less than the current capacity, all previous values with an index less than
     * `size` must be preserved, and the remainder are discarded.  If `size` is greater than
     * the current capacity, all previous indexed values must be preserved and the new values can be
     * undefined.
     *
     * This is for internal use *only*, and should not be called on decorated properties returned by
     * {@link com.lhkbob.entreri.EntitySystem#decorate(Class, PropertyFactory)}.
     *
     * @param size The new capacity, will be at least 1
     */
    public void setCapacity(int size);

    /**
     * Get the current capacity of the property. All instances must start with a capacity of at least 1.
     * The capacity represents the number of component instances the property can handle. Even if component's
     * property value is represented as multiple consecutive primitives, that is still a single value instance
     * when considering the capacity an index supplied to `get()` or `set()`.
     *
     * This is intended for internal use but provides an upper limit on valid indices for access purposes.
     * It will be at least the actual number of component instances of the associated type.
     *
     * @return The current capacity of the property
     */
    public int getCapacity();

    /**
     * Swap the value at `indexA` with `indexB`.
     *
     * This is for internal use *only*, and should not be called on decorated properties returned by
     * {@link com.lhkbob.entreri.EntitySystem#decorate(Class, PropertyFactory)}.
     *
     * @param indexA The index of the first value
     * @param indexB The index of the second value
     */
    public void swap(int indexA, int indexB);
}
