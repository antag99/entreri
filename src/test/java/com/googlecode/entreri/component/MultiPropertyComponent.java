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

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.annot.Factory;
import com.googlecode.entreri.annot.Parameter;
import com.googlecode.entreri.annot.Parameters;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.FloatPropertyFactory;
import com.googlecode.entreri.property.MultiParameterProperty;
import com.googlecode.entreri.property.NoParameterProperty;

/**
 * A ComponentData that tests a variety of property constructors.
 * 
 * @author Michael Ludwig
 */
public class MultiPropertyComponent extends ComponentData {
    @Parameters({@Parameter(type=int.class, value="2"),
                 @Parameter(type=float.class, value="0.3")})
    protected MultiParameterProperty multi;
    
    protected NoParameterProperty noparams;
    
    @Factory(FloatPropertyFactory.class)
    protected FloatProperty fromFactory;
    
    protected MultiPropertyComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public void setFloat(int offset, float f) {
        multi.setFloat(offset + getIndex() * 2, f);
    }
    
    public float getFloat(int offset) {
        return multi.getFloat(offset + getIndex() * 2);
    }
    
    public NoParameterProperty getCompactProperty() {
        return noparams;
    }
    
    public void setFactoryFloat(float f) {
        fromFactory.set(f, getIndex(), 0);
    }
    
    public float getFactoryFloat() {
        return fromFactory.get(getIndex(), 0);
    }
    
    @Override
    protected void init(Object... initParams) throws Exception {
    }
}
