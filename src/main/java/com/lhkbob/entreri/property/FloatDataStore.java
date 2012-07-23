package com.lhkbob.entreri.property;

/**
 * FloatDataStore is an IndexedDataStore that uses float arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class FloatDataStore extends AbstractIndexedDataStore {
    private final float[] array;
    
    /**
     * Create a new FloatDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public FloatDataStore(int elementSize, float[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 4 * array.length;
    }
    
    @Override
    public FloatDataStore create(int size) {
        return new FloatDataStore(elementSize, new float[elementSize * size]);
    }

    @Override
    public float[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}