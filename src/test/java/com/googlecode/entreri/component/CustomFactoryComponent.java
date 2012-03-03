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
package com.googlecode.entreri.component;

import java.util.Collections;
import java.util.Map;

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.ComponentDataFactory;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;

public class CustomFactoryComponent extends ComponentData<CustomFactoryComponent> {
    public IntProperty prop;
    
    public static class CustomFactory implements ComponentDataFactory<CustomFactoryComponent> {
        @Override
        public Map<?, PropertyFactory<?>> getPropertyFactories() {
            return Collections.<String, PropertyFactory<?>>singletonMap("prop", IntProperty.factory(1));
        }

        @Override
        public CustomFactoryComponent createInstance() {
            return new CustomFactoryComponent();
        }

        @Override
        public void setProperty(CustomFactoryComponent instance, Object key, Property property) {
            if (key.equals("prop")) {
                instance.prop = (IntProperty) property;
            } else {
                throw new RuntimeException();
            }
        }
    }
}
