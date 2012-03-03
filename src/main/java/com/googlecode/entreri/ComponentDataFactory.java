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
package com.googlecode.entreri;

import java.util.Map;

import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * ComponentDataFactory is a factory interface used to create instances of
 * ComponentData to provide a flexible means to instantiate and configure
 * ComponentData's for a specific type of component. For most purposes, the
 * default {@link ReflectionComponentDataFactory} will be sufficient, unless the
 * conventions enforced by it are too restrictive.
 * </p>
 * <p>
 * The main purpose of the ComponentDataFactory is to act as the glue between
 * the set of Properties representing all of the components in a system, and the
 * ComponentData instances used to efficiently access their values in a clean
 * manner.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The type of ComponentData created by the factory
 */
public interface ComponentDataFactory<T extends ComponentData<T>> {
    /**
     * <p>
     * Return the named PropertyFactories that can be used to create all
     * required Properties when configuring a new instance. The key of the map
     * should be the same key that is specified when
     * {@link #setProperty(ComponentData, Object, Property)} is called.
     * </p>
     * <p>
     * An example of the keys might be the field names declared in the class.
     * </p>
     * 
     * @return The PropertyFactories created by this builder
     */
    public Map<?, PropertyFactory<?>> getPropertyFactories();

    /**
     * Construct a new instance of T that has not been configured. This means it
     * should not have any assigned properties, and to expect subsequent calls
     * to {@link #setProperty(ComponentData, Object, Property)} to configure it.
     * 
     * @return A new instance
     */
    public T createInstance();

    /**
     * Inject the given property into the instance, where the property is
     * assumed to have been constructed by PropertyFactory from
     * {@link #getPropertyFactories()} that is stored by <tt>key</tt>.
     * 
     * @param instance The instance to configure
     * @param key The key to the creating PropertyFactory or source of the
     *            property
     * @param property The property instance to inject
     * @throws NullPointerException if any argument is null
     */
    public void setProperty(T instance, Object key, Property property);
}
