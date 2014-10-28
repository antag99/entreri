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

import com.lhkbob.entreri.impl.EntitySystemImpl;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;
import com.lhkbob.entreri.task.Scheduler;
import com.lhkbob.entreri.task.Task;

import java.util.Collection;
import java.util.Iterator;

/**
 * EntitySystem
 * ============
 *
 * EntitySystem is the main container for the entities within a logical system such as a game or physics
 * world. It contains all entities needed for processing the scene or data. Entities are created with {@link
 * #addEntity()} or {@link #addEntity(Entity)}. They can be removed (and effectively destroyed) with {@link
 * #removeEntity(Entity)}. When Entities are created by an EntitySystem, the created instance is assigned an
 * ID which represents its true identity. It is important to dereference entities and components that have
 * been removed since their underyling data managed by the EntitySystem is no longer valid.
 *
 * After an Entity is created, {@link com.lhkbob.entreri.Component Components} can be added to it to store
 * domain-specific data and configure its behaviors. The specifics of the data and behavior depends on the
 * Component implementations and {@link com.lhkbob.entreri.task.Task Tasks} used to process your data.
 *
 * The {@link Scheduler} of an EntitySystem can be used to create {@link com.lhkbob.entreri.task.Job Jobs}
 * composed of {@link Task Tasks} that will process the entities within an entity system in an organized
 * fashion. Generally, the processing of all controllers through their different phases constitutes a complete
 * "frame".
 *
 * Use {@link Factory#create()} to get a new instance of an EntitySystem.
 *
 * @author Michael Ludwig
 * @see Entity
 * @see Component
 */
public interface EntitySystem extends Iterable<Entity> {
    public static final class Factory {
        private Factory() {
        }

        /**
         * Create a new EntitySystem using the default implementation with entreri, which assumes Component
         * proxy implementations have been output by an annotation processor in a internal structure.
         *
         * @return A new, empty EntitySystem
         */
        public static EntitySystem create() {
            return new EntitySystemImpl();
        }
    }

    /**
     * Get all Component types within this EntitySystem that have types assignable to the input
     * `type`.
     *
     * @param type The query type
     * @return All Component interfaces that have components in this EntitySystem that are subclasses of the
     * input component data type
     * @throws NullPointerException if type is null
     */
    // FIXME should this be Class<? extends Component>?
    public Collection<Class<?>> getComponentTypes(Class<?> type);

    /**
     * Get all Component interfaces currently used by the EntitySystem. If a type has had all of its
     * components removed, it will still be returned here.
     *
     * @return All TypeIds at one point used by this system
     */
    public Collection<Class<? extends Component>> getComponentTypes();

    /**
     * Return the Scheduler for this EntitySystem that can be used to organize processing of the system using
     * {@link Task} implementations.
     *
     * @return The Scheduler for this system
     */
    public Scheduler getScheduler();

    /**
     * Return an iterator over all of the entities within the system. The returned iterator's remove() method
     * will remove the entity from the system with behavior identical to calling {@link
     * #removeEntity(Entity)}.
     *
     * @return An iterator over the entities of the system
     */
    @Override
    public Iterator<Entity> iterator();

    /**
     * Return an iterator over all components of with the given type. The returned iterator uses a single
     * flyweight Component instance of T, so it is a fast iterator. This effectively wraps a {@link
     * ComponentIterator} in a standard {@link Iterator} with a single required component type.
     *
     * @param type The type of component to iterate over
     * @return A fast iterator over components in this system
     */
    public <T extends Component> Iterator<T> iterator(Class<T> type);

    /**
     * Return a new ComponentIterator that must be configured with required and optional components before
     * use. This is equivalent to calling {@code es.fastIterator(es)} although the returned implementation may
     * be more efficient in this situation.
     *
     * @return A new iterator over the components in this system
     */
    public ComponentIterator fastIterator();

    /**
     * Return a new ComponentIterator that walks over the given collection of entities. It is assumed that all
     * entities within the system are from this system, undefined results will occur if this is not met. The
     * returned iterator must still be configured with required and optional components before use.
     *
     * The returned iterator will report the entities in the order given by the iterable. If the exact order
     * is not important, it can improve performance to sort the collection by entity id first if the
     * collection is to be iterated over multiple times. This helps order component access patterns to
     * more likely hit the cache.
     *
     * @param entities The entity collection to walk over.
     * @return A new iterator over the collection of entities
     */
    public ComponentIterator fastIterator(Iterable<Entity> entities);

    /**
     * Compact the entity and component data so that iteration is more efficient. In the life time of an
     * entity system, entities and components are added and removed, possibly causing the list of components
     * for a given type to be in a different order than the list of entities. This is due to implementation
     * details needed to make additions and removals constant time. It is not necessary to invoke `compact()`
     * every frame.
     *
     * Invoking `compact()` after a large number of additions or removals to the system is a good idea.
     * Alternatively, invoking it every few frames in a game works as well. An entity system that has no
     * additions or removals of entities (or their components) gains no benefit from compacting, except
     * potentially freeing excess memory.
     *
     * Compacting is not overly fast or slow, so it should not cause noticeably drops in frame rate. As an
     * example, on a test system with 20,000 entities compact() took ~2ms on an Intel i5 processor. Of course,
     * mileage may very.
     */
    public void compact();

    /**
     * Add a new Entity to this EntitySystem. The created Entity will not have any attached Components. You
     * can create a new entity from a template by calling {@link #addEntity(Entity)}.
     *
     * @return A new Entity in the system, without any components
     */
    public Entity addEntity();

    /**
     * Add a new Entity to this EntitySystem. If `template` is not null, the components attached to the
     * template will have their state cloned onto the new entity. The semantics of cloning is defined by the
     * `clone` method of the selected PropertyFactory used by the generated proxy. Factories often modify
     * their behavior in response to the {@link com.lhkbob.entreri.property.Clone} annotation. By default they
     * follows Java's reference/value assignment rules.
     *
     * Specifying a null template makes this behave identically to {@link #addEntity()}.
     *
     * @param template The template to clone
     * @return A new Entity in the system with the same component state as the template
     * @throws IllegalStateException if the template is not a live entity
     */
    public Entity addEntity(Entity template);

    /**
     * Remove the given entity from this system. The entity and its attached components are removed from the
     * system. This will cause the entity and its components to no longer be alive. When an entity is removed,
     * it will set its owner to null, and disown all of its owned objects. If any of those owned objects are
     * entities or components, they are removed from the system as well.
     *
     * @param e The entity to remove
     * @throws NullPointerException     if e is null
     * @throws IllegalArgumentException if the entity was not created by this system, or already removed
     */
    public void removeEntity(Entity e);

    /**
     * Dynamically update the available properties of the given Component type by adding a Property created by
     * the given PropertyFactory. The property will be managed by the system as if it was a declared property
     * of the component type.
     *
     * All components, current and new, will initially have their starting values for the decorated property
     * be the default as defined by the factory. The returned property can be accessed and used by Tasks to
     * add dynamic runtime data to statically defined component types. Game editors could also use this to
     * simulate dynamic component creation.
     *
     * @param type    The component type to mutate
     * @param factory The property factory that creates the decorating property
     * @return The property that has decorated the given component type
     * @throws NullPointerException if type or factory are null
     */
    public <T extends Component, P extends Property> P decorate(Class<T> type, PropertyFactory<P> factory);
}
