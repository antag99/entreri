package com.lhkbob.entreri;

import java.util.HashSet;
import java.util.Set;

class OwnerSupport {
    private final Ownable target;
    private final Set<Ownable> ownedObjects;
    private Owner currentOwner;

    public OwnerSupport(Ownable target) {
        this.target = target;
        ownedObjects = new HashSet<Ownable>();
        currentOwner = null;
    }

    public void notifyOwnershipGranted(Ownable obj) {
        ownedObjects.add(obj);
    }

    public void notifyOwnershipRevoked(Ownable obj) {
        ownedObjects.remove(obj);
    }

    public void setOwner(Owner owner) {
        if (currentOwner != null) {
            currentOwner.notifyOwnershipRevoked(target);
        }
        currentOwner = owner;
        if (owner != null) {
            owner.notifyOwnershipGranted(target);
        }
    }

    public Owner getOwner() {
        return currentOwner;
    }

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
                Component<?> ownedComp = (Component<?>) owned;
                ownedComp.getEntity().remove(ownedComp.getType());
            }
        }
    }
}
