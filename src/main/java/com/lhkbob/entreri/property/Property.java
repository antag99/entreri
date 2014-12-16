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
 * property value.
 *
 * This is an approach to mapped-objects where Components can be mapped onto primitive arrays so that
 * iteration sees optimal cache locality. As an example, there could be two instances of type A, with
 * properties a and b. The two 'a' references would share the same data store, and the two 'b' references
 * would share another store.
 *
 * All property implementations must expose two methods:
 * FIXME update to specify patterns
 *
 * 1. `T get(int)` - which returns the value of type `T` at the component index.
 * 2. `void set(int, T)` - which stores the value of type `T` at the particular component index.
 *
 * To support primitives without boxing, `T` need not be a subclass of Object are not part of the interface
 * definition that would require generics.  They are are required and any attempts to use a Property
 * implementation without them will throw an exception. The exposed get() and set() methods, and potentially a
 * bulk accessor (such as returning the underlying array) are the supported methods for manipulating decorated
 * property values.
 *
 * Property instances are carefully managed by an EntitySystem. There is ever only one property instance per
 * defined property in a component type for a system. Property instances are created by {@link PropertyFactory
 * PropertyFactories}. Every concrete Property class should be annotated with {@link
 * com.lhkbob.entreri.attr.ImplementedBy} to specify the PropertyFactory class that constructs it. If not every
 * component method utilizing the property must specify the `@Factory` annotation explicitly.
 *
 * # Constructors FIXME update to be within Property
 *
 * 1. Access may be public, private, or protected although public access makes it self-documenting.
 * 2. The first argument may be of type `Class` and will be passed the concrete type the property must store.
 * This is largely only valuable for generic properties that might depend on the actual class type.
 * 3. All remaining arguments must be {@link com.lhkbob.entreri.attr.Attribute}-labeled `Annotation`
 * instances.
 *
 * The default `entrer` implementation will inject the property class (if its requested as the first
 * argument), and it will inject the annotation instances that were attached to the property methods of the
 * component. If `null` is passed in, it means the attribute was not specified for that property in the
 * component description. If the `Class` argument is present in the constructor, the value will never be null.
 *
 * The following examples illustrate constructors that fit these patterns:
 *
 * ```java
 * // a factory that requires no extra configuration
 * public MyFactory() { }
 *
 * // a generic factory that wants the property class
 * public MyGenericFactory(Class<? extends T> type) {
 * // here T is the type specified in the @GenericProperty annotation
 * }
 *
 * // a factory that supports multiple attributes
 * public MyCustomizableFactory(DefaultValue dflt, Clone clonePolicy) {
 * if (dflt != null) {
 * // read default value for property
 * }
 * }
 *
 * // a generic factory with attributes
 * public MyCustomGenericFactory(Class<? extends T> type, Clone clonePolicy) {
 *
 * }
 * ```
 *
 * In the event that multiple constructors fit these patterns, the constructor with the most arguments is
 * invoked. Other constructors that have a more programmer friendly API can and should be provided if they
 * make sense and will be ignored.
 *
 *
 * @author Michael Ludwig
 */
public interface Property<T extends Property<T>> {
    /**
     * Resize the internal storage to support indexed lookups from 0 to <code>size - 1</code>.  If
     * `size` is less than the current capacity, all previous values with an index less than
     * `size` must be preserved, and the remainder are discarded.  If `size` is greater than
     * the current capacity, all previous indexed values must be preserved and the new values can be
     * undefined.
     *
     * This is for internal use *only*, and should not be called outside the management of components.
     *
     * @param size The new capacity, will be at least 1
     */
    public void setCapacity(int size);

    /**
     * Get the current capacity of the property. All instances must start with a capacity of at least 1.
     * The capacity represents the number of component instances the property can handle. Even if component's
     * property value is represented as multiple consecutive primitives, that is still a single value instance
     * when considering the capacity an index supplied to `get()` or `set()`.
     *
     * This is an upper limit on valid indices for access purposes. It will be at least the actual number of
     * component instances of the associated type.
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
     * rules. It is recommended to check for the {@link com.lhkbob.entreri.attr.DoNotClone} annotation in the
     * provided attributes set to define this behavior.
     *
     * @param src      The source property that is being cloned
     * @param srcIndex The index into src of the component being cloned
     * @param dstIndex The index into dst of the component being created
     */
    public void clone(T src, int srcIndex, int dstIndex);

    // a property must extend ReferenceSemantics xor ValueSemantics, this should be verified by the annotation processor
    // these are made as separate interfaces because:
    // 1. allows convenient semantic checks by instanceof
    // 2. is more accessible than an annotation
    // 3. really more accessible than pulling apart a ParameterizedType at runtime
    // 4. does not require defining a Semantics super-type for a generic parameter
    // 5. a generic parameter cannot specify a strict subtype so Property<Semantics> is possible, which is dumb
    public static interface ReferenceSemantics {
    }

    public static interface ValueSemantics {
    }

    // a property impl can have at most one generic parameter. if it does, it must implement generic as well.
    // the parameter passed to Generic is the value type the property supports. This is unified with the
    // declaration to get the parameter value for the actual impl, which defines the actual methods available.
    // So a collection property might be ListProperty<T> implements Property<...>, Generic<List<T>>. The
    // declaration type gets set to List<Foo>, which unifies to T = Foo so the source outputs ListProperty<Foo>,
    // which if ListProperty is defined to make sense should then have methods like add(Foo), set(List<Foo>), etc.
    //
    // The declared property type from a component CANNOT have any parameters to it
    public static interface Generic<P> {
    }
}
