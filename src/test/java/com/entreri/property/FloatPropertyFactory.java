package com.entreri.property;

public class FloatPropertyFactory implements PropertyFactory<FloatProperty> {
    @Override
    public FloatProperty create() {
        return new FloatProperty(1);
    }
}
