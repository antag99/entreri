package com.entreri.component;

import com.entreri.Component;
import com.entreri.EntitySystem;

/**
 * A Component definition with a public constructor.
 * 
 * @author Michael Ludwig
 */
public class PublicConstructorComponent extends Component {

    public PublicConstructorComponent(EntitySystem system, int index) {
        super(system, index);
    }
}
