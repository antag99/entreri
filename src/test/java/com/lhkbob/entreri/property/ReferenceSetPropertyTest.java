package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ReferenceSetProperty}.
 *
 * @author Michael Ludwig
 */
public class ReferenceSetPropertyTest {
    private void doDefaultValueTest(ReferenceSetProperty<Object> property) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertNull(property.get(0));
    }

    private void doClonePolicy(ReferenceSetProperty<Object> property, boolean clone) {
        Set<Object> v1 = new HashSet<>();

        property.setCapacity(2);
        property.set(0, v1);
        property.set(1, new HashSet<>());

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
        doDefaultValueTest(new ReferenceSetProperty<>(true));
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ReferenceSetProperty<>(true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ReferenceSetProperty<>(false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ReferenceSetProperty<>(null));
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ReferenceSetProperty<>(null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ReferenceSetProperty<>(getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ReferenceSetProperty<>(false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ReferenceSetProperty<Object> p = new ReferenceSetProperty<>(false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        Set<Object> v1 = new HashSet<>();
        ReferenceSetProperty<Object> p = new ReferenceSetProperty<>(false);
        assertNull(p.get(0));
        p.set(0, v1);
        assertSame(v1, p.get(0));
    }

    @Test
    public void testAddContainsRemove() {
        Set<Object> v = new HashSet<>();
        Object e = new Object();
        ReferenceSetProperty<Object> p = new ReferenceSetProperty<>(false);
        p.set(0, v);

        assertFalse(p.contains(0, e));
        assertTrue(p.add(0, e));
        assertTrue(v.contains(e));
        assertTrue(p.contains(0, e));
        assertEquals(1, v.size());

        assertTrue(p.remove(0, e));
        assertFalse(v.contains(e));
        assertFalse(p.contains(0, e));
        assertEquals(0, v.size());
    }

    @Test
    public void testSwap() {
        Set<Object> v1 = new HashSet<>();
        Set<Object> v2 = new HashSet<>();


        ReferenceSetProperty<Object> p = new ReferenceSetProperty<>(false);
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
        ReferenceSetProperty<String> p = new ReferenceSetProperty<>(false);
        Set<String>[] data = p.getIndexedData();
        Set<String> v = new HashSet<>(Arrays.asList("hello"));
        p.set(0, v);
        assertSame(v, data[0]);
    }

    @Test
    public void testNullValuesAllowed() {
        ReferenceSetProperty<Object> p = new ReferenceSetProperty<>(false);
        p.set(0, new HashSet<>());
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
        return ReferenceSetPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
