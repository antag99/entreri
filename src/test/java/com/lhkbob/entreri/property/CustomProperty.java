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

@Factory(CustomProperty.CustomFactoryWithAttributes.class)
public class CustomProperty implements ShareableProperty {
    private final ObjectProperty<Bletch> property;

    public CustomProperty() {
        property = new ObjectProperty<Bletch>();
    }

    @Override
    public IndexedDataStore getDataStore() {
        return property.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        property.setDataStore(store);
    }

    public void set(Bletch b, int index) {
        property.set(b, index);
    }

    public Bletch get(int index) {
        return property.get(index);
    }

    // FIXME might be wrong signature
    public void get(int index, Bletch b) {
        b.value = property.get(index).value;
    }

    public static class CustomFactoryWithAttributes
            extends AbstractPropertyFactory<CustomProperty> {
        public CustomFactoryWithAttributes(Attributes attrs) {
            super(attrs);
        }

        @Override
        public CustomProperty create() {
            return new CustomProperty();
        }

        @Override
        public void setDefaultValue(CustomProperty property, int index) {
            Bletch b = new Bletch();
            b.value = (attributes.hasAttribute(IntProperty.DefaultInt.class) ? 0
                                                                             : attributes
                               .getAttribute(IntProperty.DefaultInt.class).value());
            property.property.set(b, index);
        }
    }

    public static class Bletch {
        public int value;
    }
}
