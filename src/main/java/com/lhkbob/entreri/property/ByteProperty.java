/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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
 * ByteProperty
 * ============
 *
 * ByteProperty is an implementation of Property that stores a single byte value.
 *
 * @author Michael Ludwig
 */
@Factory(ByteProperty.Factory.class)
public final class ByteProperty implements Property {
    private byte[] data;

    /**
     * Create an ByteProperty.
     */
    public ByteProperty() {
        data = new byte[1];
    }

    /**
     * Return the backing byte array of this property. The array may be longer than necessary for the number
     * of components in the system. Data can be accessed for a component directly using the component's
     * index.
     *
     * @return The byte data for all packed properties that this property has been packed with
     */
    public byte[] getIndexedData() {
        return data;
    }

    /**
     * Get the value stored in this property for the given component index.
     *
     * @param componentIndex The component's index
     * @return The object at the given offset for the given component
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public byte get(int componentIndex) {
        return data[componentIndex];
    }

    /**
     * Store `val` in this property for the given component index.
     *
     * @param componentIndex The index of the component being modified
     * @param val            The value to store
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(int componentIndex, byte val) {
        data[componentIndex] = val;
    }

    @Override
    public void swap(int a, int b) {
        byte t = data[a];
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
     * Factory to create ByteProperties. Properties annotated with DefaultByte will use that value as the
     * default for all components.
     *
     * @author Michael Ludwig
     */
    public static class Factory implements PropertyFactory<ByteProperty> {
        private final byte defaultValue;
        private final Clone.Policy policy;

        public Factory(Attributes attrs) {
            defaultValue =
                    attrs.hasAttribute(DefaultByte.class) ? attrs.getAttribute(DefaultByte.class).value() : 0;
            policy = attrs.hasAttribute(Clone.class) ? attrs.getAttribute(Clone.class).value()
                                                     : Clone.Policy.JAVA_DEFAULT;
        }

        public Factory(byte defaultValue) {
            this.defaultValue = defaultValue;
            policy = Clone.Policy.JAVA_DEFAULT;
        }

        @Override
        public ByteProperty create() {
            return new ByteProperty();
        }

        @Override
        public void setDefaultValue(ByteProperty property, int index) {
            property.set(index, defaultValue);
        }

        @Override
        public void clone(ByteProperty src, int srcIndex, ByteProperty dst, int dstIndex) {
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
                throw new UnsupportedOperationException("Enum value not supported: " + policy);
            }
        }
    }

    /**
     * Default byte attribute for properties.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultByte {
        byte value();
    }
}
