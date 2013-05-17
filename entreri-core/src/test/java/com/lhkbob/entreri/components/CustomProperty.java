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

import com.lhkbob.entreri.property.*;

@Factory(CustomProperty.CustomFactoryWithAttributes.class)
public class CustomProperty implements ShareableProperty<CustomProperty.Bletch> {
    private final ObjectProperty<Bletch> property;

    public CustomProperty() {
        property = new ObjectProperty<Bletch>();
    }

    @Override
    public Bletch createShareableInstance() {
        return new Bletch();
    }

    public void set(int index, Bletch b) {
        property.set(index, b);
    }

    public Bletch get(int index) {
        return property.get(index);
    }

    @Override
    public void get(int index, Bletch b) {
        b.value = property.get(index).value;
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

    public static class CustomFactoryWithAttributes
            implements PropertyFactory<CustomProperty> {
        private final Attributes attributes;

        public CustomFactoryWithAttributes(Attributes attrs) {
            attributes = attrs;
        }

        @Override
        public CustomProperty create() {
            return new CustomProperty();
        }

        @Override
        public void setDefaultValue(CustomProperty property, int index) {
            Bletch b = new Bletch();
            b.value = (!attributes.hasAttribute(IntProperty.DefaultInt.class) ? 0
                                                                              : attributes
                               .getAttribute(IntProperty.DefaultInt.class).value());
            property.set(index, b);
        }

        @Override
        public void clone(CustomProperty src, int srcIndex, CustomProperty dst,
                          int dstIndex) {
            // don't care about clone policy for the tests, but make it consistent
            // with value behavior for shareable property
            src.get(srcIndex, dst.get(dstIndex));
        }
    }

    public static class Bletch {
        public int value;
    }
}
