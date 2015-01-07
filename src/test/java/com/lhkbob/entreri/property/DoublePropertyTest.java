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
 * Unit tests for {@link com.lhkbob.entreri.property.DoubleProperty}.
 *
 * @author Michael Ludwig
 */
public class DoublePropertyTest {
    private void doDefaultValueTest(DoubleProperty property, double expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0), 0.00001);
    }

    private void doClonePolicy(DoubleProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, 5.0);
        property.set(1, 0.0);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals(5.0, property.get(1), 0.00001);
        } else {
            assertEquals(0.0, property.get(1), 0.00001);
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new DoubleProperty(5.0, true), 5.0);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new DoubleProperty(0.0, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new DoubleProperty(0.0, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new DoubleProperty(getDefaultValue(), null), 5.0);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultDouble annotation maps to 0
        doDefaultValueTest(new DoubleProperty(null, null), 0.0);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new DoubleProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new DoubleProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new DoubleProperty(0.0, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        DoubleProperty p = new DoubleProperty(0.0, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        DoubleProperty p = new DoubleProperty(0.0, false);
        assertEquals(0.0, p.get(0), 0.00001);
        p.set(0, 5.0);
        assertEquals(5.0, p.get(0), 0.00001);
    }

    @Test
    public void testSwap() {
        DoubleProperty p = new DoubleProperty(0.0, false);
        p.setCapacity(2);
        p.set(0, 5.0);
        p.set(1, 0.0);
        p.swap(0, 1);

        assertEquals(5.0, p.get(1), 0.00001);
        assertEquals(0.0, p.get(0), 0.00001);

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals(5.0, p.get(0), 0.00001);
        assertEquals(0.0, p.get(1), 0.00001);
    }

    @Test
    public void testGetIndexedData() {
        DoubleProperty p = new DoubleProperty(0.0, false);
        double[] data = p.getIndexedData();
        p.set(0, 5);
        assertEquals(5.0, data[0], 0.00001);
    }

    @DefaultDouble(5.0)
    private static DefaultDouble getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultDouble.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return DoublePropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
