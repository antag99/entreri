package com.googlecode.entreri;

import java.util.Arrays;

public class ComponentIterator {
    private final EntitySystem system;
    
    private int index;
    
    private ComponentData<?>[] required; // all required except primary
    private ComponentData<?>[] optional;
    
    private ComponentData<?> primary;
    
    public ComponentIterator(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("System cannot be null");
        this.system = system;
        required = new ComponentData<?>[0];
        optional = new ComponentData<?>[0];
        primary = null;
        index = 0;
    }

    public ComponentIterator addRequired(ComponentData<?> data) {
        if (data == null)
            throw new NullPointerException("ComponentData cannot be null");
        if (data.owner.getEntitySystem() != system)
            throw new IllegalArgumentException("ComponentData not created by correct EntitySystem");
        
        // check to see if the data should be the new primary
        if (primary == null) {
            // no other required components, so just set it
            primary = data;
        } else {
            // check if the new data is shorter, but we will definitely
            // putting one data into the required array
            required = Arrays.copyOf(required, required.length + 1);

            if (data.owner.getMaxComponentIndex() < primary.owner.getMaxComponentIndex()) {
                // new primary
                required[required.length - 1] = primary;
                primary = data;
            } else {
                // not short enough so store it in the array
                required[required.length - 1] = data;
            }
        }
        
        return this;
    }
    
    public ComponentIterator addOptional(ComponentData<?> data) {
        if (data == null)
            throw new NullPointerException("ComponentData cannot be null");
        if (data.owner.getEntitySystem() != system)
            throw new IllegalArgumentException("ComponentData not created by correct EntitySystem");

        // add the data to the optional array
        optional = Arrays.copyOf(optional, optional.length + 1);
        optional[optional.length - 1] = data;
        
        return this;
    }
    
    public boolean next() {
        if (primary == null)
            return false;
        
        boolean found;
        int entity;
        int component;
        int count = primary.owner.getMaxComponentIndex();
        while(index < count - 1) {
            index++; // always increment one

            found = true;
            entity = primary.owner.getEntityIndex(index);
            if (entity != 0) {
                // we have a possible entity candidate
                primary.setFast(index);
                for (int i = 0; i < required.length; i++) {
                    component = required[i].owner.getComponentIndex(entity);
                    if (!required[i].setFast(component)) {
                        found = false;
                        break;
                    }
                }
                
                if (found) {
                    // we have satisfied all required components,
                    // so now set all optional requirements as well
                    for (int i = 0; i < optional.length; i++) {
                        component = optional[i].owner.getComponentIndex(entity);
                        optional[i].setFast(component); // we don't care if this is valid or not
                    }
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void reset() {
        index = 0;
    }
}
