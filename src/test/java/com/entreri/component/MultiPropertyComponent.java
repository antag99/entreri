package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;
import com.entreri.property.Factory;
import com.entreri.property.FloatProperty;
import com.entreri.property.FloatPropertyFactory;
import com.entreri.property.MultiParameterProperty;
import com.entreri.property.NoParameterProperty;
import com.entreri.property.Parameter;
import com.entreri.property.Parameters;

/**
 * A Component that tests a variety of property constructors.
 * 
 * @author Michael Ludwig
 */
public class MultiPropertyComponent extends Component {
    @Parameters({@Parameter(type=int.class, value="2"),
                 @Parameter(type=float.class, value="0.3")})
    protected MultiParameterProperty multi;
    
    protected NoParameterProperty noparams;
    
    @Factory(FloatPropertyFactory.class)
    protected FloatProperty fromFactory;
    
    protected MultiPropertyComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    public void setFloat(int offset, float f) {
        multi.setFloat(offset + getIndex() * 2, f);
    }
    
    public float getFloat(int offset) {
        return multi.getFloat(offset + getIndex() * 2);
    }
    
    public NoParameterProperty getCompactProperty() {
        return noparams;
    }
}
