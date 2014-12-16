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
package com.lhkbob.entreri.impl.apt;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.impl.ComponentDataStore;
import com.lhkbob.entreri.impl.EntitySystemImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ComponentDataStoreFactory
 * =========================
 *
 * This is the default factory that uses reflection to invoke the static `create` method that each generated
 * Component proxy implementation provides to create the appropriate ComponentDataStore object.
 *
 * @author Michael Ludwig
 */
public class ComponentDataStoreFactory implements ComponentDataStore.Factory {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentDataStore<T> create(EntitySystemImpl system,
                                                              Class<T> componentType) {
        String implName = ComponentGenerator.getImplementationClassName(componentType, true);
        try {
            Class<?> impl = getClass().getClassLoader().loadClass(implName);
            Method staticCtor = impl.getDeclaredMethod("create", EntitySystemImpl.class);
            return (ComponentDataStore<T>) staticCtor.invoke(null, system);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalComponentDefinitionException(componentType.getName(),
                                                          "Unable to create backing data store", e);
        }
    }
}
