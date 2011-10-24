/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) ${year}, ${owner}
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
package com.googlecode.entreri;

/**
 * Controllers are functional processors of the entities and components within
 * an EntitySystem. Different Controller implementations have different
 * purposes, such as updating transforms, computing physics or AI, or rendering
 * a scene. Generally controller implementations should be as small and as
 * independent as possible to allow for reuse and easy composability.
 * 
 * @author Michael Ludwig
 */
public interface Controller {
    /**
     * Invoke operations on the given EntitySystem that must occur before the
     * main processing of the frame. All controllers in a system will have their
     * preProcess() method called before the first process() method is called.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void preProcess(EntitySystem system, float dt);

    /**
     * Invoke controller specific operations to process the EntitySystem. All
     * controllers will have already had their preProcess() method invoked and
     * none will have postProcess() invoked until process() has completed for
     * all controllers in the system.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void process(EntitySystem system, float dt);

    /**
     * Invoked at the end of a processing phase after all controllers in a
     * system have completed their process() methods.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void postProcess(EntitySystem system, float dt);
}
