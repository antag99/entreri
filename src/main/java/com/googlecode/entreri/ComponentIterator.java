package com.googlecode.entreri;

import java.util.ArrayList;
import java.util.List;

public class ComponentIterator {
    private final List<ComponentData<?>> required;
    private final List<ComponentData<?>> optional;
    
    private final EntitySystem system;
    
    // FIXME: to avoid pulling in the Entity objects that this iterates over,
    // I think it would be best if we had an index into each component store.
    // We can then validate quickly the CDs by checking the componentToEntity index.
    private int index;
    // FIXME: do we also store the shortest component type?
    // FIXME: should I just use the iterator's over component type?
    // FIXME: if not, can I remove them?
    
    // For some time I had thought that it might be faster to walk over all component types
    // at the same time, but that actually won't work because each type's index
    // will not have the same sorted order (unless a compact() is performed).
    // Since we can't assume that, we have to use the method where the shortest
    // component type is used and the entity-component-index is checked for all
    // other types
    
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
