package com.googlecode.entreri.component;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.property.ObjectProperty;

public class TransientFieldComponent extends Component {
    private transient ObjectProperty<Object> transientProperty;
    private transient float field;
    
    protected TransientFieldComponent(EntitySystem system, int index) {
        super(system, index);
    }

    @Override
    protected void init(Object... initParams) throws Exception {
        
    }

    public void setObject(Object v) {
        transientProperty.set(v, getIndex(), 0);
    }
    
    public Object getObject() {
        return transientProperty.get(getIndex(), 0);
    }
    
    public float getFloat() {
        return field;
    }
    
    public void setFloat(float f) {
        field = f;
    }
}
