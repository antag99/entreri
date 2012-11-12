package com.lhkbob.entreri.task;

import java.util.Set;

import com.lhkbob.entreri.ComponentData;

public interface ParallelAware {
    // FIXME make sure to document that these cannot change for an instance
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents();

    public boolean isEntitySetModified();
}
