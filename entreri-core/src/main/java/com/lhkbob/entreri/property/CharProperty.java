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
package com.lhkbob.entreri.property;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * CharProperty is an implementation of Property that stores a single char value.
 *
 * @author Michael Ludwig
 */
@Factory(CharProperty.Factory.class)
public final class CharProperty implements Property {
    private char[] data;

    /**
     * Create an CharProperty.
     */
    public CharProperty() {
        data = new char[1];
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The array may be
     * longer than necessary for the number of components in the system. Data can be
     * accessed for a component directly using the component's index.
     *
     * @return The char data for all packed properties that this property has been packed
     *         with
     */
    public char[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     *
     * @return The object at the given offset for the given component
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public char get(int componentIndex) {
        return data[componentIndex];
    }

    /**
     * Store <var>val</var> in this property for the given component index.
     *
     * @param componentIndex The index of the component being modified
     * @param val            The value to store, can be null
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int componentIndex, char val) {
        data[componentIndex] = val;
    }

    @Override
    public void swap(int a, int b) {
        char t = data[a];
        data[a] = data[b];
        data[b] = t;
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size);
    }

    /**
     * Factory to create CharProperties. Properties annotated with DefaultChar will use
     * that value as the default for all components.
     *
     * @author Michael Ludwig
     */
    public static class Factory implements PropertyFactory<CharProperty> {
        private final char defaultValue;
        private final Clone.Policy policy;

        public Factory(Attributes attrs) {
            defaultValue = attrs.hasAttribute(DefaultChar.class) ? attrs
                    .getAttribute(DefaultChar.class).value() : '\0';
            policy = attrs.hasAttribute(Clone.class) ? attrs.getAttribute(Clone.class)
                                                            .value()
                                                     : Clone.Policy.JAVA_DEFAULT;
        }

        public Factory(char defaultValue) {
            this.defaultValue = defaultValue;
            policy = Clone.Policy.JAVA_DEFAULT;
        }

        @Override
        public CharProperty create() {
            return new CharProperty();
        }

        @Override
        public void setDefaultValue(CharProperty property, int index) {
            property.set(index, defaultValue);
        }

        @Override
        public void clone(CharProperty src, int srcIndex, CharProperty dst,
                          int dstIndex) {
            switch (policy) {
            case DISABLE:
                // assign default value
                setDefaultValue(dst, dstIndex);
                break;
            case INVOKE_CLONE:
                // fall through, since default implementation of INVOKE_CLONE is to
                // just function like JAVA_DEFAULT
            case JAVA_DEFAULT:
                dst.set(dstIndex, src.get(srcIndex));
                break;
            default:
                throw new UnsupportedOperationException(
                        "Enum value not supported: " + policy);
            }
        }
    }

    /**
     * Default char attribute for properties.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultChar {
        char value();
    }
}
