package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.FloatProperty;
import com.entreri.property.Parameter;

/**
 * A test component that defines a non-Property field.
 * 
 * @author Michael Ludwig
 */
public class ExtraFieldComponent extends Component {
    @SuppressWarnings("unused")
    @Parameter(type=int.class, value="1")
    private FloatProperty property;
    
    @SuppressWarnings("unused")
    private Object otherField;
    
    protected ExtraFieldComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
