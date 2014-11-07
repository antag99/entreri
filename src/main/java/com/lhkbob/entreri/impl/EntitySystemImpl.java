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
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;
import com.lhkbob.entreri.task.Scheduler;

import java.util.*;

/**
 * EntitySystemImpl
 * ================
 *
 * Main and default implementation of EntitySystem that uses the registered annotation processor,
 * {@link ComponentAnnotationProcessor} to generate Java implementations of
 * Component definitions and then have them compiled.
 *
 * @author Michael Ludwig
 */
public final class EntitySystemImpl implements EntitySystem {
    // converts valid component data types into indices into componentRepositories
    private final Map<Class<? extends Component>, Integer> typeIndexMap;
    private int typeIdSeq;

    private ComponentDataStore<?>[] componentRepositories;

    private EntityImpl[] entities;

    private int entityInsert;
    private int entityIdSeq;

    private final Scheduler manager;

    /**
     * Create a new EntitySystem that has no entities added.
     */
    public EntitySystemImpl() {
        typeIndexMap = new HashMap<>();
        typeIdSeq = 0;

        manager = new Scheduler(this);
        entities = new EntityImpl[1];
        componentRepositories = new ComponentDataStore[0];

        entityIdSeq = 1; // start at 1, id 0 is reserved for index = 0
        entityInsert = 1;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection<Class<?>> getComponentTypes(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("Type cannot be null");
        }

        List<Class<?>> ids = new ArrayList<>();
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                // check the type
                if (type.isAssignableFrom(componentRepositories[i].getType())) {
                    // this type is a subclass of the requested type
                    ids.add((Class) componentRepositories[i].getType());
                }
            }
        }
        return ids;
    }

    @Override
    public Collection<Class<? extends Component>> getComponentTypes() {
        List<Class<? extends Component>> ids = new ArrayList<>();
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                ids.add(componentRepositories[i].getType());
            }
        }
        return ids;
    }

    @Override
    public Scheduler getScheduler() {
        return manager;
    }

    @Override
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    @Override
    public <T extends Component> Iterator<T> iterator(Class<T> type) {
        return new ComponentIteratorWrapper<>(type);
    }

    @Override
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
                if (startRemove >= 0) {
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

        if (startRemove >= 0) {
            // the last gap of entities to remove is at the end of the array,
            // so all we have to do is update the size
            entityInsert = startRemove;
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

    @Override
    public Entity addEntity() {
        return addEntity(null);
    }

    @Override
    public Entity addEntity(Entity template) {
        if (template != null) {
            // validate the template before allocating a new entity
            if (!template.isAlive()) {
                throw new IllegalStateException("Entity template is not live");
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

        EntityImpl newEntity = new EntityImpl(this, entityIndex, entityIdSeq++);
        entities[entityIndex] = newEntity;

        if (template != null) {
            for (Component c : template) {
                addFromTemplate(entityIndex, c.getType(), c);
            }
        }

        return newEntity;
    }

    @Override
    public void removeEntity(Entity e) {
        if (e == null) {
            throw new NullPointerException("Cannot remove a null entity");
        }
        if (e.getEntitySystem() != this) {
            throw new IllegalArgumentException("Entity is not from this EntitySystem");
        }

        EntityImpl ei = (EntityImpl) e;
        if (ei.index == 0) {
            throw new IllegalArgumentException("Entity has already been removed");
        }

        // Handle ownership removals
        ei.setOwner(null);
        ei.delegate.disownAndRemoveChildren();

        // Remove all components from the entity (that weren't removed
        // by ownership rules)
        for (int i = 0; i < componentRepositories.length; i++) {
            if (componentRepositories[i] != null) {
                componentRepositories[i].removeComponent(ei.index);
            }
        }

        // clear out the entity
        entities[ei.index] = null;
        ei.index = 0;
    }

    @Override
    public <T extends Component, P extends Property> P decorate(Class<T> type, PropertyFactory<P> factory) {
        ComponentDataStore<?> index = getRepository(type);
        return index.decorate(factory);
    }

    @Override
    public ComponentIterator fastIterator() {
        return new SystemComponentIteratorImpl(this);
    }

    @Override
    public ComponentIterator fastIterator(Iterable<Entity> entities) {
        return new CollectionComponentIteratorImpl(this, entities);
    }

    /**
     * Return the ComponentRepository associated with the given type. Creates a new component repository if
     * the type hasn't been used or accessed before.
     *
     * @param <T>  The Component type
     * @param type The component type
     * @return The ComponentRepository for the type
     */
    @SuppressWarnings("unchecked")
    <T extends Component> ComponentDataStore<T> getRepository(Class<T> type) {
        int index = getTypeIndex(type);
        if (index >= componentRepositories.length) {
            // make sure it's the correct size
            componentRepositories = Arrays.copyOf(componentRepositories, index + 1);
        }

        ComponentDataStore<T> i = (ComponentDataStore<T>) componentRepositories[index];
        if (i == null) {
            // if the index does not exist, then we need to use the default component data factory
            i = new ComponentDataStore<>(this, type);
            i.expandEntityIndex(entities.length);
            componentRepositories[index] = i;
        }

        return i;
    }

    private int getTypeIndex(Class<? extends Component> type) {
        Integer id = typeIndexMap.get(type);
        if (id == null) {
            id = typeIdSeq++;
            typeIndexMap.put(type, id);
        }
        return id;
    }

    /**
     * @return Return an iterator over the registered component indices
     */
    Iterator<ComponentDataStore<?>> indexIterator() {
        return new ComponentRepositoryIterator();
    }

    /**
     * Return the canonical Entity instance associated with the given index.
     *
     * @param entityIndex The index that the entity is stored at within the entity array and component
     *                    indicees
     * @return The canonical Entity instance for the index
     */
    Entity getEntityByIndex(int entityIndex) {
        return entities[entityIndex];
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends Component> void addFromTemplate(int entityIndex, Class type, T c) {
        ComponentDataStore index = getRepository(type);
        index.addComponent(entityIndex, c);
    }

    private class ComponentRepositoryIterator implements Iterator<ComponentDataStore<?>> {
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
        public ComponentDataStore<?> next() {
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

    private class ComponentIteratorWrapper<T extends Component> implements Iterator<T> {
        private final T data;
        private final ComponentIterator it;

        private boolean nextCalled;
        private boolean hasNext;

        public ComponentIteratorWrapper(Class<T> type) {
            it = fastIterator();
            data = it.addRequired(type);

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
