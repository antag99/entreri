package com.lhkbob.entreri.task;

import java.util.Set;

import com.lhkbob.entreri.TypeId;

public interface ParallelAware {
    // FIXME make sure to document that these cannot change for an instance
    public Set<TypeId<?>> getAccessedComponentes();

    public boolean isEntitySetModified();
}
