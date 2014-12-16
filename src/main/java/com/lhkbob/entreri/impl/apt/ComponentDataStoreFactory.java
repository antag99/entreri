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
