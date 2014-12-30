package com.lhkbob.entreri.components;

/**
 *
 */
public class MissingSetterProperty implements com.lhkbob.entreri.property.Property<MissingSetterProperty> {
    public Object get(int index) {
        return null;
    }

    // we don't really have to implement these because the component
    // will fail validation
    @Override
    public void setCapacity(int size) {
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public void swap(int indexA, int indexB) {
    }

    @Override
    public void setDefaultValue(int index) {

    }

    @Override
    public void clone(MissingSetterProperty src, int srcIndex, int dstIndex) {

    }
}
