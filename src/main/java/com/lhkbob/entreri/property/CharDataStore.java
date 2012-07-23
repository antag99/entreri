package com.lhkbob.entreri.property;

/**
 * CharDataStore is an IndexedDataStore that uses char arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class CharDataStore extends AbstractIndexedDataStore {
    private final char[] array;
    
    /**
     * Create a new CharDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public CharDataStore(int elementSize, char[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 2 * array.length;
    }
    
    @Override
    public CharDataStore create(int size) {
        return new CharDataStore(elementSize, new char[elementSize * size]);
    }

    @Override
    public char[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}