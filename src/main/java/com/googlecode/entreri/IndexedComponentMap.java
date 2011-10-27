/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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

/**
 * IndexedComponentMap is a simple wrapper around accessing a structured
 * Component array. It provides typed access and a read-only view over the
 * source of Components. It's primary purpose is for use with the multi-type
 * Component queries in EntitySystem.
 * 
 * @author Michael Ludwig
 */
public class IndexedComponentMap {
    private final Component[] components;

    /**
     * Create an IndexedComponentMap that will take its components from the
     * given array. The Components do not need to be in any particular order,
     * and the array can be mutated after construction to change the components
     * reported by the map. The array must never contain null elements, unless
     * it is before the map is exposed to the user.
     * 
     * @param components The array of components stored in this "map"
     * @throws NullPointerException if components is null
     */
    public IndexedComponentMap(Component[] components) {
        if (components == null)
            throw new NullPointerException("Components array cannot be null");
        
        this.components = components;
    }

    /**
     * Get the Component of type T stored in this map. This performs a linear
     * scan over the components stored so it is not particular efficient,
     * although the it is unlikely that IndexedComponentMap's will get very
     * large. If possible, use {@link #get(TypedId, int)} instead.
     * 
     * @param <T> The Component type queried
     * @param id The TypedId for the component type
     * @return The Component of type T in this map, or null if it does not exist
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(TypedId<T> id) {
        for (int i = 0; i < components.length; i++) {
            if (components[i].getTypedId().equals(id))
                return (T) components[i];
        }
        
        return null;
    }

    /**
     * <p>
     * Get the Component of type T that is stored at the given <tt>index</tt>.
     * The index refers to the index into the Component array this map was
     * created with. Generally, providers of IndexedComponentMap have some
     * guarantee over where the components are assigned so that this method can
     * be used to get constant-time, typed lookup for a component query.
     * </p>
     * <p>
     * As an example, {@link EntitySystem#iterator(TypedId...)} stores the
     * Component instances in the same order as the TypedIds were specified in
     * the method call.
     * </p>
     * 
     * @param <T> The Component type queried
     * @param id The TypedId for the component type
     * @param index The expected index the component should be at
     * @return The Component at the given index, will not be null
     * @throws NullPointerException if id is null
     * @throws IllegalArgumentException if the component at <tt>index</tt> does
     *             not have the same type as <tt>id</tt>
     * @throws IndexOutOfBoundsException if index is less than 0 or greater than
     *             or equal to {@link #size()}
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(TypedId<T> id, int index) {
        Component c = components[index];
        if (!c.getTypedId().equals(id))
            throw new IllegalArgumentException("Component is not expected type, index=" + index + ", expected=" + id.getType() + ", actual=" + c.getClass());
        return (T) c;
    }
    
    /**
     * @return The number of Components in this map
     */
    public int size() {
        return components.length;
    }
}
