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

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.EnumProperty}.
 *
 * @author Michael Ludwig
 */
public class EnumPropertyTest {
    public static enum TestEnum {
        V1,
        V2,
        V3
    }

    private void doDefaultValueTest(EnumProperty<TestEnum> property, TestEnum expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(EnumProperty<TestEnum> property, boolean clone) {
        property.setCapacity(2);
        property.set(0, TestEnum.V3);
        property.set(1, TestEnum.V1);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals(TestEnum.V3, property.get(1));
        } else {
            assertEquals(TestEnum.V1, property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new EnumProperty<>(TestEnum.V3, true), TestEnum.V3);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new EnumProperty<>(TestEnum.V1, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new EnumProperty<>(TestEnum.V1, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new EnumProperty<>(TestEnum.class, getDefaultValue(), null), TestEnum.V3);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultEnum annotation maps to 0
        doDefaultValueTest(new EnumProperty<>(TestEnum.class, null, null), TestEnum.V1);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new EnumProperty<>(TestEnum.class, null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new EnumProperty<>(TestEnum.class, null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new EnumProperty<>(TestEnum.V1, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        EnumProperty<TestEnum> p = new EnumProperty<>(TestEnum.V1, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        EnumProperty<TestEnum> p = new EnumProperty<>(TestEnum.V1, false);
        assertEquals(TestEnum.V1, p.get(0));
        p.set(0, TestEnum.V2);
        assertEquals(TestEnum.V2, p.get(0));
    }

    @Test
    public void testSwap() {
        EnumProperty<TestEnum> p = new EnumProperty<>(TestEnum.V1, false);
        p.setCapacity(2);
        p.set(0, TestEnum.V3);
        p.set(1, TestEnum.V1);
        p.swap(0, 1);

        assertEquals(TestEnum.V3, p.get(1));
        assertEquals(TestEnum.V1, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals(TestEnum.V3, p.get(0));
        assertEquals(TestEnum.V1, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        EnumProperty<TestEnum> p = new EnumProperty<>(TestEnum.V1, false);
        int[] data = p.getIndexedData();
        p.set(0, TestEnum.V3);
        assertEquals(TestEnum.V3.ordinal(), data[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testNoNullValues() {
        EnumProperty<TestEnum> p = new EnumProperty<>(TestEnum.V1, false);
        p.set(0, null);
    }

    @DefaultEnum(ordinal = 2)
    private static DefaultEnum getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultEnum.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return EnumPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
