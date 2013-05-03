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

import java.util.Arrays;
import java.util.Iterator;

/**
 * <p/>
 * ComponentIterator is an {@link Iterator}-like class used for quickly iterating over
 * subsets of entities within an EntitySystem with direct access to the components you're
 * interested in. You specify the component classes using {@link #addRequired(Class)} and
 * {@link #addOptional(Class)}, which return flyweight component instances that are
 * updated as the iterator is looped through. These determine the constraints on the
 * entities reported by the iterator. ComponentIterator is reset-able, so the same
 * instance and created components can be reused for multiple iterations.
 * <p/>
 * The ComponentIterator will skip all entities that do not have components of the
 * required component types. This is very useful in Tasks because often there related
 * component types that you want to fetch and process at the same time. Optional
 * Component's will be set to the entity if present otherwise they'll be marked as dead.
 * <p/>
 * The basic workflow for using a ComponentIterator is shown below. In the example, the
 * iterator is used to iterate over all entities that have both an A and a B component,
 * while optionally loading a C component if available.
 * <pre>
 * // create iterator
 * ComponentIterator it = new ComponentIterator(system);
 *
 * // create flyweight components for data access
 * A cdA = it.addRequired(A.class);
 * B cdB = it.addRequired(B.class);
 * C cdC = it.addOptional(C.class);
 *
 * // iterate
 * it.reset(); // not actually required for first iteration
 * while(it.next()) {
 *    // cdA and cdB are both assigned to components of type A and B on the same
 *    // entity, so they can be processed without checking isAlive()
 *    ...
 *    // cdC may or may not be valid
 *    if (cdC.isAlive()) {
 *       // the current entity also has a component of type C
 *    }
 * }
 * </pre>
 *
 * @author Michael Ludwig
 */
public class ComponentIterator {
    private final EntitySystem system;

    private int index;

    private AbstractComponent<?>[] required; // all required except primary
    private AbstractComponent<?>[] optional;

    private AbstractComponent<?> primary;

    /**
     * Create a new ComponentIterator that will iterate over components within the given
     * EntitySystem. It is initialized with no required or optional components, but at
     * least one required component must be added before it can be iterated over.
     *
     * @param system The EntitySystem of the iterator
     *
     * @throws NullPointerException if system is null
     */
    public ComponentIterator(EntitySystem system) {
        if (system == null) {
            throw new NullPointerException("System cannot be null");
        }
        this.system = system;
        required = new AbstractComponent<?>[0];
        optional = new AbstractComponent<?>[0];
        primary = null;
        index = 0;
    }

    /**
     * Add the given Component type as a required component for this iterator. The
     * returned flyweight instance must be used to access the data for the component at
     * each iteration. Every call to {@link #next()} will update its identity to be the
     * corresponding component of type T whose entity has all required components.
     *
     * @return A flyweight instance to access the current values for the component type
     *
     * @throws NullPointerException if type is null
     */
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

            if (data.getRepository().getMaxComponentIndex() <
                primary.getRepository().getMaxComponentIndex()) {
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

    /**
     * Add the given Component type as an optional component for this iterator. The
     * returned flyweight instance must be used to access the data for the component at
     * each iteration. Every call to {@link #next()} will update its identity to be the
     * corresponding component of type T if the entity has a component of type T.
     * <p/>
     * If it does not then the component will be marked as dead until a future iteration
     * has a T component
     *
     * @return A flyweight instance to access the current values for the component type
     *
     * @throws NullPointerException if type is null
     */
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

    /**
     * <p/>
     * Advance the iterator to the next Entity that has components of all required types.
     * Every flyweight component returned by previous calls to {@link #addRequired(Class)}
     * will be updated to point to that entity. The optional flyweight components will be
     * updated to the entity if a component exists. They may not be, in which case they
     * will be flagged as invalid.
     * <p/>
     * It can be assumed that when an Entity is found that all required components are
     * valid and reference that entity's components of the appropriate type.
     * <p/>
     * True is returned if an Entity was found. False is returned if there were no more
     * entities matching the requirements in the system. Additionally, if there has been
     * no call to {@link #addRequired(Class)} false is always returned. The iterator must
     * be constrained by at least one required type.
     *
     * @return True if another entity was found and the required components (and any
     *         present optional components) have been updated to that entity
     */
    public boolean next() {
        if (primary == null) {
            return false;
        }

        boolean found;
        int entity;
        int component;
        int count = primary.getRepository().getMaxComponentIndex();
        while (index < count - 1) {
            index++; // always increment one

            found = true;
            entity = primary.getRepository().getEntityIndex(index);
            if (entity != 0) {
                // we have a possible entity candidate
                primary.setIndex(index);
                for (int i = 0; i < required.length; i++) {
                    component = required[i].getRepository().getComponentIndex(entity);
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
                        component = optional[i].getRepository().getComponentIndex(entity);
                        optional[i].setIndex(component);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Reset this ComponentIterator to the beginning of the system to perform another
     * complete iteration. This does not change the identities of the created flyweight
     * components until the next call to {@link #next()} is made, at which point they are
     * updated to first valid matching components.
     */
    public void reset() {
        index = 0;
    }
}
