/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig (lhkbob@gmail.com)
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
package com.googlecode.entreri.property;

/**
 * IndexedDataStore is a generic data storage interface representing packed,
 * random-access data storage for a property of a Component. All Components of
 * the same type in the same EntitySystem will have their properties share the
 * IndexedDataStores so that iteration will have much better cache locality, and
 * will avoid the reorganization caused by Java's garbage collector.
 * 
 * @author Michael Ludwig
 */
public interface IndexedDataStore {
    /**
     * Resize the internal data store of this IndexedDataStore so that it holds
     * enough space for the given number of Properties that will use this data
     * store.
     * 
     * @param size The size, in number of properties
     */
    public IndexedDataStore create(int size);
    
    /**
     * @return The number of properties that can fit into this IndexedDataStore
     */
    public int size();

    /**
     * <p>
     * Copy <tt>len</tt> property values starting at <tt>srcOffset</tt> from
     * this IndexedDataStore into <tt>dest</tt>, placing the first property's
     * values at <tt>destOffset</tt>. Both <tt>srcOffset</tt> and
     * <tt>destOffset</tt> are in units of property, and not any underlying
     * array.
     * </p>
     * <p>
     * An exception should be thrown if the destination IndexedDataStore is not
     * of the same type, or is not compatible with this IndexedDataStore.
     * </p>
     * 
     * @param srcOffset The offset of the first property to copy
     * @param len The number of properties to copy
     * @param dest The destination IndexedDataStore
     * @param destOffset The offset into dest to place the first property's
     *            values
     * @throws IndexOutOfBoundsException if the offsets or lengths cause
     *             out-of-bounds exceptions
     */
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset);
}
