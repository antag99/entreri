package com.googlecode.entreri.component;

import java.util.Collections;
import java.util.Map;

import com.googlecode.entreri.ComponentDataFactory;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;

public class DefaultComponentDataFactory implements ComponentDataFactory<DefaultFactoryComponent> {
    @Override
    public Map<String, PropertyFactory<?>> getPropertyFactories() {
        return Collections.<String, PropertyFactory<?>>singletonMap("prop", IntProperty.factory(1));
    }

    @Override
    public DefaultFactoryComponent createInstance() {
        return new DefaultFactoryComponent();
    }

    @Override
    public void setProperty(DefaultFactoryComponent instance, String key, Property property) {
        if (key.equals("prop")) {
            instance.prop = (IntProperty) property;
        } else {
            throw new RuntimeException();
        }
    }
}
