/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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
 * Component
 * =========
 *
 * Component represents a grouping of reusable and related states that are added to an {@link Entity}. An
 * Entity's behavior is defined by the union of all components attached to it, and the system's configured
 * tasks that process the entities or particular components. Tasks must be implemented and used in order to
 * take advantage of a particular component configuration. An entity can be considered as a collection of
 * aspects represented by component instances. An entity can have at most one instance of a component type at
 * a time, although over its lifetime the specific component instance may change.
 *
 * Logically a component definition is a set of named and typed properties, and a method-based API to get
 * and set the values of each property. Specific types of component are defined by creating a sub-interface of
 * Component (implementations will be auto-generated, minimizing the amount of work needed to declare a new
 * component type). Using the {@link com.lhkbob.entreri.property.Named Named}, {@link
 * com.lhkbob.entreri.property.SharedInstance SharedInstance}, {@link com.lhkbob.entreri.property.Factory
 * Factory} and custom {@link com.lhkbob.entreri.property.Attribute Attribute} annotations defined by {@link
 * com.lhkbob.entreri.property.Property Property} implementations, the data properties of a component type are
 * defined as a sub-interface. A declaration model similar to the Java Bean model is used and is outlined
 * below.
 *
 * 1. Non-void, zero-argument methods starting with 'get', 'is', and 'has' declare a property. The property
 * type is inspected from the return type of the method. The property name is the method name minus the
 * 'get'/'is'/'has' prefix with its first letter made lower-case. The {@link com.lhkbob.entreri.property.Named
 * Named} annotation can be used to override the name.
 * 2. Single-argument methods starting with 'set' are assumed to be a setter corresponding to a property.
 * The single parameter's type must equal the type of the getter. The {@link com.lhkbob.entreri.property.Named
 * Named} annotation can be applied to the setter to specify the property name modified by the method.
 * 3. Multi-argument methods starting with 'set' are assumed to be a setter that assigns values to multiple
 * properties, one for each argument. Each argument must be annotated with {@link
 * com.lhkbob.entreri.property.Named Named} to specify the property, and the argument type must equal the type
 * of the matching property.
 * 4. Setter methods identified by rules `2` or `3` must return `void` or the component's type, in which case
 * the component will return itself to allow method chaining.
 * 5. Getters with void return types or more than 0 arguments, setters with an incorrect return type, no
 * arguments, or parameter types not matching the property, and any other method not matching the conventions
 * defined above will cause the system to throw an {@link
 * com.lhkbob.entreri.IllegalComponentDefinitionException}.
 *
 * Component implements both {@link Ownable} and {@link Owner}. This can be used to create hierarchies of both
 * components and entities that share a lifetime. When a component is removed from an entity, all of its owned
 * objects are disowned. If any of them were entities or components, they are also removed from the system.
 *
 * ## Property types
 *
 * Under the hood, the data for all components of a particular data are managed by a {@link
 * com.lhkbob.entreri.property.Property} instance and its creating {@link
 * com.lhkbob.entreri.property.PropertyFactory factory}. The particular PropertyFactory that is chosen for a
 * logical property defined in a component interface is determined in three ways. First, if the method has the
 * `@Factory` annotation applied that selected factory is used. Second, if there is a default mapping that is
 * used. Third, a mapping may be defined by configuration inside META-INF.
 *
 * ### Property default type mappings
 *
 * The table below shows how the type of a logical property is mapped to a Property implementation,
 * as well as the custom {@link com.lhkbob.entreri.property.Attribute} annotation that property class defines
 * that specifies default values for each component instance.
 *
 * Type               | PropertyFactory implementation                              | Default attribute annotation
 * -------------------|-------------------------------------------------------------|-----------------------------
 * `boolean`          | {@link com.lhkbob.entreri.property.BooleanProperty.Factory} | {@link com.lhkbob.entreri.property.BooleanProperty.DefaultBoolean}
 * `byte`             | {@link com.lhkbob.entreri.property.ByteProperty.Factory}    | {@link com.lhkbob.entreri.property.ByteProperty.DefaultByte}
 * `short`            | {@link com.lhkbob.entreri.property.ShortProperty.Factory}   | {@link com.lhkbob.entreri.property.ShortProperty.DefaultShort}
 * `char`             | {@link com.lhkbob.entreri.property.CharProperty.Factory}    | {@link com.lhkbob.entreri.property.CharProperty.DefaultChar}
 * `int`              | {@link com.lhkbob.entreri.property.IntProperty.Factory}     | {@link com.lhkbob.entreri.property.IntProperty.DefaultInt}
 * `long`             | {@link com.lhkbob.entreri.property.LongProperty.Factory}    | {@link com.lhkbob.entreri.property.LongProperty.DefaultLong}
 * `float`            | {@link com.lhkbob.entreri.property.FloatProperty}           | {@link com.lhkbob.entreri.property.FloatProperty.DefaultFloat}
 * `double`           | {@link com.lhkbob.entreri.property.DoubleProperty.Factory}  | {@link com.lhkbob.entreri.property.DoubleProperty.DefaultDouble}
 * `? extends Enum`   | {@link com.lhkbob.entreri.property.EnumProperty.Factory}    | NA
 * `? extends Object` | {@link com.lhkbob.entreri.property.ObjectProperty.Factory}  | NA
 *
 * ## Advanced topics
 *
 * Internally, the entity system will generate proxy implementations of the component interfaces that
 * implement the property getters and setters but store all of the values in {@link
 * com.lhkbob.entreri.property.Property} instances of a compatible type. This allows iteration over components
 * to have much better cache locality if the component is defined in terms of primitives or types that have
 * specialized Property implementations that can pack and unpack an instance. The {@link
 * com.lhkbob.entreri.property.SharedInstance SharedInstance} annotation can be added to the getter method of
 * a property to specify that the {@link com.lhkbob.entreri.property.ShareableProperty ShareableProperty} API
 * should be leveraged by the generated class.
 *
 * Additional attribute annotations can be added to the getter method to influence the behavior of the
 * {@link com.lhkbob.entreri.property.PropertyFactory PropertyFactory} used for each property in the component
 * definition. Besides using the Factory annotation to specify the factory type, a property implementation can
 * be associated with a type with canonical name `C` by adding the file `META-INF/entreri/mapping/C` to the
 * classpath, where its contents must be:
 *
 * ```
 * <BINARY NAME OF PROPERTY>
 * ```
 *
 * where the value is suitable for passing into {@link Class#forName(String)}.
 *
 * The provided Attribute annotations and any custom annotations you write can only be applied to the getter
 * method that defines the property. They are ignored if added to the setter method. The getter method is
 * considered to be the definition of the property and the setter is only specified to complete the API.
 *
 * As interfaces, Component definitions can extend from other interfaces. However, any methods defined in
 * the additional super-interfaces must follow the exact same property specification. The total sum of methods
 * not defined by Component, Owner, Ownable, or Object must produce a valid property specification. These
 * super-interfaces do not need to extend Component themselves, indeed it is recommended that they do not
 * otherwise it would be possible for entities to have the sub-component and super-component expressed at the
 * same time.
 *
 * The generated proxies will implement `equals()` and `hashCode()` based on their type and the id of their
 * owning entity. The {@link ComponentIterator} class creates flyweight component instances whose identity
 * changes as iteration proceeds; `equals()` and `hashCode()` will behave appropriately. This means that
 * flyweight components should not be stored in collections that depend on equality or hashes. Flyweight
 * components are used for performance reasons when iterating over the entities in a system with specific
 * component configurations because the JVM does not need to load each unique component instance into the
 * cache. This works particularly well when shared instances of properties are used.
 *
 * @author Michael Ludwig
 */
public interface Component extends Owner, Ownable {
    /**
     * @return The EntitySystem that created this component
     */
    public EntitySystem getEntitySystem();

    /**
     * Get the entity that this component is attached to. If the component has been removed from the entity,
     * or is otherwise not live, this will return null.
     *
     * @return The owning entity, or null
     */
    public Entity getEntity();

    /**
     * Get whether or not the component is still attached to an alive entity in an entity system. This will
     * return false if it or its entity has been removed. If the component is a flyweight object used in
     * iteration, it can also return false when iteration has terminated.
     *
     * Using property getters or setters on a dead component produces undefined behavior.
     *
     * @return True if the component is still attached to an entity in the entity system, or false if it or
     * its entity has been removed
     */
    public boolean isAlive();

    /**
     * Get whether or not this particular instance is a flyweight component object. A flyweight instance is
     * used by ComponentIterators to efficiently view the underlying component data iteratively. It is safe to
     * set the owner of a flyweight component or to set the flyweight as an owner of another object. It will
     * correctly register the canonical component as the owner.
     *
     * Components returned by {@link Entity#add(Class)}, {@link Entity#get(Class)}, and its other component
     * returning functions will always return non-flyweight components. These components are the 'canonical'
     * instances for that component type and entity. Components created by a {@link
     * com.lhkbob.entreri.ComponentIterator}, or the wrapped Iterator from {@link
     * EntitySystem#iterator(Class)} are flyweight.
     *
     * @return True if this component is a flyweight instance iterating over a list of components
     */
    public boolean isFlyweight();

    /**
     * Get the canonical Component reference for this component. If {@link #isFlyweight()} returns false, then
     * this just returns itself. If it returns true, this will return the canonical reference corresponding to
     * this component's identity, i.e. {@code foo.getEntity().get(Foo.class)}.
     *
     * Behavior is undefined for components that are dead.
     *
     * @return The canonical component
     */
    public Component getCanonical();

    /**
     * Get the underlying index of this component used to access its properties. For most uses, you should
     * not need to use this method.  As an EntitySystem manages and compacts the component data storage, a
     * component's index can change. The index is exposed solely for Tasks accessing decorated properties,
     * which will be kept in-sync with any compactions.
     *
     * Additionally, some component instances may be flyweight objects used for iteration. In this case, every
     * iteration will update its index and the component object's identity will change as well. See {@link
     * ComponentIterator} for more information.
     *
     * @return The current index of component
     */
    public int getIndex();

    /**
     * Get the current version of the data of this component. When a component's data is mutated,
     * implementations increment its version so comparing a previously cached version number can be used to
     * determine when changes have been made.
     *
     * Within a component type for an entity system, version values will be unique across component
     * instances. Thus, `entity.as(T.class).getVersion() != cachedVersion` will correctly detect changes
     * to the original component instance as well as the removal and replacement by a new component.
     *
     * @return The current version, or a negative number if the data is invalid
     */
    public int getVersion();

    /**
     * Increment the version of the component accessed by this instance. This will be automatically called by
     * all exposed setters by the generated proxies, but if necessary it can be invoked manually as well.
     * Properties annotated with {@link com.lhkbob.entreri.NoAutoVersion} will not automatically call this
     * method.
     *
     * @see #getVersion()
     */
    public void updateVersion();

    /**
     * Get the class identifier for this component. This will be the specific sub-interface of Component that
     * this object is an instance of. getType() should be used instead of {@link Object#getClass()} because
     * that will return a specific and most likely generated proxy implementation of the component type.
     *
     * @return The component type
     */
    public Class<? extends Component> getType();
}
