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
package com.lhkbob.entreri.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * DefaultValue is an annotation that can be added to {@link Property} fields in
 * a component definition to automatically configure their creating
 * PropertyFactories.
 * </p>
 * <p>
 * The default {@link ReflectionComponentDataFactory} will look for a static
 * method named 'factory()' that takes two parameters. The first must be an int
 * and represents the element size. If the {@link ElementSize} is not present,
 * the ComponentDataFactory should use a value of 1. The second parameter is the
 * default value to use. The parameter type determines which annotation method
 * is checked (e.g. {@link #defaultByte()} or {@link #defaultDouble()}).
 * </p>
 * <p>
 * All Properties defined in com.lhkbob.entreri.property support this
 * annotation, except for ObjectProperty because it is not possible to specify a
 * default object via annotation.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DefaultValue {
    boolean defaultBoolean() default false;
    char defaultChar() default '\0';
    byte defaultByte() default 0;
    short defaultShort() default 0;
    int defaultInt() default 0;
    long defaultLong() default 0L;
    float defaultFloat() default 0f;
    double defaultDouble() default 0.0;
}
