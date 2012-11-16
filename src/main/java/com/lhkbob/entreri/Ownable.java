package com.lhkbob.entreri;

/**
 * <p>
 * An interface that designates something is ownable. Both entities and
 * components can be ownable. This can be used to create hierarchies of
 * components. As an example, a ParticleSystem component might own the Geometry
 * component that a task prepares for it.
 * <p>
 * The Ownable interface is the main entry-point for programmers to manage their
 * inter-entity/component relationships, and not {@link Owner}. The Owner
 * interface exists so that Ownable implementations can notify owners of
 * changes.
 * 
 * @author Michael Ludwig
 * 
 */
public interface Ownable {
    /**
     * <p>
     * Set the new owner of the given object. If the current owner before this
     * call is non-null, it must be first notified that its ownership has been
     * revoked, before granting ownership to the new owner.
     * <p>
     * If the new owner is null, the object becomes un-owned by anything.
     * 
     * @param owner The new owner
     */
    public void setOwner(Owner owner);

    /**
     * Get the current owner of this object. If null is returned, the object is
     * not owned by anything.
     * 
     * @return The current owner
     */
    public Owner getOwner();
}
