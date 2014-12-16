package com.lhkbob.entreri.attr;

import java.util.*;

/**
 *
 */
public @interface Collection {
    public static enum Type {
        LIST,
        SET,
        MAP,
        UNSPECIFIED
    }

    Class<? extends List> listImpl() default ArrayList.class;

    Class<? extends Set> setImpl() default HashSet.class;

    Class<? extends Map> mapImpl() default HashMap.class;

    boolean allowNullElements() default false;

    Type type() default Type.UNSPECIFIED;
}

