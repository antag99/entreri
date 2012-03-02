/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.entreri.property.BooleanProperty;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.IndexedDataStore;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;

/**
 * ComponentRepository manages storing all the componentDatas of a specific type for an
 * EntitySystem. It also controls the IndexedDataStore's for the type's set of
 * properties. It is package-private because its details are low-level and
 * complex.
 * 
 * @author Michael Ludwig
 * @param <T> The type of component stored by the index
 */
final class ComponentRepository<T extends ComponentData<T>> {
    private final EntitySystem system;
    private final TypeId<T> type;

    private final ComponentDataFactory<T> factory;
    
    // These three arrays have a special value of 0 or null stored in the 0th
    // index, which allows us to lookup componentDatas or entities when they
    // normally aren't attached.
    private int[] entityIndexToComponentRepository;
    private int[] componentIndexToEntityIndex;
    private Component<T>[] components;
    private int componentInsert;
    
    private final List<PropertyStore<?>> declaredProperties;
    private final List<PropertyStore<?>> decoratedProperties;
    
    private final BooleanProperty enabledProperty; // this is also contained in decoratedProperties
    private final IntProperty componentIdProperty; // this is contained in decoratedProperties
    private int idSeq;
     
    /**
     * Create a ComponentRepository for the given system, that will store Components
     * of the given type.
     * 
     * @param system The owning system
     * @param type The type of component
     * @throws NullPointerException if system or type are null
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ComponentRepository(EntitySystem system, TypeId<T> type, ComponentDataFactory<T> factory) {
        if (system == null || type == null)
            throw new NullPointerException("Arguments cannot be null");
        
        this.system = system;
        this.factory = factory;
        this.type = type;
        
        Map<String, PropertyFactory<?>> propertyFactories = factory.getPropertyFactories();
        
        declaredProperties = new ArrayList<PropertyStore<?>>();
        decoratedProperties = new ArrayList<PropertyStore<?>>(); // empty for now
        for (Entry<String, PropertyFactory<?>> e: propertyFactories.entrySet()) {
            PropertyStore store = new PropertyStore(e.getValue(), e.getKey());
            declaredProperties.add(store);
        }
        
        entityIndexToComponentRepository = new int[1]; // holds default 0 value in 0th index
        componentIndexToEntityIndex = new int[1]; // holds default 0 value in 0th index
        components = new Component[1]; // holds default null value in 0th index
        
        componentInsert = 1;
        
        // Make sure properties' stores hold enough space
        resizePropertyStores(declaredProperties, 1);
        
        // decorate the component data with a boolean property to track enabled status
        enabledProperty = decorate(BooleanProperty.factory(1, true));
        componentIdProperty = decorate(IntProperty.factory(1)); // we'll not assign a default value, since we change the id each time
        idSeq = 1; // start at 1, just like entity id sequences
    }
    
    /**
     * @return The type of component data stored by this component index
     */
    public TypeId<T> getTypeId() {
        return type;
    }

    /**
     * @return The upper bound (exclusive) for component index values
     */
    public int getMaxComponentIndex() {
        return componentInsert;
    }
    
    /**
     * @return The owning EntitySystem
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * Given the index of a Component (e.g. {@link Component#getIndex()}, return
     * the index of an entity within the owning system. The returned entity
     * index can be safely passed to {@link EntitySystem#getEntityByIndex(int)}.
     * 
     * @param componentIndex The component index whose owning entity is fetched
     * @return The index of the entity that has the given component index, or 0
     *         if the component is not attached
     */
    public int getEntityIndex(int componentIndex) {
        if (componentIndex >= componentIndexToEntityIndex.length) {
            System.out.println("problem!");
        }
        return componentIndexToEntityIndex[componentIndex];
    }

    /**
     * Given the index of an entity (e.g. {@link Entity#index}), return the
     * index of the attached component of this ComponentRepository's type. The
     * returned component index can be used in {@link #getComponent(int)} and
     * related methods.
     * 
     * @param entityIndex The entity index to look up
     * @return The index of the attached component, or 0 if the entity does not
     *         have a component of this type attached
     */
    public int getComponentIndex(int entityIndex) {
        return entityIndexToComponentRepository[entityIndex];
    }

    /**
     * Ensure that this ComponentRepository has enough internal space to hold its
     * entity-to-component mapping for the given number of entities.
     * 
     * @param numEntities The new number of entities
     */
    public void expandEntityIndex(int numEntities) {
        if (entityIndexToComponentRepository.length < numEntities) {
            entityIndexToComponentRepository = Arrays.copyOf(entityIndexToComponentRepository, (int) (numEntities * 1.5f) + 1);
        }
    }

