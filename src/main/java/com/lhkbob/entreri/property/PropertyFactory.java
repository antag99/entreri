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

import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.annot.Factory;

/**
 * <p>
 * A PropertyFactory is a simple factory that can be used to create Property
 * instances. Additionally, it is used when decorating a ComponentData type in
 * an EntitySystem to ensure that each decoration event gets a unique property
 * instance.
 * </p>
 * <p>
 * To be used with the {@link Factory} annotation when defining a ComponentData
 * type, the PropertyFactory must have an accessible, no-argument constructor.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The Property type created
 */
public interface PropertyFactory<T extends Property> {
    /**
     * Return a new Property instance. This must be a new instance that has not
     * been returned previously or the entity framework will have undefined
     * behavior.
     * 
     * @return A new Property of type T
     */
    public T create();

    /**
     * Set the default value that the component at the specified <tt>index</tt>
     * will see before it's init() method is invoked. In some cases, this could
     * be used in-place of initializing in init() method.
     * 
     * @param property The property whose value will be updated
     * @param index The component index to be updated
     */
    public void setDefaultValue(T property, int index);

    /**
     * Copy the value from <tt>src</tt> at component index, <tt>srcIndex</tt> to
     * <tt>dst</tt> at <tt>dstIndex</tt>. This is used when a component is
     * created and cloned from a template with
     * {@link Entity#add(com.lhkbob.entreri.Component)}. For many cases a
     * plain copy-by-value or copy-by-reference is sufficient, but some
     * component types might require more complicated cloning rules.
     * 
     * @param src The source property that is being cloned
     * @param srcIndex The index into src of the component being cloned
     * @param dst The destination property created from the template
     * @param dstIndex The index into dst of the component being created
     */
    public void clone(T src, int srcIndex, T dst, int dstIndex);
}
