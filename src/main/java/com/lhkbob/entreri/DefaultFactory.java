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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * DefaultFactory is a type-level annotation that can be added to ComponentData
 * definitions to declare a different ComponentDataFactory than
 * {@link ReflectionComponentDataFactory} as the default. This default will be
 * used by any EntitySystem unless it has a per-system factory override that was
 * set with
 * {@link EntitySystem#setFactory(com.lhkbob.entreri.TypeId, ComponentDataFactory)}
 * </p>
 * <p>
 * Runtime exceptions will be thrown if the factory type declared by the
 * annotation does not have an accessible, supported constructor. The currently
 * supported constructors are:
 * <ol>
 * <li><code>ComponentDataFactory()</code></li>
 * <li><code>ComponentDataFactory(TypeId&lt;T&gt;)</code></li>
 * <li>
 * <code>ComponentDataFactory(Class&lt;T extends ComponentData&lt;T&gt;&gt;)</code>
 * </li>
 * </ol>
 * </p>
 * 
 * @author Michael Ludwig
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultFactory {
    /**
     * @return The ComponentDataFactory implementation used to create
     *         ComponentData's
     */
    Class<? extends ComponentDataFactory<?>> value();
}
