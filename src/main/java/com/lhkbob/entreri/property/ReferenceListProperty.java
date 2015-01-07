package com.lhkbob.entreri.property;

import java.util.Arrays;
import java.util.List;

/**
 * ReferenceListProperty
 * =====================
 *
 * ReferenceListProperty is a Property implementation for storing lists of a particular element type `T`
 * with reference semantics. This means the Property can store null lists, and its default is a null list. As
 * it stores references, the List implementation that may be optionally specified with the {@link Collection}
 * attribute is ignored.
 *
 * ## Supported method patterns
 *
 * ReferenceListProperty supports the standard `get(int) -> List<T>` and `set(int, List<T>) -> void` methods
 * used for the Java Bean patterns. Additionally it provides `add(int, T) -> boolean` to add an element to the
 * current list reference; `remove(int, T) -> boolean` to remove an element from the current list reference;
 * and `contains(int, T) -> boolean` to query if an element is in the current list reference. These methods
 * are used by the collections method patterns to allow for more options in Component definitions. They will
 * throw a {@link java.lang.NullPointerException} if the current list reference is null.
 *
 * ## Generic
 *
 * As a generic property, this property can store any subclass of Object as the element type of its lists.
 *
 * @author Michael Ludwig
 */
public class ReferenceListProperty<T>
        implements Property<ReferenceListProperty<T>>, Property.ReferenceSemantics,
                   Property.Generic<List<T>> {
    private final boolean clone;
    private List<T>[] data;

    /**
     * Create a new ReferenceListProperty that will clone or not clone its values when used as a template,
     * depending on `cloneValue`.
     *
     * @param cloneValue The clone policy
     */
    @SuppressWarnings("unchecked")
    public ReferenceListProperty(boolean cloneValue) {
        clone = cloneValue;
        data = new List[1];
    }

    /**
     * Constructor meeting the requirements for automated code generation.
     */
    public ReferenceListProperty(DoNotClone clone) {
        this(clone == null);
    }

    /**
     * @param index The component index
     * @return The current List reference
     */
    public List<T> get(int index) {
        return data[index];
    }

    /**
     * Update the current List reference to `list`, which can be null.
     *
     * @param index The component index
     * @param list  The new List reference
     */
    public void set(int index, List<T> list) {
        data[index] = list;
    }

    /**
     * Add `element` to the current List reference at `index`. If the current list is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.List#add(T)}.
     *
     * @param index   The component index
     * @param element The element to append to the current list
     * @return True if the list was modified (generally always true for lists)
     */
    public boolean add(int index, T element) {
        return get(index).add(element);
    }

    /**
     * Remove `element` from the current list reference at `index`. If the current list is null a
     * NullPointerException is thrown. This has equivalent behavior to {@link java.util.List#remove(T)}.
     *
     * @param index   The component index
     * @param element The element to remove from the current list
     * @return True if the element was removed
     */
    public boolean remove(int index, T element) {
        return get(index).remove(element);
    }

    /**
     * Check if `element` is contained in the current list reference at `index`. If the current list is null
     * a NullPointerException is thrown. This has equivalent behavior to {@link java.util.List#contains(T)}.
     *
     * @param index   The component index
     * @param element The element to check for
     * @return True if the current list contains `element`
     */
    public boolean contains(int index, T element) {
        return get(index).contains(element);
    }

    /**
     * Return the backing int array of this property's data store. The array may be longer than necessary
     * for the number of components in the system. Data can be accessed for a component directly using the
     * component's index.
     *
     * @return The List data for all packed properties that this property has been packed with
     */
    public List<T>[] getIndexedData() {
        return data;
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size);
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public void swap(int indexA, int indexB) {
        List<T> t = data[indexA];
        data[indexA] = data[indexB];
        data[indexB] = t;
    }

    @Override
    public void setDefaultValue(int index) {
        set(index, null);
    }

    @Override
    public void clone(ReferenceListProperty<T> src, int srcIndex, int dstIndex) {
        if (src.clone && clone) {
            set(dstIndex, src.get(srcIndex));
        } else {
            setDefaultValue(dstIndex);
        }
    }
}
