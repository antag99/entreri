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
package com.lhkbob.entreri;

import java.util.Iterator;

/**
 * Entity
 * ======
 *
 * An Entity represents a collection of Components within an EntitySystem. Entities are created by calling
 * {@link com.lhkbob.entreri.EntitySystem#addEntity()} or the similar function that takes another Entity as a
 * template.
 *
 * Once created the Entity object will not change its identity. There are no flyweight entities, unlike
 * components which do use that design pattern.
 *
 * Entity implements both {@link Ownable} and {@link Owner}. This can be used to create hierarchies of both
 * components and entities that share a lifetime. When an entity is removed from the system, all of its owned
 * objects are disowned. If any of them were entities or components, they are also removed from the system.
 *
 * @author Michael Ludwig
 */
public interface Entity extends Iterable<Component>, Comparable<Entity>, Ownable, Owner {
    /**
     * @return The unique (in the scope of the entity system) id of this entity
     */
    public int getId();

    /**
     * @return The owning EntitySystem of this entity
     */
    public EntitySystem getEntitySystem();

    /**
     * @return True if this Entity is still in its EntitySystem, or false if it has been removed
     */
    public boolean isAlive();

    /**
     * Get the Component instance of the given type that's attached to this Entity. A null value is returned
     * if the component type has not been attached to the entity. The returned instance will *not* be
     * flyweight (e.g. it is the canonical Component instance).
     *
     * @param <T>           The parameterized type of ComponentData of the component
     * @param componentType The given type
     * @return The current Component of type T attached to this entity
     * @throws NullPointerException if componentType is null
     */
    public <T extends Component> T get(Class<T> componentType);

    /**
     * Add a new Component with a data type T to this Entity. If there already exists a component of type T,
     * it is removed first, and a new one is instantiated. The created component is returned and is the
     * canonical Component instance.
     *
     * @param <T>           The parameterized type of component being added
     * @param componentType The component type
     * @return A new component of type T
     * @throws NullPointerException if componentId is null
     */
    public <T extends Component> T add(Class<T> componentType);

    /**
     * Add a new Component with a of type `T` to this Entity, where the new component's state will be cloned
     * from the given Component instance. The `toClone` instance must still be alive. If there already exists
     * a component of type `T`, it is removed first, and a new one is instantiated. Thus the old component of
     * type `T` for this entity should not be used as the template.
     *
     * The new component is initialized by cloning the property values from `toClone` into the values of the
     * new component. The cloning behavior is dependent on any configured property attribute annotations
     * applied in the component definition, and by the implementation of each property factory managing the
     * data of the component's properties.
     *
     * @param <T>     The parameterized type of component to add
     * @param toClone The existing T to clone when attaching to this component
     * @return A new component of type T
     * @throws NullPointerException  if toClone is null
     * @throws IllegalStateException if toClone is not a live component instance
     */
    public <T extends Component> T add(T toClone);

    /**
     * Get the canonical component instance of type <var>T</var> on this Entity. Unlike {@link #get(Class)},
     * this will add the component if it is not present. Thus, this is a convenience for getting the
     * component, and adding it if the get returned null.
     *
     * @param type The class interface of the component to add
     * @param <T>  The parameterized type of component
     * @return The component of type T, potentially new if it wasn't already on the entity
     */
    public <T extends Component> T as(Class<T> type);

    /**
     * Check whether or not the a component of the given type is attached to this entity. This is a
     * convenience for getting the component and then checking if it's null.
     *
     * @param type The component type to check for
     * @return True if the entity currently has the attached type, in which case get() will return a non-null
     * alive component
     */
    public boolean has(Class<? extends Component> type);

    /**
     * <p/>
     * Remove any attached Component with the data type from this Entity. True is returned if a component was
     * removed, and false otherwise. If a component is removed, the component should no longer be used and it
     * will return false from {@link Component#isAlive()}.
     *
     * When a Component is removed, it will set its owner to null, and disown all of its owned objects. If any
     * of those owned objects are entities or components, they are removed from the system as well.
     *
     * @param componentType The component type
     * @return True if a component was removed
     * @throws NullPointerException if componentId is null
     */
    public boolean remove(Class<? extends Component> componentType);

    /**
     * Return an iterator over the components currently attached to the Entity. The iterator supports the
     * remove operation and will detach the component from the entity just like a call to {@link
     * #remove(Class)}.
     *
     * Components reported by the iterator are *not* flyweight and represent the canonical Component
     * instances returned from methods like `as()` and `get()`.
     *
     * @return An iterator over the entity's components
     */
    @Override
    public Iterator<Component> iterator();
}
