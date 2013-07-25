package com.lhkbob.entreri.components;

import com.lhkbob.entreri.Component;

/**
 *
 */
public interface EnumComponent extends Component {
    public static enum TestEnum {
        V1,
        V2
    }

    public TestEnum getValue();

    public EnumComponent setValue(TestEnum value);
}
