package com.googlecode.entreri;

import java.util.Map;

import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.PropertyFactory;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * ComponentDataFactory is a factory interface used to create instances of
 * ComponentData to provide a flexible means to instantiate and configure
 * ComponentData's for a specific type of component. For most purposes, the
 * default {@link ReflectionComponentDataFactory} will be sufficient, unless the
 * conventions enforced by it are too restrictive.
 * </p>
 * <p>
 * The main purpose of the ComponentDataFactory is to act as the glue between
 * the set of Properties representing all of the components in a system, and the
 * ComponentData instances used to efficiently access their values in a clean
 * manner.
 * </p>
 * <p>
 * ComponentDataFactory implementations must be thread safe, which should not be
 * difficult because their state is usually immutable.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The type of ComponentData created by the factory
 */
public interface ComponentDataFactory<T extends ComponentData<T>> {
    /**
     * <p>
     * Return the named PropertyFactories that can be used to create all
     * required Properties when configuring a new instance. The key of the map
     * should be the same key that is specified when
     * {@link #setProperty(ComponentData, String, Property)} is called.
     * </p>
     * <p>
     * An example of the keys might be the field names declared in the class.
     * </p>
     * 
     * @return The PropertyFactories created by this builder
     */
    public Map<String, PropertyFactory<?>> getPropertyFactories();

    /**
     * Construct a new instance of T that has not been configured. This means it
     * should not have any assigned properties, and to expect subsequent calls
     * to {@link #setProperty(ComponentData, String, Property)} to configure it.
     * 
     * @return A new instance
     */
    public T createInstance();

    /**
     * Inject the given property into the instance, where the property is
     * assumed to have been constructed by PropertyFactory from
     * {@link #getPropertyFactories()} that is stored by <tt>key</tt>.
     * 
     * @param instance The instance to configure
     * @param key The key to the creating PropertyFactory or source of the
     *            property
     * @param property The property instance to inject
     * @throws NullPointerException if any argument is null
     */
    public void setProperty(T instance, String key, Property property);
    
    // FIXME: we can generalize this to storing all data of a component, so 
    // someone could choose to use a system not tied to the property interface,
    // I'm not sure what exactly would need to change, since I'd want to still
    // support decorating of component types.
    // FIXME: is that really useful, too? They can already change the IndexedDataStore 
    // used for a lot of flexibility.  I will ponder on it.
}
