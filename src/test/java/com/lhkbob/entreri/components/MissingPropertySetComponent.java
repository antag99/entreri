package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.Factory;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Invalid component type because it references a property type that doesn't have the
 * expected set method.
 */
public interface MissingPropertySetComponent extends Component {
    @Factory(MissingSetterFactory.class)
    public Object getValue();

    public void setValue(Object o);

    public static class MissingSetterProperty implements Property {
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
    }

    public static class MissingSetterFactory
            implements PropertyFactory<MissingSetterProperty> {
        @Override
        public MissingSetterProperty create() {
            return new MissingSetterProperty();
        }

        @Override
        public void setDefaultValue(MissingSetterProperty property, int index) {
        }

        @Override
        public void clone(MissingSetterProperty src, int srcIndex,
                          MissingSetterProperty dst, int dstIndex) {
        }
    }
}
