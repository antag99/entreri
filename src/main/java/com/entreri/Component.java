/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig (lhkbob@gmail.com)
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
package com.entreri;

import java.util.concurrent.ConcurrentHashMap;

import com.entreri.property.IndexedDataStore;
import com.entreri.property.Property;

/**
 * <p>
 * Component represents a grouping of reusable and related states that are added
 * to an {@link Entity}. Components are intended to be data storage objects, so
 * their definition should not contain methods for processing or updating (that
 * is the responsibility of a {@link Controller}).
 * </p>
 * <p>
 * The behavior or purpose of a Component should be well defined, including its
 * behavior with respect to other Components attached to the same Entity. It may
 * be that to function correctly or--more likely--usefully, related Components
 * will have to be used as well. An example of this might be a transform
 * component and a shape component for rendering.
 * </p>
 * <p>
 * Each Component class gets a {@link TypedId}, which can be looked up with
 * {@link #getTypedId(Class)}, passing in the desired class type. Because the
 * entity-component design pattern does not follow common object-oriented
 * principles, certain rules are followed when handling Component types in a
 * class hierarchy:
 * <ol>
 * <li>Any abstract type extending Component cannot get a TypedId</li>
 * <li>All concrete classes extending Component get separate TypedIds, even if
 * they extend from the same intermediate classes beneath Component.</li>
 * <li>All intermediate classes in a Component type's hierarchy must be abstract
 * or runtime exceptions will be thrown.</li>
 * </ol>
 * As an example, an abstract component could be Light, with concrete subclasses
 * SpotLight and DirectionLight. SpotLight and DirectionLight would be separate
 * component types as determined by TypedId. Light would not have any TypedId
 * and only serves to consolidate property definition among related component
 * types.
 * </p>
 * <p>
 * Implementations of Components must follow certain rules with respect to their
 * declared fields. For performance reasons, an EntitySystem packs all
 * components of the same type into the same region of memory using the
 * {@link Property} and {@link IndexedDataStore} API. To ensure that Components
 * behave correctly, a type can only declare private or protected Property
 * fields. These fields should be considered "final" from the Components point
 * of view and will be assigned by the EntitySystem. The can be declared final
 * but any assigned value will be overwritten.
 * </p>
 * <p>
 * They can declare any methods they wish to expose the data these properties
 * represent. It is strongly recommended to not expose the Property objects
 * themselves. See {@link #getTypedId(Class)} for the complete contract.
 * </p>
 * <p>
 * Component instances are tied to an index into the IndexedDataStores used by
 * their properties. The index can be fetched by calling {@link #getIndex()}. An
 * instance of Component may have its index changed, effectively changing it to
 * a different "instance". This is most common when using the fast iterators.
 * Because of this, reference equality may not work, instead you should rely on
 * {@link #equals(Object)}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Component {
    // Use a ConcurrentHashMap to perform reads. It is still synchronized completely to do
    // an insert to make sure a type doesn't try to use two different id values.
    private static final ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>> typeMap 
        = new ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>>();
    private static final ConcurrentHashMap<TypedId<? extends Component>, ComponentBuilder<?>> builderMap
        = new ConcurrentHashMap<TypedId<? extends Component>, ComponentBuilder<?>>();
    
    private static int idSeq = 0;
    
    /**
     * <var>index</var> is a sliding component index into the indexed data store
     * for each property of the component. It can be mutated by the EntitySystem
     * to effectively change the Component instance's values to another
     * component in the system.
     */
    int index;

    final ComponentIndex<?> owner;
    private final TypedId<? extends Component> typedId;

    /**
     * <p>
     * Create a new Component instance that has its property data managed by the
     * given EntitySystem. Multiple Component instances may represent the same
     * "component" if their index's are the same.
     * </p>
     * <p>
     * Subclasses must call this constructor with the arguments as passed-in and
     * must not change them. Abstract subclasses can add additional arguments,
     * but concrete subclasses must have the same constructor signatures except
     * that it is private.
     * </p>
     * 
     * @param system The owning EntitySystem of the Component
     * @param index The initial index of this Component
     * @throws NullPointerException if system is null
     * @throws IllegalArgumentException if index is less than 0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Component(EntitySystem system, int index) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        if (index < 0)
            throw new IllegalArgumentException("Index must be at least 0: " + index);
        TypedId raw = getTypedId(getClass());

        this.owner = system.getIndex(raw);
        this.index = index;
        typedId = raw;
    }

    /**
     * Called when the EntitySystem creates a new Component and has properly
     * configured its declared properties. This is only called when the
     * Component is being added to an Entity. This is not called when a new
     * component instance is created for the purposes of a fast iterator
     * (because it's just acting as a shell in that case).
     */
    protected void init() {
        // do nothing by default
    }
    
    /**
     * <p>
     * Return the unique TypedId associated with this Component's class type.
     * All Components of the same class will return this id, too.
     * </p>
     * <p>
     * It is recommended that implementations override this method to use the
     * proper return type. Component does not perform this cast to avoid a
     * parameterizing Component. Do not change the actual returned instance,
     * though.
     * </p>
     * 
     * @return The TypedId of this Component
     */
    public TypedId<? extends Component> getTypedId() {
        return typedId;
    }

    /**
     * Get the Entity that owns this Component. The Entity will still be part of
     * an EntitySystem, and the component can be iterated over via
     * {@link EntitySystem#iterator(TypedId)}. If a Component is removed from an
     * Entity (or the Entity is removed from the system), this will return null.
     * 
     * @return The owning Entity
     * @throws IndexOutOfBoundsException if the Component has been removed from
     *             an Entity, or if its owning Entity has been removed from its
     *             EntitySystem.
     */
    public final Entity getEntity() {
        int entityIndex = owner.getEntityIndex(index);
        return owner.getEntitySystem().getEntityByIndex(entityIndex);
    }

    /**
     * Return the index of this Component within the IndexedDataStores that back
     * the defined properties of a Component. A Component instance may have its
     * index change if it is being used to slide over the component data (e.g.
     * in a fast iterator).
     * 
     * @return The index of the component used to access its IndexedDataStores.
     */
    public final int getIndex() {
        return index;
    }
    
    @Override
    public int hashCode() {
        return index;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Component))
            return false;
        Component c = (Component) o;
        if (c.owner == owner && c.typedId == typedId) {
            // We can't use index because a canonical component might have it change,
            // instead use its owning entity
            int tei = owner.getEntitySystem().getEntityId(owner.getEntityIndex(index));
            int cei = c.owner.getEntitySystem().getEntityId(c.owner.getEntityIndex(c.index));
            return tei == cei;
        } else {
            // type and owner don't match
            return false;
        }
    }

    /**
     * <p>
     * Return the unique TypedId instance for the given <tt>type</tt>. If a
     * TypedId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypedId is stored for
     * later, so that subsequent calls to {@link #getTypedId(Class)} with
     * <tt>type</tt> will return the same instance.
     * {@link Component#Component()} implicitly calls this method when a
     * Component is created.
     * </p>
     * <p>
     * This method also performs runtime checks to ensure the validity of the
     * Component type definition. The following rules must be upheld or an
     * {@link IllegalComponentDefinitionException} is thrown.
     * <ul>
     * <li>If the class extends from a class other than Component, that class
     * must be a subclass of Component and be declared abstract. Additional
     * rules might affect these parent classes.</li>
     * <li>A concrete Component type must have only one constructor; it must be
     * private and with arguments: EntitySystem, int. Abstract Component types
     * do not have this restriction.</li>
     * <li>Any non-static fields defined in a Component (abstract or concrete)
     * must implement Property and be declared private or protected.</li>
     * </ul>
     * Additionally, abstract Component types cannot have a TypedId assigned to
     * them.
     * </p>
     * 
     * @param <T> The Component class type
     * @param type The Class whose TypedId is fetched, which must be a subclass
     *            of Component
     * @return A unique TypedId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             Component, or if it is abstract
     * @throws IllegalComponentDefinitionException if the type does not follow
     *             the definition rules described above
     * @throws SecurityException if the reflection needed to create and analyze
     *             the Component fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> TypedId<T> getTypedId(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        
        // Do a look up first without locking to avoid the synchronized lock and expensive
        // error checking.  If we found one, we know it passed validation the first time, otherwise
        // we'll validate it before creating a new TypedId.
        TypedId<T> id = (TypedId<T>) typeMap.get(type);
        if (id != null)
            return id; // Found an existing id
        
        // Create a ComponentBuilder for the type - theoretically we could double-up
        // on ComponentBuilder creation if the same type is requested, but that has no
        // adverse consequences. The first builder will get stored for later
        ComponentBuilder<T> builder = new ComponentBuilder<T>(type);
        
        synchronized(typeMap) {
            // Must create a new id, we lock completely to prevent concurrent getTypedId() on the
            // same type using two different ids.  One would get overridden and its returned TypedId
            // would be invalid.
            // - Double check, though, before creating a new id
            id = (TypedId<T>) typeMap.get(type);
            if (id != null)
                return id; // Someone else put in the type after we checked but before we locked
            
            id = new TypedId<T>(type, idSeq++);
            typeMap.put(type, id);
            builderMap.put(id, builder);
            return id;
        }
    }
    
    @SuppressWarnings("unchecked")
    static <T extends Component> ComponentBuilder<T> getBuilder(TypedId<T> id) {
        // If they have the TypedId instance, then we already have created the builder
        return (ComponentBuilder<T>) builderMap.get(id);
    }
}
