/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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
package com.lhkbob.entreri.components;

import com.lhkbob.entreri.*;

/**
 * A test component that is a class to test component validation, e.g. this type is
 * invalid.
 *
 * @author Michael Ludwig
 */
public class BadConstructorComponent implements Component {

    @Override
    public EntitySystem getEntitySystem() {
        return null;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean isFlyweight() {
        return false;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public void updateVersion() {
    }

    @Override
    public Class<? extends Component> getType() {
        return null;
    }

    @Override
    public void setOwner(Owner owner) {
    }

    @Override
    public Owner getOwner() {
        return null;
    }

    @Override
    public Owner notifyOwnershipGranted(Ownable obj) {
        return null;
    }

    @Override
    public void notifyOwnershipRevoked(Ownable obj) {
    }
}
