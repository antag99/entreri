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
 * ComponentData is used to define types of components that can be added to entities. For
 * performance reasons, the identity of a component is represented by instances of {@link
 * Component}. ComponentData instances are used as views into the data of the components.
 * This allows multiple instances to have their data packed together in primitive arrays
 * or direct memory, allowing for significantly faster iteration.
 * <p/>
 * <p/>
 * Additionally, by using a single ComponentData instance during the iteration, there is
 * no need to follow the usual object references needed for each instance.
 * <p/>
 * <p/>
 * ComponentData's are defined like any other class, but they are intended to be created
 * and configured by a {@link ComponentDataFactory}. These factories may impose certain
 * restrictions or requirements in what constructors or fields are valid. ComponentData
 * implementations can define a default ComponentDataFactory type with the {@link
 * DefaultFactory} annotation. By default ComponentData implementations are created using
 * The {@link ReflectionComponentDataFactory}
 *
 * @param <T> The self-referencing type of the ComponentData
 *
 * @author Michael Ludwig
 */
public abstract class ComponentData<T extends ComponentData<T>> {
    private int id;
    private int index;

    // this should be considered final, but is assigned in ComponentRepository
    // to simplify implementation constructor requirements.
    ComponentRepository<T> owner;

    protected ComponentData() {
    }

    /**
     * Get the Entity that owns this ComponentData. This is a convenience for
     * <code>getComponent().getEntity()</code>. This should not be invoked if {@link
     * #isValid()} returns false.
     *
     * @return The owning Entity
     */
    public final Entity getEntity() {
        int entityIndex = owner.getEntityIndex(index);
        return owner.getEntitySystem().getEntityByIndex(entityIndex);
    }

    /**
     * Return the index of this ComponentData within the IndexedDataStores that back the
     * defined properties of a ComponentData. A ComponentData's index will change as calls
     * to {@link #set(Component)} are made.
     *
     * @return The index of the component used to access its IndexedDataStores.
     */
    public final int getIndex() {
        return index;
    }

    /**
     * <p/>
     * Return whether or not this ComponentData is still attached to the last Component
     * passed into {@link #set(Component)}, and that that component is still live.
     * <p/>
     * It is possible for a ComponentData's component to be invalidated underneath it.
     * This happens if the component or its owning entity is removed from the system, or
     * if the entity system is {@link EntitySystem#compact() compacted}. Since this
     * library is meant to be single-threaded (or externally synchronized), this should be
     * predictable.
     *
     * @return True if this ComponentData is attached to the data of a Component that is
     *         still live
     */
    public final boolean isValid() {
        // we have to check the index of the ComponentData because the ComponentRepository
        // does not make sure the data's indices stay within bounds of the repository arrays
        if (index != 0 && index < owner.getMaxComponentIndex()) {
            return owner.getId(index) == id;
        }
        return false;
    }

    /**
     * Return whether or not the component this data is attached to is enabled. This is an
     * optimized shortcut for <code>getComponent().isEnabled()</code>
     *
     * @return True if the component is enabled, false if disabled or invalid
     */
    public final boolean isEnabled() {
        return owner.isEnabled(index);
    }

    /**
     * Set whether or not the component this data is attached to is enabled. This is an
     * optimized shortcut for <code>getComponent().setEnabled(enable)</code>
     *
     * @param enable True if the component should be enabled
     */
    public final void setEnabled(boolean enable) {
        owner.setEnabled(index, enable);
    }

    /**
     * <p/>
     * Get the current version of the data accessed by this ComponentData. When data is
     * mutated by a ComponentData, implementations increment its associated component's
     * version so comparing a previously cached version number can be used to determine
     * when changes have been made.
     * <p/>
     * Additionally, for a given component type, versions will be unique. Thus it is
     * possible to identify when the components are replaced by new components as well.
     *
     * @return The current version, or a negative number if the data is invalid
     */
    public final int getVersion() {
        return owner.getVersion(index);
    }

    /**
     * Increment the version of the component accessed by this instance. It is recommended
     * for component data implementations to call this automatically from within their
     * exposed mutators, but if necessary it can be invoked manually as well.
     *
     * @see #getVersion()
     */
    public final void updateVersion() {
        if (isValid()) {
            owner.incrementVersion(index);
        }
    }

    /**
     * Return the Component that this data reads and writes to. If {@link #isValid()}
     * returns false, the returned Component is undefined. It may be the proper component,
     * another component in the system, or null, or throw an exception.
     *
     * @return The component this data is attached to
     */
    public final Component<T> getComponent() {
        return owner.getComponent(index);
    }

    /**
     * <p/>
     * Set this ComponentData to read and write from the given Component. If the component
     * reference is a non-null, live component, this ComponentData will be considered a
     * valid ComponentData. While valid, its defined accessors and mutators will affect
     * the property state of <var>component</var>.
     * <p/>
     * Invoking set() with another Component will shift this data to the new component,
     * allowing it to mutate that.
     * <p/>
     * The ComponentData will remain valid until an action is taken that would invalidate
     * the data or its component. At the moment, the only actions capable of this are
     * removing the component or its entity from the system, or invoking {@link
     * EntitySystem#compact()}.
     *
     * @param component The component this data should point to
     *
     * @return True if the data is now valid
     *
     * @throws IllegalArgumentException if component was not created by the same entity
     *                                  system
     */
    public final boolean set(Component<T> component) {
        if (component == null) {
            return setFast(0);
        } else {
            // we check repository since it is guaranteed type safe
            if (component.getRepository() != owner) {
                throw new IllegalArgumentException(
                        "Component not created by expected EntitySystem");
            }

            return setFast(component.index);
        }
    }

    @Override
    public String toString() {
        if (index == 0) {
            return "ComponentData(" + getClass().getSimpleName() + ")";
        } else {
            int entityId = owner.getEntitySystem()
                                .getEntityByIndex(owner.getEntityIndex(index)).getId();
            return "ComponentData(" + getClass().getSimpleName() + ", entity=" +
                   entityId + ")";
        }
    }

    /**
     * Event hook called when this ComponentData is assigned to a valid component at the
     * provided non-zero index.
     *
     * @param index The new index
     */
    protected void onSet(int index) {
        // do nothing in base class
    }

    /**
     * A slightly faster method that requires only an index to a component, and performs
     * no validation. It also does not look up the component reference since it assumes
     * it's valid. These are lazily done when needed.
     *
     * @param componentIndex The index of the component
     *
     * @return True if the index is not 0
     */
    boolean setFast(int componentIndex) {
        index = componentIndex;
        id = owner.getId(index);
        onSet(index);
        return index != 0;
    }
}
