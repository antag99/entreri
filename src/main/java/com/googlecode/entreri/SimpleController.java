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
package com.googlecode.entreri;

/**
 * SimpleController implements Controller by performing no action on each of
 * Controller's process or event hooks. Subclasses can override just the methods
 * they are interested in implementing.
 * 
 * @author Michael Ludwig
 */
public class SimpleController implements Controller {
    private EntitySystem system;
    
    @Override
    public void preProcess(float dt) {
        // do nothing in base class
    }

    @Override
    public void process(float dt) {
        // do nothing in base class
    }

    @Override
    public void postProcess(float dt) {
        // do nothing in base class
    }

    @Override
    public void init(EntitySystem system) {
        if (this.system != null)
            throw new IllegalStateException("Controller is already used in another EntitySystem");
        this.system = system;
    }
    
    @Override
    public void destroy() {
        system = null; 
    }

    @Override
    public void onEntityAdd(Entity e) {
        // do nothing in base class
    }

    @Override
    public void onEntityRemove(Entity e) {
        // do nothing in base class
    }

    @Override
    public void onComponentAdd(Component<?> c) {
        // do nothing in base class
    }

    @Override
    public void onComponentRemove(Component<?> c) {
        // do nothing in base class
    }

    @Override
    public EntitySystem getEntitySystem() {
        return system;
    }
}
