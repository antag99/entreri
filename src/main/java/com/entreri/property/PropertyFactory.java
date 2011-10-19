package com.entreri.property;

/**
 * <p>
 * A PropertyFactory is a simple factory that can be used to create Property
 * instances if the Property requires a more complicated constructor than can be
 * described by the {@link Parameters} annotation on a Property field.
 * Additionally, it is used when decorating a Component type in an EntitySystem
 * to ensure that each decoration event gets a unique property instance.
 * </p>
 * <p>
 * To be used with the {@link Factory} when defining a Component type, the
 * PropertyFactory must have an accessible, no-argument constructor.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The Property type created
 */
public interface PropertyFactory<T extends Property> {
    /**
     * Return a new Property instance. This must be a new instance that has not
     * been returned previously or the entity framework will have undefined
     * behavior.
     * 
     * @return A new Property of type T
     */
    public T create();
}
