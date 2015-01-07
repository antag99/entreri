package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ReferenceListProperty}.
 *
 * @author Michael Ludwig
 */
public class ReferenceListPropertyTest {
    private void doDefaultValueTest(ReferenceListProperty<Object> property) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertNull(property.get(0));
    }

    private void doClonePolicy(ReferenceListProperty<Object> property, boolean clone) {
        List<Object> v1 = new ArrayList<>();

        property.setCapacity(2);
        property.set(0, v1);
        property.set(1, new ArrayList<>());

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
        doDefaultValueTest(new ReferenceListProperty<>(true));
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ReferenceListProperty<>(true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ReferenceListProperty<>(false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ReferenceListProperty<>(null));
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ReferenceListProperty<>(null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ReferenceListProperty<>(getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ReferenceListProperty<>(false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ReferenceListProperty<Object> p = new ReferenceListProperty<>(false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        List<Object> v1 = new ArrayList<>();
        ReferenceListProperty<Object> p = new ReferenceListProperty<>(false);
        assertNull(p.get(0));
        p.set(0, v1);
        assertSame(v1, p.get(0));
    }

    @Test
    public void testAddContainsRemove() {
        List<Object> v = new ArrayList<>();
        Object e = new Object();
        ReferenceListProperty<Object> p = new ReferenceListProperty<>(false);
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
        List<Object> v1 = new ArrayList<>();
        List<Object> v2 = new ArrayList<>();


        ReferenceListProperty<Object> p = new ReferenceListProperty<>(false);
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
        ReferenceListProperty<String> p = new ReferenceListProperty<>(false);
        List<String>[] data = p.getIndexedData();
        List<String> v = Arrays.asList("hello");
        p.set(0, v);
        assertSame(v, data[0]);
    }

    @Test
    public void testNullValuesAllowed() {
        ReferenceListProperty<Object> p = new ReferenceListProperty<>(false);
        p.set(0, new ArrayList<>());
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
        return ReferenceListPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
