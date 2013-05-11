package com.lhkbob.entreri;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * CompiledFactoryProvider searches the classpath for existing class definitions of the
 * component types. This is suitable for generating component implementations as part of a
 * project build and then not needing to rely on Janino at runtime.
 *
 * @author Michael Ludwig
 */
class CompiledFactoryProvider extends ComponentFactoryProvider {
    @Override
    public <T extends Component> Factory<T> getFactory(Class<T> componentType) {
        try {
            return new CompiledFactory<T>(componentType);
        } catch (ClassNotFoundException cfe) {
            // if class is not present we just return null to delegate to Janino
            return null;
        }
    }

    private static class CompiledFactory<T extends Component> implements Factory<T> {
        final Class<? extends AbstractComponent<T>> implType;
        final List<PropertySpecification> specification;

        final Constructor<? extends AbstractComponent<T>> ctor;

        @SuppressWarnings("unchecked")
        public CompiledFactory(Class<T> type) throws ClassNotFoundException {
            String implName = ComponentFactoryProvider
                    .getImplementationClassName(type, true);

            Class<?> loaded = Class.forName(implName);

            // although the compiled classes should have been produced from the same
            // generated source used by Janino, we can't be certain because we're reading
            // them from the classpath, so this factory has to validate certain elements
            // of the component type
            if (!loaded.getSuperclass().equals(AbstractComponent.class)) {
                throw new IllegalStateException(
                        "Discovered impl. class does not extend AbstractComponent for " +
                        type);
            }
            Type paramType = ((ParameterizedType) loaded.getGenericSuperclass())
                    .getActualTypeArguments()[0];
            if (!paramType.equals(type)) {
                throw new IllegalStateException(
                        "Discovered impl. uses wrong type parameter for AbstractComponent, was " +
                        paramType + " instead of " + type);
            }
            if (!type.isAssignableFrom(loaded)) {
                throw new IllegalStateException(
                        "Discovered impl. does not implement the expected interface: " +
                        type);
            }

            // at this point it's a safe cast
            implType = (Class<? extends AbstractComponent<T>>) loaded;
            specification = PropertySpecification.getSpecification(type);

            try {
                ctor = implType.getConstructor(ComponentRepository.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Discovered impl. does not have mandated constructor for component: " +
                        type);
            }
        }


        @Override
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository) {
            try {
                return ctor.newInstance(forRepository);
            } catch (InstantiationException e) {
                throw new RuntimeException(
                        "Exception instantiating compiled component impl", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "Exception instantiating compiled component impl", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(
                        "Exception instantiating compiled component impl", e);
            }
        }

        @Override
        public List<PropertySpecification> getSpecification() {
            return specification;
        }
    }
}
