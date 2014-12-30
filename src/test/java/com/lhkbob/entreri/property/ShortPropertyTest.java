package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ShortProperty}.
 *
 * @author Michael Ludwig
 */
public class ShortPropertyTest {
    private void doDefaultValueTest(ShortProperty property, short expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(ShortProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, (short) 5);
        property.set(1, (short) 0);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals((short) 5, property.get(1));
        } else {
            assertEquals((short) 0, property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new ShortProperty((short) 5, true), (short) 5);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ShortProperty((short) 0, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ShortProperty((short) 0, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ShortProperty(getDefaultValue(), null), (short) 5);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultShort annotation maps to 0
        doDefaultValueTest(new ShortProperty(null, null), (short) 0);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ShortProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ShortProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ShortProperty((short) 0, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ShortProperty p = new ShortProperty((short) 0, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        ShortProperty p = new ShortProperty((short) 0, false);
        assertEquals((short) 0, p.get(0));
        p.set(0, (short) 5);
        assertEquals((short) 5, p.get(0));
    }

    @Test
    public void testSwap() {
        ShortProperty p = new ShortProperty((short) 0, false);
        p.setCapacity(2);
        p.set(0, (short) 5);
        p.set(1, (short) 0);
        p.swap(0, 1);

        assertEquals((short) 5, p.get(1));
        assertEquals((short) 0, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals((short) 5, p.get(0));
        assertEquals((short) 0, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        ShortProperty p = new ShortProperty((short) 0, false);
        short[] data = p.getIndexedData();
        p.set(0, (short) 5);
        assertEquals((short) 5, data[0]);
    }

    @DefaultShort(5)
    private static DefaultShort getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultShort.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return ShortPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
