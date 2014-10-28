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
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Ownable;
import com.lhkbob.entreri.Owner;

import java.util.HashSet;
import java.util.Set;

/**
 * OwnerSupport
 * ============
 *
 * Utility class for shared implementation of {@link com.lhkbob.entreri.Ownable} and {@link
 * com.lhkbob.entreri.Owner}
 *
 * @author Michael Ludwig
 */
public class OwnerSupport {
    private final Ownable target;
    private final Set<Ownable> ownedObjects;
    private Owner currentOwner;

    /**
     * Create a new OwnerSupport that functions as both the {@link Owner} and {@link Ownable} implementation
     * for `target`.
     *
     * @param target The actual ownable
     */
    public OwnerSupport(Ownable target) {
        if (target == null) {
            throw new NullPointerException("Ownable cannot be null");
        }
        this.target = target;
        ownedObjects = new HashSet<>();
        currentOwner = null;
    }

    /**
     * @param obj The object now owned
     * @see Owner#notifyOwnershipGranted(Ownable)
     */
    public void notifyOwnershipGranted(Ownable obj) {
        ownedObjects.add(obj);
    }

    /**
     * @param obj The object no longer owned
     * @see Owner#notifyOwnershipRevoked(Ownable)
     */
    public void notifyOwnershipRevoked(Ownable obj) {
        ownedObjects.remove(obj);
    }

    /**
     * @param owner The owner, possibly flyweight
     * @see Ownable#setOwner(Owner)
     */
    public void setOwner(Owner owner) {
        if (currentOwner != null) {
            currentOwner.notifyOwnershipRevoked(target);
        }
        if (owner != null) {
            currentOwner = owner.notifyOwnershipGranted(target);
        } else {
            currentOwner = null;
        }
    }

    /**
     * @return The owner
     * @see Ownable#getOwner()
     */
    public Owner getOwner() {
        return currentOwner;
    }

    /**
     * Set the owner of all currently owned children to null. If any of the children are entities or
     * components, they are removed from their creating system or entity, respectively.
     */
    public void disownAndRemoveChildren() {
        // Mark all owned objects as not owned
        // if they are an entity or component, recurse and remove them as well
        Set<Ownable> cloned = new HashSet<Ownable>(ownedObjects);
        for (Ownable owned : cloned) {
            owned.setOwner(null);
            if (owned instanceof Entity) {
                Entity ownedEntity = (Entity) owned;
                ownedEntity.getEntitySystem().removeEntity(ownedEntity);
            } else if (owned instanceof Component) {
                Component ownedComp = (Component) owned;
                ownedComp.getEntity().remove(ownedComp.getType());
            }
        }
    }
}
