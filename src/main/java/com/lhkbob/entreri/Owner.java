package com.lhkbob.entreri;

/**
 * Owner is a listener and tag interface so that {@link Ownable} implementations
 * can report ownership changes to their owners. This is used by both Components
 * and Entities to track which objects they own and disown them when they are
 * removed from the EntitySystem.
 * 
 * @author Michael Ludwig
 * 
 */
public interface Owner {
    /**
     * Notify this Owner that it is now <tt>obj</tt>'s owner. This must only be
     * called by {@link Ownable} implementations in response to calls to
     * {@link Ownable#setOwner(Owner)}.
     * 
     * @param obj The newly owned object
     */
    public void notifyOwnershipGranted(Ownable obj);

    /**
     * <p>
     * Notify this Owner that it is no longer <tt>obj</tt>'s owner. This must
     * only be called by {@link Ownable} implementations in response to calls to
     * {@link Ownable#setOwner(Owner)}.
     * <p>
     * Ownership is revoked when the Ownable is assigned a new owner, or the
     * null owner but was previously owned by this instance.
     * 
     * @param obj The disowned object
     */
    public void notifyOwnershipRevoked(Ownable obj);
}
