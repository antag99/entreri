package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.*;

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
        public IndexedDataStore getDataStore() {
            return null;
        }

        @Override
        public void setDataStore(IndexedDataStore store) {
        }
    }

    public static class MissingGetterFactory
            extends AbstractPropertyFactory<MissingGetterProperty> {

        public MissingGetterFactory(Attributes attrs) {
            super(attrs);
        }

        @Override
        public MissingGetterProperty create() {
            return new MissingGetterProperty();
        }

        @Override
        public void setDefaultValue(MissingGetterProperty property, int index) {
        }
    }
}
