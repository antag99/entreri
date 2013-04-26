package com.lhkbob.entreri;

import java.util.List;
import java.util.Set;

/**
 *
 */
abstract class ComponentFactoryProvider {
    public static interface Factory<T extends Component> {
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository);

        public List<PropertySpecification> getSpecification();

        public Set<Class<? extends Component>> getRequiredTypes();
    }

    public abstract <T extends Component> Factory<T> getFactory(Class<T> componentType);

    public static ComponentFactoryProvider getInstance() {
        // FIXME impl
        throw new UnsupportedOperationException();
    }
}
