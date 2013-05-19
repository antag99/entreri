/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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

import com.lhkbob.entreri.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Entity implementation used by EntitySystemImpl.
 *
 * @author Michael Ludwig
 */
public final class EntityImpl implements Entity {
    private final EntitySystemImpl system;
    private final int id;

    final OwnerSupport delegate;

    int index;

    /**
     * Create an Entity that will be owned by the given system and is placed at the given
     * index.
     *
     * @param system The owning system
     * @param index  The index into the system
     * @param id     The unique id of the entity in the system
     */
    EntityImpl(EntitySystemImpl system, int index, int id) {
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

    @Override
    public int getId() {
        return id;
    }

    @Override
    public EntitySystem getEntitySystem() {
        return system;
    }

    @Override
    public boolean isAlive() {
        return index != 0;
    }

    @Override
    public <T extends Component> T get(Class<T> componentType) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        return ci.getComponent(ci.getComponentIndex(index));
    }

    @Override
    public <T extends Component> T add(Class<T> componentType) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        return ci.addComponent(index);
    }

    @Override
    public <T extends Component> T as(Class<T> componentType) {
        ComponentRepository<T> ci = system.getRepository(componentType);
        int componentIndex = ci.getComponentIndex(index);
        if (componentIndex > 0) {
            return ci.getComponent(componentIndex);
        } else {
            return ci.addComponent(index);
        }
    }

    @Override
    public boolean has(Class<? extends Component> componentType) {
        ComponentRepository<?> ci = system.getRepository(componentType);
        return ci.getComponentIndex(index) > 0;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Component> T add(T toClone) {
        if (toClone == null) {
            throw new NullPointerException(
                    "ComponentData template, toClone, cannot be null");
        }
        ComponentRepository ci = system.getRepository(toClone.getType());
        return (T) ci.addComponent(index, toClone);
    }

    @Override
    public boolean remove(Class<? extends Component> componentType) {
        ComponentRepository<?> ci = system.getRepository(componentType);
        return ci.removeComponent(index);
    }

    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator(system, index);
    }

    /*
     * Iterator implementation that iterates over the components attached to an
     * entity, based on entity index rather than reference
     */
    private static class ComponentIterator implements Iterator<Component> {
        private final int entityIndex;
        private final Iterator<ComponentRepository<?>> indices;

        private ComponentRepository<?> currentIndex;
        private ComponentRepository<?> nextIndex;

        public ComponentIterator(EntitySystemImpl system, int entityIndex) {
            this.entityIndex = entityIndex;
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
        public Component next() {
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
                if (index != 0) {
                    break;
                } else {
                    nextIndex = null; // must set to null if this was last element
                }
            }
        }
    }

    @Override
    public int compareTo(Entity o) {
        return id - o.getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityImpl && o == this;
    }

    @Override
    public Owner notifyOwnershipGranted(Ownable obj) {
        delegate.notifyOwnershipGranted(obj);
        return this;
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

        Iterator<Component> it = iterator();
        while (it.hasNext()) {
            sb.append(",\n\t").append(it.next());
        }

        sb.append(")");
        return sb.toString();
    }
}
