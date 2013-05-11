package com.lhkbob.entreri;

import java.util.HashMap;
import java.util.Map;

/**
 * CachingDelegatingFactoryProvider implements the ComponentFactoryProvider interface to
 * delegate to a list of factory providers and caches their created factories in a thread
 * safe manner so that actual implementations can be implemented simpler.
 *
 * @author Michael Ludwig
 */
class CachingDelegatingFactoryProvider extends ComponentFactoryProvider {
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
