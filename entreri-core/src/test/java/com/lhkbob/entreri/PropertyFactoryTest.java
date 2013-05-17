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

import com.lhkbob.entreri.property.Attributes;
import com.lhkbob.entreri.property.Clone;
import com.lhkbob.entreri.property.Clone.Policy;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.ObjectProperty;
import junit.framework.Assert;
import org.junit.Test;

@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
public class PropertyFactoryTest {
    /*
     * ObjectProperty fields for Attributes creation
     */
    private ObjectProperty<Object> objectPropertyNoPolicy;

    @Clone(Policy.DISABLE)
    private ObjectProperty<Object> objectPropertyDisabled;

    @Clone(Policy.JAVA_DEFAULT)
    private ObjectProperty<Object> objectPropertyDefault;

    @Clone(Policy.INVOKE_CLONE)
    private ObjectProperty<Object> objectPropertyInvoke;

    /*
     * DoubleProperty fields for Attributes creation
     */
    @DefaultDouble(1.0)
    private DoubleProperty doublePropertyNoPolicy;

    @DefaultDouble(2.0)
    @Clone(Policy.DISABLE)
    private DoubleProperty doublePropertyDisabled;

    @Clone(Policy.JAVA_DEFAULT)
    private DoubleProperty doublePropertyDefault;

    @Clone(Policy.INVOKE_CLONE)
    private DoubleProperty doublePropertyInvoke;

    private Attributes createAttributes(String fieldName) throws Exception {
        return new Attributes(getClass().getDeclaredField(fieldName).getAnnotations());
    }

    @Test
    public void testObjectPropertyCloneNoPolicy() throws Exception {
        ObjectProperty.Factory factory = new ObjectProperty.Factory(
                createAttributes("objectPropertyNoPolicy"));

        ObjectProperty p1 = factory.create();
        ObjectProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        Object val = new Object();
        p1.set(0, val);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNull(p2.get(0));

        factory.clone(p1, 0, p2, 0);

        Assert.assertSame(val, p1.get(0));
        Assert.assertSame(val, p2.get(0));
    }

    @Test
    public void testObjectPropertyCloneDisabled() throws Exception {
        ObjectProperty.Factory factory = new ObjectProperty.Factory(
                createAttributes("objectPropertyDisabled"));

        ObjectProperty p1 = factory.create();
        ObjectProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        Object val = new Object();
        p1.set(0, val);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNull(p2.get(0));

        factory.clone(p1, 0, p2, 0);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNull(p2.get(0));
    }

    @Test
    public void testObjectPropertyCloneJavaDefault() throws Exception {
        ObjectProperty.Factory factory = new ObjectProperty.Factory(
                createAttributes("objectPropertyDefault"));

        ObjectProperty p1 = factory.create();
        ObjectProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        Object val = new Object();
        p1.set(0, val);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNull(p2.get(0));

        factory.clone(p1, 0, p2, 0);

        Assert.assertSame(val, p1.get(0));
        Assert.assertSame(val, p2.get(0));
    }

    @Test
    public void testObjectPropertyCloneInvoke() throws Exception {
        ObjectProperty.Factory factory = new ObjectProperty.Factory(
                createAttributes("objectPropertyInvoke"));

        ObjectProperty p1 = factory.create();
        ObjectProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        CloneObject val = new CloneObject(5);
        p1.set(0, val);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNull(p2.get(0));

        factory.clone(p1, 0, p2, 0);

        Assert.assertSame(val, p1.get(0));
        Assert.assertNotSame(val, p2.get(0));
        Assert.assertNotNull(p2.get(0));
        Assert.assertEquals(5, ((CloneObject) p2.get(0)).foo);
    }

    @Test
    public void testPrimitivePropertyCloneNoPolicy() throws Exception {
        DoubleProperty.Factory factory = new DoubleProperty.Factory(
                createAttributes("doublePropertyNoPolicy"));

        DoubleProperty p1 = factory.create();
        DoubleProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        p1.set(0, 4.0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(1.0, p2.get(0), 0.0001);

        factory.clone(p1, 0, p2, 0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(4.0, p2.get(0), 0.0001);
    }

    @Test
    public void testPrimitivePropertyCloneDisabled() throws Exception {
        DoubleProperty.Factory factory = new DoubleProperty.Factory(
                createAttributes("doublePropertyDisabled"));

        DoubleProperty p1 = factory.create();
        DoubleProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        p1.set(0, 4.0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(2.0, p2.get(0), 0.0001);

        factory.clone(p1, 0, p2, 0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(2.0, p2.get(0), 0.0001);
    }

    @Test
    public void testPrimitivePropertyCloneJavaDefault() throws Exception {
        DoubleProperty.Factory factory = new DoubleProperty.Factory(
                createAttributes("doublePropertyDefault"));

        DoubleProperty p1 = factory.create();
        DoubleProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        p1.set(0, 4.0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(0.0, p2.get(0), 0.0001);

        factory.clone(p1, 0, p2, 0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(4.0, p2.get(0), 0.0001);
    }

    @Test
    public void testPrimitivePropertyCloneInvoke() throws Exception {
        DoubleProperty.Factory factory = new DoubleProperty.Factory(
                createAttributes("doublePropertyInvoke"));

        DoubleProperty p1 = factory.create();
        DoubleProperty p2 = factory.create();

        factory.setDefaultValue(p1, 0);
        factory.setDefaultValue(p2, 0);

        p1.set(0, 4.0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(0.0, p2.get(0), 0.0001);

        factory.clone(p1, 0, p2, 0);

        Assert.assertEquals(4.0, p1.get(0), 0.0001);
        Assert.assertEquals(4.0, p2.get(0), 0.0001);
    }

    public static class CloneObject implements Cloneable {
        public final int foo;

        public CloneObject(int foo) {
            this.foo = foo;
        }

        @Override
        public CloneObject clone() {
            return new CloneObject(foo);
        }
    }
}
