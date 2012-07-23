package com.lhkbob.entreri.property;

/**
 * DoubleDataStore is an IndexedDataStore that uses double arrays to store
 * multi-element component data.
 * 
 * @author Michael Ludwig
 */
public class DoubleDataStore extends AbstractIndexedDataStore {
    private final double[] array;
    
    /**
     * Create a new DoubleDataStore with the given number of elements per
     * logical component, and backed by the given array. The array's length must
     * be a multiple of element size.
     * 
     * @param elementSize The number of elements per component
     * @param array Backing array
     * @throws IllegalArgumentException if array length is not a multiple of
     *             element size
     */
    public DoubleDataStore(int elementSize, double[] array) {
        super(elementSize);
        this.array = array;
    }
    
    @Override
    public long memory() {
        return 8 * array.length;
    }
    
    @Override
    public DoubleDataStore create(int size) {
        return new DoubleDataStore(elementSize, new double[elementSize * size]);
    }

    @Override
    public double[] getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }
}