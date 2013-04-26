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

/**
 * <p/>
 * Component represents a grouping of reusable and related states that are added to an
 * {@link Entity}. The specific state of a component is stored
 * and defined in {@link ComponentData} implementations. This separation is to support
 * fast iteration over blocks of packed, managed memory. All of the component data is
 * packed into buffers or arrays for cache locality. A single ComponentData instance can
 * then be used to access multiple Components.
 * <p/>
 * Component instances represent the identity of the conceptual components, while
 * instances of ComponentData can be configured to read and write to specific components.
 * ComponentData's can change which component they reference multiple times throughout
 * their life time.
 * <p/>
 * Component implements both {@link com.lhkbob.entreri.Ownable} and {@link
 * com.lhkbob.entreri.Owner}. This can be used to create hierarchies of both components
 * and entities that share a lifetime. When a component is removed from an entity, all of
 * its owned objects are disowned. If any of them were entities or components, they are
 * also removed from the system.
 *
 * @param <T> The ComponentData type defining the data of this component
 *
 * @author Michael Ludwig
 */
class AbstractComponent<T extends Component> implements Component {
    private final ComponentRepository<T> owner;

    private int index;
    private int id;


    /**
     * Create a new Component stored in the given ComponentRepository, at the given array
     * position within the ComponentRepository.
     *
     * @param owner The ComponentRepository owner
     */
    public AbstractComponent(ComponentRepository<T> owner) {
        this.owner = owner;
        this.index = 0;
    }

    public void setIndex(int index) {
        this.index = index;
        this.id = owner.getId(index);
    }

    public ComponentRepository<T> getRepository() {
        return owner;
    }

    @Override
    public boolean isAlive() {
        // we have to check the index of the Component because the ComponentRepository
        // does not make sure the data's indices stay within bounds of the repository arrays
        if (index != 0 && index < owner.getMaxComponentIndex()) {
            return owner.getId(index) == id;
        }
        return false;
    }

    @Override
    public Entity getEntity() {
        // if isAlive() returns false, then the entity index will also be 0,
        // so getEntityByIndex() returns null, which is expected
        int entityIndex = owner.getEntityIndex(index);
        return owner.getEntitySystem().getEntityByIndex(entityIndex);
    }

    @Override
    public EntitySystem getEntitySystem() {
        return owner.getEntitySystem();
    }

    @Override
    public final int getVersion() {
        return owner.getVersion(index);
    }

    @Override
    public final void updateVersion() {
        if (isAlive()) {
            owner.incrementVersion(index);
        }
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void notifyOwnershipGranted(Ownable obj) {
        owner.getOwnerDelegate(index).notifyOwnershipGranted(obj);
    }

    @Override
    public void notifyOwnershipRevoked(Ownable obj) {
        owner.getOwnerDelegate(index).notifyOwnershipRevoked(obj);
    }

    @Override
    public void setOwner(Owner owner) {
        this.owner.getOwnerDelegate(index).setOwner(owner);
    }

    @Override
    public Owner getOwner() {
        return owner.getOwnerDelegate(index).getOwner();
    }

    @Override
    public String toString() {
        if (index == 0) {
            return "Component(" + getClass().getSimpleName() + ")";
        } else {
            int entityId = owner.getEntitySystem()
                                .getEntityByIndex(owner.getEntityIndex(index)).getId();
            return "Component(" + getClass().getSimpleName() + ", entity=" + entityId +
                   ")";
        }
    }
}
