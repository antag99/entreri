package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.FloatProperty;
import com.entreri.property.Parameter;

/**
 * A test component that tests the parameter for FloatProperty.
 * 
 * @author Michael Ludwig
 */
public class FloatComponent extends Component {
    @Parameter(type=int.class, value="3")
    private FloatProperty property;
    
    protected FloatComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public float getFloat(int offset) {
        int index = getIndex() * 3 + offset;
        return property.getIndexedData()[index];
    }
    
    public void setFloat(int offset, float value) {
        int index = getIndex() * 3 + offset;
        property.getIndexedData()[index] = value;
    }
}
