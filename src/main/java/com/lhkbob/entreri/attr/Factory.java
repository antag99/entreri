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

import com.lhkbob.entreri.property.PropertyFactory;

import java.lang.annotation.*;

/**
 * Factory
 * =======
 *
 * The Factory annotation is a dual purpose annotation. It can be placed on every Property implementation to
 * specify the default PropertyFactory that instantiates it. The factory type must have a no-argument
 * constructor or a constructor that takes as arguments Annotation subclasses which have been annotated with
 * `@Attribute`. If a Property class does not have this annotation applied to it, the default `entreri`
 * backend will not know how to create instances of it.
 *
 * Additionally, this annotation can be used as a component property attribute if Property does not provide
 * a default factory with sufficient flexibility via its supported annotation attributes (e.g. {@link
 * DefaultInt} for the Properties defined in com.lhkbob.entreri.property). If placed on a component method, it
 * overrides the PropertyFactory to use for that property implementation, completely bypassing any default or
 * configured type.
 *
 * @author Michael Ludwig
 * @see com.lhkbob.entreri.property.PropertyFactory
 * @see com.lhkbob.entreri.property.Property
 */
@Documented
@Attribute(Attribute.Level.PROPERTY)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Factory {
    /**
     * @return Class of the PropertyFactory to instantiate, must have an accessible no-argument constructor
     */
    Class<? extends PropertyFactory<?>> value();
}
