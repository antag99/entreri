package com.lhkbob.entreri.components;

import com.lhkbob.entreri.property.Property;

import java.util.List;

/**
 *
 */
public class GenericProperty<T extends Number>
        implements Property<GenericProperty<T>>, Property.ReferenceSemantics, Property.Generic<List<T>> {
    public GenericProperty(Class<T> type) {

    }

    public List<T> get(int index) {
        return null;
    }

    public void set(int index, List<T> val) {
        // no impl needed
    }

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
    public void clone(GenericProperty src, int srcIndex, int dstIndex) {

    }
}
