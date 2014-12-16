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
package com.lhkbob.entreri.components;

import com.lhkbob.entreri.attr.DefaultInt;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.property.Property;

public class CustomProperty implements Property<CustomProperty>, Property.ReferenceSemantics {
    private final int dflt;
    private final ObjectProperty<Bletch> property;

    public CustomProperty(int dflt) {
        this.dflt = dflt;
        property = new ObjectProperty<>(true);
    }

    public CustomProperty(DefaultInt dflt) {
        this(dflt == null ? 0 : dflt.value());
    }

    public void set(int index, Bletch b) {
        get(index).value = b.value;
    }

    public Bletch get(int index) {
        return property.get(index);
    }

    public void get(int index, Bletch b) {
        b.value = get(index).value;
    }

    @Override
    public void setCapacity(int size) {
        property.setCapacity(size);
    }

    @Override
    public int getCapacity() {
        return property.getCapacity();
    }

    @Override
    public void swap(int indexA, int indexB) {
        property.swap(indexA, indexB);
    }

    @Override
    public void setDefaultValue(int index) {
        Bletch b = new Bletch();
        b.value = dflt;
        property.set(index, b);
    }

    @Override
    public void clone(CustomProperty src, int srcIndex, int dstIndex) {
        set(dstIndex, src.get(srcIndex));
    }

    public static class Bletch {
        public int value;
    }
}
