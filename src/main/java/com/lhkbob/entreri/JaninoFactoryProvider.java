package com.lhkbob.entreri;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * JaninoFactoryProvider is a fallback component provider that uses the Janino runtime
 * compiler to generate and load component implementations.
 *
 * @author Michael Ludwig
 */
class JaninoFactoryProvider extends ComponentFactoryProvider {

    @Override
    public <T extends Component> Factory<T> getFactory(Class<T> componentType) {
        return new JaninoFactory<T>(componentType);
    }

    private static class JaninoFactory<T extends Component> implements Factory<T> {
        final Class<? extends AbstractComponent<T>> implType;
        final List<PropertySpecification> specification;

        final Constructor<? extends AbstractComponent<T>> ctor;

        @SuppressWarnings("unchecked")
        public JaninoFactory(Class<T> type) {
            String implName = ComponentFactoryProvider
                    .getImplementationClassName(type, true);
            specification = PropertySpecification.getSpecification(type);

            String source = ComponentFactoryProvider
                    .generateJavaCode(type, specification);
            SimpleCompiler compiler = new SimpleCompiler();
            compiler.setParentClassLoader(getClass().getClassLoader());

            try {
                compiler.cook(source);
            } catch (CompileException e) {
                throw new RuntimeException(
                        "Unexpected runtime compilation failure for " + type +
                        ", source:\n" + source, e);
            } catch (NoClassDefFoundError e) {
                throw new RuntimeException(
                        "Unexpected runtime compilation failure for " + type +
                        ", source:\n" + source, e);
            }

            try {
                implType = (Class<? extends AbstractComponent<T>>) compiler
                        .getClassLoader().loadClass(implName);
            } catch (ClassNotFoundException e) {
                // should never happen if compilation was successful
                throw new RuntimeException("Cannot find generated class for " + type, e);
            }

            try {
                ctor = implType.getConstructor(ComponentRepository.class);
            } catch (NoSuchMethodException e) {
                // should never happen since we're using the trusted generated source
                throw new RuntimeException("Cannot find expected constructor for " + type,
                                           e);
            }
        }

        @Override
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository) {
            try {
                return ctor.newInstance(forRepository);
            } catch (InstantiationException e) {
                throw new RuntimeException(
                        "Exception instantiating generated component impl", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "Exception instantiating generated component impl", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(
                        "Exception instantiating generated component impl", e);
            }
        }

        @Override
        public List<PropertySpecification> getSpecification() {
            return specification;
        }
    }
}
