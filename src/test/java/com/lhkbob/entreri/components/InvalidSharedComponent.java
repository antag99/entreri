package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.SharedInstance;

/**
 * Invalid component type that tries to have a shared instance from a property that
 * doesn't support it.
 */
public interface InvalidSharedComponent extends Component {
    @SharedInstance
    public int getSharedValue();

    public void setSharedValue(int v);
}
