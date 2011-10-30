/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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
package com.googlecode.entreri.component;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.InitParams;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.ObjectProperty;

@InitParams({float.class, Object.class})
public class InitParamsComponent extends Component {
    private FloatProperty floatData;
    private ObjectProperty<Object> objectData;
    
    private InitParamsComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    protected void init(Object... initParams) throws Exception {
        if (initParams[0] == null)
            throw new NullPointerException("Invalid");
        
        floatData.set((Float) initParams[0], getIndex(), 0);
        objectData.set(initParams[1], getIndex(), 0);
    }
    
    public float getFloat() {
        return floatData.get(getIndex(), 0);
    }
    
    public Object getObject() {
        return objectData.get(getIndex(), 0);
    }
}
