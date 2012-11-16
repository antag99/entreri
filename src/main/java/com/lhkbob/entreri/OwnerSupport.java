package com.lhkbob.entreri;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for shared implementation of {@link Ownable} and {@link Owner}
 * 
 * @author Michael Ludwig
 * 
 */
class OwnerSupport {
    private final Ownable target;
    private final Set<Ownable> ownedObjects;
    private Owner currentOwner;

    /**
     * Create a new OwnerSupport that functions as both the {@link Owner} and
     * {@link Ownable} implementation for <tt>target</tt>
     * 
     * @param target The actual ownable
     */
    public OwnerSupport(Ownable target) {
        if (target == null) {
            throw new NullPointerException("Ownable cannot be null");
        }
        this.target = target;
        ownedObjects = new HashSet<Ownable>();
        currentOwner = null;
    }

    /**
     * @see Owner#notifyOwnershipGranted(Ownable)
     * @param obj
     */
    public void notifyOwnershipGranted(Ownable obj) {
        ownedObjects.add(obj);
    }

    /**
     * @see Owner#notifyOwnershipRevoked(Ownable)
     * @param obj
     */
    public void notifyOwnershipRevoked(Ownable obj) {
        ownedObjects.remove(obj);
    }

    /**
     * @see Ownable#setOwner(Owner)
     * @param owner
     */
    public void setOwner(Owner owner) {
        if (currentOwner != null) {
            currentOwner.notifyOwnershipRevoked(target);
        }
        currentOwner = owner;
        if (owner != null) {
            owner.notifyOwnershipGranted(target);
        }
    }

    /**
     * @see Ownable#getOwner()
     * @return
     */
    public Owner getOwner() {
        return currentOwner;
    }

    /**
     * Set the owner of all currently owned children to null. If any of the
     * children are entities or components, they are removed from their creating
     * system or entity, respectively.
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
                Component<?> ownedComp = (Component<?>) owned;
                ownedComp.getEntity().remove(ownedComp.getType());
            }
        }
    }
}
