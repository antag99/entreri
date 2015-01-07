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
import java.util.*;

/**
 * Collection
 * ==========
 *
 * Collection is an attribute that exposes configuration options for properties that have types of {@link
 * java.util.List}, {@link java.util.Map}, or {@link java.util.Set}. It can configure whether or not these
 * collections are allowed to store null elements (verified by the component implementation). For
 * value-semantics collections, it can specify the implementation of the collection to use. This is
 * configuration is ignored for reference-semantic collections because the implementation can be chosen on a
 * per-reference basis.
 *
 *  Additionally, it can be used to explicitly declare the collection type of the property if the component
 *  type does not specify methods that implicitly define the type. It is an error to declare one collection
 *  type that conflicts with the implicitly defined collection types inferred from the other component
 *  methods.
 *
 * @author Michael Ludwig
 */
@Documented
@Attribute
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Collection {
    public static enum Type {
        LIST,
        SET,
        MAP,
        UNSPECIFIED
    }

    /**
     * Override the list class used for value-semantics list properties. The returned list type must have a
     * public default constructor. This is ignored if the property does not store value lists.
     *
     * @return The list implementation used
     */
    Class<? extends List> listImpl() default ArrayList.class;

    /**
     * Override the set class used for value-semantics set properties. The returned set type must have a
     * public default constructor. This is ignored if the property does not store value sets.
     *
     * @return The set implementation used
     */
    Class<? extends Set> setImpl() default HashSet.class;

    /**
     * Override the map class used for value-semantics map properties. The returned map type must have a
     * public default constructor. This is ignored if the property does not store value maps.
     *
     * @return The map implementation used
     */
    Class<? extends Map> mapImpl() default HashMap.class;

    /**
     * Specify the policy for if the collection may hold null elements. This will automatically be enforced
     * by the generated components (and is not a responsibility of the property implementations). Setter
     * methods that set the entire collection at once are also validated. Maps will disallow or allow `null`
     * keys and values together.
     *
     * @return True if null elements are okay, or false if a NullPointerException should be thrown by the
     * component methods when a null element passed to the collection
     */
    boolean allowNullElements() default true;

    /**
     * Declare the collection type of the property. If UNSPECIFIED is used (the default) the collection type
     * is inferred from the other methods that reference the property. However, if other methods do not
     * provide such a known collection type, this must be declared explicitly to one of SET, LIST, or MAP.
     *
     * @return The collection type of the property
     */
    Type type() default Type.UNSPECIFIED;
}

