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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>
 * EntitySystem is the main container for the entities within a logical system
 * such as a game or physics world. It contains all entities needed for
 * processing the scene or data. Entities are created with {@link #addEntity()}
 * or {@link #addEntity(Entity)}. They can be removed (and effectively
 * destroyed) with {@link #removeEntity(Entity)}.
 * </p>
 * <p>
 * After an Entity is created, Components can be added to it to store
 * domain-specific data and configure its behaviors. The specifics of the data
 * and behavior depends on the ComponentData implementations and Controllers
 * used to process your data.
 * </p>
 * <p>
 * The {@link ControllerManager} of an EntitySystem can be used to register
 * Controllers that will process the entities within an entity system in an
 * organized fashion. Generally, the processing of all controllers through their
 * different phases constitutes a complete "frame".
 * </p>
 * <p>
 * When Entities are created by an EntitySystem, the created instance is
 * assigned an ID which represents its true identity.
 * </p>
 * 
 * @see Entity
 * @see Component
 * @see ComponentData
 * @author Michael Ludwig
 */
public final class EntitySystem implements Iterable<Entity> {
    private ComponentRepository<?>[] componentRepositories;

    private Entity[] entities;

    private int entityInsert;
    private int entityIdSeq;

    private final ControllerManager manager;

    /**
     * Create a new EntitySystem that has no entities added.
     */
    public EntitySystem() {
        manager = new ControllerManager(this);
        entities = new Entity[1];
        componentRepositories = new ComponentRepository[0];

        entityIdSeq = 1; // start at 1, id 0 is reserved for index = 0
        entityInsert = 1;
    }

    /**
     * <p>
     * Assign a specific ComponentDataFactory instance to create and manage
     * ComponentData instances of the given type for this system. This will
     * override the default {@link ReflectionComponentDataFactory} or any
     * default type declared by the {@link DefaultFactory} annotation.
     * </p>
     * <p>
     * However, a ComponentDataFactory cannot be assigned if there is already
     * another factory in use for that type. This situation arises if
     * setFactory() is called multiple times, or if the EntitySystem needs to
     * use a given type before setFactory() is called and it must fall back onto
     * a default factory.
     * </p>
     * <p>
     * This rule exists to ensure that all ComponentData instances of a given
     * type created by an EntitySystem come from the same factory, regardless of
     * when they were instantiated.
     * </p>
     * 
     * @param <T> The ComponentData type created by the factory
     * @param id The type id for the component type
     * @param factory The factory to use in this system for the given type
     * @throws NullPointerException if id or factory are null
     * @throws IllegalArgumentException if the factory does not actually create
     *             instances of type T
     * @throws IllegalStateException if the EntitySystem already has a factory
     *             for the given type
     */
    @SuppressWarnings("unchecked")
    public <T extends ComponentData<T>> void setFactory(TypeId<T> id,
                                                        ComponentDataFactory<T> factory) {
        if (id == null) {
            throw new NullPointerException("TypeId cannot be null");
        }
        if (factory == null) {
            throw new NullPointerException("ComponentDataFactory cannot be null");
        }

        int index = id.getId();
        if (index >= componentRepositories.length) {
            // make sure it's the correct size
            componentRepositories = Arrays.copyOf(componentRepositories, index + 1);
        }

        ComponentRepository<T> i = (ComponentRepository<T>) componentRepositories[index];
        if (i != null) {
            // a factory is already defined
            throw new IllegalStateException("A ComponentDataFactory is already assigned to the type: " + id);
        }

        // verify that the factory creates the proper instances
        T data = factory.createInstance();
        if (!id.getType().isInstance(data)) {
            throw new IllegalArgumentException("ComponentDataFactory does not create instances of type: " + id);
        }

        i = new ComponentRepository<T>(this, id, factory);
        i.expandEntityIndex(entities.length);
        componentRepositories[index] = i;
    }

    /**
     * Create a new instance of type T for use with accessing the component data
     * of this EntitySystem. The returned instance will be invalid until it is
     * assigned a Component to access (explicitly or via a
     * {@link ComponentIterator}).
     * 
     * @param <T> The type of ComponentData to create
     * @param id The type id of the ComponentDat
     * @return A new instance of T linked to this EntitySystem
     */
    public <T extends ComponentData<T>> T createDataInstance(TypeId<T> id) {
        return getRepository(id).createDataInstance();
    }

    /**
     * Estimate the memory usage of the components with the given TypeId in this
     * EntitySystem. The returned long is measured in bytes. For ComponentData
     * types that store primitive data, the estimate will be quite accurate. For
     * types that store references to objects, it is likely be an underestimate.
     * 
     * @param id The TypeId to estimate
     * @return The memory estimate for the given type
     * @throws NullPointerException if id is null
     */
    public long estimateMemory(TypeId<?> id) {
        int index = id.getId();
        if (index < componentRepositories.length) {
            ComponentRepository<?> repo = componentRepositories[index];
            return repo.estimateMemory();
        } else {
            return 0L;
        }
    }

    /**
     * Get all TypeIds within this EntitySystem that have types assignable to
     * the input <tt>type</tt>.
     * 
     * @param type The query type
     * @return All TypeIds that have components in this EntitySystem that are
     *         subclasses of the input component data type
     * @throws NullPointerException if type is null
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends ComponentData<? extends T>> Collection<TypeId<? extends T>> getTypes(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Type cannot be null");
        }

        List<TypeId<? extends T>> ids = new ArrayList<TypeId<? extends T>>();
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                // check the type
                if (type.isAssignableFrom(componentRepositories[i].getTypeId().getType())) {
                    // this type is a subclass of the requested type
                    ids.add((TypeId) componentRepositories[i].getTypeId());
                }
            }
        }
        return ids;
    }

    /**
     * Get all TypeIds currently used by the EntitySystem. If a type has had all
     * of its components removed, it will still be returned here.
     * 
     * @return All TypeIds at one point used by this system
     */
    public Collection<TypeId<?>> getTypes() {
        List<TypeId<?>> ids = new ArrayList<TypeId<?>>();
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                // check the type
                // this type is a subclass of the requested type
                ids.add(componentRepositories[i].getTypeId());
            }
        }
        return ids;
    }

    /**
     * Return the ControllerManager for this EntitySystem that can be used to
     * organize processing of the system using {@link Controller}
     * implementations.
     * 
     * @return The ControllerManager for this system
     */
    public ControllerManager getControllerManager() {
        return manager;
    }

    /**
     * Return an iterator over all of the entities within the system. The
     * returned iterator's remove() method will remove the entity from the
     * system.
     * 
     * @return An iterator over the entities of the system
     */
    @Override
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    /**
     * Return an iterator over all components of with the given type. The
     * returned iterator reuses a single ComponentData instance of T, so it is a
     * fast iterator. This effectively wraps a {@link ComponentIterator} in a
     * standard {@link Iterator} with a single required component type.
     * 
     * @param id The type of component to iterate over
     * @return A fast iterator over components in this system
     */
    public <T extends ComponentData<T>> Iterator<T> iterator(TypeId<T> id) {
        return new ComponentIteratorWrapper<T>(id);
    }

    /**
     * <p>
     * Compact the entity and component data so that iteration is more
     * efficient. In the life time of an entity system, entities and components
     * are added and removed, possibly causing the list of components for a
     * given type to be in a different order than the list of entities. This is
     * due to implementation details needed to make additions and removals
     * constant time.
     * </p>
     * <p>
     * Invoking {@link #compact()} after a large number of additions or removals
     * to the system is a good idea. Alternatively, invoking it every few frames
     * in a game works as well. An entity system that has no additions or
     * removals of entities (or their components) gains no benefit from
     * compacting, except potentially freeing excess memory.
     * </p>
     * <p>
     * Compacting is not overly fast or slow, so it should not cause noticeably
     * drops in frame rate. As an example, on a test system with 20,000 entities
     * compact() took ~2ms on an Intel i5 processor. Of course, mileage may
     * very.
     * </p>
     */
    public void compact() {
        // Pack the data
        int startRemove = -1;
        for (int i = 1; i < entityInsert; i++) {
            if (entities[i] == null) {
                // found an entity to remove
                if (startRemove < 0) {
                    startRemove = i;
                }
            } else {
                // found an entity to preserve
                if (startRemove > 0) {
                    // we have a gap from [startRemove, i - 1] that can be compacted
                    System.arraycopy(entities, i, entities, startRemove, entityInsert - i);

                    // update entityInsert
                    entityInsert = entityInsert - i + startRemove;

                    // now reset loop
                    i = startRemove;
                    startRemove = -1;
                }
            }
        }

        // Build a map from oldIndex to newIndex and repair entity's index
        int[] oldToNew = new int[entities.length];
        for (int i = 1; i < entityInsert; i++) {
            oldToNew[entities[i].index] = i;
            entities[i].index = i;
        }

        if (entityInsert < .6f * entities.length) {
            // reduce the size of the entities/ids arrays
            int newSize = (int) (1.2f * entityInsert) + 1;
            entities = Arrays.copyOf(entities, newSize);
        }

        // Now index and update all ComponentIndices
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                componentRepositories[i].compact(oldToNew, entityInsert);
            }
        }
    }

    /**
     * Add a new Entity to this EntitySystem. The created Entity will not have
     * any attached Components. You can create a new entity from a template by
     * calling {@link #addEntity(Entity)}.
     * 
     * @return A new Entity in the system, without any components
     */
    public Entity addEntity() {
        return addEntity(null);
    }

    /**
     * <p>
     * Add a new Entity to this EntitySystem. If <tt>template</tt> is not null,
     * the components attached to the template will have their state cloned onto
     * the new entity. The semantics of cloning is defined by
     * {@link PropertyFactory#clone(Property, int, Property, int)}, but by
     * default it follows Java's reference/value rule.
     * </p>
     * <p>
     * Specifying a null template makes this behave identically to
     * {@link #addEntity()}.
     * </p>
     * 
     * @param template The template to clone
     * @return A new Entity in the system with the same component state as the
     *         template
     * @throws IllegalStateException if the template is not a live entity
     * @throws IllegalArgumentException if the template was not created by this
     *             entity system
     */
    public Entity addEntity(Entity template) {
        if (template != null) {
            // validate the template before allocating a new entity
            if (!template.isLive()) {
                throw new IllegalStateException("Entity template is not live");
            }
            if (template.getEntitySystem() != this) {
                throw new IllegalArgumentException("Entity template was not created by this EntitySystem");
            }
        }

        int entityIndex = entityInsert++;
        if (entityIndex >= entities.length) {
            entities = Arrays.copyOf(entities, (int) (entityIndex * 1.5f) + 1);
        }

        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                componentRepositories[i].expandEntityIndex(entityIndex + 1);
            }
        }

        Entity newEntity = new Entity(this, entityIndex, entityIdSeq++);
        entities[entityIndex] = newEntity;

        // invoke add-event listeners now before we invoke any listeners
        // due to templating so events have proper sequencing
        manager.fireEntityAdd(newEntity);

        if (template != null) {
            for (Component<?> c : template) {
                addFromTemplate(entityIndex, c.getTypeId(), c);
            }
        }

        return newEntity;
    }

    /**
     * Remove the given entity from this system. The entity and its attached
     * components are removed from the system. This will cause the entity and
     * its components to no longer be alive. Any ComponentData's referencing the
     * entity's components will become invalid until assigned to a new
     * component.
     * 
     * @param e The entity to remove
     * @throws NullPointerException if e is null
     * @throws IllegalArgumentException if the entity is not owned by this
     *             system, or already removed
     */
    public void removeEntity(Entity e) {
        if (e == null) {
            throw new NullPointerException("Cannot remove a null entity");
        }
        if (e.getEntitySystem() != this) {
            throw new IllegalArgumentException("Entity is not from this EntitySystem");
        }
        if (e.index == 0) {
            throw new IllegalArgumentException("Entity has already been removed");
        }

        // Remove all components from the entity
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                componentRepositories[i].removeComponent(e.index);
            }
        }

        // clear out the entity
        manager.fireEntityRemove(e);
        entities[e.index] = null;
        e.index = 0;
    }

    /**
     * <p>
     * Dynamically update the available properties of the given ComponentData
     * type by adding a Property created by the given PropertyFactory. The
     * property will be managed by the system as if it was a declared property
     * of the component type.
     * </p>
     * <p>
     * All components, current and new, will initially have their starting
     * values for the decorated property equal the state of the property after
     * being created by the factory. The returned property can be accessed and
     * used by Controllers to add dynamic runtime data to statically defined
     * component types.
     * </p>
     * 
     * @param <P> The created property type
     * @param type The component type to mutate
     * @param factory The property factory that creates the decorating property
     * @return The property that has decorated the given component type
     * @throws NullPointerException if type or factory are null
     */
    public <T extends ComponentData<T>, P extends Property> P decorate(TypeId<T> type,
                                                                       PropertyFactory<P> factory) {
        ComponentRepository<?> index = getRepository(type);
        return index.decorate(factory);
    }

    /**
     * Remove the property, <tt>p</tt>, that was previously decorated onto the
     * given type by the method {@link #decorate(TypeId, PropertyFactory)}. If
     * the property has not been decorated onto that type or is no longer
     * decorated, this does nothing.
     * 
     * @param type The component type to undecorate
     * @param p The property to remove from the dynamic type definition
     * @throws NullPointerException if type is null
     */
    public <T extends ComponentData<T>> void undecorate(TypeId<T> type, Property p) {
        ComponentRepository<?> index = getRepository(type);
        index.undecorate(p);
    }

    /**
     * Return the ComponentRepository associated with the given type. Fails if
     * the type is not registered
     * 
     * @param <T> The ComponentData type
     * @param id The id for the component type
     * @return The ComponentRepository for the type
     */
    @SuppressWarnings("unchecked")
    <T extends ComponentData<T>> ComponentRepository<T> getRepository(TypeId<T> id) {
        int index = id.getId();
        if (index >= componentRepositories.length) {
            // make sure it's the correct size
            componentRepositories = Arrays.copyOf(componentRepositories, index + 1);
        }

        ComponentRepository<T> i = (ComponentRepository<T>) componentRepositories[index];
        if (i == null) {
            // if the index does not exist, then we need to use the default component data factory
            i = new ComponentRepository<T>(this, id, createDefaultFactory(id));
            i.expandEntityIndex(entities.length);
            componentRepositories[index] = i;
        }

        return i;
    }

    /*
     * Create a new ComponentDataFactory for the given id, using the default
     * annotation if available.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends ComponentData<T>> ComponentDataFactory<T> createDefaultFactory(TypeId<T> id) {
        DefaultFactory factoryAnnot = id.getType().getAnnotation(DefaultFactory.class);
        if (factoryAnnot != null) {
            Class factoryType = factoryAnnot.value();
            // check for supported constructors, priority: TypeId, Class, default
            ComponentDataFactory<T> factory = (ComponentDataFactory<T>) attemptInstantiation(factoryType,
                                                                                             id);
            if (factory == null) {
                factory = (ComponentDataFactory<T>) attemptInstantiation(factoryType,
                                                                         id.getType());
            }
            if (factory == null) {
                factory = (ComponentDataFactory<T>) attemptInstantiation(factoryType);
            }
            if (factory == null) {
                throw new IllegalComponentDefinitionException(id.getType(),
                                                              "Cannot instantiate default ComponentDataFactory of type: " + factoryType);
            }

            return factory;
        } else {
            // use the reflection default
            return new ReflectionComponentDataFactory<T>(id.getType());
        }
    }

    /*
     * Look for a constructor that takes the given params and use it. Returns
     * null if any exception is thrown.
     */
    private Object attemptInstantiation(Class<?> type, Object... params) {
        Class<?>[] argTypes = new Class<?>[params.length];
        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(argTypes);
            return constructor.newInstance(params);
        } catch (Exception e) {
            // just return null, calling methods will fallback as appropriate
            return null;
        }
    }

    /**
     * @return Return an iterator over the registered component indices
     */
    Iterator<ComponentRepository<?>> indexIterator() {
        return new ComponentRepositoryIterator();
    }

    /**
     * Return the canonical Entity instance associated with the given index.
     * 
     * @param entityIndex The index that the entity is stored at within the
     *            entity array and component indicees
     * @return The canonical Entity instance for the index
     */
    Entity getEntityByIndex(int entityIndex) {
        return entities[entityIndex];
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends ComponentData<T>> void addFromTemplate(int entityIndex,
                                                              TypeId typeId,
                                                              Component<T> c) {
        ComponentRepository index = getRepository(typeId);
        index.addComponent(entityIndex, c);
    }

    private class ComponentRepositoryIterator implements Iterator<ComponentRepository<?>> {
        private int index;
        private boolean advanced;

        public ComponentRepositoryIterator() {
            index = -1;
            advanced = false;
        }

        @Override
        public boolean hasNext() {
            if (!advanced) {
                advance();
            }
            return index < componentRepositories.length;
        }

        @Override
        public ComponentRepository<?> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            advanced = false;
            return componentRepositories[index];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void advance() {
            do {
                index++;
            } while (index < componentRepositories.length && componentRepositories[index] == null);
            advanced = true;
        }
    }

    private class ComponentIteratorWrapper<T extends ComponentData<T>> implements Iterator<T> {
        private final T data;
        private final ComponentIterator it;

        private boolean nextCalled;
        private boolean hasNext;

        public ComponentIteratorWrapper(TypeId<T> type) {
            data = createDataInstance(type);
            it = new ComponentIterator(EntitySystem.this);
            it.addRequired(data);

            nextCalled = false;
            hasNext = false;
        }

        @Override
        public boolean hasNext() {
            if (!nextCalled) {
                hasNext = it.next();
                nextCalled = true;
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextCalled = false;
            return data;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class EntityIterator implements Iterator<Entity> {
        private int index;
        private boolean advanced;

        public EntityIterator() {
            index = 0;
            advanced = false;
        }

        @Override
        public boolean hasNext() {
            if (!advanced) {
                advance();
            }
            return index < entityInsert;
        }

        @Override
        public Entity next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            advanced = false;
            return entities[index];
        }

        @Override
        public void remove() {
            if (advanced || index == 0) {
                throw new IllegalStateException("Must call next() before remove()");
            }
            if (entities[index] == null) {
                throw new IllegalStateException("Entity already removed");
            }
            removeEntity(entities[index]);
        }

        private void advance() {
            do {
                index++; // always advance at least 1
            } while (index < entities.length && entities[index] == null);
            advanced = true;
        }
    }
}