    /**
     * @param componentIndex The component to look up
     * @return True if the component at componentIndex is enabled
     */
    public boolean isEnabled(int componentIndex) {
        return enabledProperty.get(componentIndex, 0);
    }

    /**
     * Set whether or not the component at <tt>componentIndex</tt> is enabled.
     * 
     * @param componentIndex The component index
     * @param enabled True if the component is enabled
     */
    public void setEnabled(int componentIndex, boolean enabled) {
        enabledProperty.set(enabled, componentIndex, 0);
    }
    
    public int getId(int componentIndex) {
        return componentIdProperty.get(componentIndex, 0);
    }
    
    /*
     * As expandEntityIndex() but expands all related component data and arrays
     * to hold the number of components.
     */
    private void expandComponentRepository(int numComponents) {
        if (numComponents < components.length)
            return;

        int size = (int) (numComponents * 1.5f) + 1;
        
        // Expand the indexed data stores for the properties
        resizePropertyStores(declaredProperties, size);
        resizePropertyStores(decoratedProperties, size);
        
        // Expand the canonical component array
        components = Arrays.copyOf(components, size);
        
        // Expand the component index
        componentIndexToEntityIndex = Arrays.copyOf(componentIndexToEntityIndex, size);
    }

    /*
     * Convenience to create a new data store for each property with the given
     * size, copy the old data over, and assign it back to the property.
     */
    private void resizePropertyStores(List<PropertyStore<?>> properties, int size) {
        int ct = properties.size();
        for (int i = 0; i < ct; i++) {
            IndexedDataStore oldStore = properties.get(i).property.getDataStore();
            IndexedDataStore newStore = oldStore.create(size);
            oldStore.copy(0, Math.min(oldStore.size(), size), newStore, 0);
            properties.get(i).property.setDataStore(newStore);
        }
    }
    
    /**
     * @param componentIndex The component index whose component is fetched
     * @return The component reference at the given index, may be null
     */
    public Component<T> getComponent(int componentIndex) {
        return components[componentIndex];
    }

    /**
     * Create a new component and attach to it the entity at the given entity
     * index, the new component will have its values copied from the existing
     * template.
     * 
     * @param entityIndex The entity index which the component is attached to
     * @param fromTemplate A template to assign values to the new component
     * @return A new component of type T
     * @throws NullPointerException if fromTemplate is null
     * @throws IllegalArgumentException if the template was not created by this
     *             index
     * @throws IllegalStateException if the template is not live
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Component<T> addComponent(int entityIndex, Component<T> fromTemplate) {
        if (fromTemplate.getEntitySystem() != getEntitySystem())
            throw new IllegalArgumentException("Component not owned by expected EntitySystem");
        if (!fromTemplate.getTypeId().equals(type))
            throw new IllegalArgumentException("Component not of expected type, expected: " + type + ", but was: " + type);
        if (!fromTemplate.isLive())
            throw new IllegalStateException("Template component is not live");
        
        Component<T> instance = addComponent(entityIndex);
        for (int i = 0; i < declaredProperties.size(); i++) {
            PropertyStore store = declaredProperties.get(i);
            store.clone(fromTemplate.index, store.property, instance.index);
        }
        
        // fire add-event listener after cloning is completed
        system.getControllerManager().fireComponentAdd(instance);
        return instance;
    }

    /**
     * Create a new component and attach to it the entity at the given entity
     * index. The component will have the default state as specified by its
     * properties.
     * 
     * @param entityIndex The entity index which the component is attached to
     * @param initParams The var-args of parameters that must match the
     *            InitParams annotation of the type
     * @return A new component of type T
     * @throws IllegalArgumentException if initParams is incorrect
     */
    public Component<T> addComponent(int entityIndex) {
        Component<T> instance = allocateComponent(entityIndex);
        
        // fire add-event listener after initialization is completed
        system.getControllerManager().fireComponentAdd(instance);
        return instance;
    }
    
