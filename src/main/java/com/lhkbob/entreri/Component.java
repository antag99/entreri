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
 * component type). Method patterns are used to identify groups of methods that access and mutate the
 * logical property values. One such pattern is the Java Bean pattern, which is supported and sufficient
 * for the majority of use cases. All methods must match a pattern, and the properties declared by the matched
 * method groups must not conflict with one another.
 *
 * Component implements both {@link Ownable} and {@link Owner}. This can be used to create hierarchies of both
 * components and entities that share a lifetime. When a component is removed from an entity, all of its owned
 * objects are disowned. If any of them were entities or components, they are also removed from the system.
 *
 * ## Semantics
 *
 * By default, the logical properties in a component definition are assumed to have value semantics. That
 * means modifying the input to a setter after its been called will not affect the component state, nor will
 * modifying the returned object from a getter. This is straightforward for all primitive types in Java but
 * requires special back-end handling for Object types to guarantee these semantics. If there is no backing
 * {@link com.lhkbob.entreri.property.Property} for an Object type that supports value semantics, the method
 * in the component definition should be annotated with {@link com.lhkbob.entreri.property.Reference} to declare
 * that normal Java reference semantics are to be used instead.
 *
 * ## Method patterns
 *
 * As `entreri` auto generates the actual component implementations, it must know how to implement the
 * methods declared in the component type interfaces. Internally, each method is matched against the
 * set of supported patterns that are capable of providing the templated method body. These body templates
 * must interact with the {@link com.lhkbob.entreri.property.Property} data store used under the hood
 * for each logical property of the component type. Because of this, a declared method may match the
 * pattern syntactically but the chosen Property does not provide the required API for use with the pattern.
 * See the advanced topics section below for a discussion on how to solve this issue. The next subsections
 * describe the currently defined method patterns.
 *
 * In this model, every method declared in a component subinterface defines one-to-many logical properties,
 * based on the rules of the pattern the method matches. Multiple methods may define properties of the same
 * name, in which case they operate on the same property. However, the property definition from the method
 * group defining the same name must all be consistent: the property must have the same type from each method,
 * and it cannot have different attribute annotations of the same type on different methods. Although that's
 * a long-winded way of describing it, this policy behaves the way you'd expect it to. Getter and setter
 * methods for the same property name match up and share the same property definition.
 *
 * ### Bean getters
 *
 * Methods that start with 'get', 'is', or 'has', that take no arguments, and return a type `T` are
 * considered to be bean getter methods. The type `T` may be a primitive or any other Java type, although note
 * that Object types should be annotated with {@link com.lhkbob.entreri.property.Reference} if the backing
 * Property does not support value semantics. The default property name defined by one of these bean getters
 * is the remainder of the method name after the prefix, with its first character made lowercase. The defined
 * type of the property is the return type of the method. The {@link Named} annotation
 * applied to the method overrides the name of the property, but does not affect how the method is
 * pattern-matched.
 *
 * This pattern requires that the backing Property define a method `T get(int)`, which all default property
 * implementations do.
 *
 * ### Bean setters
 *
 * Methods that start with 'set', that take a single argument and return `void` or the component
 * subinterface type are considered bean setter methods. The type `T` of the single argument is the defined
 * property's type, and the default property name is the remainder of the name after the prefix, with its
 * first character made lowercase. This is consistent with naming used for bean getters. If the setter method
 * has the component subinterface as the return type, the generated implementation returns `this` to support
 * method chaining. This pattern supports the {@link com.lhkbob.entreri.property.Within} attribute and {@link
 * Validate} annotation to modify the generated method body. The {@link
 * Named} annotation applied to either the method or single parameter overrides the
 * name of the property, but does not affect how the method is pattern-matched.
 *
 * This pattern requires that the backing Property define a method `void set(int, T)`, which all default
 * property implementations do.
 *
 * ### Reusable result getters
 *
 * Methods that start with 'get', 'is', or 'has', that take a single argument of `T` and return a `T` are
 * treated similarly to bean getters. The main difference is that the object instance passed into the matched
 * method represents the result and is updated in place before being returned. This is particularly useful
 * when the backing property unpacks an object type into its primitive pieces. Instead of having to
 * instantiate new objects to pack them back together, a single instance can be mutated to match the value for
 * the components. The defined property uses the same default name and type as bean getters. Like bean
 * setters, applying {@link Named} to the argument can be used to override the name in
 * addition to applying it to the method itself.
 *
 * This pattern requires that the backing Property define a method `void get(int, T)`. None of the provided
 * property implementations define this, so reusable getters are only possible if a type has a custom Property
 * defined for it.
 *
 * ### Multi-argument setters
 *
 * Methods that start with 'set', return `void` or the component type, and take more than one argument are
 * matched by this pattern. This pattern defines a property for each of the arguments. For each parameter, the
 * defined property's name is either the variable name from the source code or the declared name from {@link
 * Named} and its type is the type of the argument. The generated method will set
 * values for each the defined properties. This pattern is most useful when combined with the {@link
 * Validate} annotation to perform validation across multiple arguments before the
 * component is actually modified.
 *
 * This pattern requires that the backing Property define the same method required by bean setters.
 *
 * ## Property types
 *
 * Under the hood, the data for all components of a particular data are managed by a {@link
 * com.lhkbob.entreri.property.Property} instance. The particular Property that is chosen for a
 * logical property defined in a component interface is determined in two ways. First, if the method has the
 * `@ImplementedBy` annotation applied that Property class is used. Second, a mapping is searched for
 * within the META-INF directories in the class path. The exact file searched for depends on the type of the
 * logical property and that properties required semantics. See {@link com.lhkbob.entreri.property.Property}
 * for more complete details.
 *
 * ### Property default type mappings
 *
 * The table below shows how the type of a logical property is mapped to a Property implementation, for the
 * implementations provided within `entreri`. It also shows the {@link com.lhkbob.entreri.property.Attribute}
 * annotation that property class defines that allows specification of default values for a component.
 *
 * Type               | PropertyFactory implementation                      | Default attribute annotation
 * -------------------|-----------------------------------------------------|-----------------------------
 * `boolean`          | {@link com.lhkbob.entreri.property.BooleanProperty} | {@link com.lhkbob.entreri.property.DefaultBoolean}
 * `byte`             | {@link com.lhkbob.entreri.property.ByteProperty}    | {@link com.lhkbob.entreri.property.DefaultByte}
 * `short`            | {@link com.lhkbob.entreri.property.ShortProperty}   | {@link com.lhkbob.entreri.property.DefaultShort}
 * `char`             | {@link com.lhkbob.entreri.property.CharProperty}    | {@link com.lhkbob.entreri.property.DefaultChar}
 * `int`              | {@link com.lhkbob.entreri.property.IntProperty}     | {@link com.lhkbob.entreri.property.DefaultInt}
 * `long`             | {@link com.lhkbob.entreri.property.LongProperty}    | {@link com.lhkbob.entreri.property.DefaultLong}
 * `float`            | {@link com.lhkbob.entreri.property.FloatProperty}   | {@link com.lhkbob.entreri.property.DefaultFloat}
 * `double`           | {@link com.lhkbob.entreri.property.DoubleProperty}  | {@link com.lhkbob.entreri.property.DefaultDouble}
 * `? extends Enum`   | {@link com.lhkbob.entreri.property.EnumProperty}    | {@link com.lhkbob.entreri.property.DefaultEnum}
 * `? extends Object` | {@link com.lhkbob.entreri.property.ObjectProperty}  | NA
 *
 * ## Advanced topics
 *
 * Internally, the entity system will generate proxy implementations of the component interfaces that
 * implement the property getters and setters but store all of the values in {@link
 * com.lhkbob.entreri.property.Property} instances of a compatible type. This allows iteration over components
 * to have much better cache locality if the component is defined in terms of primitives or types that have
 * specialized Property implementations that can pack and unpack an instance.
 *
 * Additional attribute annotations can be added to methods to influence the behavior of the {@link
 * com.lhkbob.entreri.property.Property} used for each property in the component definition. Attributes are
 * either property or method level. Property-level attributes applied to any method of a logical property are
 * visible to any method that defines the same logical property. Method-level attributes are only visible to
 * that particular method.
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
     * Properties annotated with {@link DoNotAutoVersion} will not automatically call this
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
