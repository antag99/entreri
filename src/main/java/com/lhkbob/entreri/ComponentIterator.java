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
 * subsets of entities within an EntitySystem. Required and optional ComponentData
 * instances are added to it during initialization. These then determine the constraints
 * on the entities reported by the iterator. ComponentIterator is reset-able, so the same
 * instance can be reused for multiple iterations.
 * <p/>
 * The ComponentIterator will skip all entities that do not have components of the
 * required component types. This is very useful for Controllers because they often have
 * related component types that they want to fetch at the same time. Optional
 * ComponentData's will be updated if present, and will otherwise be flagged as invalid.
 * <p/>
 * The basic workflow for using a ComponentIterator is shown below. In the example, the
 * iterator is used to iterate over all entities that have both an A and a B component,
 * while optionally loading a C component if available. <p/>
 * <pre>
 * // create ComponentData instances
 * A cdA = system.createDataInstance(TypeId.get(A.class));
 * B cdB = system.createDataInstance(TypeId.get(B.class));
 * C cdC = system.createDataInstance(TypeId.get(C.class));
 *
 * // initialize iterator
 * ComponentIterator it = new ComponentIterator(system);
 * it.addRequired(cdA).addRequired(cdB).addOptional(cdC);
 * it.reset(); // not actually required for first iteration
 *
 * // iterate
 * while(it.next()) {
 *    // cdA and cdB are both assigned to components of type A and B on the same
 *    // entity, so they can be processed without checking isValid()
 *    ...
 *    // cdC may or may not be valid
 *    if (cdC.isValid()) {
 *       // the current entity also has a component of type C
 *    }
 * }
 * </pre>
 * <p/>
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
     * Create a new ComponentIterator that will iterate over components or entities within
     * the given EntitySystem. It will have no attached required or optional
     * ComponentDatas.
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
     * Add the given ComponentData instance as a required component type for this
     * iterator. Besides implicitly specifying the required type, the provided instance
     * will be set to each component of that type during iteration. Thus it is recommended
     * to hold onto instance for later use.
     *
     * @return A flyweight instance to access the current values for the component type
     *
     * @throws NullPointerException if type is null
     */
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
     * <p/>
     * Add the given ComponentData instance as an optional component type for this
     * iterator. Besides implicitly specifying the required type, the provided instance
     * will be set to each component of that type during iteration. Thus it is recommended
     * to hold onto instance for later use.
     * <p/>
     *
     * @return A flyweight instance to access the current values for the component type
     *
     * @throws NullPointerException if type is null
     */
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
     * All ComponentData's of the required types will be updated to access the components
     * of the next entity. Optional ComponentData's will be updated to those components of
     * the entity's, if they are attached. They may not be, in which case they will be
     * flagged as invalid.
     * <p/>
     * It can be assumed that when an Entity is found that all required ComponentData's
     * are valid and reference that entity's components of the appropriate type.
     * <p/>
     * True is returned if an Entity was found. False is returned if there were no more
     * entities matching the requirements in the system. Additionally, if there are no
     * required ComponentData's, then false is always returned. The iterator must be
     * constrained by at least one required type.
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
     * complete iteration.
     */
    public void reset() {
        index = 0;
    }
}