    /*
     * Allocate and store a new component, but don't initialize it yet.
     */
    private Component<T> allocateComponent(int entityIndex) {
        if (entityIndexToComponentRepository[entityIndex] != 0)
            removeComponent(entityIndex);
        
        int componentIndex = componentInsert++;
        if (componentIndex >= components.length)
            expandComponentRepository(componentIndex + 1);

        Component<T> instance = new Component<T>(this, componentIndex);
        components[componentIndex] = instance;
        componentIndexToEntityIndex[componentIndex] = entityIndex;
        entityIndexToComponentRepository[entityIndex] = componentIndex;

        // Set default value for declared and decorated properties,
        // this is needed because we might be overwriting a previously removed
        // component, or the factory might be doing something tricky
        for (int i = 0; i < declaredProperties.size(); i++) {
            declaredProperties.get(i).setValue(componentIndex);
        }
        
        for (int i = 0; i < decoratedProperties.size(); i++) {
            decoratedProperties.get(i).setValue(componentIndex);
        }
        
        // although there could be a custom PropertyFactory for setting the id,
        // it's easier to assign a new id here
        componentIdProperty.set(idSeq++, componentIndex, 0);
        
        return instance;
    }

    /**
     * Create a new ComponentData of type T that can be used to view components
     * in this index.
     * 
     * @return A new data instance
     */
    public T createDataInstance() {
        // create a new instance from the factory - it will be completely detached
        T t = factory.createInstance();
        
        // attach it to this data index, at the 0th index
        //  - at this point the ComponentData's owner should be considered final
        t.owner = this;
        
        // assign all property values
        for (int i = 0; i < declaredProperties.size(); i++) {
            PropertyStore<?> p = declaredProperties.get(i);
            factory.setProperty(t, p.key, p.property);
        }
        
        t.set(null);
        return t;
    }

    /**
     * Detach or remove any component of this index's type from the entity with
     * the given index. True is returned if a component was removed, or false
     * otherwise.
     * 
     * @param entityIndex The entity's index whose component is removed
     * @return True if a component was removed
     */
    public boolean removeComponent(int entityIndex) {
        int componentIndex = entityIndexToComponentRepository[entityIndex];

        // This code works even if componentIndex is 0
        Component<T> oldComponent = components[componentIndex];
        if (oldComponent != null) {
            // perform component clean up before data is invalidated
            system.getControllerManager().fireComponentRemove(oldComponent);
            oldComponent.index = 0;
        }

        components[componentIndex] = null;
        entityIndexToComponentRepository[entityIndex] = 0; // entity does not have component
        componentIndexToEntityIndex[componentIndex] = 0; // component does not have entity
        
        return oldComponent != null;
    }

    /*
     * Update all component data in the list of properties. If possible the data
     * store in swap is reused.
     */
    private void update(List<PropertyStore<?>> properties, Component<T>[] newToOldMap) {
        for (int i = 0; i < properties.size(); i++) {
            PropertyStore<?> p = properties.get(i);
            IndexedDataStore origStore = p.property.getDataStore();
            
            p.property.setDataStore(update(origStore, p.swap, newToOldMap));
            p.swap = origStore;
        }
    }

    /*
     * Update all component data in src to be in dst by shuffling it to match
     * newToOldMap.
     */
    private IndexedDataStore update(IndexedDataStore src, IndexedDataStore dst, 
                                    Component<T>[] newToOldMap) {
        int dstSize = newToOldMap.length;
        
        if (dst == null || dst.size() < dstSize)
            dst = src.create(dstSize);
        
        int i;
        int lastIndex = -1;
        int copyIndexNew = -1;
        int copyIndexOld = -1;
        for (i = 1; i < componentInsert; i++) {
            if (newToOldMap[i] == null) {
                // we've hit the end of existing componentDatas, so break
                break;
            }
            
            if (newToOldMap[i].index != lastIndex + 1) {
                // we are not in a contiguous section
                if (copyIndexOld >= 0) {
                    // we have to copy over the last section
                    src.copy(copyIndexOld, (i - copyIndexNew), dst, copyIndexNew);
                }
                
                // set the copy indices
                copyIndexNew = i;
                copyIndexOld = newToOldMap[i].index;
            }
            lastIndex = newToOldMap[i].index;
        }
        
        if (copyIndexOld >= 0) {
            // final copy
            src.copy(copyIndexOld, (i - copyIndexNew), dst, copyIndexNew);
        }

        return dst;
    }
    
    private void notifyCompactAwareProperties(List<PropertyStore<?>> props) {
        PropertyStore<?> p;
        for (int i = 0; i < props.size(); i++) {
            p = props.get(i);
            if (p.property instanceof CompactAwareProperty)
                ((CompactAwareProperty) p.property).onCompactComplete();
        }
    }

