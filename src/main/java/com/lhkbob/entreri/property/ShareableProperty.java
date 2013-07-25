/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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
 * ShareableProperty designates a special type of property that can mutate a shared instance to a specific
 * component's value, instead of returning internal references. Often, shareable properties are capable of
 * actually unpacking the type into primitive arrays and restoring the value into the shared instance for
 * improved cache coherence.
 * <p/>
 * Because simple primitives cannot be shared, ShareableProperty declares the required methods using
 * generics.
 *
 * @param <T> The type stored by the property
 *
 * @author Michael Ludwig
 */
public interface ShareableProperty<T> extends Property {
    /**
     * Create a new instance of type T for use by components that need a shareable instance to pass into
     * {@link #get(int, Object)}.
     *
     * @return A new instance of type T
     */
    public T createShareableInstance();

    /**
     * Get the property value at <var>index</var>, but instead of returning a new instance or the value, the
     * <var>result</var> parameter is mutated to equal the property value. It can be assumed the instance was
     * previously created by a call to {@link #createShareableInstance()}.
     *
     * @param index  The index to access
     * @param result The instance of T to modify
     */
    public void get(int index, T result);
}
