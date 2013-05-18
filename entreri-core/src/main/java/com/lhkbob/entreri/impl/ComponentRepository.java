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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * ComponentRepository manages storing all the componentDatas of a specific type for an
 * EntitySystem. It also controls the IndexedDataStore's for the type's set of properties.
 * It is package-private because its details are low-level and complex.
 *
 * @param <T> The type of component stored by the index
 *
 * @author Michael Ludwig
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class ComponentRepository<T extends Component> {
    private final EntitySystemImpl system;
    private final Class<T> type;

    private final ComponentFactoryProvider.Factory<T> factory;
    private final Class<? extends Component>[] requiredTypes;

    // These three arrays have a special value of 0 or null stored in the 0th
    // index, which allows us to lookup componentDatas or entities when they
    // normally aren't attached.
    private int[] entityIndexToComponentRepository;
    private int[] componentIndexToEntityIndex;
    private T[] components;
    private int componentInsert;

    private final List<DeclaredPropertyStore<?>> declaredProperties;
    private final List<DecoratedPropertyStore<?>> decoratedProperties;

    // this is contained in decoratedProperties
    private final IntProperty componentIdProperty;
    private final IntProperty componentVersionProperty;

    private final ObjectProperty ownerDelegatesProperty;

    private int idSeq;
    private int versionSeq;

    /**
     * Create a ComponentRepository for the given system, that will store Components of
     * the given type.
     *
     * @param system The owning system
     * @param type   The type of component
     *
     * @throws NullPointerException if system or type are null
     */
    public ComponentRepository(EntitySystemImpl system, Class<T> type) {
        if (system == null || type == null) {
            throw new NullPointerException("Arguments cannot be null");
        }

        this.system = system;
        this.factory = ComponentFactoryProvider.getInstance().getFactory(type);
        this.type = type;

        if (type.getAnnotation(Requires.class) != null) {
            requiredTypes = type.getAnnotation(Requires.class).value();
        } else {
            requiredTypes = new Class[0];
        }

        declaredProperties = new ArrayList<>();
        decoratedProperties = new ArrayList<>(); // empty for now
        for (PropertyDeclaration p : factory.getSpecification().getProperties()) {
            DeclaredPropertyStore store = new DeclaredPropertyStore(
                    p.getPropertyFactory(), p.getName());
            declaredProperties.add(store);
        }

        entityIndexToComponentRepository = new int[1]; // holds default 0 value in 0th index
        componentIndexToEntityIndex = new int[1]; // holds default 0 value in 0th index
        components = (T[]) new Component[1]; // holds default null value in 0th index

        componentInsert = 1;

        // Make sure properties' stores hold enough space
        resizePropertyStores(declaredProperties, 1);

        // decorate the component data with a boolean property to track enabled status
        // we set a unique id for every component
        componentIdProperty = decorate(new IntProperty.Factory(0));
        componentVersionProperty = decorate(new IntProperty.Factory(0));
        ownerDelegatesProperty = decorate(ObjectProperty.<OwnerSupport>factory(null));

        idSeq = 1; // start at 1, just like entity id sequences versionSeq = 0;

        // initialize version for the 0th index
        componentVersionProperty.set(0, -1);
    }

    /**
     * @return The type of component data stored by this component index
     */
    public Class<T> getType() {
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
    public EntitySystemImpl getEntitySystem() {
        return system;
    }

    /**
     * Given the index of a Component (e.g. {@link Component#getIndex()}, return the index
     * of an entity within the owning system. The returned entity index can be safely
     * passed to {@link EntitySystemImpl#getEntityByIndex(int)}.
     *
     * @param componentIndex The component index whose owning entity is fetched
     *
     * @return The index of the entity that has the given component index, or 0 if the
     *         component is not attached
     */
    public int getEntityIndex(int componentIndex) {
        return componentIndexToEntityIndex[componentIndex];
    }

    /**
     * Given the index of an entity (e.g. {@link EntityImpl#index}), return the index of
     * the attached component of this ComponentRepository's type. The returned component
     * index can be used in {@link #getComponent(int)} and related methods.
     *
     * @param entityIndex The entity index to look up
     *
     * @return The index of the attached component, or 0 if the entity does not have a
     *         component of this type attached
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
            entityIndexToComponentRepository = Arrays
                    .copyOf(entityIndexToComponentRepository,
                            (int) (numEntities * 1.5) + 1);
        }
    }

    /**
     * @param componentIndex The component index
     *
     * @return The component id of the component at the given index
     */
    public int getId(int componentIndex) {
        return componentIdProperty.get(componentIndex);
    }

    /**
     * @param componentIndex The component index
     *
     * @return The component version of the component at the given index
     */
    public int getVersion(int componentIndex) {
        return componentVersionProperty.get(componentIndex);
    }

    /**
     * Increment the component's version at the given index. This does nothing if the
     * index is 0, preserving the guarantee that an invalid component has a negative
     * version.
     *
     * @param componentIndex The component to update
     */
    public void incrementVersion(int componentIndex) {
        if (componentIndex != 0) {
            // clamp it to be above 0, instead of going negative
            int newVersion = (0xefffffff & (versionSeq++));
            componentVersionProperty.set(componentIndex, newVersion);
        }
    }

    /**
     * @param componentIndex The component index
     *
     * @return The OwnerSupport delegate for the component by the given index
     */
    public OwnerSupport getOwnerDelegate(int componentIndex) {
        return (OwnerSupport) ownerDelegatesProperty.get(componentIndex);
    }

    /**
     * @param propertyIndex The index of the property, which is the corresponding index
     *                      from the property specification of the component type
     *
     * @return The declared property used for the given index by this repository
     */
    public Property getProperty(int propertyIndex) {
        return declaredProperties.get(propertyIndex).getProperty();
    }

    /**
     * @param propertyIndex The index of the property
     *
     * @return The logical name of the property
     */
    public String getDeclaredPropertyName(int propertyIndex) {
        return declaredProperties.get(propertyIndex).key;
    }

    /**
     * @return The number of declared properties
     */
    public int getDeclaredPropertyCount() {
        return declaredProperties.size();
    }

    /*
     * As expandEntityIndex() but expands all related component data and arrays
     * to hold the number of components.
     */
    private void expandComponentRepository(int numComponents) {
        if (numComponents < components.length) {
            return;
        }

        int size = (int) (numComponents * 1.5) + 1;

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
    private void resizePropertyStores(List<? extends PropertyStore<?>> properties,
                                      int size) {
        int ct = properties.size();
        for (int i = 0; i < ct; i++) {
            properties.get(i).resize(size);
        }
    }

    /**
     * @param componentIndex The component index whose component is fetched
     *
     * @return The component reference at the given index, may be null
     */
    public T getComponent(int componentIndex) {
        return components[componentIndex];
    }

    /**
     * Create a new component and attach to it the entity at the given entity index, the
     * new component will have its values copied from the existing template.
     *
     * @param entityIndex  The entity index which the component is attached to
     * @param fromTemplate A template to assign values to the new component
     *
     * @return A new component of type T
     *
     * @throws NullPointerException  if fromTemplate is null
     * @throws IllegalStateException if the template is not live
     */
    public T addComponent(int entityIndex, T fromTemplate) {
        if (!type.isInstance(fromTemplate)) {
            throw new IllegalArgumentException(
                    "Component not of expected type, expected: " + type + ", but was: " +
                    fromTemplate.getClass());
        }
        if (!fromTemplate.isAlive()) {
            throw new IllegalStateException("Template component is not live");
        }

        T instance = addComponent(entityIndex);
        // this is safe across systems because the property spec for a component type
        // will be the same so the factories will be consistent and the order is the
        // same since they're reported in name order
        for (int i = 0; i < declaredProperties.size(); i++) {
            DeclaredPropertyStore dstStore = declaredProperties.get(i);
            DeclaredPropertyStore templateStore = ((AbstractComponent<T>) fromTemplate)
                    .owner.declaredProperties.get(i);

            templateStore.creator
                         .clone(templateStore.getProperty(), fromTemplate.getIndex(),
                                dstStore.property, instance.getIndex());
        }

        return instance;
    }

    /**
     * Create a new component and attach to it the entity at the given entity index. The
     * component will have the default state as specified by its properties.
     *
     * @param entityIndex The entity index which the component is attached to
     *
     * @return A new component of type T
     *
     * @throws IllegalArgumentException if initParams is incorrect
     */
    public T addComponent(int entityIndex) {
        if (entityIndexToComponentRepository[entityIndex] != 0) {
            removeComponent(entityIndex);
        }

        int componentIndex = componentInsert++;
        if (componentIndex >= components.length) {
            expandComponentRepository(componentIndex + 1);
        }

        AbstractComponent<T> instance = factory.newInstance(this);
        components[componentIndex] = (T) instance;
        componentIndexToEntityIndex[componentIndex] = entityIndex;
        entityIndexToComponentRepository[entityIndex] = componentIndex;

        // Set default value for declared and decorated properties,
        // this is needed because we might be overwriting a previously removed
        // component, or the factory might be doing something tricky
        for (int i = 0; i < declaredProperties.size(); i++) {
            declaredProperties.get(i).setDefaultValue(componentIndex);
        }
        for (int i = 0; i < decoratedProperties.size(); i++) {
            decoratedProperties.get(i).setDefaultValue(componentIndex);
        }

        // although there could be a custom PropertyFactory for setting the id,
        // it's easier to assign a new id here
        componentIdProperty.set(componentIndex, idSeq++);
        // same goes for assigning a new owner delegate
        ownerDelegatesProperty.set(componentIndex, new OwnerSupport(instance));

        // start with a unique version as well
        incrementVersion(componentIndex);

        // connect component back to the index too
        instance.setIndex(componentIndex);

        // ensure required components are added as well
        Entity entity = system.getEntityByIndex(entityIndex);
        for (int i = 0; i < requiredTypes.length; i++) {
            if (entity.get((Class) requiredTypes[i]) == null) {
                Component added = entity.add((Class) requiredTypes[i]);
                added.setOwner(instance);
            }
        }

        return (T) instance;
    }

    /**
     * Create a new ComponentData of type T that can be used to view components in this
     * index.
     *
     * @return A new data instance
     */
    public AbstractComponent<T> createDataInstance() {
        AbstractComponent<T> t = factory.newInstance(this);
        t.setIndex(0);
        return t;
    }

    /**
     * Detach or remove any component of this index's type from the entity with the given
     * index. True is returned if a component was removed, or false otherwise.
     *
     * @param entityIndex The entity's index whose component is removed
     *
     * @return True if a component was removed
     */
    public boolean removeComponent(int entityIndex) {
        int componentIndex = entityIndexToComponentRepository[entityIndex];

        // This code works even if componentIndex is 0
        T oldComponent = components[componentIndex];
        AbstractComponent<T> casted = (AbstractComponent<T>) oldComponent;
        if (oldComponent != null) {
            oldComponent.setOwner(null);
            getOwnerDelegate(componentIndex).disownAndRemoveChildren();
            casted.setIndex(0);
        }

        components[componentIndex] = null;
        entityIndexToComponentRepository[entityIndex] = 0; // entity does not have component
        componentIndexToEntityIndex[componentIndex] = 0; // component does not have entity
        componentIdProperty.set(componentIndex, 0); // clear id
        ownerDelegatesProperty.set(componentIndex, null);

        return oldComponent != null;
    }

    private void sort() {
        // perform an insertion sort, since most components are likely to be
        // ordered correctly the performance will be almost linear

        for (int i = 1; i < componentInsert; i++) {
            int vi = componentIndexToEntityIndex[i];
            for (int j = i - 1; j >= 1; j--) {
                int vj = componentIndexToEntityIndex[j];

                // move an index left if it is valid and it's less than
                // the prior index or if the prior index is invalid
                if (vi > 0 && (vi < vj || vj == 0)) {
                    // must swap in place
                    componentIndexToEntityIndex[j] = vi;
                    componentIndexToEntityIndex[j + 1] = vj;

                    T t = components[j];
                    components[j] = components[j + 1];
                    components[j + 1] = t;

                    // keep property data inline with components
                    swap(declaredProperties, j + 1, j);
                    swap(decoratedProperties, j + 1, j);
                } else {
                    // reached proper point in sorted sublist
                    break;
                }
            }
        }
    }

    private void swap(List<? extends PropertyStore<?>> store, int a, int b) {
        for (int i = 0; i < store.size(); i++) {
            store.get(i).swap(a, b);
        }
    }

    /**
     * <p/>
     * Compact the data of this ComponentRepository to account for removals and additions
     * to the index. This will ensure that all active componentDatas are packed into the
     * underlying arrays, and that they will be accessed in the same order as iterating
     * over the entities directly.
     * <p/>
     * The map from old to new entity index must be used to properly update the component
     * index's data so that the system is kept in sync.
     *
     * @param entityOldToNewMap A map from old entity index to new index
     * @param numEntities       The number of entities that are in the system
     */
    public void compact(int[] entityOldToNewMap, int numEntities) {
        // Remove all WeakPropertyStores that no longer have a property
        Iterator<DecoratedPropertyStore<?>> it = decoratedProperties.iterator();
        while (it.hasNext()) {
            if (it.next().getProperty() == null) {
                it.remove();
            }
        }

        // Sort the canonical components array to order them by their entity, which
        // also keeps the property data valid
        sort();

        // Repair the componentToEntityIndex and the component.index values
        componentInsert = 1;
        for (int i = 1; i < components.length; i++) {
            if (components[i] != null) {
                componentIndexToEntityIndex[i] = entityOldToNewMap[componentIndexToEntityIndex[i]];
                ((AbstractComponent<T>) components[i]).setIndex(i);
                componentInsert = i + 1;
            } else {
                // we can terminate now since all future components should be null
                // since we've sorted it that way
                break;
            }
        }

        // Possibly compact the component data
        if (componentInsert < .6 * components.length) {
            int newSize = (int) (1.2 * componentInsert) + 1;
            components = Arrays.copyOf(components, newSize);
            componentIndexToEntityIndex = Arrays
                    .copyOf(componentIndexToEntityIndex, newSize);
            resizePropertyStores(declaredProperties, newSize);
            resizePropertyStores(decoratedProperties, newSize);
        }

        // Repair entityIndexToComponentRepository - and possible shrink the index
        // based on the number of packed entities
        if (numEntities < .6 * entityIndexToComponentRepository.length) {
            entityIndexToComponentRepository = new int[(int) (1.2 * numEntities) + 1];
        } else {
            Arrays.fill(entityIndexToComponentRepository, 0);
        }

        for (int i = 1; i < componentInsert; i++) {
            entityIndexToComponentRepository[componentIndexToEntityIndex[i]] = i;
        }
    }

    /**
     * Decorate the type information of this ComponentRepository to add a property created
     * by the given factory. The returned property will have default data assigned for
     * each current Component in the index, and will have the default value assigned for
     * each new Component. Decorators can then access the returned property to manipulate
     * the decorated component data.
     *
     * @param <P>     The type of property created
     * @param factory The factory that will create a unique Property instance associated
     *                with the decorated property and this index
     *
     * @return The property decorated onto the type of the index
     */
    public <P extends Property> P decorate(PropertyFactory<P> factory) {
        int size = (declaredProperties.isEmpty() ? componentInsert
                                                 : declaredProperties.get(0).property
                            .getCapacity());
        P prop = factory.create();
        DecoratedPropertyStore<P> pstore = new DecoratedPropertyStore<>(factory, prop);

        // Set values from factory to all component slots
        prop.setCapacity(size);
        for (int i = 1; i < size; i++) {
            pstore.setDefaultValue(i);
        }

        decoratedProperties.add(pstore);
        return prop;
    }

    /*
     * Type wrapping a key, property, and factory, as well as an auxiliary data
     * store for compaction.
     */
    private static abstract class PropertyStore<P extends Property> {
        final PropertyFactory<P> creator;

        PropertyStore(PropertyFactory<P> creator) {
            this.creator = creator;
        }

        void setDefaultValue(int index) {
            P prop = getProperty();
            if (prop != null) {
                creator.setDefaultValue(prop, index);
            }
        }

        void resize(int size) {
            P property = getProperty();
            if (property != null) {
                property.setCapacity(size);
            }
        }

        void swap(int a, int b) {
            P property = getProperty();
            if (property != null) {
                property.swap(a, b);
            }
        }

        abstract P getProperty();
    }

    private static class DeclaredPropertyStore<P extends Property>
            extends PropertyStore<P>

    {
        final String key;
        final P property;

        public DeclaredPropertyStore(PropertyFactory<P> creator, String key) {
            super(creator);
            this.key = key;
            property = creator.create();
        }

        @Override
        P getProperty() {
            return property;
        }
    }

    private static class DecoratedPropertyStore<P extends Property>
            extends PropertyStore<P> {
        final WeakReference<P> property;

        public DecoratedPropertyStore(PropertyFactory<P> creator, P property) {
            super(creator);
            this.property = new WeakReference<>(property);
        }

        @Override
        P getProperty() {
            return property.get();
        }
    }
}
