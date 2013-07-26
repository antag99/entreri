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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;

import java.util.Arrays;

/**
 * Implementation of ComponentIterator used by EntitySystemImpl.
 *
 * @author Michael Ludwig
 */
public class SystemComponentIteratorImpl implements ComponentIterator {
    private final EntitySystemImpl system;

    private int index;

    private AbstractComponent<?>[] required; // all required except primary
    private AbstractComponent<?>[] optional;

    private AbstractComponent<?> primary;

    /**
     * Create a new ComponentIterator that will iterate over components within the given EntitySystem. It is
     * initialized with no required or optional components, but at least one required component must be added
     * before it can be iterated over.
     *
     * @param system The EntitySystem of the iterator
     *
     * @throws NullPointerException if system is null
     */
    public SystemComponentIteratorImpl(EntitySystemImpl system) {
        if (system == null) {
            throw new NullPointerException("System cannot be null");
        }
        this.system = system;
        required = new AbstractComponent<?>[0];
        optional = new AbstractComponent<?>[0];
        primary = null;
        index = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addRequired(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Component type cannot be null");
        }
        AbstractComponent<T> data = system.getRepository(type).createDataInstance();

        // check to see if the data should be the new primary
        if (primary == null) {
            // no other required components, so just set it
            primary = data;
        } else {
            // check if the new data is shorter, but we will definitely
            // putting one data into the required array
            required = Arrays.copyOf(required, required.length + 1);

            if (data.owner.getMaxComponentIndex() < primary.owner.getMaxComponentIndex()) {
                // new primary
                required[required.length - 1] = primary;
                primary = data;
            } else {
                // not short enough so store it in the array
                required[required.length - 1] = data;
            }
        }

        return (T) data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addOptional(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Component type cannot be null");
        }

        AbstractComponent<T> data = system.getRepository(type).createDataInstance();

        // add the data to the optional array
        optional = Arrays.copyOf(optional, optional.length + 1);
        optional[optional.length - 1] = data;

        return (T) data;
    }

    @Override
    public boolean next() {
        if (primary == null) {
            return false;
        }

        boolean found;
        int entity;
        int component;
        int count = primary.owner.getMaxComponentIndex();
        while (index < count - 1) {
            index++; // always increment one

            found = true;
            entity = primary.owner.getEntityIndex(index);
            if (entity != 0) {
                // we have a possible entity candidate
                primary.setIndex(index);
                for (int i = 0; i < required.length; i++) {
                    component = required[i].owner.getComponentIndex(entity);
                    if (component == 0) {
                        found = false;
                        break;
                    } else {
                        required[i].setIndex(component);
                    }
                }

                if (found) {
                    // we have satisfied all required components,
                    // so now set all optional requirements as well
                    for (int i = 0; i < optional.length; i++) {
                        component = optional[i].owner.getComponentIndex(entity);
                        optional[i].setIndex(component);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void reset() {
        index = 0;
    }
}
