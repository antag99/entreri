package com.entreri.property;

public class MultiParameterProperty implements Property {
    private final FloatProperty property;
    
    public MultiParameterProperty(int elementCount, float unused) {
        property = new FloatProperty(elementCount);
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return property.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        property.setDataStore(store);
    }
    
    public float getFloat(int offset) {
        return property.getIndexedData()[offset];
    }
    
    public void setFloat(int offset, float f) {
        property.getIndexedData()[offset] = f;
    }
}
