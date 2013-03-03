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
 * Owner is a listener and tag interface so that {@link Ownable} implementations can
 * report ownership changes to their owners. This is used by both Components and Entities
 * to track which objects they own and disown them when they are removed from the
 * EntitySystem.
 *
 * @author Michael Ludwig
 */
public interface Owner {
    /**
     * Notify this Owner that it is now <tt>obj</tt>'s owner. This must only be called by
     * {@link Ownable} implementations in response to calls to {@link
     * Ownable#setOwner(Owner)}.
     *
     * @param obj The newly owned object
     */
    public void notifyOwnershipGranted(Ownable obj);

    /**
     * <p/>
     * Notify this Owner that it is no longer <tt>obj</tt>'s owner. This must only be
     * called by {@link Ownable} implementations in response to calls to {@link
     * Ownable#setOwner(Owner)}.
     * <p/>
     * Ownership is revoked when the Ownable is assigned a new owner, or the null owner
     * but was previously owned by this instance.
     *
     * @param obj The disowned object
     */
    public void notifyOwnershipRevoked(Ownable obj);
}