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

            // make sure to not use generics since that is not supported by janino
            String source = ComponentFactoryProvider
                    .generateJavaCode(type, specification, false);
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
