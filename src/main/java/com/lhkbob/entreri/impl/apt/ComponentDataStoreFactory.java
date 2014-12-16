package com.lhkbob.entreri.impl.apt;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.impl.ComponentDataStore;
import com.lhkbob.entreri.impl.EntitySystemImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
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
            throw new RuntimeException("Unable to create backing ComponentDataStore for " + componentType, e);
        }
    }
}
