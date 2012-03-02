/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
