package com.lhkbob.entreri.property;

import com.lhkbob.entreri.attr.DefaultLong;
import com.lhkbob.entreri.attr.DoNotClone;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.LongProperty}.
 *
 * @author Michael Ludwig
 */
public class LongPropertyTest {
    private void doDefaultValueTest(LongProperty property, long expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(LongProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, 5L);
        property.set(1, 0L);

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals(5L, property.get(1));
        } else {
            assertEquals(0L, property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new LongProperty(5L, true), 5L);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new LongProperty(0L, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new LongProperty(0L, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new LongProperty(getDefaultValue(), null), 5L);
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultLong annotation maps to 0
        doDefaultValueTest(new LongProperty(null, null), 0L);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new LongProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new LongProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new LongProperty(0L, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        LongProperty p = new LongProperty(0L, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        LongProperty p = new LongProperty(0L, false);
        assertEquals(0L, p.get(0));
        p.set(0, 5L);
        assertEquals(5L, p.get(0));
    }

    @Test
    public void testSwap() {
        LongProperty p = new LongProperty(0L, false);
        p.setCapacity(2);
        p.set(0, 5L);
        p.set(1, 0L);
        p.swap(0, 1);

        assertEquals(5L, p.get(1));
        assertEquals(0L, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals(5L, p.get(0));
        assertEquals(0L, p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        LongProperty p = new LongProperty(0L, false);
        long[] data = p.getIndexedData();
        p.set(0, 5L);
        assertEquals(5L, data[0]);
    }

    @DefaultLong(5L)
    private static DefaultLong getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultLong.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return LongPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
