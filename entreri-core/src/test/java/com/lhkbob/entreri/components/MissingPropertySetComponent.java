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
package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.Factory;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Invalid component type because it references a property type that doesn't have the
 * expected set method.
 */
public interface MissingPropertySetComponent extends Component {
    @Factory(MissingSetterFactory.class)
    public Object getValue();

    public void setValue(Object o);

    public static class MissingSetterProperty implements Property {
        public Object get(int index) {
            return null;
        }

        // we don't really have to implement these because the component
        // will fail validation
        @Override
        public void setCapacity(int size) {
        }

        @Override
        public int getCapacity() {
            return 0;
        }

        @Override
        public void swap(int indexA, int indexB) {
        }
    }

    public static class MissingSetterFactory
            implements PropertyFactory<MissingSetterProperty> {
        @Override
        public MissingSetterProperty create() {
            return new MissingSetterProperty();
        }

        @Override
        public void setDefaultValue(MissingSetterProperty property, int index) {
        }

        @Override
        public void clone(MissingSetterProperty src, int srcIndex,
                          MissingSetterProperty dst, int dstIndex) {
        }
    }
}