package com.googlecode.entreri.component;

import java.util.Collections;
import java.util.Map;

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.ComponentDataFactory;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;

public class CustomFactoryComponent extends ComponentData<CustomFactoryComponent> {
    public IntProperty prop;
    
    public static class CustomFactory implements ComponentDataFactory<CustomFactoryComponent> {
        @Override
        public Map<String, PropertyFactory<?>> getPropertyFactories() {
            return Collections.<String, PropertyFactory<?>>singletonMap("prop", IntProperty.factory(1));
        }

        @Override
        public CustomFactoryComponent createInstance() {
            return new CustomFactoryComponent();
        }

        @Override
        public void setProperty(CustomFactoryComponent instance, String key, Property property) {
            if (key.equals("prop")) {
                instance.prop = (IntProperty) property;
            } else {
                throw new RuntimeException();
            }
        }
    }
}
