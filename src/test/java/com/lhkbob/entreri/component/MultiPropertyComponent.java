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
package com.lhkbob.entreri.component;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.annot.DefaultValue;
import com.lhkbob.entreri.annot.ElementSize;
import com.lhkbob.entreri.annot.Factory;
import com.lhkbob.entreri.property.FloatProperty;
import com.lhkbob.entreri.property.FloatPropertyFactory;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.LongProperty;
import com.lhkbob.entreri.property.NoParameterProperty;

/**
 * A ComponentData that tests a variety of property constructors.
 * 
 * @author Michael Ludwig
 */
public class MultiPropertyComponent extends ComponentData<MultiPropertyComponent> {
    @DefaultValue(defaultLong=Long.MAX_VALUE)
    @ElementSize(3)
    protected LongProperty longProp; // should use factory(int, long)
    
    @DefaultValue(defaultFloat=0.5f)
    protected FloatProperty floatProp; // should use factory(1, float)
    
    @ElementSize(2)
    protected IntProperty intProp; // should use factory(int)
    
    @Factory(FloatPropertyFactory.class)
    protected FloatProperty fromFactory; // should create a new FloatPropertyFactory
    
    // this should find the createFactory() method
    protected NoParameterProperty noparams;
    
    protected MultiPropertyComponent() {}
    
    public void setLong(long i1, long i2, long i3) {
        longProp.set(i1, getIndex(), 0);
        longProp.set(i2, getIndex(), 1);
        longProp.set(i3, getIndex(), 2);
    }
    
    public long[] getLong() {
        long[] v = new long[] { longProp.get(getIndex(), 0), longProp.get(getIndex(), 1), longProp.get(getIndex(), 2) };
        return v;
    }
    
    public void setInt(int i1, int i2) {
        intProp.set(i1, getIndex(), 0);
        intProp.set(i2, getIndex(), 1);
    }
    
    public int[] getInt() {
        int[] v = new int[] { intProp.get(getIndex(), 0), intProp.get(getIndex(), 1) };
        return v;
    }
    
    public void setFloat(float f) {
        floatProp.set(f, getIndex(), 0);
    }
    
    public float getFloat() {
        return floatProp.get(getIndex(), 0);
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
}
