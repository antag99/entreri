package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.SharedInstance;

/**
 * Invalid component definition that double checks that we fail when using a shared
 * instance annotation with an incompatible type
 */
public interface IllegalSharedPropertyComponent extends Component {
    @SharedInstance
    public Object getValue();

    public void setValue(Object v);
}