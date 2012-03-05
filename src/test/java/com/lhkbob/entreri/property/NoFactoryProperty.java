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
package com.lhkbob.entreri.property;

import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.CompactAwareProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.PropertyFactory;

public class NoFactoryProperty implements CompactAwareProperty {
    private final IntProperty property;
    
    private boolean compacted;
    
    public NoFactoryProperty(int size) {
        property = new IntProperty(size);
    }
    
    // add a 3rd arg so this does not match the method checked
    public static PropertyFactory<NoFactoryProperty> createFactory(final int size, final int dflt, Object extraArg) {
        return new AbstractPropertyFactory<NoFactoryProperty>() {
            @Override
            public void setDefaultValue(NoFactoryProperty property, int index) {
                property.property.set(dflt, index, 0);
            }
            
            @Override
            public NoFactoryProperty create() {
                return new NoFactoryProperty(size);
            }
        };
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return property.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        property.setDataStore(store);
    }

    @Override
    public void onCompactComplete() {
        compacted = true;
    }
    
    public boolean wasCompacted() {
        return compacted;
    }
}
