package com.lhkbob.entreri.property;

/**
 * IntDataStore is an IndexedDataStore that uses int arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class IntDataStore extends AbstractIndexedDataStore {
    private final int[] array;
    
    /**
     * Create a new IntDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public IntDataStore(int elementSize, int[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 4 * array.length;
    }
    
    @Override
    public IntDataStore create(int size) {
        return new IntDataStore(elementSize, new int[elementSize * size]);
    }

    @Override
    public int[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}