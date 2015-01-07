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
package com.lhkbob.entreri.property;

/**
 * Property
 * ========
 *
 * Property represents a generic field or property of a {@link com.lhkbob.entreri.Component} definition. A
 * component can have multiple properties. A single Property instance holds the corresponding values for every
 * component of that type in an EntitySystem. It is effectively an indexed map from component index to
 * property value. This is an approach to mapped-objects where Components can be mapped onto primitive arrays
 * so that iteration sees optimal cache locality. As an example, there could be two component instances of
 * type A, with properties a and b. The two components would share a Property for 'a' and a Property for 'b',
 * which hold the data for both components.
 *
 * ## Defining new Property classes
 *
 * Although `entreri` comes with predefined Property types for all Java primitives, any Enum, and Object
 * references it can be desirable to define new Property types. This is necessary to take full advantage of
 * the mapped-object design pattern and see the performance gains `entreri` is capable of. A canonical example
 * of an Object type benefiting from a custom Property are linear algebra vectors and matrices, which can be
 * efficiently packed into float or double arrays.
 *
 * The following subsections discuss the details of implementing a new Property type.
 *
 * ### Semantics
 *
 * Any class extending Property must implement one of {@link
 * com.lhkbob.entreri.property.Property.ValueSemantics} or {@link
 * com.lhkbob.entreri.property.Property.ReferenceSemantics}. The annotation processor will verify that one and
 * only one of these interfaces is implemented. Depending on the chosen semantics, the behavior of any
 * accessors, mutators, and {@link #clone(Property, int, int)} will change and must be implemented
 * appropriately by the property.
 *
 * ### Accessor and mutator methods
 *
 * The Property interface defines a minimal number of methods to allow the `entreri` implementation to
 * manage the data in a generic manner. However, it does not include methods to get or set the state for a
 * particular component instance. This is done so that properties can declare these methods with primitive
 * types in their signature, that the use of generics might otherwise prohibit.
 *
 * For a property, the index of a component represents its identity, and can be thought of as a pointer to
 * the component. This component index is logically independent of how the property chooses to store data for
 * its components. For properties that store references or single values, the component index mapping
 * one-to-one with an array index is appropriate. For linear algebra data types, scaling the index by a number
 * of primitives is reasonable. The exact mechanism is up to the Property, but it is important to remember
 * that for the following discussion all indices refer to the component index.
 *
 * A property implementation can define different methods to expose support for the different component
 * method patterns described in {@link com.lhkbob.entreri.Component}. If the property supports a type `T`,
 * the following are the signatures that the component method patterns look for (for some patterns two
 * types, the key `K` and value `V` are referenced):
 *
 * Component pattern             | Signature                                | Description
 * ------------------------------|------------------------------------------|------------
 * `T get[NAME]()`               | `T get(int index)`                       | Get the value of type `T` for the component at `index`.
 * `* set[NAME](T)`              | `void set(int index, T value)`           | Update the stored value for the component at `index` to be `value`.
 * `T get[NAME](@ReturnValue T)` | `void get(int index, T result)`          | Update `result` to equal the value for the component at `index`.
 * `V get[NAME](K)`              | `V get(int index, K key)`                | Get the value associated with key from a map.
 * `* put[NAME](K, V)`           | `V put(int index, K key, V value)`       | Put the new value for the key into the map, return the old one.
 * `boolean contains[NAME](K)`   | `boolean contains(int index, K key)`     | True if the map has a value for the given key.
 * `boolean contains[NAME](T)`   | `boolean contains(int index, T element)` | True if the list or set has the given element.
 * `* remove[NAME](K)`           | `V remove(int index, K key)`             | Remove the key's value from the map and return it.
 * `* remove[NAME](T)`           | `boolean remove(int index, T element)`   | Remove the element and return true if the list or set was modified.
 * `* add[NAME](T)`              | `boolean add(int index, T element)`      | Append the element to the list or set and return true if it was modified.
 *
 * These getters and setters exposed by the Property subclass must respect the requirements of its chosen
 * semantics. It is not required for a property type to support all of these methods. It is not validated if a
 * property declares both mutators and accessors, but not doing so would limit the use of the property type
 * since it would prevent the component method patterns from supporting that type.
 *
 * In a property implementation, the type `T` may be a concrete type or generic type. If it is generic, the
 * Property must also implement {@link com.lhkbob.entreri.property.Property.Generic} where Generic's type
 * variable is set to the type `T`.
 *
 * ### Instantiation
 *
 * Property instances are carefully managed by an EntitySystem. There is ever only one property instance per
 * defined property in a component type for a system. Property instances are constructed for a component type
 * the first time a component type is referenced by the system. In order to automatically construct the
 * Property instances, subclasses must define a public constructor that fits the following criteria:
 *
 * 1. Constructor access must be public.
 * 2. The first argument may be of type `Class` and will be passed the concrete type the property must store.
 * This is largely only valuable for generic properties that might depend on the actual class type.
 * 3. All remaining arguments must be {@link Attribute}-labeled `Annotation`
 * instances.
 *
 * The default `entreri` implementation will inject the property class (if its requested as the first
 * argument), and it will inject the annotation instances that were attached to the property methods of the
 * component. If `null` is passed in, it means the attribute was not specified for that property in the
 * component description. If the `Class` argument is present in the constructor, the value will never be null.
 *
 * The following examples illustrate constructors that fit these patterns:
 *
 * ```java
 * // a property that requires no extra configuration
 * public MyProperty() { }
 *
 * // a generic property that wants the property class
 * public MyGenericProperty(Class<? extends T> type) {
 *     // here T is the type specified in the Generic<> interface
 * }
 *
 * // a property that supports multiple attributes
 * public MyCustomizableProperty(DefaultValue dflt, Clone clonePolicy) {
 *     if (dflt != null) {
 *         // read default value for property
 *     }
 * }
 *
 * // a generic factory with attributes
 * public MyCustomGenericProperty(Class<? extends T> type, Clone clonePolicy) {
 *
 * }
 * ```
 *
 * In the event that multiple constructors fit these patterns, the constructor with the most arguments is
 * invoked. Other constructors that have a more programmer friendly API can and should be provided if they
 * make sense and will not be used during the code generation stage.
 *
 * ### Mapping to a Property implementation
 *
 * Once a Property class is defined and implemented, `entreri` must be told of its existence when code is
 * being generated for component implementations. In the discussion that follows, assume the new Property
 * implementation is type `P` and supports values of type `T`. If a component method references type `T`, the
 * method can be annotated with {@link ImplementedBy} and its value set to `P.class`
 * to select the custom property implementation for that component's method. Other references to type `T`
 * would use the default property for that type.
 *
 * To map the custom `P` to every property declaration of `T`, a mapping file must be placed in the META-INF
 * directory for the code generation process to discover. The file `META-INF/entreri/mapping/<T>-<semantics>`
 * is searched for. If found, its contents are read and assumed to be the qualified class name of the Property
 * implementation supporting `T`. The filename should replace `<T>` with the qualified class name of `T`, and
 * replace `<semantics>` with the string `value` or `reference` depending on the supported semantics of `P`.
 *
 * @author Michael Ludwig
 */
