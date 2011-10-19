package com.entreri.property;

public class NoParameterProperty implements CompactAwareProperty {
    private final IntProperty property;
    
    private boolean compacted;
    
    public NoParameterProperty() {
        property = new IntProperty(1);
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return property.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        property.setDataStore(store);
    }

    @Override
    public void onCompactComplete() {
        compacted = true;
    }
    
    public boolean wasCompacted() {
        return compacted;
    }
}