    /**
     * <p>
     * Compact the data of this ComponentRepository to account for removals and
     * additions to the index. This will ensure that all active componentDatas are
     * packed into the underlying arrays, and that they will be accessed in the
     * same order as iterating over the entities directly.
     * </p>
     * <p>
     * The map from old to new entity index must be used to properly update the
     * component index's data so that the system is kept in sync.
     * </p>
     * 
     * @param entityOldToNewMap A map from old entity index to new index
     * @param numEntities The number of entities that are in the system
     */
    public void compact(int[] entityOldToNewMap, int numEntities) {
        // First sort the canonical componentDatas array
        Arrays.sort(components, 1, componentInsert, new Comparator<Component<T>>() {
            @Override
            public int compare(Component<T> o1, Component<T> o2) {
                if (o1 != null && o2 != null)
                    return componentIndexToEntityIndex[o1.index] - componentIndexToEntityIndex[o2.index];
                else if (o1 != null)
                    return -1; // push null o2 to end of array
                else if (o2 != null)
                    return 1; // push null o1 to end of array
                else
                    return 0; // both null so they are "equal"
            }
        });
        
        // Update all of the property stores to match up with the componentDatas new positions
        update(declaredProperties, components);
        update(decoratedProperties, components);
        
        // Repair the componentToEntityIndex and the component.index values
        componentInsert = 1;
        int[] newComponentRepository = new int[components.length];
        for (int i = 1; i < components.length; i++) {
            if (components[i] != null) {
                newComponentRepository[i] = entityOldToNewMap[componentIndexToEntityIndex[components[i].index]];
                components[i].index = i;
                componentInsert = i + 1;
            }
        }
        componentIndexToEntityIndex = newComponentRepository;
        
        // Possibly compact the component data
        /*if (componentInsert < .6f * components.length) {
            int newSize = (int) (1.2f * componentInsert) + 1;
            components = Arrays.copyOf(components, newSize);
            componentIndexToEntityIndex = Arrays.copyOf(componentIndexToEntityIndex, newSize);
            resizePropertyStores(declaredProperties, newSize);
            resizePropertyStores(decoratedProperties, newSize);
        }*/
        
        // Repair entityIndexToComponentRepository - and possible shrink the index
        // based on the number of packed entities
        /*if (numEntities < .6f * entityIndexToComponentRepository.length)
            entityIndexToComponentRepository = new int[(int) (1.2f * numEntities) + 1];
        else
            Arrays.fill(entityIndexToComponentRepository, 0);*/
        
        for (int i = 1; i < componentInsert; i++)
            entityIndexToComponentRepository[componentIndexToEntityIndex[i]] = i;
        
        notifyCompactAwareProperties(declaredProperties);
        notifyCompactAwareProperties(decoratedProperties);
    }

    /**
     * Decorate the type information of this ComponentRepository to add a property
     * created by the given factory. The returned property will have default
     * data assigned for each current Component in the index, and will have the
     * default value assigned for each new Component. Decorators can then access
     * the returned property to manipulate the decorated component data.
     * 
     * @param <P> The type of property created
     * @param factory The factory that will create a unique Property instance
     *            associated with the decorated property and this index
     * @return The property decorated onto the type of the index
     */
    public <P extends Property> P decorate(PropertyFactory<P> factory) {
        int size = (declaredProperties.isEmpty() ? componentInsert
                                                 : declaredProperties.get(0).property.getDataStore().size());
        
        PropertyStore<P> pstore = new PropertyStore<P>(factory, "decorated");

        // Set values from factory to all component slots
        IndexedDataStore newStore = pstore.property.getDataStore().create(size);
        pstore.property.setDataStore(newStore);
        for (int i = 1; i < size; i++) {
            pstore.setValue(i);
        }
        
        decoratedProperties.add(pstore);
        return pstore.property;
    }

    /**
     * Remove the given property from the set of decorated properties on this
     * index's type. If the property is invalid or not a decorated property for
     * the index, this does nothing.
     * 
     * @param p The property to remove
     */
    public void undecorate(Property p) {
        Iterator<PropertyStore<?>> it = decoratedProperties.iterator();
        while(it.hasNext()) {
            if (it.next().property == p) {
                it.remove();
                break;
            }
        }
    }
    
    /*
     * Type wrapping a key, property, and factory, as well as an auxiliary data
     * store for compaction.
     */
    private static class PropertyStore<P extends Property> {
        final String key;
        final P property;
        final PropertyFactory<P> creator;
        IndexedDataStore swap; // may be null
        
        public PropertyStore(PropertyFactory<P> creator, String key) {
            this.creator = creator;
            this.key = key;
            property = creator.create();
        }
        
        private void clone(int srcIndex, P dst, int dstIndex) {
            creator.clone(property, srcIndex, dst, dstIndex);
        }
        
        private void setValue(int index) {
            creator.setDefaultValue(property, index);
        }
    }
}
