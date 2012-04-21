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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lhkbob.entreri.property.ElementSize;
import com.lhkbob.entreri.property.IntProperty.DefaultInt;


/**
 * <p>
 * The Factory annotation can be declared on a Property field in a ComponentData
 * definition to specify the type of PropertyFactory to use when creating
 * instances of the Property for the component. The factory type must have a
 * no-argument constructor in order to be instantiated correctly. This
 * annotation should be used if Property does not provide a default factory with
 * sufficient flexibility with annotation attributes (e.g. {@link DefaultInt} or
 * {@link ElementSize} for the Properties defined in
 * com.lhkbob.entreri.property).
 * </p>
 * <p>
 * Factory can also be placed at the type level on a Property implementation to
 * declare the default PropertyFactory to use. When using the
 * {@link ReflectionComponentDataFactory}, it checks for a constructor taking a
 * single {@link Attributes} object, or the default constructor.
 * </p>
 * 
 * @see ReflectionComponentDataFactory
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface Factory {
    /**
     * @return Class of the PropertyFactory to instantiate, must have an
     *         accessible no-argument constructor
     */
    Class<? extends PropertyFactory<?>> value();
}
