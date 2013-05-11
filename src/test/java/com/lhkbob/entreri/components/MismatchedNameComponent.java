package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Named;

/**
 * A component type that uses mismatched Named annotations to break an otherwise valid
 * definition.
 */
public interface MismatchedNameComponent extends Component {
    @Named("right")
    public void setValue(int f);

    @Named("left")
    public int getValue();
}
