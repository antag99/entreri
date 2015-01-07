package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Named;
import com.lhkbob.entreri.property.Collection;
import com.lhkbob.entreri.property.Reference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A test component that covers many of the options for collection properties.
 *
 * @author Michael Ludwig
 */
public interface CollectionComponent extends Component {
    // testing reference lists, testing the `return this` option for adding,
    // and that null elements are not allowed, and setter container validation
    @Collection(allowNullElements = false)
    @Reference
    @Named("points")
    public CollectionComponent addPoint(Point p);

    @Named("points")
    public boolean containsPoint(Point p);

    public boolean removePoint(Point p);

    public List<Point> getPoints();

    public void setPoints(List<Point> points);


    // testing reference sets, and getting container type from beans, testing the `return this` option for
    // removing, and null elements are allowed (the default with no @Collection)
    @Reference
    public Set<Point> getUniquePoints();

    public void setUniquePoints(Set<Point> points);

    public boolean addUniquePoint(@Named("uniquePoints") Point p);

    public boolean containsUniquePoint(@Named("uniquePoints") Point p);

    public CollectionComponent removeUniquePoint(@Named("uniquePoints") Point p);


    // testing reference maps, value put return, boolean remove, with no null elements
    @Reference
    public Map<String, Point> getMappedPoints();

    @Collection(allowNullElements = false)
    public void setMappedPoints(Map<String, Point> points);

    public Point putMappedPoint(String key, Point value);

    public Point getMappedPoint(String key);

    public boolean removeMappedPoint(String key);

    public boolean containsMappedPoint(String key);


    // testing value lists, testing the `return void` option for adding and removing,
    // and testing the collection type identified from the attribute
    @Collection(type = Collection.Type.LIST)
    public void addValuePoint(Point p);

    public boolean containsValuePoint(Point p);

    public void removeValuePoint(Point p);


    // testing value sets, testing overriding the default implementation, and pluralized name finding,
    // and basic value semantics for collections
    @Collection(setImpl = TreeSet.class, allowNullElements = false)
    public CollectionComponent addValueUniquePoint(Point p);

    public boolean containsValueUniquePoint(Point p);

    public boolean removeValueUniquePoint(Point p);

    public Set<Point> getValueUniquePoints();

    public void setValueUniquePoints(Set<Point> points);


    // testing value maps, map inference from get, boolean put return, void remove, null elements allowed
    public Point getValueMappedPoint(String key);

    public boolean putValueMappedPoint(String key, Point value);

    public void removeValueMappedPoint(String key);

    public boolean containsValueMappedPoint(String key);


    public static class Point implements Comparable<Point> {
        public int x, y;

        public Point() {
            this(0, 0);
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Point o) {
            if (y == o.y) {
                return x - o.x;
            } else {
                return y - o.y;
            }
        }

        @Override
        public int hashCode() {
            return x ^ y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) {
                return false;
            }
            return compareTo((Point) o) == 0;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
