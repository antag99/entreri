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
 * AbstractComponent is the base class used for all generated proxy implementations of
 * component subtypes. It provides an implementation for all of the declared methods in
 * Component as well as equals() and hashCode().
 *
 * @param <T> The type of component the AbstractComponent is safely cast-able to
 */
abstract class AbstractComponent<T extends Component> implements Component {
    protected final ComponentRepository<T> owner;

    private int index;
    private int id;


    /**
     * Create a new component stored in the given ComponentRepository.
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
        return index != 0 && index < owner.getMaxComponentIndex() &&
               owner.getId(index) == id;
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
    public Owner notifyOwnershipGranted(Ownable obj) {
        owner.getOwnerDelegate(index).notifyOwnershipGranted(obj);
        // make sure to return the canonical component at the current index
        return owner.getComponent(index);
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
    public Class<T> getType() {
        return owner.getType();
    }

    @Override
    public boolean isFlyweight() {
        return !isAlive() || owner.getComponent(index) != this;
    }

    @Override
    public int hashCode() {
        if (isAlive()) {
            return (getType().hashCode() + 17 * (getEntity().getId() + 31));
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractComponent)) {
            return false;
        }
        AbstractComponent<?> a = (AbstractComponent<?>) o;
        return a.owner == owner && a.index == index;
    }

    @Override
    public String toString() {
        // FIXME improve toString() to include values for properties
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
