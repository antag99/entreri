package com.lhkbob.entreri.property;

import com.lhkbob.entreri.attr.DefaultChar;
import com.lhkbob.entreri.attr.DoNotClone;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.CharProperty}.
 *
 * @author Michael Ludwig
 */
public class CharPropertyTest {
    private void doDefaultValueTest(CharProperty property, char expectedValue) {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertEquals(expectedValue, property.get(0));
    }

    private void doClonePolicy(CharProperty property, boolean clone) {
        property.setCapacity(2);
        property.set(0, 'a');
        property.set(1, '\0');

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals('a', property.get(1));
        } else {
            assertEquals('\0', property.get(1));
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() {
        doDefaultValueTest(new CharProperty('a', true), 'a');
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new CharProperty('\0', true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new CharProperty('\0', false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws NoSuchMethodException {
        doDefaultValueTest(new CharProperty(getDefaultValue(), null), 'a');
    }

    @Test
    public void testAnnotationConstructorNoDefaultValue() {
        // no DefaultChar annotation maps to 0
        doDefaultValueTest(new CharProperty(null, null), '\0');
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new CharProperty(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new CharProperty(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new CharProperty('\0', false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        CharProperty p = new CharProperty('\0', false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
        assertEquals(5, p.getIndexedData().length);
    }

    @Test
    public void testGetAndSet() {
        CharProperty p = new CharProperty('\0', false);
        assertEquals('\0', p.get(0));
        p.set(0, 'a');
        assertEquals('a', p.get(0));
    }

    @Test
    public void testSwap() {
        CharProperty p = new CharProperty('\0', false);
        p.setCapacity(2);
        p.set(0, 'a');
        p.set(1, '\0');
        p.swap(0, 1);

        assertEquals('a', p.get(1));
        assertEquals('\0', p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals('a', p.get(0));
        assertEquals('\0', p.get(1));
    }

    @Test
    public void testGetIndexedData() {
        CharProperty p = new CharProperty('\0', false);
        char[] data = p.getIndexedData();
        p.set(0, 'a');
        assertEquals('a', data[0]);
    }

    @DefaultChar('a')
    private static DefaultChar getDefaultValue() throws NoSuchMethodException {
        return getAnnotation(DefaultChar.class, "getDefaultValue");
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return CharPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
