/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig (lhkbob@gmail.com)
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
package com.entreri.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Parameter specifies a single argument to a constructor of a Property. If a
 * Property has a single-argument constructor, Parameter can be used directly on
 * the Property field. Otherwise {@link Parameters} can be used to select a
 * multiple-argument constructor.
 * </p>
 * <p>
 * The definition of a Property must be constant for all Component instances of
 * the same type because they share a Property instance for each declared
 * property (and access the indexed data as needed). Because of this, a
 * Parameter can only use primitive and boxed primitive values, Strings, and
 * Classes. The primitives and Classes are encoded in strings and are parsed
 * when Properties are instantiated.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameter {
    /**
     * <p>
     * Return the value of the parameter, converted to a string. Depending on
     * the type of the parameter, this will be converted to its final form in
     * different ways.
     * </p>
     * <p>
     * If the type is a primitive or boxed primitive, it is parsed using the
     * appropriate parseX() method (e.g. {@link Integer#parseInt(String)}).
     * String parameters take the value as is; Class parameters attempt to load
     * the Class via {@link Class#forName(String)}.
     * </p>
     * 
     * @return The constant value of the parameter
     */
    String value();

    /**
     * @return The class type of the parameter argument, must match the
     *         parameter to the constructor of the Property
     */
    Class<?> type();
}
