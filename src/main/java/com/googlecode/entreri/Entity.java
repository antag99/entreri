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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * An Entity represents a collection of Components within an EntitySystem.
 * Entities are created by calling {@link EntitySystem#addEntity()} or the
 * similar function that takes another Entity as a template.
 * </p>
 * <p>
 * Like {@link Component}, a given instance of Entity might change its true
 * identity by having its index into the system changed. An Entity's identity is
 * determined by its id, which can be found with {@link #getId()}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Entity implements Iterable<Component> {
    private final EntitySystem system;
    int index;

    /**
     * Create an Entity that will be owned by the given system and is placed at
     * the given index.
     * 
     * @param system The owning system
     * @param index The index into the system
     */
    Entity(EntitySystem system, int index) {
        if (system == null)
            throw new NullPointerException("System cannot be null");
        if (index < 0)
            throw new IllegalArgumentException("Index must be at least 0, not: " + index);
        
        this.system = system;
        this.index = index;
    }
    
    /**
     * @return The owning EntitySystem of this entity
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * @return The unique id of this Entity, or 0 if the entity has been removed
     *         from its system
     */
    public int getId() {
        return system.getEntityId(index);
    }

    /**
     * @return True if this Entity is still in its EntitySystem, or false if it
     *         has been removed
     */
    public boolean isLive() {
        return getId() != 0;
    }

    /**
     * Get the Component instance of the given type that's attached to this
     * Entity. A null value is returned if the component type has not been
     * attached to the entity. The same Component instance is returned for a
     * given type until that component has been removed, meaning that the
     * component can be safely stored in collections.
     * 
     * @param <T> The parameterized type of Component being fetched
     * @param componentId The TypedId representing the given type
     * @return The current Component of type T attached to this container
     * @throws NullPointerException if id is null
     */
    public <T extends Component> T get(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.getComponent(index);
    }

    /**
     * <p>
     * Attach a Component of type T to this Entity. If the Entity already has
     * component of type T attached, that component is removed and a new one is
     * created. Otherwise, a new instance is created with its default values and
     * added to the system. The returned instance will be the canonical
     * component for the given type (until its removed) and can be safely stored
     * in collections.
     * </p>
     * <p>
     * Some Component types may require arguments to be properly initialized. If
     * they do, they will have been defined with the {@link InitParams}
     * annotation that describes their required arguments. If present, instances
     * of the declared types must be passed in in the var-args
     * <tt>initParams</tt>, in the order they were declared by the type. An
     * exception will be thrown if the arguments are incorrect, or if the type's
     * own validation rules fails (such as not allowing null's, etc).
     * </p>
     * <p>
     * A Component that does not require init parameters can have a 0-length or
     * null var-args passed in.
     * </p>
     * 
     * @param <T> The parameterized type of component being added
     * @param componentId The TypedId of the component type
     * @param initParams The initialization parameters required for the
     *            component type
     * @return A new component of type T
     * @throws NullPointerException if componentId is null
     * @throws IllegalArgumentException if any of the init params are invalid
     */
    public <T extends Component> T add(TypedId<T> componentId, Object... initParams) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.addComponent(index, initParams);
    }

    /**
     * Add a Component of type T to this Entity, but clone its state from the
     * existing component of type T. The existing component must still be
     * attached to an Entity other than this entity, but it could be from a
     * different EntitySystem. If there already exists a component of type T
     * added to this entity, it is removed first, and a new one is instantiated.
     * 
     * @param <T> The parameterized type of component to add
     * @param toClone The existing T to clone when attaching to this component
     * @return A new component of type T
     * @throws NullPointerException if toClone is null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Component> T add(T toClone) {
        if (toClone == null)
            throw new NullPointerException("Component template, toClone, cannot be null");
        ComponentIndex ci = system.getIndex(toClone.getTypedId());
        return (T) ci.addComponent(index, toClone);
    }

    /**
     * Remove any attached Component of the given type, T, from this Entity.
     * True is returned if a component was removed, and false otherwise. If a
     * component is removed, the component should no longer be used and it will
     * return null from {@link Component#getEntity()}.
     * 
     * @param <T> The parameterized type of component to remove
     * @param componentId The TypedId of the component type
     * @return True if a component was removed
     * @throws NullPointerException if componentId is null
     */
    public <T extends Component> boolean remove(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.removeComponent(index);
    }

    /**
     * Return an iterator over the components currently attached to the Entity.
     * The iterator supports the remove operation and will detach the component
     * from the entity. The returned components are the canonical component
     * instances for the entity and can be safely held in collections.
     * 
     * @return An iterator over the entity's components
     */
    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator(system, index);
    }
    
    @Override
    public int hashCode() {
        return getId();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Entity))
            return false;
        Entity e = (Entity) o;
        return e.system == system && e.getId() == getId();
    }
    
    /*
     * Iterator implementation that iterates over the components
     * attached to an entity, based on entity index rather than reference
     */
    private static class ComponentIterator implements Iterator<Component> {
        private final int entityIndex;
        private final Iterator<ComponentIndex<?>> indices;
        
        private ComponentIndex<?> currentIndex;
        private ComponentIndex<?> nextIndex;
        
        public ComponentIterator(EntitySystem system, int entityIndex) {
            this.entityIndex = entityIndex;
            indices = system.iterateComponentIndices();
        }
        
        @Override
        public boolean hasNext() {
            if (nextIndex == null)
                advance();
            return nextIndex != null;
        }

        @Override
        public Component next() {
            if (!hasNext())
                throw new NoSuchElementException();
            
            currentIndex = nextIndex;
            nextIndex = null;
            return currentIndex.getComponent(entityIndex);
        }

        @Override
        public void remove() {
            if (currentIndex == null)
                throw new IllegalStateException("Must call next first");
            
            if (currentIndex.removeComponent(entityIndex))
                currentIndex = null; // so next call to remove() fails
            else
                throw new IllegalStateException("Already removed");
        }
        
        private void advance() {
            while(indices.hasNext()) {
                nextIndex = indices.next();
                if (nextIndex.getComponentIndex(entityIndex) != 0)
                    break;
                else
                    nextIndex = null; // must set to null if this was last element
            }
        }
    }
}
