package com.lhkbob.entreri.property;

/**
 * LongDataStore is an IndexedDataStore that uses long arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class LongDataStore extends AbstractIndexedDataStore {
    private final long[] array;
    
    /**
     * Create a new LongDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public LongDataStore(int elementSize, long[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 8 * array.length;
    }
    
    @Override
    public LongDataStore create(int size) {
        return new LongDataStore(elementSize, new long[elementSize * size]);
    }

    @Override
    public long[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}