package com.lhkbob.entreri.component;

import com.lhkbob.entreri.Component;

/**
 * A component where the setter and getter types don't match for a property.
 */
public interface MismatchedTypeComponent extends Component {
    public int getValue();

    public void setValue(float f);
}
