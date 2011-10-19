package com.entreri;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.entreri.property.FloatProperty;
import com.entreri.property.ObjectProperty;
import com.entreri.property.Property;
import com.entreri.property.PropertyFactory;

/**
 * <p>
 * EntitySystem is the main container for the entities within a logical system
 * such as a game or physics world. It contains all entities needed for
 * processing the scene or data. Entities are created with {@link #addEntity()}
 * or {@link #addEntity(Entity)}. They can be removed (and effectively
 * destroyed) with {@link #removeEntity(Entity)} or from the remove() methods of
 * the various iterators over the system data.
 * </p>
 * <p>
 * After an Entity is created, Components can be added to it to store
 * domain-specific data and control its behaviors. This depends on the Component
 * implementations and Controllers used to process your data.
 * </p>
 * <p>
 * The {@link ControllerManager} of an EntitySystem can be used to register
 * Controllers that will process the entities within an entity system in an
 * organized fashion. Generally, the processing of all controllers through their
 * different phases constitutes a complete "frame".
 * </p>
 * <p>
 * When Entities are created by an EntitySystem, the created instance is
 * assigned an ID which represents its true identity. Certain iterators in the
 * system may create a single Entity object which slides over the underlying
 * entity data for performance purposes. With each iteration, its ID changes
 * even though the reference does not. This is same way that {@link Component
 * Components} are stored and treated by the EntitySystem.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class EntitySystem {
    private ComponentIndex<?>[] componentIndices;
    private int[] entityIds; // binary search provides index
    
    private Entity[] entities;
    
    private int entityInsert;
    private int entityIdSeq;
    
    private boolean requireReIndex;
    
    private final ControllerManager manager;

    /**
     * Create a new EntitySystem that has no entities added.
     */
    public EntitySystem() {
        manager = new ControllerManager(this);
        
        entities = new Entity[1];
        entityIds = new int[1];
        
        componentIndices = new ComponentIndex[0];
        
        entityIdSeq = 1; // start at 1, id 0 is reserved for index = 0 
        entityInsert = 1;
    }

    /**
     * Return the ControllerManager for this EntitySystem that can be used to
     * organize processing of the system using {@link Controller}
     * implementations.
     * 
     * @return The ControllerManager for this system
     */
    public ControllerManager getControllerManager() {
        return manager;
    }
    
    /**
     * Return an iterator over all of the entities within the system. The
     * returned iterator's remove() method will remove the entity from the
     * system. The returned entities are the "canonical" entities and can be
     * safely used stored outside of the iterator.
     * 
     * @return An iterator over the entities of the system
     */
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    /**
     * <p>
     * Return a "fast" iterator over all the entities within the system. To
     * avoid potential cache misses, a single Entity object is created and
     * slides over the entity data stored within the system. If entities do not
     * need to be held onto after iteration, this is faster than
     * {@link #iterator()}.
     * </p>
     * <p>
     * The returned iterator's remove() method will remove the entity from the
     * system (where entity is determined by the entity's id and not Entity
     * instance). The returned iterator will return the same Entity object with
     * every call to next(), but its index into the system will be updated every
     * iteration.
     * </p>
     * 
     * @return A fast iterator over the entities of the system
     */
    public Iterator<Entity> fastIterator() {
        return new FastEntityIterator();
    }

    /**
     * <p>
     * Return an iterator over the components of type T that are in this system.
     * The returned iterator supports the remove() operation, and will remove
     * the component from its owning entity. The entity attached to the
     * component can be found with {@link Component#getEntity()}.
     * </p>
     * <p>
     * The iterator returns the canonical Component instance for each component
     * of the type in the system. This is the same instance that was returned by
     * {@link Entity#add(TypedId)} and is safe to access and store after
     * iteration has completed.
     * </p>
     * 
     * @param <T> The component type that is iterated over
     * @param id The TypedId of the iterated component
     * @return An iterator over all Components of type T in the system
     * @throws NullPointerException if id is null
     */
    public <T extends Component> Iterator<T> iterator(TypedId<T> id) {
        return getIndex(id).iterator();
    }

    /**
     * As {@link #iterator(TypedId)} but the iterator will reuse a single
     * instance of Component. Every call to next() will update the Component's
     * index within the system. Using a fast iterator helps cache performance,
     * but cannot be used if the component must be stored for later processing.
     * 
     * @param <T> The component type that is iterated over
     * @param id The TypedId of the iterated component
     * @return A fast iterator over all Components of type T in the system
     * @throws NullPointerException if id is null
     */
    public <T extends Component> Iterator<T> fastIterator(TypedId<T> id) {
        return getIndex(id).fastIterator();
    }

    /**
     * <p>
     * Return an iterator over all entities within the system that have the
     * given component types attached to them. Entities returned by the iterator
     * will have all requested components; if even one desired component is not
     * present on an entity, it is not included.
     * </p>
     * <p>
     * For performance reasons, the "entity" is exposed as an
     * IndexedComponentMap. The components can be queried using the index of
     * their type within <tt>ids</tt>. Alternatively, it can be queried by
     * TypedId.
     * </p>
     * <p>
     * The returned iterator will always return the same IndexedComponentMap,
     * although each call to next() will update the Components returned by the
     * map. The accessed components are canonical components in the same way
     * that {@link #iterator(TypedId)}'s components are. The remove() operation
     * is not supported.
     * </p>
     * 
     * @param ids The ordered set of ids that constrain the returned entities
     * @return An iterator that efficiently exposes all entities and their
     *         components which satisfy the given type constraint
     * @throws NullPointerException if ids is null or contains null elements
     * @throws IllegalArgumentException if ids is empty
     */
    public Iterator<IndexedComponentMap> iterator(TypedId<?>... ids) { 
        return bulkIterator(false, ids);
    }

    /**
     * As {@link #iterator(TypedId...)} except that the Components returned by
     * the IndexedComponentMap are not canonical. Like
     * {@link #fastIterator(TypedId)}, new Component instances are reused by the
     * iterator to slide over the entity components which pass the constraint.
     * 
     * @param ids The ordered set of ids that constrain the returned entities
     * @return A fast iterator that efficiently exposes all entities and their
     *         components which satisfy the given type constraint throws
     *         NullPointerException if ids is null or contains null elements
     * @throws IllegalArgumentException if ids is empty
     */
    public Iterator<IndexedComponentMap> fastIterator(TypedId<?>... ids) {
        return bulkIterator(true, ids);
    }
    
    /*
     * Internal method that prepares the bulk iterators by finding the type
     * with the smallest number of entities and using it as the primary iterator.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Iterator<IndexedComponentMap> bulkIterator(boolean fast, TypedId<?>... ids) {
        if (ids == null)
            throw new NullPointerException("TypedIds cannot be null");
        if (ids.length < 1)
            throw new IllegalArgumentException("Must have at least one TypedId");
        
        TypedId[] rawIds = ids;
        
        int minIndex = -1;
        int minSize = Integer.MAX_VALUE;
        
        ComponentIndex index;
        ComponentIndex[] rawIndices = new ComponentIndex[ids.length];
        
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == null)
                throw new NullPointerException("TypedId in id set cannot be null");
            
            index = getIndex(rawIds[i]);
            if (index.getSizeEstimate() < minSize) {
                minIndex = i;
                minSize = index.getSizeEstimate();
            }
            
            rawIndices[i] = index;
        }

        if (fast)
            return new FastBulkComponentIterator(rawIndices, minIndex);
        else
            return new BulkComponentIterator(rawIndices, minIndex);
    }

    /**
     * <p>
     * Compact the entity and component data so that iteration is more
     * efficient. In the life time of an entity system, entities and components
     * are added and removed, possibly causing the list of components for a
     * given type to be in a different order than the list of entities. This is
     * due to implementation details needed to make additions and removals
     * constant time.
     * </p>
     * <p>
     * Invoking {@link #compact()} after a large number of additions or removals
     * to the system is a good idea. Alternatively, invoking it every few frames
     * in a game works as well. An entity system that has no additions or
     * removals of entities (or their components) gains no benefit from
     * compacting, except potentially for freeing excess memory.
     * </p>
     * <p>
     * Compacting is not overly fast or slow, so it should not cause noticeably
     * drops in frame rate. As an example, on a test system with 20,000 entities
     * compact() took ~2ms on an Intel i5 processor. Of course, mileage may
     * very.
     * </p>
     */
    public void compact() {
        // Pack the data
        int startRemove = -1;
        for (int i = 1; i < entityInsert; i++) {
            if (entityIds[i] == 0) {
                // found an entity to remove
                if (startRemove < 0)
                    startRemove = i;
            } else {
                // found an entity to preserve
                if (startRemove > 0) {
                    // we have a gap from [startRemove, i - 1] that can be compacted
                    System.arraycopy(entityIds, i, entityIds, startRemove, entityInsert - i);
                    System.arraycopy(entities, i, entities, startRemove, entityInsert - i);
                    
                    // update entityInsert
                    entityInsert = entityInsert - i + startRemove;
                    
                    // now reset loop
                    i = startRemove;
                    startRemove = -1;
                }
            }
        }
        
        // Build a map from oldIndex to newIndex and repair entity's index
        int[] oldToNew = new int[entityIds.length];
        for (int i = 1; i < entityInsert; i++) {
                oldToNew[entities[i].index] = i;
                entities[i].index = i;
        }
        
        if (entityInsert < .6f * entities.length) {
            // reduce the size of the entities/ids arrays
            int newSize = (int) (1.2f * entityInsert) + 1;
            entities = Arrays.copyOf(entities, newSize);
            entityIds = Arrays.copyOf(entityIds, newSize);
        }
        
        // Now index and update all ComponentIndices
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[i] != null)
                componentIndices[i].compact(oldToNew, entityInsert);
        }
    }

    /**
     * <p>
     * Retrieve the canonical Entity instance associated with the given integer
     * id. This Entity instance can be safely held for later purposes, just like
     * the Entity instances form {@link #iterator()}. All entity ids are at
     * least 1, so any value less than 1 will return null.
     * </p>
     * <p>
     * This performs a binary search over the list of entities in the system,
     * which is usually very fast. However, if the list has not been compacted
     * and is not ordered correctly, the binary search may fail at which point a
     * linear search is used.
     * </p>
     * 
     * @param entityId The entity's id
     * @return The Entity in the system with the given ID, or null if it does
     *         not exist
     */
    public Entity getEntity(int entityId) {
        // opt-out of search quickly
        if (entityId <= 0)
            return null;
        
        if (requireReIndex)
            compact();
        
        int index = Arrays.binarySearch(entityIds, 1, entityInsert, entityId);
        if (index >= 0) {
            // Double check that we're returning the expected Entity,
            // (this might be overworking it, though)
            if (entities[index] != null && entityIds[index] == entityId)
                return entities[index];
        }
        
        // Could not find it, this could be because it's not there, or because
        // the list has not been compacted
        for (int i = 1; i < entityInsert; i++) {
            if (entityIds[i] == entityId)
                return entities[i];
        }
        
        return null;
    }

    /**
     * Add a new Entity to this EntitySystem. The created Entity will not have
     * any attached Components. The returned instance will be the canonical
     * Entity object tied to its {@link Entity#getId() ID}. You can create a new
     * entity from a template by calling {@link #addEntity(Entity)}.
     * 
     * @return A new Entity without any components in the system
     */
    public Entity addEntity() {
        return addEntity(null);
    }

    /**
     * <p>
     * Add a new Entity to this EntitySystem. If <tt>template</tt> is not null,
     * the components attached to the template will have their state cloned onto
     * the new entity. If the template's components store references, the
     * references are copied, e.g {@link ObjectProperty}; if the component
     * properties store values the values are copied, e.g. {@link FloatProperty}
     * .
     * </p>
     * <p>
     * The template does not need to have been created by this system, it must
     * only still be a valid entity in some system. Specifying a null template
     * makes this behave identically to {@link #addEntity()}. If the template
     * has a component whose type is not registered with this system, it is
     * automatically registered.
     * </p>
     * 
     * @param template The template to clone
     * @return A new Entity in the system with the same component state as the
     *         template
     */
    public Entity addEntity(Entity template) {
        int entityIndex = entityInsert++;
        if (entityIndex >= entityIds.length) {
            entityIds = Arrays.copyOf(entityIds, (int) (entityIndex * 1.5f) + 1);
            entities = Arrays.copyOf(entities, (int) (entityIndex * 1.5f) + 1);
        }
        
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[i] != null)
                componentIndices[i].expandEntityIndex(entityIndex + 1);
        }
        
        Entity newEntity = new Entity(this, entityIndex);
        entities[entityIndex] = newEntity;
        entityIds[entityIndex] = entityIdSeq++;
        
        if (template != null) {
            for (Component c: template) {
                addFromTemplate(entityIndex, c.getTypedId(), c);
            }
        }

        return newEntity;
    }

    /**
     * Remove the given entity from this system. The entity and its attached
     * components are removed from the system. The canonical instances
     * associated with each will be updated to reference null regions of data
     * and should not be used.
     * 
     * @param e The entity to remove (this does not need to be the canonical
     *            instance, just meet equals() equality).
     * @throws NullPointerException if e is null
     * @throws IllegalArgumentException if the entity is not owned by this
     *             system
     */
    public void removeEntity(Entity e) {
        if (e == null)
            throw new NullPointerException("Cannot remove a null entity");
        if (e.getEntitySystem() != this)
            throw new IllegalArgumentException("Entity is not from this EntitySystem");
        if (e.index == 0)
            throw new IllegalArgumentException("Entity has already been removed");
        
        removeEntity(e.index);
        // Fix e's index in case it was an entity from a fast iterator
        e.index = 0;
    }

    /**
     * <p>
     * Dynamically update the available properties of the given Component type
     * by adding a Property created by the given PropertyFactory. The property
     * will be managed by the system as if it was a declared property of the
     * component type.
     * </p>
     * <p>
     * All components, current and new, will initially have their starting
     * values for the decorated property equal the state of the property after
     * being created by the factory. The returned property can be accessed and
     * used by Controllers to add dynamic runtime data to statically defined
     * component types.
     * </p>
     * 
     * @param <P> The created property type
     * @param type The component type to mutate
     * @param factory The property factory that creates the decorating property
     * @return The property that has decorated the given component type
     * @throws NullPointerException if type or factory are null
     */
    public <P extends Property> P decorate(TypedId<? extends Component> type, PropertyFactory<P> factory) {
        ComponentIndex<?> index = getIndex(type);
        return index.decorate(factory);
    }

    /**
     * Remove the property, <tt>p</tt>, that was previously decorated onto the
     * given type by the method {@link #decorate(TypedId, PropertyFactory)}. If
     * the property has not been decorated onto that type or is no longer
     * decorated, this does nothing.
     * 
     * @param type The component type to undecorate
     * @param p The property to remove from the dynamic type definition
     * @throws NullPointerException if type is null
     */
    public void undecorate(TypedId<? extends Component> type, Property p) {
        ComponentIndex<?> index = getIndex(type);
        index.undecorate(p);
    }

    /**
     * Return the ComponentIndex associated with the given type. Fails if the
     * type is not registered
     * 
     * @param <T> The Component type
     * @param id The id for the component type
     * @return The ComponentIndex for the type
     */
    @SuppressWarnings("unchecked")
    <T extends Component> ComponentIndex<T> getIndex(TypedId<T> id) {
        int index = id.getId();
        if (index >= componentIndices.length) {
            // make sure it's the correct size
            componentIndices = Arrays.copyOf(componentIndices, index + 1);
        }
        
        ComponentIndex<T> i = (ComponentIndex<T>) componentIndices[index];
        if (i == null) {
            i = new ComponentIndex<T>(this, id);
            i.expandEntityIndex(entities.length);
            componentIndices[index] = i;
        }
        
        return i;
    }
    
    /**
     * @return Return an iterator over the registered component indices
     */
    Iterator<ComponentIndex<?>> iterateComponentIndices() {
        return new ComponentIndexIterator();
    }

    /**
     * Return the entity id associated with an index into the system's backing
     * store
     * 
     * @param entityIndex The index that the entity is stored at within the id
     *            array and component indices
     * @return The id
     */
    int getEntityId(int entityIndex) {
        return entityIds[entityIndex];
    }

    /**
     * Return the canonical Entity instance associated with the given index.
     * 
     * @param entityIndex The index that the entity is stored at within the
     *            entity array and component indicees
     * @return The canonical Entity instance for the index
     */
    Entity getEntityByIndex(int entityIndex) {
        return entities[entityIndex];
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends Component> void addFromTemplate(int entityIndex, TypedId typedId, Component c) {
        ComponentIndex index = getIndex(typedId);
        index.addComponent(entityIndex, c);
    }
    
    private boolean removeEntity(int index) {
        requireReIndex = true;
        
        // Remove all components from the entity
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[i] != null)
                componentIndices[i].removeComponent(index);
        }
        
        // clear out id and canonical entity
        Entity old = entities[index];
        entityIds[index] = 0;
        entities[index] = null;
        
        if (old != null) {
            // update its index
            old.index = 0;
            return true;
        } else
            return false;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class BulkComponentIterator implements Iterator<IndexedComponentMap> {
        private final ComponentIndex[] indices;
        private final int minIndex;
        private final Iterator<Component> minComponentIterator;
        private final Component[] result;
        
        private final IndexedComponentMap map;
        
        private boolean hasAdvanced;
        private boolean resultValid;
        
        public BulkComponentIterator(ComponentIndex[] indices, int minIndex) {
            this.indices = indices;
            this.minIndex = minIndex;
            
            minComponentIterator = indices[minIndex].iterator();
            result = new Component[indices.length];
            map = new IndexedComponentMap(result);
            
            hasAdvanced = false;
            resultValid = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!hasAdvanced)
                advance();
            return resultValid;
        }

        @Override
        public IndexedComponentMap next() {
            if (!hasNext())
                throw new NoSuchElementException();
            hasAdvanced = false;
            return map;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            Component c;
            boolean foundAll;
            int entityIndex;
            
            hasAdvanced = true;
            resultValid = false;
            while(minComponentIterator.hasNext()) {
                foundAll = true;
                c = minComponentIterator.next();
                entityIndex = indices[minIndex].getEntityIndex(c.index);
                
                result[minIndex] = c;
                
                // now look for every other component
                for (int i = 0; i < result.length; i++) {
                    if (i == minIndex)
                        continue;
                    
                    c = indices[i].getComponent(entityIndex);
                    if (c == null) {
                        foundAll = false;
                        break;
                    } else {
                        result[i] = c;
                    }
                }
                
                if (foundAll) {
                    resultValid = true;
                    break;
                }
            }
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class FastBulkComponentIterator implements Iterator<IndexedComponentMap> {
        private final ComponentIndex[] indices;
        private final int minIndex;
        private final Iterator<Component> minComponentIterator;
        private final Component[] result;
        private final IndexedComponentMap map;
        
        private boolean hasAdvanced;
        private boolean resultValid;
        
        public FastBulkComponentIterator(ComponentIndex[] indices, int minIndex) {
            this.indices = indices;
            this.minIndex = minIndex;
            
            minComponentIterator = indices[minIndex].fastIterator();
            result = new Component[indices.length];
            map = new IndexedComponentMap(result);
            
            hasAdvanced = false;
            resultValid = false;
            
            // now create local instances for the components
            for (int i = 0; i < indices.length; i++) {
                if (i == minIndex)
                    continue;
                result[i] = indices[i].newInstance(0);
            }
        }
        
        @Override
        public boolean hasNext() {
            if (!hasAdvanced)
                advance();
            return resultValid;
        }

        @Override
        public IndexedComponentMap next() {
            if (!hasNext())
                throw new NoSuchElementException();
            hasAdvanced = false;
            return map;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            Component c;
            boolean foundAll;
            int entityIndex;
            int ci;
            
            hasAdvanced = true;
            resultValid = false;
            while(minComponentIterator.hasNext()) {
                foundAll = true;
                c = minComponentIterator.next();
                entityIndex = indices[minIndex].getEntityIndex(c.index);
                
                // we use the fastIterator()'s returned instance for the min component,
                // so we have to assign it here
                result[minIndex] = c;
                
                // now look for every other component
                for (int i = 0; i < result.length; i++) {
                    if (i == minIndex)
                        continue;
                    
                    ci = indices[i].getComponentIndex(entityIndex);
                    if (ci == 0) {
                        foundAll = false;
                        break;
                    } else {
                        result[i].index = ci;
                    }
                }
                
                if (foundAll) {
                    resultValid = true;
                    break;
                }
            }
        }
    }
    
    private class ComponentIndexIterator implements Iterator<ComponentIndex<?>> {
        private int index;
        private boolean advanced;
        
        public ComponentIndexIterator() {
            index = -1;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < componentIndices.length;
        }

        @Override
        public ComponentIndex<?> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            return componentIndices[index];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            index++;
            while(index < componentIndices.length && componentIndices[index] == null) {
                index++;
            }
            advanced = true;
        }
    }
    
    private class EntityIterator implements Iterator<Entity> {
        private int index;
        private boolean advanced;
        
        public EntityIterator() {
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < entityInsert;
        }

        @Override
        public Entity next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            return entities[index];
        }

        @Override
        public void remove() {
            if (advanced || index == 0)
                throw new IllegalStateException("Must call next() before remove()");
            if (entities[index] == null)
                throw new IllegalStateException("Entity already removed");
            removeEntity(index);
        }
        
        private void advance() {
            index++; // always advance at least 1
            while(index < entities.length && entities[index] == null) {
                index++;
            }
            advanced = true;
        }
    }
    
    private class FastEntityIterator implements Iterator<Entity> {
        private final Entity instance;
        
        private int index;
        private boolean advanced;
        
        public FastEntityIterator() {
            instance = new Entity(EntitySystem.this, 0);
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < entityInsert;
        }

        @Override
        public Entity next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            instance.index = index;
            return instance;
        }

        @Override
        public void remove() {
            if (advanced || index == 0)
                throw new IllegalStateException("Must call next() before remove()");
            if (entityIds[index] == 0)
                throw new IllegalStateException("Component already removed");
            removeEntity(index);
        }
        
        private void advance() {
            // Check entityIds so we don't pull in an instance 
            // and we can just iterate along the int[] array. A 0 value implies that
            // the component does not have an attached entity, and has been removed
            
            index++; // always advance
            while(index < entityIds.length && 
                  entityIds[index] == 0) {
                index++;
            }
            advanced = true;
        }
    }
}
