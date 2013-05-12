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
package com.lhkbob.entreri;

import com.lhkbob.entreri.property.*;

import java.lang.ref.WeakReference;
import java.util.*;

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
    private final EntitySystem system;
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

    private final ObjectProperty<OwnerSupport> ownerDelegatesProperty;

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
    public ComponentRepository(EntitySystem system, Class<T> type) {
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

        List<PropertySpecification> spec = factory.getSpecification();

        declaredProperties = new ArrayList<DeclaredPropertyStore<?>>();
        decoratedProperties = new ArrayList<DecoratedPropertyStore<?>>(); // empty for now
        for (PropertySpecification p : spec) {
            DeclaredPropertyStore store = new DeclaredPropertyStore(p.getFactory(),
                                                                    p.getName());
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
        componentVersionProperty.set(-1, 0);
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
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * Given the index of a Component (e.g. {@link Component#getIndex()}, return the index
     * of an entity within the owning system. The returned entity index can be safely
     * passed to {@link EntitySystem#getEntityByIndex(int)}.
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
     * Given the index of an entity (e.g. {@link Entity#index}), return the index of the
     * attached component of this ComponentRepository's type. The returned component index
     * can be used in {@link #getComponent(int)} and related methods.
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
     * @return Estimated memory usage of this component repository
     */
    public long estimateMemory() {
        long total =
                estimateMemory(declaredProperties) + estimateMemory(decoratedProperties);

        // also add in an estimate for other structures used by
        // this repository
        total += 4 * entityIndexToComponentRepository.length;
        total += 4 * componentIndexToEntityIndex.length;

        // estimate each Component object as 4 bytes for a pointer,
        // 4 bytes for its index, 4 bytes for its repository reference
        total += 12 * components.length;

        return total;
    }

    private long estimateMemory(List<? extends PropertyStore<?>> properties) {
        long total = 0L;
        int ct = properties.size();
        for (int i = 0; i < ct; i++) {
            Property p = properties.get(i).getProperty();
            if (p != null) {
                total += p.getDataStore().memory();
            }
        }
        return total;
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
            componentVersionProperty.set(newVersion, componentIndex);
        }
    }

    /**
     * @param componentIndex The component index
     *
     * @return The OwnerSupport delegate for the component by the given index
     */
    public OwnerSupport getOwnerDelegate(int componentIndex) {
        return ownerDelegatesProperty.get(componentIndex);
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
        componentIdProperty.set(idSeq++, componentIndex);
        // same goes for assigning a new owner delegate
        ownerDelegatesProperty.set(new OwnerSupport(instance), componentIndex);

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
        componentIdProperty.set(0, componentIndex); // clear id

        return oldComponent != null;
    }

    /*
     * Update all component data in the list of properties. If possible the data
     * store in swap is reused.
     */
    private void update(List<? extends PropertyStore<?>> properties) {
        for (int i = 0; i < properties.size(); i++) {
            PropertyStore<?> store = properties.get(i);
            Property p = store.getProperty();
            if (p != null) {
                IndexedDataStore origStore = p.getDataStore();
                p.setDataStore(update(origStore, store.swap));
                store.swap = origStore;
            }
        }
    }

    /*
     * Update all component data in src to be in dst by shuffling it to match
     * current state of components array.
     */
    private IndexedDataStore update(IndexedDataStore src, IndexedDataStore dst) {
        int dstSize = components.length;

        if (dst == null || dst.size() < dstSize) {
            dst = src.create(dstSize);
        }

        int i;
        int lastIndex = -1;
        int copyIndexNew = -1;
        int copyIndexOld = -1;
        for (i = 1; i < componentInsert; i++) {
            if (components[i] == null) {
                // we've hit the end of existing componentDatas, so break
                break;
            }

            if (components[i].getIndex() != lastIndex + 1) {
                // we are not in a contiguous section
                if (copyIndexOld >= 0) {
                    // we have to copy over the last section
                    src.copy(copyIndexOld, (i - copyIndexNew), dst, copyIndexNew);
                }

                // set the copy indices
                copyIndexNew = i;
                copyIndexOld = components[i].getIndex();
            }
            lastIndex = components[i].getIndex();
        }

        if (copyIndexOld >= 0) {
            // final copy
            src.copy(copyIndexOld, (i - copyIndexNew), dst, copyIndexNew);
        }

        return dst;
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
        // First sort the canonical components array to order them by their entity
        Arrays.sort(components, 1, componentInsert, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                if (o1 != null && o2 != null) {
                    return componentIndexToEntityIndex[o1.getIndex()] -
                           componentIndexToEntityIndex[o2.getIndex()];
                } else if (o1 != null) {
                    return -1; // push null o2 to end of array
                } else if (o2 != null) {
                    return 1; // push null o1 to end of array
                } else {
                    return 0; // both null so they are "equal"
                }
            }
        });

        // Remove all WeakPropertyStores that no longer have a property
        Iterator<DecoratedPropertyStore<?>> it = decoratedProperties.iterator();
        while (it.hasNext()) {
            if (it.next().getProperty() == null) {
                it.remove();
            }
        }

        // Update all of the property stores to match up with the components new positions
        update(declaredProperties);
        update(decoratedProperties);

        // Repair the componentToEntityIndex and the component.index values
        componentInsert = 1;
        int[] newComponentRepository = new int[components.length];
        for (int i = 1; i < components.length; i++) {
            if (components[i] != null) {
                newComponentRepository[i] = entityOldToNewMap[componentIndexToEntityIndex[components[i]
                        .getIndex()]];
                ((AbstractComponent<T>) components[i]).setIndex(i);
                componentInsert = i + 1;
            }
        }
        componentIndexToEntityIndex = newComponentRepository;

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
                            .getDataStore().size());
        P prop = factory.create();
        DecoratedPropertyStore<P> pstore = new DecoratedPropertyStore<P>(factory, prop);

        // Set values from factory to all component slots
        IndexedDataStore newStore = prop.getDataStore().create(size);
        prop.setDataStore(newStore);
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
        IndexedDataStore swap; // may be null;

        PropertyStore(PropertyFactory<P> creator) {
            this.creator = creator;
            swap = null;
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
                IndexedDataStore oldStore = property.getDataStore();
                IndexedDataStore newStore = oldStore.create(size);
                oldStore.copy(0, Math.min(oldStore.size(), size), newStore, 0);
                property.setDataStore(newStore);
            }
        }

        abstract P getProperty();
    }

    private static class DeclaredPropertyStore<P extends Property>
            extends PropertyStore<P>

    {
        final Object key;
        final P property;

        public DeclaredPropertyStore(PropertyFactory<P> creator, Object key) {
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
            this.property = new WeakReference<P>(property);
        }

        @Override
        P getProperty() {
            return property.get();
        }
    }
}
