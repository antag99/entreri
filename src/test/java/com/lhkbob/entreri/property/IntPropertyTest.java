package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.IntProperty}.
 *
 * @author Michael Ludwig
 */
public class IntPropertyTest {
    private void doDefaultValueTest(IntProperty property, int expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(IntProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, 5);
        property.set(1, 0);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals(5, property.get(1));
        } else {
            assertEquals(0, property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new IntProperty(5, true), 5);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new IntProperty(0, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new IntProperty(0, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new IntProperty(getDefaultValue(), null), 5);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultInt annotation maps to 0
        doDefaultValueTest(new IntProperty(null, null), 0);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new IntProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new IntProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new IntProperty(0, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        IntProperty p = new IntProperty(0, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        IntProperty p = new IntProperty(0, false);
        assertEquals(0, p.get(0));
        p.set(0, 5);
        assertEquals(5, p.get(0));
    }

    @Test
    public void testSwap() {
        IntProperty p = new IntProperty(0, false);
        p.setCapacity(2);
        p.set(0, 5);
        p.set(1, 0);
        p.swap(0, 1);

        assertEquals(5, p.get(1));
        assertEquals(0, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals(5, p.get(0));
        assertEquals(0, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        IntProperty p = new IntProperty(0, false);
        int[] data = p.getIndexedData();
        p.set(0, 5);
        assertEquals(5, data[0]);
    }

    @DefaultInt(5)
    private static DefaultInt getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultInt.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return IntPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
