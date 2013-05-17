package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.Factory;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Invalid component type because it references a property type that doesn't have the
 * expected get method.
 */
public interface MissingPropertyGetComponent extends Component {
    @Factory(MissingGetterFactory.class)
    public Object getValue();

    public void setValue(Object o);

    public static class MissingGetterProperty implements Property {
        public void set(Object o, int index) {

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

    public static class MissingGetterFactory
            implements PropertyFactory<MissingGetterProperty> {
        @Override
        public MissingGetterProperty create() {
            return new MissingGetterProperty();
        }

        @Override
        public void setDefaultValue(MissingGetterProperty property, int index) {
        }

        @Override
        public void clone(MissingGetterProperty src, int srcIndex,
                          MissingGetterProperty dst, int dstIndex) {
        }
    }
}
