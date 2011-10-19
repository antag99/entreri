package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.FloatProperty;
import com.entreri.property.Parameter;

/**
 * An invalid component definition where a Property is declared as a public
 * field.
 * 
 * @author Michael Ludwig
 */
public class PublicPropertyComponent extends Component {
    @Parameter(type=int.class, value="1")
    public FloatProperty property;
    
    protected PublicPropertyComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
