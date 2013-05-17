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

/**
 * <p/>
 * Component represents a grouping of reusable and related states that are added to an
 * {@link Entity}. An Entity's behavior is defined by the union of all components attached
 * to it, and the system's configured tasks that process the entities. Tasks must be
 * implemented and used in order to take advantage of a particular component
 * configuration.
 * <p/>
 * An entity can be considered as a collection of aspects represented by component
 * instances. An entity can have at most one instance of a component type at a time,
 * although over its lifetime the specific component instance may change.
 * <p/>
 * Logically a component definition is a set of named and typed properties, and a
 * method-based API to get and set the values of each property. Specific types of
 * component are defined by creating a sub-interface of Component. Using the {@link Named}
 * {@link SharedInstance}, and {@link com.lhkbob.entreri.property.Factory} annotations and
 * specific conventions the data properties of the component type are specified in the
 * sub-interface. A declaration model similar to the Java Bean model is used and is
 * outlined below:
 * <p/>
 * <ol> <li>Non-void, zero-argument methods starting with 'get', 'is', and 'has' declare a
 * property. The property type is inspected from the return type of the method. The
 * property name is the method name minus the 'get'/'is'/'has' prefix with its first
 * letter made lower-case. The {@link Named} annotation can be used to override the
 * name.</li> <li>Single-argument methods starting with 'set' are assumed to be a setter
 * corresponding to a property. The single parameter's type must equal the type the
 * getter. The {@link Named} annotation can be applied to either the setter or the
 * parameter to specify the property name.</li> <li>Multi-argument methods starting with
 * 'set' are assumed to be a setter that assigns values to multiple property, one for each
 * argument. Each argument must be annotated with {@link Named} to specify the property,
 * and the argument type must equal the type of the matching property.</li> <li>Setter
 * methods must return void or return the components type, in which case the proxy will
 * return itself to allow for method chaining.</li> <li>Getters with void return types or
 * more than 0 arguments, setters with an invalid return type or no arguments, and any
 * other method not matching the conventions above will cause the system to throw an
 * {@link IllegalComponentDefinitionException}.</li> </ol>
 * <p/>
 * Internally, the entity system will generate proxy implementations of the component
 * interfaces that implement the property getters and setters but store all of the values
 * in {@link com.lhkbob.entreri.property.Property} instances of a compatible type. This
 * allows iteration over components to have much better cache locality if the component is
 * defined in terms of primitives or types that have specialized Property implementations
 * that can pack and unpack an instance. The {@link SharedInstance} annotation can be
 * added to the getter method of a property to specify that the {@link
 * com.lhkbob.entreri.property.ShareableProperty} API should be leveraged by the generated
 * class.
 * <p/>
 * Additional attribute annotations can be added to the getter method to influence the
 * behavior of the {@link com.lhkbob.entreri.property.PropertyFactory} used for each
 * property in the component definition. Besides using the Factory annotation to specify
 * the factory type, a property implementation can be associated with a type with
 * canonical name <var>C</var> by adding the file META-INF/entreri/mapping/C to the
 * classpath, where its contents must be:
 * <pre>
 *     &lt;BINARY NAME OF PROPERTY&gt;
 * </pre>
 * where the value is suitable for passing into {@link Class#forName(String)}.
 * <p/>
 * The generated proxies will implement equals() and hashCode() based on their type and
 * the id of their owning entity. The {@link ComponentIterator} class creates flyweight
 * component instances whose identity changes as iteration proceeds; equals() and
 * hashCode() will behave appropriately. This means that flyweight components should not
 * be stored in collections that depend on equality or hashes. Flyweight components are
 * used for performance reasons when iterating over the entities in a system with specific
 * component configurations because the JVM does not need to load each unique component
 * instance into the cache.
 * <p/>
 * Component implements both {@link com.lhkbob.entreri.Ownable} and {@link
 * com.lhkbob.entreri.Owner}. This can be used to create hierarchies of both components
 * and entities that share a lifetime. When a component is removed from an entity, all of
 * its owned objects are disowned. If any of them were entities or components, they are
 * also removed from the system.
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
     * <p/>
     * Get whether or not the component is still attached to an alive entity in an entity
     * system. This will return false if it or its entity has been removed. If the
     * component is a flyweight object used in iteration, it can also return false when
     * iteration has terminated.
     * <p/>
     * Using property getters or setters on a dead component produces undefined behavior.
     *
     * @return True if the component is still attached to an entity in the entity system,
     *         or false if it or its entity has been removed
     */
    public boolean isAlive();

    /**
     * <p/>
     * Get whether or not this particular instance is a flyweight component object. A
     * flyweight instance is used by ComponentIterators to efficiently view the underlying
     * component data iteratively. It is safe to set the owner of a flyweight component or
     * to set the flyweight as an owner of another object. It will correctly register the
     * canonical component as the owner.
     * <p/>
     * Components returned by {@link Entity#add(Class)}, {@link Entity#get(Class)}, and
     * its other component returning functions will always return non-flyweight
     * components. These components are the 'canonical' instances for that component type
     * and entity.
     *
     * @return True if this component is a flyweight instance iterating over a list of
     *         components
     */
    public boolean isFlyweight();

    /**
     * <p/>
     * Get the underlying index of this component used to access its properties. For most
     * uses, you should not need to use this method.  As an EntitySystem manages and
     * compacts the component data storage, a component's index can change. The index
     * should be used to access decorated properties, which will be kept in-sync with any
     * compactions.
     * <p/>
     * Additionally, some component instances may be flyweight objects used for iteration.
     * In this case, every iteration will update its index and the component object's
     * identity will change as well. See {@link ComponentIterator} for more information.
     *
     * @return The current index of component
     */
    public int getIndex();

    /**
     * <p/>
     * Get the current version of the data of this component. When a component's data is
     * mutated, implementations increment its version so comparing a previously cached
     * version number can be used to determine when changes have been made.
     * <p/>
     * Within a component type for an entity system, version values will be unique across
     * component instances. Thus, {@code entity.as(T.class).getVersion() != cachedVersion}
     * will correctly detect changes to the original component instance as well as the
     * removal and replacement by a new component of type T.
     *
     * @return The current version, or a negative number if the data is invalid
     */
    public int getVersion();

    /**
     * Increment the version of the component accessed by this instance. This will be
     * automatically called by all exposed setters by the generated proxies, but if
     * necessary it can be invoked manually as well.
     *
     * @see #getVersion()
     */
    public void updateVersion();

    /**
     * Get the class identifier for this component. This will be the specific
     * sub-interface of Component that this object is an instance of. getType() should be
     * used instead of {@link Object#getClass()} because that will return a specific and
     * most likely generated proxy implementation of the component type.
     *
     * @return The component type
     */
    public Class<? extends Component> getType();
}
