package com.googlecode.entreri.component;

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.annot.DefaultFactory;
import com.googlecode.entreri.property.IntProperty;

@DefaultFactory(DefaultComponentDataFactory.class)
public class DefaultFactoryComponent extends ComponentData<DefaultFactoryComponent> {
    public IntProperty prop;
}
