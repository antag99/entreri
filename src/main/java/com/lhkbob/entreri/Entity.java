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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * An Entity represents a collection of Components within an EntitySystem.
 * Entities are created by calling {@link EntitySystem#addEntity()} or the
 * similar function that takes another Entity as a template.
 * </p>
 * <p>
 * Entities use instance equality, just like {@link Component}. Once created the
 * Entity object will not change its identity.
 * </p>
 * <p>
 * Entity implements both {@link Ownable} and {@link Owner}. This can be used to
 * create hierarchies of both components and entities that share a lifetime.
 * When an entity is removed from the system, all of its owned objects are
 * disowned. If any of them were entities or components, they are also removed
 * from the system.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Entity implements Iterable<Component<?>>, Comparable<Entity>, Ownable, Owner {
    private final EntitySystem system;
    private final int id;

    final OwnerSupport delegate;

    int index;

    /**
     * Create an Entity that will be owned by the given system and is placed at
     * the given index.
     * 
     * @param system The owning system
     * @param index The index into the system
     * @param id The unique id of the entity in the system
     */
    Entity(EntitySystem system, int index, int id) {
        if (system == null) {
            throw new NullPointerException("System cannot be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index must be at least 0, not: " + index);
        }

        this.system = system;
        this.index = index;
        this.id = id;

        delegate = new OwnerSupport(this);
    }

    /**
     * @return The unique (in the scope of the entity system) id of this entity
     */
    public int getId() {
        return id;
    }

    /**
     * @return The owning EntitySystem of this entity
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * @return True if this Entity is still in its EntitySystem, or false if it
     *         has been removed
     */
    public boolean isLive() {
        return index != 0;
    }

    /**
     * <p>
     * Get the Component instance of the given type that's attached to this
     * Entity. A null value is returned if the component type has not been
     * attached to the entity, or if the component is disabled.
     * </p>
     * <p>
     * If the entity has a component of the given type, but it has been marked
     * as disabled, this will return null. Use {@link #get(Class, boolean)} to
     * override this behavior and return disabled components.
     * </p>
     * 
     * @param <T> The parameterized type of ComponentData of the component
     * @param componentType The given type
     * @return The current Component of type T attached to this entity
     * @throws NullPointerException if id is null
     */
    public <T extends ComponentData<T>> Component<T> get(Class<T> componentType) {
        return get(componentType, false);
    }

    /**
     * Get the Component instance of the given type that's attached to this
     * entity. If <tt>ignoreEnable</tt> is true, then disabled components will
     * be returned as well.
     * 
     * @param <T> The parameterized type of ComponentData of the component
     * @param componentType The data type
     * @param ignoreEnable True if disabled components should be returned as
     *            well
     * @return The current Component of type T attached to this entity
     * @throws NullPointerException if componentType is null
     */
    public <T extends ComponentData<T>> Component<T> get(Class<T> componentType,
                                                         boolean ignoreEnable) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        Component<T> c = ci.getComponent(ci.getComponentIndex(index));

        if (c == null || ignoreEnable || c.isEnabled()) {
            return c;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Get the component of type T attached to this entity, by setting the given
     * data's reference. This will return true if the data instance has been set
     * to a valid component and that component is enabled. This is a shortcut
     * for:
     * 
     * <pre>
     * Component&lt;T&gt; c = entity.get(TypeId.get(T.class));
     * if (c != null &amp;&amp; c.isEnabled()) {
     *     if (data.set(c)) {
     *         // process c via data
     *     }
     * }
     * </pre>
     * 
     * </p>
     * <p>
     * Note that there is no equivalent {@link #get(TypeId, boolean)} that takes
     * a ComponentData. This is because the data instance will still be set to a
     * valid component even when it's disabled. It is possible to identify this
     * case if get() returns false but isValid() returns true.
     * </p>
     * 
     * @param <T> The component data type
     * @param data An instance of the data that will be set to this entity's
     *            component
     * @return True if the data now references a valid and enabled component,
     *         and false if the component is invalid or if the component is
     *         disabled
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if the data was created by another
     *             entity system
     */
    public <T extends ComponentData<T>> boolean get(T data) {
        if (data.owner.getEntitySystem() != getEntitySystem()) {
            throw new IllegalArgumentException("ComponentData was not created by expected EntitySystem");
        }

        ComponentRepository<T> ci = data.owner;
        int componentIndex = ci.getComponentIndex(index);
        return data.setFast(componentIndex) && ci.isEnabled(componentIndex);
    }

    /**
     * <p>
     * Get the component of type T attached to this entity only if the entity
     * has a component of type T, it is enabled, and it's version is different
     * than <tt>priorVersion</tt>. This method is a convenience method to easily
     * retrieve the component data only if the data has changed.
     * <p>
     * If the entity has a component of type T, that is enabled, and has a
     * version not equal to <tt>priorVersion</tt>, then <tt>data</tt> will be
     * set to access the component and true will be returned. In any other case,
     * false is returned. Unlike {@link #get(ComponentData)}, it's not possible
     * to inspect the data instance to determine why false was returned.
     * 
     * @param <T> The component data type
     * @param data The data instance used to access the component if available
     * @param priorVersion The previously valid version
     * @return True if the entity has an enabled component of type T that has
     *         been modified since <tt>priorVersion</tt>
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if data was created by another entity
     *             system
     */
    public <T extends ComponentData<T>> boolean getIfModified(T data, int priorVersion) {
        if (data.owner.getEntitySystem() != getEntitySystem()) {
            throw new IllegalArgumentException("ComponentData was not created by expected EntitySystem");
        }

        ComponentRepository<T> ci = data.owner;
        int componentIndex = ci.getComponentIndex(index);

        if (ci.getVersion(componentIndex) != priorVersion) {
            return data.setFast(componentIndex) && ci.isEnabled(componentIndex);
        } else {
            // note that in this situation, the component data is invalid
            return false;
        }
    }

    /**
     * <p>
     * Add a new Component with a data type T to this Entity. If there already
     * exists a component of type T, it is removed first, and a new one is
     * instantiated.
     * </p>
     * 
     * @param <T> The parameterized type of component being added
     * @param componentType The component type
     * @return A new component of type T
     * @throws NullPointerException if componentId is null
     */
    public <T extends ComponentData<T>> Component<T> add(Class<T> componentType) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        return ci.addComponent(index);
    }

    /**
     * <p>
     * Add a new Component with a data of type T to this Entity, but the new
     * component's state will be cloned from the given Component instance. The
     * <tt>toClone</tt> instance must still be live. If there already exists a
     * component of type T, it is removed first, and a new one is instantiated.
     * </p>
     * <p>
     * The new component is initialized by cloning the property values from
     * <tt>toClone</tt> into the values of the new component. This is performed
     * by invoking {@link PropertyFactory#clone(Property, int, Property, int)}
     * with the factories that created each property. All default property
     * factories perform a copy by value (or copy by reference for object
     * types).
     * </p>
     * 
     * @param <T> The parameterized type of component to add
     * @param toClone The existing T to clone when attaching to this component
     * @return A new component of type T
     * @throws NullPointerException if toClone is null
     * @throws IllegalArgumentException if toClone is not from the same system
     *             as this entity
     * @throws IllegalStateException if toClone is not a live component instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends ComponentData<T>> Component<T> add(Component<T> toClone) {
        if (toClone == null) {
            throw new NullPointerException("ComponentData template, toClone, cannot be null");
        }
        ComponentRepository ci = system.getRepository(toClone.getType());
        return ci.addComponent(index, toClone);
    }

    /**
     * <p>
     * Remove any attached Component with the data type, T, from this Entity.
     * True is returned if a component was removed, and false otherwise. If a
     * component is removed, the component should no longer be used and it will
     * return false from {@link Component#isLive()}. This will remove the
     * component even if the component has been disabled.
     * <p>
     * When a Component is removed, it will set its owner to null, and disown
     * all of its owned objects. If any of those owned objects are entities or
     * components, they are removed from the system as well.
     * 
     * @param <T> The parameterized type of component to remove
     * @param componentType The component type
     * @return True if a component was removed
     * @throws NullPointerException if componentId is null
     */
    public <T extends ComponentData<T>> boolean remove(Class<T> componentType) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        return ci.removeComponent(index);
    }

    /**
     * <p>
     * Return an iterator over the components currently attached to the Entity.
     * The iterator supports the remove operation and will detach the component
     * from the entity.
     * </p>
     * <p>
     * This will not report disabled components. Use {@link #iterator(boolean)}
     * to override this behavior.
     * </p>
     * 
     * @return An iterator over the entity's components
     */
    @Override
    public Iterator<Component<?>> iterator() {
        return iterator(false);
    }

    /**
     * Return an iterator over the components attached to this entity. If
     * <tt>ignoreEnabled</tt> is true, disabled components will be included in
     * the iterator's results. If it is false, disabled components are not
     * returned.
     * 
     * @param ignoreEnabled True if disabled components are returned
     * @return An iterator over the entity's components
     */
    public Iterator<Component<?>> iterator(boolean ignoreEnabled) {
        return new ComponentIterator(system, index, ignoreEnabled);
    }

    /*
     * Iterator implementation that iterates over the components attached to an
     * entity, based on entity index rather than reference
     */
    private static class ComponentIterator implements Iterator<Component<?>> {
        private final int entityIndex;
        private final Iterator<ComponentRepository<?>> indices;
        private final boolean ignoreEnable;

        private ComponentRepository<?> currentIndex;
        private ComponentRepository<?> nextIndex;

        public ComponentIterator(EntitySystem system, int entityIndex,
                                 boolean ignoreEnable) {
            this.entityIndex = entityIndex;
            this.ignoreEnable = ignoreEnable;
            indices = system.indexIterator();
        }

        @Override
        public boolean hasNext() {
            if (nextIndex == null) {
                advance();
            }
            return nextIndex != null;
        }

        @Override
        public Component<?> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            currentIndex = nextIndex;
            nextIndex = null;
            return currentIndex.getComponent(currentIndex.getComponentIndex(entityIndex));
        }

        @Override
        public void remove() {
            if (currentIndex == null) {
                throw new IllegalStateException("Must call next first");
            }

            if (currentIndex.removeComponent(entityIndex)) {
                currentIndex = null; // so next call to remove() fails
            } else {
                throw new IllegalStateException("Already removed");
            }
        }

        private void advance() {
            while (indices.hasNext()) {
                nextIndex = indices.next();

                int index = nextIndex.getComponentIndex(entityIndex);
                if (index != 0 && (ignoreEnable || nextIndex.isEnabled(index))) {
                    break;
                } else {
                    nextIndex = null; // must set to null if this was last element
                }
            }
        }
    }

    @Override
    public int compareTo(Entity o) {
        return id - o.id;
    }

    @Override
    public void notifyOwnershipGranted(Ownable obj) {
        delegate.notifyOwnershipGranted(obj);
    }

    @Override
    public void notifyOwnershipRevoked(Ownable obj) {
        delegate.notifyOwnershipRevoked(obj);
    }

    @Override
    public void setOwner(Owner owner) {
        delegate.setOwner(owner);
    }

    @Override
    public Owner getOwner() {
        return delegate.getOwner();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Entity(");
        sb.append(id);

        Iterator<Component<?>> it = iterator();
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(it.next().getType().getSimpleName());
        }

        sb.append(")");
        return sb.toString();
    }
}