public interface Property<T extends Property<T>> {
    /**
     * ReferenceSemantics
     * ==================
     *
     * ReferenceSemantics is a tag interface that a Property subclass can implement to declare that the
     * semantics it uses for accessing and mutating state are the reference semantics Java uses for any `Object`
     * type. Features of reference semantics:
     *
     * * A component's value may be null.
     * * The state of the referred object can be changed after assigning it to the component and the
     * component will reflect that change.
     * * The state of the component's value can be changed after retrieving it without having to reassign it
     * to the component.
     *
     * A property declaration in a component definition uses value semantics by default. To use a Property
     * implementation that has reference semantics, the {@link Reference} attribute
     * must be added to the component's property.
     *
     * @author Michael Ludwig
     * @see com.lhkbob.entreri.property.Property.ValueSemantics
     */
    public static interface ReferenceSemantics {
    }

    /**
     * ValueSemantics
     * ==============
     *
     * ValueSemantics is a tag interface that a Property subclass can implement to declare that the
     * semantics it uses for accessing and mutating state are value-like semantics that are usually associated
     * with primitive types. Requirements of value semantics:
     *
     * * Mutations to an object after its been assigned to a component's property does not affect the state of the component.
     * * Mutations to an object retrieved from a component's property do not affect the state of the component, *or* the
     * returned value cannot be modified.
     * * Null values are not allowed, either to be assigned or returned.
     *
     * Java primitive types automatically fit the requirements of value semantics. `Object` types that
     * are immutable, such as {@link String} also fit value semantics without any extra work on the Property
     * implementer's part. However, other object types do not fit value semantics without special support
     * from the property. To achieve value semantics, the property may clone instances on input and output,
     * copy and decompose state into primitive arrays, or wrap values in unmodifiable interfaces (as is often
     * done for collections).
     *
     * The value semantic contract is not validated, it is assumed that implementing
     * this interface declares the requirements have been met.
     *
     * @author Michael Ludwig
     * @see com.lhkbob.entreri.property.Property.ReferenceSemantics
     */
    public static interface ValueSemantics {
    }

