package com.entreri.property;

/**
 * <p>
 * Property represents a generic field or property of a Component definition.
 * It's interface allows it's values and underlying data store to be packed
 * together with the corresponding property instances of all the other
 * Components of the same type in an EntitySystem.
 * </p>
 * <p>
 * This is an approach to mapped-objects where Components can be mapped onto
 * primitive arrays so that iteration sees optimal cache locality. As an
 * example, there could be two instances of type A, with properties a and b. The
 * two 'a' properties would share the same data store, and the two 'b'
 * properties would share a separate store.
 * </p>
 * <p>
 * Property instances are carefully managed by an EntitySystem. There is ever
 * only one property instance per defined property in a component type for a
 * system. Every component of that type uses their index into the property's
 * IndexedDataStore to access their data. This helps keep memory usage low and
 * simplifies system maintenance. The Property instances are created by using
 * the {@link Parameters} annotation on the declared fields in a type, and
 * reflection is used to assign them to a Component instance.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Property {
    /**
     * <p>
     * Return the IndexedDataStore holding this property's values. The data
     * store may also hold other property values if the owning Component is in
     * an EntitySystem with many other components of the same type.
     * </p>
     * <p>
     * This should not be used by Component implementations, and manipulating
     * the IndexedDataStore outside of the EntitySystem code could cause
     * unexpected behavior. Instead Property implementations should expose other
     * ways to access their data; as an example see
     * {@link FloatProperty#getIndexedData()}.
     * </p>
     * 
     * @return The current IndexedDataStore used by the property
     */
    public IndexedDataStore getDataStore();

    /**
     * <p>
     * Assign a new IndexedDataStore to this Property. If the old values for
     * this Property were not copied out of its previous IndexedDataStore into
     * the new one, this assignment will change the apparent value of this
     * property.
     * </p>
     * <p>
     * This should only be called internally by the EntitySystem. Calling it
     * within a Component implementation or otherwise will result in undefined
     * consequences.
     * </p>
     * <p>
     * It can be assumed that the new store is not null.
     * </p>
     * 
     * @param store The new IndexedDataStore
     */
    public void setDataStore(IndexedDataStore store);
}
