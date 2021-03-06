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

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.BooleanProperty}.
 *
 * @author Michael Ludwig
 */
public class BooleanPropertyTest {
    private void doDefaultValueTest(BooleanProperty property, boolean expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(BooleanProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, true);
        property.set(1, false);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertTrue(property.get(1));
        } else {
            assertFalse(property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new BooleanProperty(true, true), true);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new BooleanProperty(false, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new BooleanProperty(false, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new BooleanProperty(getDefaultValue(), null), true);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultBoolean annotation maps to false
        doDefaultValueTest(new BooleanProperty(null, null), false);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new BooleanProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new BooleanProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new BooleanProperty(false, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        BooleanProperty p = new BooleanProperty(false, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        BooleanProperty p = new BooleanProperty(false, false);
        assertFalse(p.get(0));
        p.set(0, true);
        assertTrue(p.get(0));
    }

    @Test
    public void testSwap() {
        BooleanProperty p = new BooleanProperty(false, false);
        p.setCapacity(2);
        p.set(0, true);
        p.set(1, false);
        p.swap(0, 1);

        assertTrue(p.get(1));
        assertFalse(p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertTrue(p.get(0));
        assertFalse(p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        BooleanProperty p = new BooleanProperty(false, false);
        boolean[] data = p.getIndexedData();
        p.set(0, true);
        assertTrue(data[0]);
    }

    @DefaultBoolean(true)
    private static DefaultBoolean getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultBoolean.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return BooleanPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