    /**
     * Generic
     * =======
     *
     * Generic is an informational interface that a Property subclass must implement if it has type
     * variables in it's definition. In a number of circumstances it makes sense for a single Property
     * implementation to be valid across multiple types. Examples of this are {@link
     * com.lhkbob.entreri.property.EnumProperty} and {@link com.lhkbob.entreri.property.ObjectProperty}.
     * Implementing `Generic` does not mandate the semantics the property must adhere to.
     *
     * The type variable `P` is the parameterized type that the Property supports. This does not have to be
     * equal to the declared type variable of the implementation, although it should reference it. `P` should
     * be the type returned by the `get(int)` method or the value parameter for the `set(int, P)` method most
     * Properties declare. In simple cases `P = T` where `T` is the type variable declared by the
     * implementation. In complex cases, such as for collections, `T` might be the element type and `P =
     * List<T>`.
     *
     * If a Property subclass does not implement this method, it cannot have any type variables. This
     * interface is required by the code generator so that it can unify the generic type `P` with the
     * component's property type to determine if the Property implementation supports it.
     *
     * @author Michael Ludwig
     */
    public static interface Generic<P> {
    }

    /**
     * Resize the internal storage to support indexed lookups from 0 to `size - 1`.  If `size` is less than
     * the current capacity, all previous values with an index less than `size` must be preserved, and the
     * remainder are discarded.  If `size` is greater than the current capacity, all previous indexed values
     * must be preserved and the new values can be undefined.
     *
     * This is for internal use *only*, and should not be called outside the management of components.
     *
     * @param size The new capacity, will be at least 1
     */
    public void setCapacity(int size);

    /**
     * Get the current capacity of the property. All instances must start with a capacity of at least 1.
     * The capacity represents the number of component instances the property can handle.
     *
     * This is an upper limit on valid indices for access purposes. It will be at least the actual number of
     * component instances of the associated type. Accessing data beyond the last valid component has
     * undefined values but will not produce an index-out-of-bounds unless the index exceeds the capacity.
     *
     * @return The current capacity of the property
     */
    public int getCapacity();

    /**
     * Swap the value at `indexA` with `indexB`.
     *
     * This is for internal use *only*, and should not be called outside the management of components.
     *
     * @param indexA The index of the first value
     * @param indexB The index of the second value
     */
    public void swap(int indexA, int indexB);

    /**
     * Set the default value that the component at the specified <var>index</var>. This is the value
     * every component is initialized with.
     *
     * @param index The component index to be updated
     */
    public void setDefaultValue(int index);

    /**
     * Copy the value from `src` at component index, `srcIndex` into this property at index `dstIndex. This
     * is used when a component is created and cloned from a template with {@link
     * com.lhkbob.entreri.Entity#add(com.lhkbob.entreri.Component)}. For many cases a assignment respecting
     * the property semantics is sufficient, but some component types might require more complicated cloning
     * rules. It is recommended to check for the {@link DoNotClone} annotation in the
     * provided attributes set to define this behavior.
     *
     * @param src      The source property that is being cloned
     * @param srcIndex The index into src of the component being cloned
     * @param dstIndex The index into dst of the component being created
     */
    public void clone(T src, int srcIndex, int dstIndex);
}
