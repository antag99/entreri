package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.*;

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
        public IndexedDataStore getDataStore() {
            return null;
        }

        @Override
        public void setDataStore(IndexedDataStore store) {
        }
    }

    public static class MissingSetterFactory
            extends AbstractPropertyFactory<MissingSetterProperty> {

        public MissingSetterFactory(Attributes attrs) {
            super(attrs);
        }

        @Override
        public MissingSetterProperty create() {
            return new MissingSetterProperty();
        }

        @Override
        public void setDefaultValue(MissingSetterProperty property, int index) {
        }
    }
}
