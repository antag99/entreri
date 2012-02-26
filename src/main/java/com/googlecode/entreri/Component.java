package com.googlecode.entreri;

import com.googlecode.entreri.property.IndexedDataStore;
import com.googlecode.entreri.property.Property;

/**
 * <p>
 * ComponentData represents a grouping of reusable and related states that are added
 * to an {@link Entity}. Components are intended to be data storage objects, so
 * their definition should not contain methods for processing or updating (that
 * is the responsibility of a {@link Controller}). Some Components may be
 * defined with an {@link InitParams} annotation, which defines their required
 * arguments when adding a new component to an Entity.
 * </p>
 * <p>
 * The behavior or purpose of a ComponentData should be well defined, including its
 * behavior with respect to other Components attached to the same Entity. It may
 * be that to function correctly or--more likely--usefully, related Components
 * will have to be used as well. An example of this might be a transform
 * component and a shape component for rendering.
 * </p>
 * <p>
 * Each ComponentData class gets a {@link TypeId}, which can be looked up with
 * {@link #getTypedId(Class)}, passing in the desired class type. Because the
 * entity-component design pattern does not follow common object-oriented
 * principles, certain rules are followed when handling ComponentData types in a
 * class hierarchy:
 * <ol>
 * <li>Any abstract type extending ComponentData cannot get a TypeId</li>
 * <li>All concrete classes extending ComponentData get separate TypedIds, even if
 * they extend from the same intermediate classes beneath ComponentData.</li>
 * <li>All intermediate classes in a ComponentData type's hierarchy must be abstract
 * or runtime exceptions will be thrown.</li>
 * </ol>
 * As an example, an abstract component could be Light, with concrete subclasses
 * SpotLight and DirectionLight. SpotLight and DirectionLight would be separate
 * component types as determined by TypeId. Light would not have any TypeId
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
 * ComponentData instances are tied to an index into the IndexedDataStores used by
 * their properties. The index can be fetched by calling {@link #getIndex()}. An
 * instance of ComponentData may have its index changed, effectively changing it to
 * a different "instance". This is most common when using the fast iterators.
 * Because of this, reference equality may not work, instead you should rely on
 * {@link #equals(Object)}.
 * </p>
 * <p>
 * Compone
 * 
 * @author Michael Ludwig
 */
public final class Component<T extends ComponentData<T>> {
    private final ComponentIndex<T> owner;
    
    private int index;
    private int version;
    
    Component(ComponentIndex<T> owner, int index) {
        this.owner = owner;
        this.index = index;
        this.version = 0;
    }
    
    public T getData() {
        T data = getEntitySystem().createDataInstance(getTypeId());
        if (data.set(this))
            return data;
        else
            return null; 
    }
    
    public boolean isLive() {
        return index != 0;
    }
    
    public boolean isEnabled() {
        // if isLive() returns false, index references the 0th index, which
        // just contains garbage
        return owner.isEnabled(index);
    }
    
    public void setEnabled(boolean enable) {
        // if isLive() returns false, index references the 0th index, which
        // just contains garbage so this setter is safe
        owner.setEnabled(index, enable);
    }
    
    public Entity getEntity() {
        // if isLive() returns false, then the entity index will also be 0,
        // so getEntityByIndex() returns null, which is expected
        int entityIndex = owner.getEntityIndex(index);
        return owner.getEntitySystem().getEntityByIndex(entityIndex);
    }
    
    public EntitySystem getEntitySystem() {
        return owner.getEntitySystem();
    }
    
    public TypeId<T> getTypeId() {
        return owner.getTypeId();
    }
    
    void setIndex(int index) {
        this.index = index;
        version++;
    }
    
    int getIndex() {
        return index;
    }
    
    int getVersion() {
        return version;
    }
}
