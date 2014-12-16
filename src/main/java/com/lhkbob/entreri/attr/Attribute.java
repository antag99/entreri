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
package com.lhkbob.entreri.attr;

import java.lang.annotation.*;

/**
 * Attribute
 * =========
 *
 * Attribute is used to declare an annotation type is an 'attribute' of a property declaration in a
 * Component type definition. They can then be accessed by Properties to configure its behavior. Examples
 * include describing cloning behavior and default values. Attribute annotations should have a runtime
 * retention. See {@link com.lhkbob.entreri.property.Property} for details on how a Property is provided with
 * the attributes it's interested in. Attributes become part of the component definitions contract. To help
 * with automatic documentation, attributes should be annotated with the `@Documented` annotation.
 *
 * An attribute annotation has an associated level, which determines what it influences. For now there are
 * only two levels:
 *
 * ## Property-level attributes
 *
 * Attribute annotations at the property level affect the definition of the property, regardless of what
 * method or parameter they were placed on. Thus for simple bean property methods, the attribute annotation
 * can be placed on either method (or the setter's parameter). Regardless, the attribute will be available to
 * the Property object that manages the property.
 *
 * In some cases it does not make sense to place a property-level annotation on a method. The frequent
 * example of this is a setter method with more than argument that sets multiple property values. In this case
 * the method does not have a 1-to-1 mapping with a property, and the property-level attribute should be
 * placed on the setter parameter.
 *
 * ## Method-level attributes
 *
 * Often method level attributes are used when finer-grained control is needed. Attributes that are method
 * level are not accessible to the Properties, but are used by the implementation when generating the
 * source code of the component. They only affect the method they were applied to, instead of every method
 * that references the property. Because it affects the generation of the component code, method-level
 * attributes cannot be as easily defined as property-level attributes.
 *
 * In situations where a method references multiple properties, the method-level attribute can be applied to
 * the method or the method parameters. When applied to a method parameter, it has no different affect from
 * being applied to the method. Because of this it is highly recommended that a method-level attribute only
 * use the METHOD target type.
 *
 * Most attributes should be property-level, but validation attributes are often method-level to allow for
 * validation to change per-method or operate across multiple properties.
 * @author Michael Ludwig
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Attribute {
    /**
     * The attribute levels currently supported.
     */
    public static enum Level {
        /**
         * Property level attributes are visible to all methods referencing the property, regardless of which
         * method or parameter of the property had the attribute defined on it.
         */
        PROPERTY,
        /**
         * Method level attributes are only visible to the method it is applied to. Applying one to a method
         * parameter behaves identically to when applied to the method.
         */
        METHOD
    }

    /**
     * @return The Level this attribute applies to, default is PROPERTY
     */
    Level level() default Level.PROPERTY;

    /**
     * Get whether or not this attribute affects the Property class chosen for the property
     * implementation. Method-level attributes should not set this to true, as this has a property-level
     * influence.
     *
     * @return True if this attribute influences the chosen PropertyFactory
     */
    boolean influencePropertyChoice() default false;
}
