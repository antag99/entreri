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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ReferenceMapProperty}.
 *
 * @author Michael Ludwig
 */
public class ReferenceMapPropertyTest {
    private void doDefaultValueTest(ReferenceMapProperty<Object, Object> property) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertNull(property.get(0));
    }

    private void doClonePolicy(ReferenceMapProperty<Object, Object> property, boolean clone) {
        Map<Object, Object> v1 = new HashMap<>();

        property.setCapacity(2);
        property.set(0, v1);
        property.set(1, new HashMap<>());

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertSame(v1, property.get(1));
        } else {
            // sets the default value on clone (does not leave the value alone)
            assertNull(property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new ReferenceMapProperty<>(true));
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ReferenceMapProperty<>(true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ReferenceMapProperty<>(false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ReferenceMapProperty<>(null));
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ReferenceMapProperty<>(null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ReferenceMapProperty<>(getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ReferenceMapProperty<>(false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ReferenceMapProperty<Object, Object> p = new ReferenceMapProperty<>(false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        Map<Object, Object> v1 = new HashMap<>();
        ReferenceMapProperty<Object, Object> p = new ReferenceMapProperty<>(false);
        assertNull(p.get(0));
        p.set(0, v1);
        assertSame(v1, p.get(0));
    }

    @Test
    public void testPutGetContainsRemove() {
        Map<Object, Object> m = new HashMap<>();
        Object k = new Object();
        Object v = new Object();
        Object v2 = new Object();

        ReferenceMapProperty<Object, Object> p = new ReferenceMapProperty<>(false);
        p.set(0, m);

        // totally new value
        assertFalse(p.contains(0, k));
        assertNull(p.put(0, k, v));
        assertTrue(m.containsKey(k));
        assertTrue(p.contains(0, k));
        assertSame(v, p.get(0, k));
        assertEquals(1, m.size());

        // overwrite a key
        assertSame(v, p.put(0, k, v2));
        assertTrue(m.containsKey(k));
        assertTrue(p.contains(0, k));
        assertSame(v2, p.get(0, k));
        assertEquals(1, m.size());

        // remove a key
        assertSame(v2, p.remove(0, k));
        assertFalse(m.containsKey(k));
        assertFalse(p.contains(0, k));
        assertEquals(0, m.size());
    }

    @Test
    public void testSwap() {
        Map<Object, Object> v1 = new HashMap<>();
        Map<Object, Object> v2 = new HashMap<>();

        ReferenceMapProperty<Object, Object> p = new ReferenceMapProperty<>(false);
        p.setCapacity(2);
        p.set(0, v1);
        p.set(1, v2);
        p.swap(0, 1);

        assertSame(v1, p.get(1));
        assertSame(v2, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertSame(v1, p.get(0));
        assertSame(v2, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        ReferenceMapProperty<String, String> p = new ReferenceMapProperty<>(false);
        Map<String, String>[] data = p.getIndexedData();

        Map<String, String> v = new HashMap<>();
        v.put("hello", "world");

        p.set(0, v);
        assertSame(v, data[0]);
    }

    @Test
    public void testNullValuesAllowed() {
        ReferenceMapProperty<Object, Object> p = new ReferenceMapProperty<>(false);
        p.set(0, new HashMap<>());
        assertNotNull(p.get(0));
        p.set(0, null);
        assertNull(p.get(0));
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return ReferenceMapPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
