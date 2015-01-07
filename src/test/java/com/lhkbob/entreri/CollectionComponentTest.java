package com.lhkbob.entreri;

import com.lhkbob.entreri.components.CollectionComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 *
 */
public class CollectionComponentTest {
    private CollectionComponent c;

    @Before
    public void setup() throws Exception {
        EntitySystem system = EntitySystem.Factory.create();
        c = system.addEntity().add(CollectionComponent.class);

        // make the values non-null for the tests
        c.setPoints(new ArrayList<CollectionComponent.Point>());
        c.setUniquePoints(new HashSet<CollectionComponent.Point>());
        c.setMappedPoints(new HashMap<String, CollectionComponent.Point>());
    }

    @Test
    public void testReferenceAddToList() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        CollectionComponent result = c.addPoint(p);
        assertSame(c, result);

        List<CollectionComponent.Point> points = c.getPoints();
        assertEquals(1, points.size());
        assertEquals(p, points.get(0));

        // add again, ensure we have list semantics for duplicate adds
        result = c.addPoint(p);
        assertSame(c, result);
        assertEquals(2, points.size());
        assertEquals(p, points.get(0));
        assertEquals(p, points.get(1));
    }

    @Test
    public void testReferenceRemoveFromList() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertFalse(c.removePoint(p));

        c.addPoint(p);
        assertEquals(1, c.getPoints().size());
        assertTrue(c.removePoint(p));
        assertEquals(0, c.getPoints().size());
    }

    @Test
    public void testReferenceListContains() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertFalse(c.containsPoint(p));

        c.addPoint(p);
        assertTrue(c.containsPoint(p));
    }

    @Test
    public void testReferenceAddToSet() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertTrue(c.addUniquePoint(p));
        assertEquals(1, c.getUniquePoints().size());
        assertTrue(c.getUniquePoints().contains(p));

        // add again, ensure we have set semantics for duplicate adds
        assertFalse(c.addUniquePoint(p));
        assertEquals(1, c.getUniquePoints().size());
        assertTrue(c.getUniquePoints().contains(p));
    }

    @Test
    public void testReferenceRemoveFromSet() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertSame(c, c.removeUniquePoint(p));

        c.addUniquePoint(p);
        assertEquals(1, c.getUniquePoints().size());
        assertSame(c, c.removeUniquePoint(p));
        assertEquals(0, c.getUniquePoints().size());
    }

    @Test
    public void testReferenceSetContains() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertFalse(c.containsUniquePoint(p));

        c.addUniquePoint(p);
        assertTrue(c.containsUniquePoint(p));
    }

    @Test
    public void testReferenceMapMethods() throws Exception {
        CollectionComponent.Point p1 = new CollectionComponent.Point();
        CollectionComponent.Point p2 = new CollectionComponent.Point(1, 1);

        assertFalse(c.containsMappedPoint("hello"));
        assertNull(c.getMappedPoint("hello"));
        assertNull(c.putMappedPoint("hello", p1));

        assertTrue(c.containsMappedPoint("hello"));
        assertSame(p1, c.getMappedPoint("hello"));
        assertSame(p1, c.putMappedPoint("hello", p2));
        assertSame(p2, c.getMappedPoints().get("hello"));

        assertTrue(c.removeMappedPoint("hello"));
        assertFalse(c.removeMappedPoint("hello"));
    }

    @Test
    public void testPutNotAllowedNullElement() throws Exception {
        try {
            c.putMappedPoint("hello", null);
            fail("Expected exception");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            c.putMappedPoint(null, new CollectionComponent.Point());
            fail("Expected exception");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetNotAllowedNullElement() throws Exception {
        c.getMappedPoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMapRemoveNotAllowedNullElements() throws Exception {
        c.removeMappedPoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMapContainsNotAllowedNullElements() throws Exception {
        c.containsMappedPoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionAddNotAllowedNullElement() throws Exception {
        c.addPoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionRemoveNotAllowedNullElement() throws Exception {
        c.removePoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionContainsNotAllowedNullElement() throws Exception {
        c.containsPoint(null);
    }

    @Test
    public void testReferenceCollectionAddAllowedNullElement() throws Exception {
        c.addUniquePoint(null);
        assertEquals(1, c.getUniquePoints().size());
        assertTrue(c.containsUniquePoint(null));
    }

    @Test(expected = NullPointerException.class)
    public void testReferenceCollectionValidateNoNullElements() throws Exception {
        List<CollectionComponent.Point> points = new ArrayList<>();
        points.add(null);
        points.add(new CollectionComponent.Point());

        c.setPoints(points);
    }

    @Test
    public void testReferenceMapValidateNoNullElements() throws Exception {
        try {
            Map<String, CollectionComponent.Point> nullKeys = new HashMap<>();
            nullKeys.put(null, new CollectionComponent.Point());
            c.setMappedPoints(nullKeys);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Map<String, CollectionComponent.Point> nullValues = new HashMap<>();
            nullValues.put("hello", null);
            c.setMappedPoints(nullValues);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testValueList() throws Exception {
        // Because the list types don't expose any full collection views, we have to test contains and
        // remove at the same time to make sure list semantics are respected
        CollectionComponent.Point p = new CollectionComponent.Point();
        c.addValuePoint(p);
        assertTrue(c.containsValuePoint(p));

        c.addValuePoint(p);
        assertTrue(c.containsValuePoint(p));
        c.removeValuePoint(p);
        assertTrue(c.containsValuePoint(p));
        c.removeValuePoint(p);
        assertFalse(c.containsValuePoint(p));
    }

    @Test
    public void testValueAddToSet() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertSame(c, c.addValueUniquePoint(p));
        assertEquals(1, c.getValueUniquePoints().size());
        assertTrue(c.getValueUniquePoints().contains(p));

        // add again, ensure we have set semantics for duplicate adds
        assertSame(c, c.addValueUniquePoint(p));
        assertEquals(1, c.getValueUniquePoints().size());
        assertTrue(c.getValueUniquePoints().contains(p));
    }

    @Test
    public void testValueRemoveFromSet() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertFalse(c.removeValueUniquePoint(p));

        c.addValueUniquePoint(p);
        assertEquals(1, c.getValueUniquePoints().size());
        assertTrue(c.removeValueUniquePoint(p));
        assertEquals(0, c.getValueUniquePoints().size());
    }

    @Test
    public void testValueSetContains() throws Exception {
        CollectionComponent.Point p = new CollectionComponent.Point();
        assertFalse(c.containsValueUniquePoint(p));

        c.addValueUniquePoint(p);
        assertTrue(c.containsValueUniquePoint(p));
    }

    @Test
    public void testValueMapMethods() throws Exception {
        CollectionComponent.Point p1 = new CollectionComponent.Point();
        CollectionComponent.Point p2 = new CollectionComponent.Point(1, 1);

        assertFalse(c.containsMappedPoint("hello"));
        assertNull(c.getMappedPoint("hello"));
        assertNull(c.putMappedPoint("hello", p1));

        assertTrue(c.containsMappedPoint("hello"));
        assertSame(p1, c.getMappedPoint("hello"));
        assertSame(p1, c.putMappedPoint("hello", p2));
        assertSame(p2, c.getMappedPoints().get("hello"));

        assertTrue(c.removeMappedPoint("hello"));
        assertFalse(c.removeMappedPoint("hello"));
    }

    @Test
    public void testPutGetContainsRemoveAllowedNullElement() throws Exception {
        c.putValueMappedPoint("hello", null);
        assertTrue(c.containsValueMappedPoint("hello"));
        assertNull(c.getValueMappedPoint("hello"));

        c.putValueMappedPoint(null, new CollectionComponent.Point());
        assertTrue(c.containsValueMappedPoint(null));
        assertEquals(new CollectionComponent.Point(), c.getValueMappedPoint(null));

        c.removeValueMappedPoint(null);
        assertFalse(c.containsValueMappedPoint(null));
        c.removeValueMappedPoint("hello");
        assertFalse(c.containsValueMappedPoint("hello"));
    }

    @Test(expected = NullPointerException.class)
    public void testValueCollectionAddNotAllowedNullElement() throws Exception {
        c.addValueUniquePoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testValueCollectionRemoveNotAllowedNullElement() throws Exception {
        c.removeValueUniquePoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void testValueCollectionContainsNotAllowedNullElement() throws Exception {
        c.containsValueUniquePoint(null);
    }

    @Test
    public void testValueCollectionAddRemoveContainsAllowedNullElement() throws Exception {
        c.addValuePoint(null);
        assertTrue(c.containsValuePoint(null));
        c.removeValuePoint(null);
        assertFalse(c.containsValuePoint(null));
    }

    @Test(expected = NullPointerException.class)
    public void testValueCollectionValidateNoNullElements() throws Exception {
        Set<CollectionComponent.Point> points = new HashSet<>();
        points.add(null);
        points.add(new CollectionComponent.Point());

        c.setValueUniquePoints(points);
    }

    @Test
    public void testValueBackingImplementationOverride() throws Exception {
        // because the backing set is wrapped in an unmodifiable set, we make sure the returned set
        // follows the iteration order guaranteed by a TreeSet as a proxy for it correctly using a TreeSet
        CollectionComponent.Point p1 = new CollectionComponent.Point(3, 2);
        CollectionComponent.Point p2 = new CollectionComponent.Point(1, 2);
        CollectionComponent.Point p3 = new CollectionComponent.Point(5, 1);

        List<CollectionComponent.Point> expectedOrder = Arrays.asList(p3, p2, p1);
        c.addValueUniquePoint(p1);
        c.addValueUniquePoint(p2);
        c.addValueUniquePoint(p3);

        Set<CollectionComponent.Point> points = c.getValueUniquePoints();
        List<CollectionComponent.Point> pointsOrdered = new ArrayList<>(points);
        assertEquals(expectedOrder, pointsOrdered);
    }

    @Test
    public void testValueSetSemantics() throws Exception {
        CollectionComponent.Point p1 = new CollectionComponent.Point(1, 1);
        CollectionComponent.Point p2 = new CollectionComponent.Point(2, 2);
        CollectionComponent.Point p3 = new CollectionComponent.Point(3, 3);

        Set<CollectionComponent.Point> newPoints = new HashSet<>();
        newPoints.add(p1);
        c.setValueUniquePoints(newPoints);

        // make sure that the input set being modified does not affect the component
        newPoints.add(p2);
        assertFalse(c.containsValueUniquePoint(p2));
        // make sure that the component being modified does not affect the input
        c.addValueUniquePoint(p3);
        assertFalse(newPoints.contains(p3));

        try {
            Set<CollectionComponent.Point> readOnlyPoints = c.getValueUniquePoints();
            readOnlyPoints.add(p2);
            fail("Exception expected");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
