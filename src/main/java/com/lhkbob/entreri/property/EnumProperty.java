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

import java.util.Arrays;

/**
 * EnumProperty is a specialized generic property for enum values that stores just the ordinal values of a
 * specific enum class in a packed int array.  The type-mapping of component implementation generation
 * automatically uses an EnumProperty for any enum properties unless there's an explicit mapping declared in
 * META-INF.
 *
 * @author Michael Ludwig
 */
@GenericProperty(superClass = Enum.class)
@Factory(EnumProperty.Factory.class)
public class EnumProperty implements Property {
    private final Enum[] values;
    private int[] data;

    /**
     * Create an EnumProperty for the given enum class type.
     *
     * @param enumType The enum class
     */
    public EnumProperty(Class<? extends Enum> enumType) {
        values = enumType.getEnumConstants();
        data = new int[1];
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than necessary for
     * the number of components in the system. Data can be accessed for a component directly using the
     * component's index. The int values correspond to the ordinals of the enum class stored by this
     * EnumProperty.
     *
     * @return The int data for all packed properties that this property has been packed with
     */
    public int[] getIndexedData() {
        return data;
    }

    public Enum get(int index) {
        return values[data[index]];
    }

    public void set(int index, Enum value) {
        data[index] = value.ordinal();
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size);
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void swap(int indexA, int indexB) {
        int ord = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = ord;
    }

    /**
     * Factory implementation for EnumProperty.
     */
    public static class Factory implements PropertyFactory<EnumProperty> {
        private final Class<? extends Enum> enumType;
        private final Clone.Policy policy;

        /**
         * Default factory constructor for use by the implementation.
         *
         * @param attrs The target attributes
         */
        @SuppressWarnings("unchecked")
        public Factory(Attributes attrs) {
            enumType = (Class<? extends Enum<?>>) attrs.getPropertyType();
            policy = attrs.hasAttribute(Clone.class) ? attrs.getAttribute(Clone.class).value()
                                                     : Clone.Policy.JAVA_DEFAULT;
        }

        /**
         * Create a new factory for the given enum type.
         *
         * @param enumType The enum class
         */
        public Factory(Class<? extends Enum> enumType) {
            this.enumType = enumType;
            policy = Clone.Policy.JAVA_DEFAULT;
        }

        @Override
        public EnumProperty create() {
            return new EnumProperty(enumType);
        }

        @Override
        public void setDefaultValue(EnumProperty property, int index) {
            property.data[index] = 0;
        }

        @Override
        public void clone(EnumProperty src, int srcIndex, EnumProperty dst, int dstIndex) {
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
}
