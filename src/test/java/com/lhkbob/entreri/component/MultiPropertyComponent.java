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
import com.lhkbob.entreri.property.Factory;
import com.lhkbob.entreri.property.*;
import com.lhkbob.entreri.property.FloatProperty.DefaultFloat;
import com.lhkbob.entreri.property.LongProperty.DefaultLong;

/**
 * A ComponentData that tests a variety of property constructors.
 *
 * @author Michael Ludwig
 */
public class MultiPropertyComponent extends ComponentData<MultiPropertyComponent> {
    @DefaultLong(Long.MAX_VALUE)
    protected LongProperty longProp;

    @DefaultFloat(0.5f)
    protected FloatProperty floatProp;

    protected IntProperty intProp;

    @Factory(FloatPropertyFactory.class)
    protected FloatProperty fromFactory; // should create a new FloatPropertyFactory

    // this should find the createFactory() method
    protected NoParameterProperty noparams;

    protected MultiPropertyComponent() {
    }

    public void setLong(long i) {
        longProp.set(i, getIndex());
    }

    public long[] getLong() {
        long[] v = new long[] { longProp.get(getIndex()) };
        return v;
    }

    public void setInt(int i) {
        intProp.set(i, getIndex());
    }

    public int[] getInt() {
        int[] v = new int[] { intProp.get(getIndex()) };
        return v;
    }

    public void setFloat(float f) {
        floatProp.set(f, getIndex());
    }

    public float getFloat() {
        return floatProp.get(getIndex());
    }

    public NoParameterProperty getCompactProperty() {
        return noparams;
    }

    public void setFactoryFloat(float f) {
        fromFactory.set(f, getIndex());
    }

    public float getFactoryFloat() {
        return fromFactory.get(getIndex());
    }
}
