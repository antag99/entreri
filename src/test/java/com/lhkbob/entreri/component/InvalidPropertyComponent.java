package com.lhkbob.entreri.component;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.NoFactoryProperty;

/**
 * Test component that uses the invalid property type NoFactoryProperty.
 */
public interface InvalidPropertyComponent extends Component {
    public NoFactoryProperty.Crass getCrass();

    public void setCrass(NoFactoryProperty.Crass value);
}
