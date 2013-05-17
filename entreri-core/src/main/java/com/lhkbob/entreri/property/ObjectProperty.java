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
package com.lhkbob.entreri.property;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * ObjectProperty is an implementation of Property that stores the property data as a
 * number of packed Object references for each property. Because it is not primitive data,
 * cache locality will suffer compared to the primitive property types, but it will allow
 * you to store arbitrary objects.
 *
 * @author Michael Ludwig
 */
@Factory(ObjectProperty.Factory.class)
public final class ObjectProperty<T> implements Property {
    private Object[] data;

    /**
     * Create an ObjectProperty.
     */
    public ObjectProperty() {
        data = new Object[1];
    }

    /**
     * Return a PropertyFactory that creates ObjectProperties with the given element size
     * and default value.
     *
     * @param <T>
     * @param dflt The default value assigned to each component and element
     *
     * @return A PropertyFactory for ObjectProperty
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> PropertyFactory<ObjectProperty<T>> factory(T dflt) {
        PropertyFactory superRaw = new Factory(dflt);
        return superRaw;
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The array may be
     * longer than necessary for the number of components in the system. Data can be
     * accessed for a component directly using the component's index.
     *
     * @return The Object data for all packed properties that this property has been
     *         packed with
     */
    public Object[] getIndexedData() {
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
    @SuppressWarnings("unchecked")
    public T get(int componentIndex) {
        return (T) data[componentIndex];
    }

    /**
     * Store <var>val</var> in this property for the given component index.
     *
     * @param val            The value to store, can be null
     * @param componentIndex The index of the component being modified
     *
     * @throws ArrayIndexOutOfBoundsException if the componentIndex is invalid
     */
    public void set(T val, int componentIndex) {
        data[componentIndex] = val;
    }

    @Override
    public void swap(int a, int b) {
        Object t = data[a];
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
     * Factory to create ObjectProperties.
     *
     * @author Michael Ludwig
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static class Factory implements PropertyFactory<ObjectProperty> {
        private final Object defaultValue;
        private final Clone.Policy policy;

        public Factory(Attributes attrs) {
            defaultValue = null;
            policy = attrs.hasAttribute(Clone.class) ? attrs.getAttribute(Clone.class)
                                                            .value()
                                                     : Clone.Policy.JAVA_DEFAULT;
        }

        public Factory(Object defaultValue) {
            this.defaultValue = defaultValue;
            policy = Clone.Policy.JAVA_DEFAULT;
        }

        @Override
        public ObjectProperty create() {
            return new ObjectProperty();
        }

        @Override
        public void setDefaultValue(ObjectProperty property, int index) {
            property.set(defaultValue, index);
        }

        @Override
        public void clone(ObjectProperty src, int srcIndex, ObjectProperty dst,
                          int dstIndex) {
            switch (policy) {
            case DISABLE:
                // assign default value
                setDefaultValue(dst, dstIndex);
                break;
            case INVOKE_CLONE:
                Object orig = src.get(srcIndex);
                if (orig instanceof Cloneable) {
                    try {
                        // if they implemented Cloneable properly, clone() should
                        // be public and take no arguments
                        Method cloneMethod = orig.getClass().getMethod("clone");
                        Object cloned = cloneMethod.invoke(orig);
                        dst.set(cloned, dstIndex);
                        break;
                    } catch (Exception e) {
                        // if they implement Cloneable, this shouldn't fail
                        // and if it does it's not really our fault
                        throw new RuntimeException(e);
                    }
                }
                // else fall through to java default
            case JAVA_DEFAULT:
                dst.set(src.get(srcIndex), dstIndex);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Enum value not supported: " + policy);
            }
        }
    }
}
