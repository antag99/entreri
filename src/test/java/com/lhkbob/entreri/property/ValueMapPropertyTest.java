package com.lhkbob.entreri.property;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.lhkbob.entreri.property.ValueMapProperty}.
 *
 * @author Michael Ludwig
 */
public class ValueMapPropertyTest {
    private void doDefaultValueTest(ValueMapProperty<Object, Object> property, Class<? extends Map> baseSet)
            throws Exception {
        property.setCapacity(1);
        property.setDefaultValue(0);
        assertTrue(property.get(0).isEmpty());

        // pull out original data list from the property (wrapped in an unmodifiable set)
        Field origSet = getBaseCollectionField(property.get(0).getClass());
        origSet.setAccessible(true);
        assertTrue(baseSet.isInstance(origSet.get(property.get(0))));
    }

    private static Field getBaseCollectionField(Class<?> type) throws Exception {
        return type.getDeclaredField("m");
    }

    private void doClonePolicy(ValueMapProperty<Object, Object> property, boolean clone) {
        Map<Object, Object> v1 = new HashMap<>();
        v1.put(new Object(), new Object());

        property.setCapacity(2);
        property.set(0, v1);
        property.set(1, new HashMap<>());

        // now for the actual clone
        property.clone(property, 0, 1);
        if (clone) {
            assertEquals(v1, property.get(1));
            assertNotSame(v1, property.get(1));
        } else {
            // sets the default value on clone (does not leave the value alone)
            assertTrue(property.get(1).isEmpty());
        }
    }

    @Test
    public void testSimpleConstructorDefaultValue() throws Exception {
        doDefaultValueTest(new ValueMapProperty<>(HashMap.class, true), HashMap.class);
    }

    @Test
    public void testSimpleConstructorClone() {
        doClonePolicy(new ValueMapProperty<>(HashMap.class, true), true);
    }

    @Test
    public void testSimpleConstructorDoNotClone() {
        doClonePolicy(new ValueMapProperty<>(HashMap.class, false), false);
    }

    @Test
    public void testAnnotationConstructorDefaultValue() throws Exception {
        doDefaultValueTest(new ValueMapProperty<>(getCollection(), null), LinkedHashMap.class);
    }

    @Test
    public void testAnnotationConstructorClone() {
        // no DoNotClone annotation maps to true
        doClonePolicy(new ValueMapProperty<>(null, null), true);
    }

    @Test
    public void testAnnotationConstructorDoNotClone() throws NoSuchMethodException {
        doClonePolicy(new ValueMapProperty<>(null, getClonePolicy()), false);
    }

    @Test
    public void testInitialCapacity() {
        assertEquals(1, new ValueMapProperty<>(HashMap.class, false).getCapacity());
    }

    @Test
    public void testSetCapacity() {
        ValueMapProperty<Object, Object> p = new ValueMapProperty<>(HashMap.class, false);
        p.setCapacity(5);
        assertEquals(5, p.getCapacity());
    }

    @Test
    public void testGetAndSet() {
        Map<Object, Object> v1 = new HashMap<>();
        v1.put(new Object(), new Object());

        ValueMapProperty<Object, Object> p = new ValueMapProperty<>(HashMap.class, false);
        assertNotNull(p.get(0));
        p.set(0, v1);
        assertNotSame(v1, p.get(0));
        assertEquals(v1, p.get(0));

        v1.put(new Object(), new Object());
        assertFalse(v1.equals(p.get(0)));

        try {
            p.get(0).put(new Object(), new Object());
            fail("Exception expected");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testPutGetContainsRemove() {
        Object k = new Object();
        Object v = new Object();
        Object v2 = new Object();

        ValueMapProperty<Object, Object> p = new ValueMapProperty<>(HashMap.class, false);
        Map<Object, Object> m = p.get(0);

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
        v1.put(new Object(), new Object());
        Map<Object, Object> v2 = new HashMap<>();
        v2.put(new Object(), new Object());

        ValueMapProperty<Object, Object> p = new ValueMapProperty<>(HashMap.class, false);
        p.setCapacity(2);
        p.set(0, v1);
        p.set(1, v2);
        p.swap(0, 1);

        assertEquals(v1, p.get(1));
        assertEquals(v2, p.get(0));

        // make sure order doesn't matter
        p.swap(1, 0);
        assertEquals(v1, p.get(0));
        assertEquals(v2, p.get(1));
    }

    @Test
    public void testNullValuesDisallowed() {
        ValueMapProperty<Object, Object> p = new ValueMapProperty<>(HashMap.class, false);
        assertNotNull(p.get(0));
        try {
            p.set(0, null);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @DoNotClone
    private static DoNotClone getClonePolicy() throws NoSuchMethodException {
        return getAnnotation(DoNotClone.class, "getClonePolicy");
    }

    @Collection(mapImpl = LinkedHashMap.class)
    private static Collection getCollection() throws NoSuchMethodException {
        return getAnnotation(Collection.class, "getCollection");
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, String methodName)
            throws NoSuchMethodException {
        return ValueMapPropertyTest.class.getDeclaredMethod(methodName).getAnnotation(type);
    }
}
