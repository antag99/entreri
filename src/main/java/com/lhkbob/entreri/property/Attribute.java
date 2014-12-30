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
 * Some attributes influence how the Property behaves, what actual Property implementation is used,
 * and others modify how the Component implementations are generated.
 *
 * Attribute annotations the definition of the property, regardless of what
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
 * There are several annotations that feel very similar to a property attribute, namely {@link
 * com.lhkbob.entreri.Validate} and {@link com.lhkbob.entreri.Named}. The entreri implementation for
 * component generation can handle more generic annotations for modifying its process, but supporting new
 * annotations is more low-level compared to defining a Property type that can accept the attribute. These are
 * mentioned here to point out their usage similarities and to draw attention to the primary difference with
 * property attributes: these annotations modify the behavior of the method and/or method parameter they are
 * applied to, without considering the property at all.
 *
 * @author Michael Ludwig
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Attribute {
    /**
     * Get whether or not this attribute affects the Property class chosen for the property implementation.
     * Unless the component generation implementation is aware of this annotation, this should remain set to
     * false. Currently this is provided to document the property attributes that currently can influence
     * choice.
     *
     * @return True if this attribute influences the chosen PropertyFactory
     */
    boolean influencePropertyChoice() default false;
}
