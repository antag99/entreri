package com.lhkbob.entreri.property;

/**
 * ShortDataStore is an IndexedDataStore that uses short arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class ShortDataStore extends AbstractIndexedDataStore {
    private final short[] array;
    
    /**
     * Create a new ShortDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public ShortDataStore(int elementSize, short[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 2 * array.length;
    }
    
    @Override
    public ShortDataStore create(int size) {
        return new ShortDataStore(elementSize, new short[elementSize * size]);
    }

    @Override
    public short[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}