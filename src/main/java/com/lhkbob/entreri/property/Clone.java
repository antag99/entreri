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

import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Clone
 * =====
 *
 * Clone is an attribute that can be applied to property declarations to change how the property's values are
 * cloned when a Component or Entity are created from a template Component or Entity. At the moment it can be
 * used to:
 *
 * 1. Copy values using Java's assignment semantics.
 * 2. Clone objects using their clone() method if it exists (falling back to assignment semantics).
 * 3. Do not copy the value, and use the default value the factory normally would have assigned.
 *
 * If these options are not sufficient, a custom {@link PropertyFactory} can be implemented and specified by
 * using the {@link Factory} annotation.
 *
 * @author Michael Ludwig
 */
@Attribute
@Retention(RetentionPolicy.RUNTIME)
public @interface Clone {
    /**
     * Policy is an enum describing a number of different behaviors performed by a {@link PropertyFactory}
     * when its {@link PropertyFactory#clone(Property, int, Property, int)} method is invoked in response to
     * {@link Entity#add(com.lhkbob.entreri.Component)} or {@link EntitySystem#addEntity(Entity)}.
     */
    public static enum Policy {
        /**
         * Cloning policy that disables the clone action for the given property. The new component being
         * cloned into gets the default value, just as if it was initialized via {@link Entity#add(Class)}.
         */
        DISABLE,
        /**
         * <p/>
         * Cloning policy that follows Java's assignment semantics, e.g. primitives are copied by value from
         * the component being cloned to the new one, and references are copied but the actual instances are
         * shared.
         * <p/>
         * This is the default policy if the Clone attribute is not present on any of the property
         * implementations provided in this package.
         */
        JAVA_DEFAULT,
        /**
         * <p/>
         * Cloning policy that attempts to invoke {@link Object#clone()} on cloned component's current value.
         * If the value is null, null is assigned without throwing an NPE.
         * <p/>
         * If the property type is a primitive data type, or is not {@link Cloneable}, this behaves like
         * JAVA_DEFAULT.
         */
        INVOKE_CLONE
    }

    /**
     * @return The cloning policy to use
     */
    Policy value();
}
