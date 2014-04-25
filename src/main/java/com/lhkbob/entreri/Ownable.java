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
package com.lhkbob.entreri;

/**
 * <p/>
 * An interface that designates something is ownable. Both entities and components can be ownable. This can be
 * used to create hierarchies of components. As an example, a ParticleSystem component might own the Geometry
 * component that a task prepares for it.
 * <p/>
 * The Ownable interface is the main entry-point for programmers to manage their inter-entity/component
 * relationships, and not {@link Owner}. The Owner interface exists so that Ownable implementations can notify
 * owners of changes.
 *
 * @author Michael Ludwig
 */
public interface Ownable {
    /**
     * <p/>
     * Set the new owner of the given object. If the current owner before this call is non-null, it must be
     * first notified that its ownership has been revoked, before granting ownership to the new owner.
     * <p/>
     * If the new owner is null, the object becomes un-owned by anything.
     *
     * @param owner The new owner
     */
    public void setOwner(Owner owner);

    /**
     * Get the current owner of this object. If null is returned, the object is not owned by anything.
     *
     * @return The current owner
     */
    public Owner getOwner();
}
