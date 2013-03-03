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
package com.lhkbob.entreri;

import com.lhkbob.entreri.property.FloatProperty;

/**
 * <p/>
 * Property represents a generic field or property of a ComponentData definition. It's
 * interface allows it's values and underlying data store to be packed together with the
 * corresponding property instances of all the other Components of the same type in an
 * EntitySystem.
 * <p/>
 * This is an approach to mapped-objects where Components can be mapped onto primitive
 * arrays so that iteration sees optimal cache locality. As an example, there could be two
 * instances of type A, with properties a and b. The two 'a' properties would share the
 * same data store, and the two 'b' properties would share a separate store.
 * <p/>
 * Property instances are carefully managed by an EntitySystem. There is ever only one
 * property instance per defined property in a component type for a system. Every
 * component of that type uses their index into the property's IndexedDataStore to access
 * their data. This helps keep memory usage low and simplifies system maintenance.
 * <p/>
 * Property instances are created by {@link PropertyFactory PropertyFactories}, which are
 * created by {@link ComponentDataFactory ComponentDataFactories}. ComponentDataFactory
 * implementations define how property factories are declared.
 *
 * @author Michael Ludwig
 * @see ReflectionComponentDataFactory
 */
public interface Property {
    /**
     * <p/>
     * Return the IndexedDataStore holding this property's values. The data store may also
     * hold other property values if the owning ComponentData is in an EntitySystem with
     * many other components of the same type.
     * <p/>
     * This should not be used by ComponentData implementations, and manipulating the
     * IndexedDataStore outside of the EntitySystem code could cause unexpected behavior.
     * Instead Property implementations should expose other ways to access their data; as
     * an example see {@link FloatProperty#getIndexedData()}.
     * <p/>
     * The returned data store must always have at least 1 element in it.
     *
     * @return The current IndexedDataStore used by the property
     */
    public IndexedDataStore getDataStore();

    /**
     * <p/>
     * Assign a new IndexedDataStore to this Property. If the old values for this Property
     * were not copied out of its previous IndexedDataStore into the new one, this
     * assignment will change the apparent value of this property.
     * <p/>
     * This should only be called internally by the EntitySystem. Calling it within a
     * ComponentData implementation or otherwise will result in undefined consequences.
     * <p/>
     * <p/>
     * It can be assumed that the new store is not null.
     *
     * @param store The new IndexedDataStore
     */
    public void setDataStore(IndexedDataStore store);
}
