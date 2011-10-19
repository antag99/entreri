package com.entreri.property;

import com.entreri.Component;

/**
 * ObjectProperty is an implementation of Property that stores the property data
 * as a number of packed Object references for each property. Because it is not
 * primitive data, cache locality will suffer compared to the primitive property
 * types, but it will allow you to store arbitrary objects.
 * 
 * @author Michael Ludwig
 */
public final class ObjectProperty implements Property {
    private ObjectDataStore store;
    
    /**
     * Create a new ObjectProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public ObjectProperty(int elementSize) {
        store = new ObjectDataStore(elementSize, new Object[elementSize]);
    }

    /**
     * Return a PropertyFactory that creates ObjectProperties with the given
     * element size. If it is less than 1, the factory's create() method will
     * fail.
     * 
     * @param elementSize The element size of the created properties
     * @return A PropertyFactory for ObjectProperty
     */
    public static PropertyFactory<ObjectProperty> factory(final int elementSize) {
        return new PropertyFactory<ObjectProperty>() {
            @Override
            public ObjectProperty create() {
                return new ObjectProperty(elementSize);
            }
        };
    }
    
    /**
     * Return the backing int array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link Component#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The Object data for all packed properties that this property has
     *         been packed with
     */
    public Object[] getIndexedData() {
        return store.array;
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return store;
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        if (store == null)
            throw new NullPointerException("Store cannot be null");
        if (!(store instanceof ObjectDataStore))
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong type: " + store.getClass());
        
        ObjectDataStore newStore = (ObjectDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }

    private static class ObjectDataStore extends AbstractIndexedDataStore<Object[]> {
        private final Object[] array;
        
        public ObjectDataStore(int elementSize, Object[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        public ObjectDataStore create(int size) {
            return new ObjectDataStore(elementSize, new Object[elementSize * size]);
        }

        @Override
        protected Object[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(Object[] array) {
            return array.length;
        }
    }
}
