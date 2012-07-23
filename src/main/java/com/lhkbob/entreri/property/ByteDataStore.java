package com.lhkbob.entreri.property;

/**
 * ByteDataStore is an IndexedDataStore that uses byte arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class ByteDataStore extends AbstractIndexedDataStore {
    private final byte[] array;
    
    /**
     * Create a new ByteDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public ByteDataStore(int elementSize, byte[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return array.length;
    }
    
    @Override
    public ByteDataStore create(int size) {
        return new ByteDataStore(elementSize, new byte[elementSize * size]);
    }

    @Override
    public byte[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}