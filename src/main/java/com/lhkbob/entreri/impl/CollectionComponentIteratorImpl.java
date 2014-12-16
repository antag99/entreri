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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;

import java.util.Arrays;
import java.util.Iterator;

/**
 * CollectionComponentIteratorImpl
 * ===============================
 *
 * A ComponentIterator implementation that iterates over an Iterable of entities. Each time it is reset it
 * gets a new iterator from the Iterable.
 *
 * @author Michael Ludwig
 */
public class CollectionComponentIteratorImpl implements ComponentIterator {
    private final EntitySystemImpl system;
    private Iterable<Entity> entities;

    private AbstractComponent<?>[] required; // all required except primary
    private AbstractComponent<?>[] optional;

    private Iterator<Entity> currentIterator;

    /**
     * Create a new iterator that runs over the data in `system`, but restricted to the entities
     * returned by the given Iterable.
     *
     * @param system   The system owning the entities
     * @param entities The ordered restriction of entities to iterate over
     */
    public CollectionComponentIteratorImpl(EntitySystemImpl system, Iterable<Entity> entities) {
        if (entities == null) {
            throw new NullPointerException("Entity collection cannot be null");
        }
        this.entities = entities;
        this.system = system;

        required = new AbstractComponent<?>[0];
        optional = new AbstractComponent<?>[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addRequired(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Component type cannot be null");
        }

        AbstractComponent<T> data = system.getRepository(type).createDataInstance();

        // add the data to the required array
        required = Arrays.copyOf(required, required.length + 1);
        required[required.length - 1] = data;

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
        if (currentIterator == null) {
            currentIterator = entities.iterator();
        }

        int entityIndex;
        int component;
        boolean found;
        while (currentIterator.hasNext()) {
            entityIndex = ((EntityImpl) currentIterator.next()).index;

            found = true;
            for (int i = 0; i < required.length; i++) {
                component = required[i].owner.getComponentIndex(entityIndex);
                if (component == 0) {
                    found = false;
                    break;
                } else {
                    required[i].setIndex(component);
                }
            }

            if (found) {
                // valid entity
                for (int i = 0; i < optional.length; i++) {
                    component = optional[i].owner.getComponentIndex(entityIndex);
                    optional[i].setIndex(component);
                }

                return true;
            }
        }

        // if we've run out of entities, we don't have anymore
        return false;
    }

    @Override
    public void reset() {
        currentIterator = null;
    }
}
