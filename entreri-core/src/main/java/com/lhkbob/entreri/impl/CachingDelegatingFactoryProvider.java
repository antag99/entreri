/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CachingDelegatingFactoryProvider implements the ComponentFactoryProvider interface to
 * delegate to a list of factory providers and caches their created factories in a thread
 * safe manner so that actual implementations can be implemented simpler.
 *
 * @author Michael Ludwig
 */
public class CachingDelegatingFactoryProvider extends ComponentFactoryProvider {
    private final Map<Class<? extends Component>, Factory<?>> cachedFactories;

    private final CompiledFactoryProvider compiledFactoryProvider;
    private final JaninoFactoryProvider janinoFactoryProvider;

    private final Object lock;

    public CachingDelegatingFactoryProvider() {
        cachedFactories = new HashMap<Class<? extends Component>, Factory<? extends Component>>();
        compiledFactoryProvider = new CompiledFactoryProvider();
        janinoFactoryProvider = new JaninoFactoryProvider();

        lock = new Object();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Factory<T> getFactory(Class<T> componentType) {
        // blocking synchronization is easiest to do and it shouldn't be a high contention
        // point because factories are only gotten when the component repository is created
        // the first time for a system and type
        synchronized (lock) {
            Factory<?> cached = cachedFactories.get(componentType);
            if (cached != null) {
                return (Factory<T>) cached;
            }

            Factory<T> precompiled = compiledFactoryProvider.getFactory(componentType);
            if (precompiled != null) {
                cachedFactories.put(componentType, precompiled);
                return precompiled;
            }

            Factory<T> runtime = janinoFactoryProvider.getFactory(componentType);
            if (runtime != null) {
                cachedFactories.put(componentType, runtime);
                return runtime;
            }
        }

        throw new UnsupportedOperationException(
                "Unable to find or generate a component implementation for " +
                componentType);
    }
}
