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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * CompiledFactoryProvider searches the classpath for existing class definitions of the
 * component types. This is suitable for generating component implementations as part of a
 * project build in conjunction with the {@link ComponentImplementationProcessor}.
 *
 * @author Michael Ludwig
 */
class CompiledFactoryProvider extends ComponentFactoryProvider {
    @Override
    public <T extends Component> Factory<T> getFactory(Class<T> componentType) {
        try {
            return new CompiledFactory<>(componentType);
        } catch (ClassNotFoundException cfe) {
            // if class is not present we just return null to delegate to other providers
            // (which could be Janino-based, but we're not using that right now)
            return null;
        }
    }

    private static class CompiledFactory<T extends Component> implements Factory<T> {
        final Class<? extends AbstractComponent<T>> implType;
        final ComponentSpecification specification;

        final Constructor<? extends AbstractComponent<T>> ctor;

        @SuppressWarnings("unchecked")
        public CompiledFactory(Class<T> type) throws ClassNotFoundException {
            specification = ComponentSpecification.Factory.fromClass(type);
            String implName = ComponentFactoryProvider
                    .getImplementationClassName(specification, true);

            Class<?> loaded = Class.forName(implName);
            // although the compiled classes should have been produced from the same
            // generated source used by APT, we can't be certain because we're reading
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
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(
                        "Exception instantiating compiled component impl", e);
            }
        }

        @Override
        public ComponentSpecification getSpecification() {
            return specification;
        }
    }
}
