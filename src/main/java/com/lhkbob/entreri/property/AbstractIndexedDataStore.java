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

import java.nio.Buffer;
import java.util.List;

import com.lhkbob.entreri.IndexedDataStore;


/**
 * <p>
 * AbstractIndexedDataStore is an implementation of IndexedDataStore that uses
 * an array to hold the packed property values of the store. It implements the
 * vast majority of the logic needed for an IndexedDataStore, and concrete
 * classes are only required to create and store the arrays.
 * <p>
 * An AbstractIndexedDataStore instance will only have one array in its
 * lifetime.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractIndexedDataStore implements IndexedDataStore {
    protected final int elementSize;

    /**
     * Create an AbstractIndexedDataStore that will use <var>elementSize</var>
     * array elements per ComponentData in the data store. This does not create a
     * backing array, so concrete classes must allocate an initial array.
     * 
     * @param elementSize The number of array elements per property instance
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public AbstractIndexedDataStore(int elementSize) {
        if (elementSize < 1)
            throw new IllegalArgumentException("Element size must be at least 1");
        this.elementSize = elementSize;
    }

    @Override
    public int size() {
        return getArrayLength() / elementSize;
    }

    @Override
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
        if (dest == null)
            throw new NullPointerException("Destination store cannot be null");
        if (!(getClass().isInstance(dest)))
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
        
        AbstractIndexedDataStore dstStore = (AbstractIndexedDataStore) dest;
        if (dstStore.elementSize != elementSize)
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
        
        arraycopy(getArray(), srcOffset * elementSize, dstStore.getArray(), destOffset * elementSize, len * elementSize);
    }

    /**
     * <p>
     * Copy <tt>len</tt> elements of <tt>oldArray</tt> starting at
     * <tt>srcOffset</tt> into <tt>newArray</tt> at <tt>dstOffset</tt>. The
     * default implementation uses
     * {@link System#arraycopy(Object, int, Object, int, int)}, which is
     * suitable unless the backing data types are not primitive Java arrays.
     * </p>
     * <p>
     * This can be overridden if the backing data is some other type, such as a
     * {@link List} or {@link Buffer}, in which case the "array copy" can be
     * simulated in this method.
     * </p>
     * 
     * @param oldArray The source array
     * @param srcOffset The element offset into the source array
     * @param newArray The destination array where data is copied
     * @param dstOffset The element offset into the new array
     * @param len The number of array elements to copy
     */
    protected void arraycopy(Object oldArray, int srcOffset, Object newArray, int dstOffset, int len) {
        System.arraycopy(oldArray, srcOffset, newArray, dstOffset, len);
    }
    
    /**
     * @return The array instance storing property data
     */
    protected abstract Object getArray();

    /**
     * @return The length of the array backing this data store
     */
    protected abstract int getArrayLength();
}
