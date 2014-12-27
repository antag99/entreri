package com.lhkbob.entreri.property;

import com.lhkbob.entreri.attr.DefaultByte;
import com.lhkbob.entreri.attr.DoNotClone;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ByteProperty}.
 *
 * @author Michael Ludwig
 */
public class BytePropertyTest {
    private void doDefaultValueTest(ByteProperty property, byte expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(ByteProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, (byte) 5);
        property.set(1, (byte) 0);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals((byte) 5, property.get(1));
        } else {
            assertEquals((byte) 0, property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new ByteProperty((byte) 5, true), (byte) 5);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ByteProperty((byte) 0, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ByteProperty((byte) 0, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new ByteProperty(getDefaultValue(), null), (byte) 5);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultByte annotation maps to 0
        doDefaultValueTest(new ByteProperty(null, null), (byte) 0);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ByteProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ByteProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ByteProperty((byte) 0, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ByteProperty p = new ByteProperty((byte) 0, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        ByteProperty p = new ByteProperty((byte) 0, false);
        assertEquals(0, p.get(0));
        p.set(0, (byte) 5);
        assertEquals((byte) 5, p.get(0));
    }

    @Test
    public void testSwap() {
        ByteProperty p = new ByteProperty((byte) 0, false);
        p.setCapacity(2);
        p.set(0, (byte) 5);
        p.set(1, (byte) 0);
        p.swap(0, 1);

        assertEquals((byte) 5, p.get(1));
        assertEquals((byte) 0, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals((byte) 5, p.get(0));
        assertEquals((byte) 0, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        ByteProperty p = new ByteProperty((byte) 0, false);
        byte[] data = p.getIndexedData();
        p.set(0, (byte) 5);
        assertEquals((byte) 5, data[0]);
    }

    @DefaultByte(5)
    private static DefaultByte getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultByte.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return BytePropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
