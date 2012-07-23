package com.lhkbob.entreri.property;

/**
 * ObjectDataStore is an IndexedDataStore that uses Object arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class ObjectDataStore extends AbstractIndexedDataStore {
    private final Object[] array;
    
    /**
     * Create a new ObjectDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public ObjectDataStore(int elementSize, Object[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        // since this is just an approximate, we use 12 bytes as a minimum
        // size for each object
        return 12 * array.length;
    }
    
    @Override
    public ObjectDataStore create(int size) {
        return new ObjectDataStore(elementSize, new Object[elementSize * size]);
    }

    @Override
    public Object[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}