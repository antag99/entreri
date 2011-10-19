package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.FloatProperty;
import com.entreri.property.Parameter;
import com.entreri.property.Parameters;

/**
 * A test component that uses parameters to reference a non-existent property
 * constructor.
 * 
 * @author Michael Ludwig
 */
public class BadParametersComponent extends Component {
    @SuppressWarnings("unused")
    @Parameters({@Parameter(type=int.class, value="3"),
                 @Parameter(type=float.class, value="0.3")})
    private FloatProperty property;
    
    protected BadParametersComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
