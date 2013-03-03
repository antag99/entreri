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
 * BooleanDataStore is an IndexedDataStore that uses boolean arrays to store multi-element
 * component data.
 *
 * @author Michael Ludwig
 */
public class BooleanDataStore extends AbstractIndexedDataStore {
    private final boolean[] array;

    /**
     * Create a new BooleanDataStore with the given number of elements per logical
     * component, and backed by the given array. The array's length must be a multiple of
     * element size.
     *
     * @param elementSize The number of elements per component
     * @param array       Backing array
     *
     * @throws IllegalArgumentException if array length is not a multiple of element size
     */
    public BooleanDataStore(int elementSize, boolean[] array) {
        super(elementSize);
        if (array.length % elementSize != 0) {
            throw new IllegalArgumentException(
                    "Array length must be a multiple of the element size");
        }
        this.array = array;
    }

    @Override
    public long memory() {
        return array.length;
    }

    @Override
    public BooleanDataStore create(int size) {
        return new BooleanDataStore(elementSize, new boolean[elementSize * size]);
    }

    @Override
    public boolean[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}