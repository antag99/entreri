package com.lhkbob.entreri.property;

/**
 * BooleanDataStore is an IndexedDataStore that uses boolean arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class BooleanDataStore extends AbstractIndexedDataStore {
    private final boolean[] array;
    
    /**
     * Create a new BooleanDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public BooleanDataStore(int elementSize, boolean[] array) {
        super(elementSize);
        if (array.length % elementSize != 0)
            throw new IllegalArgumentException("Array length must be a multiple of the element size");
        this.array = array;
    }
    
    @Override
    public long memory() {
        return array.length;
    }
    
    @Override
    public BooleanDataStore create(int size) {
        return new BooleanDataStore(elementSize, new boolean[elementSize * size]);
    }

    @Override
    public boolean[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}