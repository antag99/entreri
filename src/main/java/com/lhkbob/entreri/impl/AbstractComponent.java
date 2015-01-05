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

import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.Property;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * AbstractComponent
 * =================
 *
 * AbstractComponent is the base class used for all generated proxy implementations of component subtypes. It
 * provides an implementation for all of the declared methods in Component as well as equals() and hashCode().
 * It should not be subclassed or extended directly, but is used as the parent class of generated proxies. As
 * specified in {@link com.lhkbob.entreri.Component}, all component type definitions are sub-interfaces of
 * Component.
 *
 * @param <T> The type of component the AbstractComponent is safely cast-able to
 * @author Michael Ludwig
 */
public abstract class AbstractComponent<T extends Component> implements Component {
    /**
     * The ComponentRepository managing this component and all of its property data
     */
    protected final ComponentDataStore<T> owner;

    /**
     * The current index of the component. Subclasses must not modify directly, but should call setIndex().
     * This is provided to avoid the virtual getIndex() call.
     */
    protected int index;
    private int id;

    /**
     * Create a new component stored in the given ComponentRepository.
     *
     * @param owner The ComponentRepository owner
     */
    public AbstractComponent(ComponentDataStore<T> owner) {
        this.owner = owner;
        this.index = 0;
    }

    /**
     * Set the index that defines the identity of this component.
     *
     * @param index The new index
     */
    void setIndex(int index) {
        this.index = index;
        this.id = owner.getId(index);
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
    public T getCanonical() {
        return owner.getComponent(index);
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
        if (!isAlive()) {
            return "dead " + getType().getSimpleName();
        } else {
            StringBuilder sb = new StringBuilder(getType().getSimpleName());
            sb.append("(").append(getEntity().getId());

            for (int i = 0; i < owner.getDeclaredPropertyCount(); i++) {
                sb.append(", ").append(owner.getDeclaredPropertyName(i)).append("=")
                  .append(inspectProperty(owner.getProperty(i)));
            }

            sb.append(")");
            return sb.toString();
        }
    }

    private String inspectProperty(Property p) {
        try {
            Method get = p.getClass().getMethod("get", int.class);
            Object v = get.invoke(p, index);

            if (v != null) {
                // strip out newlines
                return v.toString().replaceAll("[\n\r]", "");
            } else {
                return "null";
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return "unable to inspect";
        }
    }

    /**
     * Search through annotation array `annots` for an annotation type of `cls` and return it. This is used
     * to select a particular typed annotation from the arrays returned by {@link
     * Method#getParameterAnnotations()}.
     *
     * @param cls    The annotation type to look for
     * @param annots The annotations to search through
     * @param <T>    The annotation type
     * @return The annotation, or null if not found
     */
    protected static <T extends Annotation> T getAnnotation(Class<T> cls, Annotation[] annots) {
        for (Annotation a : annots) {
            if (a.annotationType().equals(cls)) {
                return cls.cast(a);
            }
        }
        return null;
    }
}
