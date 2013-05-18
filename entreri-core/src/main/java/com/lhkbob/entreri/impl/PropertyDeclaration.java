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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.property.PropertyFactory;

/**
 * PropertyDeclaration represents a particular "property" instance declared in a Component
 * sub-interface. A property is represented by a bean getter method and an associated
 * setter. This interface captures the requisite information needed to implement a
 * component type.
 *
 * @author Michael Ludwig
 */
public interface PropertyDeclaration extends Comparable<PropertyDeclaration> {
    /**
     * Get the logical name of the property, either the name extracted from the getter
     * bean method, or from the {@link com.lhkbob.entreri.property.Named} annotation.
     *
     * @return The property name
     */
    public String getName();

    /**
     * Get the canonical class name of the type of value stored by this property.
     *
     * @return The type of this property, suitable for inclusion in source code
     */
    public String getType();

    /**
     * Get the canonical class name of the {@link com.lhkbob.entreri.property.Property
     * Property} implementation corresponding to the type of this property.
     *
     * @return The property implementation, suitable for inclusion in source code
     */
    public String getPropertyImplementation();

    /**
     * Get the name of the setter method that will be used to modify this property value.
     * Multiply properties may use the same setter, and {@link #getSetterParameter()}
     * determines their order in the signature. All properties in a ComponentSpecification
     * that share a setter name use the same setter method.
     *
     * @return The name of the setter method
     */
    public String getSetterMethod();

    /**
     * Get the name of the getter method that is used to access the value of this
     * property. Its return type is equal to {@link #getType()} and it will not take any
     * arguments.
     *
     * @return The bean getter method
     */
    public String getGetterMethod();

    /**
     * Get the parameter index used by this property in the signature of its corresponding
     * setter method. This will be 0 for properties that use the standard bean setter
     * method, but may be different when properties share the same setter with multiple
     * parameters.
     *
     * @return The method parameter corresponding to this property
     */
    public int getSetterParameter();

    /**
     * Get whether or not the signature of the setter method of this property returns the
     * owning Component or if it returns void.  When multiple properties have the same
     * setter method name, this will always agree.
     *
     * @return True if the method signature returns the component
     */
    public boolean getSetterReturnsComponent();

    /**
     * Get whether or not this property should use the shared instance API to store and
     * get values of the property.
     *
     * @return True if the getter was annotated with {@link com.lhkbob.entreri.property.SharedInstance}
     */
    public boolean isShared();

    /**
     * Get the PropertyFactory instance that was configured for this property. It must be
     * used to instantiate the property objects that will be assignable to the type
     * returned by {@link #getPropertyImplementation()}, and will be configured by all
     * attribute annotations applied to the getter method.
     * <p/>
     * This is only available during a runtime situation, and should not be called when
     * ComponentSpecification came from the mirror API.
     *
     * @return The property factory for this property
     */
    public PropertyFactory<?> getPropertyFactory();
}
