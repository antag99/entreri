package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.IntProperty;
import com.entreri.property.Parameter;

/**
 * A test component that tests the parameter constructor for IntProperty.
 * 
 * @author Michael Ludwig
 */
public class IntComponent extends Component {
    @Parameter(type=int.class, value="3")
    private IntProperty property;
    
    protected IntComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public int getInt(int offset) {
        int index = getIndex() * 3 + offset;
        return property.getIndexedData()[index];
    }
    
    public void setInt(int offset, int value) {
        int index = getIndex() * 3 + offset;
        property.getIndexedData()[index] = value;
    }
}
