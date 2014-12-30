package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ObjectProperty}.
 *
 * @author Michael Ludwig
 */
public class ObjectPropertyTest {
    private void doDefaultValueTest(ObjectProperty<Object> property) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertNull(property.get(0));
    }

    private void doClonePolicy(ObjectProperty<Object> property, boolean clone) {
        Object v1 = new Object();

        property.setCapacity(2);
        property.set(0, v1);
        property.set(1, new Object());

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
        doDefaultValueTest(new ObjectProperty<>(Object.class, true));
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ObjectProperty<>(Object.class, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ObjectProperty<>(Object.class, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ObjectProperty<>(Object.class, null));
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ObjectProperty<>(Object.class, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ObjectProperty<>(Object.class, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ObjectProperty<>(Object.class, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ObjectProperty<Object> p = new ObjectProperty<>(Object.class, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        Object v1 = new Object();
        ObjectProperty<Object> p = new ObjectProperty<>(Object.class, false);
        assertNull(p.get(0));
        p.set(0, v1);
        assertSame(v1, p.get(0));
    }

    @Test
    public void testSwap() {
        Object v1 = new Object();
        Object v2 = new Object();

        ObjectProperty<Object> p = new ObjectProperty<>(Object.class, false);
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
        ObjectProperty<String> p = new ObjectProperty<>(String.class, false);
        String[] data = p.getIndexedData();
        String v = "hello";
        p.set(0, v);
        assertSame(v, data[0]);
        assertTrue(String[].class.isInstance(data));
    }

    @Test
    public void testNullValuesAllowed() {
        ObjectProperty<Object> p = new ObjectProperty<>(Object.class, false);
        p.set(0, new Object());
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
        return ObjectPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
