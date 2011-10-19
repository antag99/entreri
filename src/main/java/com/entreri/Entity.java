package com.entreri;

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
     * @param id The TypedId representing the given type
     * @return The current Component of type T attached to this container
     * @throws NullPointerException if id is null
     */
    public <T extends Component> T get(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.getComponent(index);
    }

    /**
     * Attach a Component of type T to this Entity. If the Entity already has
     * component of type T attached, that component is returned unmodified.
     * Otherwise, a new instance is created with its default values and added to
     * the system. The returned instance will be the canonical component for the
     * given type (until its removed) and can be safely stored in collections.
     * 
     * @param <T> The parameterized type of component being added
     * @param componentId The TypedId of the component type
     * @return A new component of type T, or an existing T if already attached
     * @throws NullPointerException if componentId is null
     */
    public <T extends Component> T add(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.addComponent(index, null);
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
