/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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

import com.lhkbob.entreri.components.*;
import org.junit.Assert;
import org.junit.Test;

public class ComponentTest {
    @Test
    public void testIsAliveComponentRemove() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);

        Assert.assertTrue(c.isAlive()); // sanity check
        e.remove(IntComponent.class);
        Assert.assertFalse(c.isAlive());
    }

    @Test
    public void testIsAliveEntityRemove() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);

        Assert.assertTrue(c.isAlive()); // sanity check
        system.removeEntity(e);
        Assert.assertFalse(c.isAlive());
    }

    @Test
    public void testIsAlivePostCompact() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();
        Entity e3 = system.addEntity();

        IntComponent cd1 = e1.add(IntComponent.class); // removed
        IntComponent cd2 = e2.add(IntComponent.class); // will shift over
        IntComponent cd3 = e3.add(IntComponent.class); // will shift over

        cd2.setOwner(e2);
        cd3.setOwner(e3);

        int v2 = cd2.getVersion();
        int v3 = cd3.getVersion();

        e1.remove(IntComponent.class);
        system.compact(); // since e1's component was moved, this shifts e2 and e3

        // verify all state of the component
        Assert.assertFalse(cd1.isAlive());

        Assert.assertTrue(cd2.isAlive());
        Assert.assertFalse(cd2.isFlyweight());
        Assert.assertSame(e2, cd2.getEntity());
        Assert.assertSame(e2, cd2.getOwner());
        Assert.assertEquals(v2, cd2.getVersion());

        Assert.assertTrue(cd3.isAlive());
        Assert.assertFalse(cd3.isFlyweight());
        Assert.assertSame(e3, cd3.getEntity());
        Assert.assertSame(e3, cd3.getOwner());
        Assert.assertEquals(v3, cd3.getVersion());
    }

    @Test
    public void testNewlyAddedComponentState() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);

        Assert.assertTrue(c.isAlive());
        Assert.assertFalse(c.isFlyweight());
        Assert.assertNull(c.getOwner());
        Assert.assertSame(system, c.getEntitySystem());
        Assert.assertSame(e, c.getEntity());
    }

    @Test
    public void testIsAlivePostNoopCompact() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();

        e1.add(IntComponent.class);
        e2.add(IntComponent.class);
        IntComponent cd = e2.get(IntComponent.class);

        Assert.assertTrue(cd.isAlive()); // sanity check
        Assert.assertFalse(cd.isFlyweight());
        system.compact(); // no changes
        Assert.assertTrue(cd.isAlive());
        Assert.assertFalse(cd.isFlyweight());
    }

    @Test
    public void testVersionUpdate() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent cd = e.add(IntComponent.class);

        Assert.assertEquals(0, cd.getVersion());
        cd.updateVersion();
        Assert.assertEquals(1, cd.getVersion());
    }

    @Test
    public void testAutomaticVersionUpdate() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent cd = e.add(IntComponent.class);

        int originalVersion = cd.getVersion();
        cd.setInt(1500);
        Assert.assertFalse(originalVersion == cd.getVersion());
    }

    @Test
    public void testAutomaticVersionDisabled() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        FloatComponent cd = e.add(FloatComponent.class);

        int originalVersion = cd.getVersion();
        cd.setFloat(500);
        Assert.assertEquals(originalVersion, cd.getVersion());
    }

    @Test
    public void testUniqueVersionUpdate() {
        EntitySystem system = EntitySystem.Factory.create();
        IntComponent cd1 = system.addEntity().add(IntComponent.class);
        IntComponent cd2 = system.addEntity().add(IntComponent.class);

        Assert.assertEquals(0, cd1.getVersion());
        Assert.assertEquals(1, cd2.getVersion());

        cd1.updateVersion();
        cd2.updateVersion();

        Assert.assertEquals(2, cd1.getVersion());
        Assert.assertEquals(3, cd2.getVersion());

        // assert unique version after a re-add
        Assert.assertEquals(4, cd1.getEntity().add(IntComponent.class).getVersion());
    }

    @Test
    public void testInvalidComponentVersion() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent cd = e.add(IntComponent.class);
        e.remove(IntComponent.class);

        // sanity check
        Assert.assertEquals(0, cd.getIndex());

        int oldVersion = cd.getVersion();
        Assert.assertTrue(oldVersion < 0);
        cd.updateVersion();
        Assert.assertEquals(oldVersion, cd.getVersion());
    }

    @Test
    public void testCanonical() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent cd = e.add(IntComponent.class);

        Assert.assertFalse(cd.isFlyweight());
        Assert.assertSame(cd, cd.getCanonical());

        ComponentIterator it = system.fastIterator();
        IntComponent cd2 = it.addRequired(IntComponent.class);

        it.next();
        Assert.assertEquals(cd, cd2);
        Assert.assertTrue(cd2.isFlyweight());
        Assert.assertNotSame(cd2, cd2.getCanonical());
        Assert.assertSame(cd, cd2.getCanonical());
    }

    @Test
    public void testBeanMethodInvocation() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e1 = system.addEntity();
        ComplexComponent c1 = e1.add(ComplexComponent.class);

        CustomProperty.Bletch b = new CustomProperty.Bletch();
        b.value = 19;
        c1.setBletch(b);
        c1.setFactoryFloat(23.2f);
        c1.setLong(4000L);
        c1.setNamedParamSetter(true);
        c1.setParams((short) 4, (short) 5);
        c1.setFloat(2.0f);
        c1.setInt(140);
        c1.setSuperValue(12);
        c1.setEnum(ComplexComponent.TestEnum.V2);

        Assert.assertEquals(19, c1.hasBletch().value);
        Assert.assertEquals(23.2f, c1.getFactoryFloat(), 0.00001f);
        Assert.assertEquals(4000L, c1.getLong());
        Assert.assertTrue(c1.isNamedParamGetter());
        Assert.assertEquals((short) 4, c1.getParam1());
        Assert.assertEquals((short) 5, c1.getParam2());
        Assert.assertEquals(2.0f, c1.getFloat(), 0.00001f);
        Assert.assertEquals(140, c1.getInt());
        Assert.assertEquals(12, c1.getSuperValue());
        Assert.assertEquals(ComplexComponent.TestEnum.V2, c1.getEnum());

        // add a second component and make sure things didn't get goofed up
        Entity e2 = system.addEntity();
        ComplexComponent c2 = e2.add(ComplexComponent.class);

        Assert.assertEquals(14, c2.hasBletch().value);
        Assert.assertEquals(FloatPropertyFactory.DEFAULT, c2.getFactoryFloat(), 0.00001f);
        Assert.assertEquals(Long.MAX_VALUE, c2.getLong());
        Assert.assertFalse(c2.isNamedParamGetter());
        Assert.assertEquals((short) 0, c2.getParam1());
        Assert.assertEquals((short) 0, c2.getParam2());
        Assert.assertEquals(0f, c2.getFloat(), 0.00001f);
        Assert.assertEquals(0, c2.getInt());
        Assert.assertEquals(0, c2.getSuperValue());
        Assert.assertEquals(ComplexComponent.TestEnum.V1, c2.getEnum());
    }

    @Test
    public void testValidation() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();

        ObjectComponent nullTest = e.add(ObjectComponent.class);
        FloatComponent withinTest = e.add(FloatComponent.class);
        ComplexComponent validTest = e.add(ComplexComponent.class);

        try {
            nullTest.setObject(null);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException ne) {
            // expected
        }
        nullTest.setObject(new ObjectComponent.FooBlah());

        try {
            withinTest.setFloat(-56);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ie) {
            // expected
        }
        try {
            withinTest.setFloat(5000);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ie) {
            // expected
        }
        withinTest.setFloat(0);

        try {
            validTest.setParams((short) 10, (short) 1);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ie) {
            // expected
        }
        validTest.setParams((short) 1, (short) 10);
    }

    @Test
    public void testFlyweightIsAliveAfterRemoval() {
        EntitySystem system = EntitySystem.Factory.create();

        IntComponent c1 = system.addEntity().add(IntComponent.class);
        IntComponent c2 = system.addEntity().add(IntComponent.class);

        ComponentIterator it = system.fastIterator();
        IntComponent flyweight = it.addRequired(IntComponent.class);

        int count = 0;
        while (it.next()) {
            flyweight.getEntity().remove(IntComponent.class);
            Assert.assertFalse(flyweight.isAlive());
            count++;
        }

        Assert.assertFalse(c1.isAlive());
        Assert.assertFalse(c2.isAlive());
        Assert.assertEquals(2, count);
    }
}
