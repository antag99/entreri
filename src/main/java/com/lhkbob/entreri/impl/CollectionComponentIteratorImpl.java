package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 */
public class CollectionComponentIteratorImpl implements ComponentIterator {
    private final EntitySystemImpl system;
    private final Iterable<Entity> entities;

    private AbstractComponent<?>[] required; // all required except primary
    private AbstractComponent<?>[] optional;

    private Iterator<Entity> currentIterator;

    public CollectionComponentIteratorImpl(EntitySystemImpl system, Iterable<Entity> entities) {
        this.entities = entities;
        this.system = system;

        required = new AbstractComponent<?>[0];
        optional = new AbstractComponent<?>[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addRequired(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Component type cannot be null");
        }

        AbstractComponent<T> data = system.getRepository(type).createDataInstance();

        // add the data to the required array
        required = Arrays.copyOf(required, required.length + 1);
        required[required.length - 1] = data;

        return (T) data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addOptional(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Component type cannot be null");
        }

        AbstractComponent<T> data = system.getRepository(type).createDataInstance();

        // add the data to the optional array
        optional = Arrays.copyOf(optional, optional.length + 1);
        optional[optional.length - 1] = data;

        return (T) data;
    }

    @Override
    public boolean next() {
        if (currentIterator == null) {
            currentIterator = entities.iterator();
        }

        int entityIndex;
        int component;
        boolean found;
        while (currentIterator.hasNext()) {
            entityIndex = ((EntityImpl) currentIterator.next()).index;

            found = true;
            for (int i = 0; i < required.length; i++) {
                component = required[i].owner.getComponentIndex(entityIndex);
                if (component == 0) {
                    found = false;
                    break;
                } else {
                    required[i].setIndex(component);
                }
            }

            if (found) {
                // valid entity
                for (int i = 0; i < optional.length; i++) {
                    component = optional[i].owner.getComponentIndex(entityIndex);
                    optional[i].setIndex(component);
                }

                return true;
            }
        }

        // if we've run out of entities, we don't have anymore
        return false;
    }

    @Override
    public void reset() {
        currentIterator = null;
    }
}
