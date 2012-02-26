package com.googlecode.entreri;

import java.util.ArrayList;
import java.util.List;

public class ComponentIterator {
    private final List<ComponentData<?>> required;
    private final List<ComponentData<?>> optional;
    
    private final EntitySystem system;
    
    private int index;
    // FIXME: do we also store the shortest component type?
    // FIXME: should I just use the iterator's over component type?
    // FIXME: if not, can I remove them?
    
    public ComponentIterator(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("System cannot be null");
        this.system = system;
        required = new ArrayList<ComponentData<?>>();
        optional = new ArrayList<ComponentData<?>>();
    }

    public ComponentIterator addRequired(ComponentData<?> data) {
        return add(data, required);
    }
    
    public ComponentIterator addOptional(ComponentData<?> data) {
        return add(data, optional);
    }
    
    private ComponentIterator add(ComponentData<?> data, List<ComponentData<?>> list) {
        
    }
    
    public boolean next() {
        
    }
    
    public void reset() {
        
    }
}
