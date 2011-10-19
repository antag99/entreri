package com.entreri.component;

import com.entreri.EntitySystem;
import com.entreri.property.ObjectProperty;
import com.entreri.property.Parameter;

/**
 * A test component that tests the parameter constructor for ObjectProperty.
 * 
 * @author Michael Ludwig
 */
public class ObjectComponent extends AbstractComponent {
    @Parameter(type=int.class, value="3")
    private ObjectProperty property;
    
    protected ObjectComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public Object getObject(int offset) {
        int index = getIndex() * 3 + offset;
        return property.getIndexedData()[index];
    }
    
    public void setObject(int offset, Object value) {
        int index = getIndex() * 3 + offset;
        property.getIndexedData()[index] = value;
    }
}
