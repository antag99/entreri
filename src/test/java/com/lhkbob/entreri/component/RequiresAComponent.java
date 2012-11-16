package com.lhkbob.entreri.component;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;

@Requires({IntComponent.class, FloatComponent.class})
public class RequiresAComponent extends ComponentData<RequiresAComponent> {
    private RequiresAComponent() {}
}
