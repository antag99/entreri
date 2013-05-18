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
package com.lhkbob.entreri.components;

import com.lhkbob.entreri.property.Factory;
import com.lhkbob.entreri.property.IntProperty.DefaultInt;
import com.lhkbob.entreri.property.LongProperty.DefaultLong;
import com.lhkbob.entreri.property.Named;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * A Component that tests a variety of things: multiple properties, different types,
 * customized default values, an overridden property factory, named properties, and
 * multi-parameter methods, extending component types, auto-detected properties, sharable
 * instances.
 *
 * @author Michael Ludwig
 */
public interface ComplexComponent extends IntComponent, FloatComponent {
    public void setLong(long i);

    @DefaultLong(Long.MAX_VALUE)
    public long getLong();

    public void setFactoryFloat(float f);

    @Factory(FloatPropertyFactory.class)
    public float getFactoryFloat();

    public short getParam1();

    public short getParam2();

    public ComplexComponent setParams(@Named("param1") short p1,
                                      @Named("param2") short p2);

    @Named("foo-blah")
    public boolean isNamedParamGetter();

    @Named("foo-blah")
    public ComplexComponent setNamedParamSetter(boolean foo);

    @DefaultInt(14)
    @SharedInstance
    public CustomProperty.Bletch hasBletch();

    public void setBletch(CustomProperty.Bletch b);
}
