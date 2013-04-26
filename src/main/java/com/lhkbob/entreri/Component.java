package com.lhkbob.entreri;

/**
 * <p/>
 * ComponentData is used to define types of components that can be added to entities. For
 * performance reasons, the identity of a component is represented by instances of {@link
 * com.lhkbob.entreri.Component}. ComponentData instances are used as views into the data of the components.
 * This allows multiple instances to have their data packed together in primitive arrays
 * or direct memory, allowing for significantly faster iteration.
 * <p/>
 * <p/>
 * Additionally, by using a single ComponentData instance during the iteration, there is
 * no need to follow the usual object references needed for each instance.
 * <p/>
 * <p/>
 * ComponentData's are defined like any other class, but they are intended to be created
 * and configured by a {@link ComponentDataFactory}. These factories may impose certain
 * restrictions or requirements in what constructors or fields are valid. ComponentData
 * implementations can define a default ComponentDataFactory type with the {@link
 * DefaultFactory} annotation. By default ComponentData implementations are created using
 * The {@link ReflectionComponentDataFactory}
 *
 * @param <T> The self-referencing type of the ComponentData
 *
 * @author Michael Ludwig
 */
public interface Component extends Owner, Ownable {
    /**
     * @return The EntitySystem that created this component
     */
    public EntitySystem getEntitySystem();

    /**
     * Get the entity that this component is attached to. If the component has been
     * removed from the entity, or is otherwise not live, this will return null.
     *
     * @return The owning entity, or null
     */
    public Entity getEntity();

    /**
     * @return True if the component is still attached to an entity in the entity system,
     *         or false if it or its entity has been removed
     */
    public boolean isAlive();

    /**
     * Get the underlying index of this component used to access its properties. In most
     * cases you can just use {@link ComponentData#getIndex()} because you'll have a
     * ComponentData instance on hand. However, it can be useful to use this method to
     * access decorated data to avoid setting the component data to a particular component
     * just to get its index.
     *
     * @return The current index of component
     */
    public int getIndex();

    /**
     * <p/>
     * Get the current version of the data accessed by this ComponentData. When data is
     * mutated by a ComponentData, implementations increment its associated component's
     * version so comparing a previously cached version number can be used to determine
     * when changes have been made.
     * <p/>
     * Additionally, for a given component type, versions will be unique. Thus it is
     * possible to identify when the components are replaced by new components as well.
     *
     * @return The current version, or a negative number if the data is invalid
     */
    public int getVersion();

    /**
     * Increment the version of the component accessed by this instance. It is recommended
     * for component data implementations to call this automatically from within their
     * exposed mutators, but if necessary it can be invoked manually as well.
     *
     * @see #getVersion()
     */
    public void updateVersion();
}
