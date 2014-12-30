package com.lhkbob.entreri.components;

import com.lhkbob.entreri.property.Property;

/**
 *
 */
public class MissingGetterProperty
        implements com.lhkbob.entreri.property.Property<MissingGetterProperty>, Property.ValueSemantics {
    public void set(int index, Object o) {

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
    public void clone(MissingGetterProperty src, int srcIndex, int dstIndex) {

    }
}
