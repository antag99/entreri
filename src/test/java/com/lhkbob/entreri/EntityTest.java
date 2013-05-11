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

import com.lhkbob.entreri.components.FloatComponent;
import com.lhkbob.entreri.components.IntComponent;
import com.lhkbob.entreri.components.RequiresAComponent;
import com.lhkbob.entreri.components.RequiresBComponent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class EntityTest {
    @Test
    public void testGetEntitySystem() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Assert.assertEquals(system, e.getEntitySystem());
    }

    @Test
    public void testAddWithRequiredComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        RequiresBComponent rb = e.add(RequiresBComponent.class);
        RequiresAComponent ra = e.get(RequiresAComponent.class);
        Assert.assertNotNull(ra);
        Assert.assertNotNull(e.get(IntComponent.class));
        Assert.assertNotNull(e.get(FloatComponent.class));

        Assert.assertSame(rb, ra.getOwner());
        Assert.assertSame(ra, e.get(IntComponent.class).getOwner());
        Assert.assertSame(ra, e.get(FloatComponent.class).getOwner());

        e.remove(RequiresBComponent.class);

        Assert.assertNull(e.get(RequiresAComponent.class));
        Assert.assertNull(e.get(IntComponent.class));
        Assert.assertNull(e.get(FloatComponent.class));
    }

    @Test
    public void testAddWithRequiredComponentsAlreadyPresent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        IntComponent ci = e.add(IntComponent.class);

        RequiresAComponent ra = e.add(RequiresAComponent.class);
        Assert.assertSame(ci, e.get(IntComponent.class));
        Assert.assertNotNull(e.get(FloatComponent.class));

        Assert.assertNull(ci.getOwner());
        Assert.assertSame(ra, e.get(FloatComponent.class).getOwner());

        e.remove(RequiresAComponent.class);

        Assert.assertSame(ci, e.get(IntComponent.class));
        Assert.assertNull(e.get(FloatComponent.class));
    }

    @Test
    public void testAddRemoveComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);

        c.setInt(1);
        Assert.assertEquals(1, c.getInt());

        Assert.assertEquals(c, e.get(IntComponent.class));
        Assert.assertEquals(1, e.get(IntComponent.class).getInt());

        Assert.assertTrue(e.remove(IntComponent.class));

        Assert.assertNull(e.get(IntComponent.class));
        Assert.assertNull(e.get(FloatComponent.class));

        Assert.assertFalse(c.isAlive());
    }

    @Test
    public void testReAddComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);
        IntComponent c2 = e.add(IntComponent.class);

        Assert.assertNotSame(c, c2);
        Assert.assertFalse(c.isAlive());
        Assert.assertTrue(c2.isAlive());
        Assert.assertSame(c2, e.get(IntComponent.class));
    }

    @Test
    public void testGetComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        IntComponent c = e.add(IntComponent.class);
        c.setInt(2);

        int count = 0;
        for (Entity e2 : system) {
            Assert.assertSame(e, e2);
            Assert.assertSame(c, e2.get(IntComponent.class));
            Assert.assertEquals(2, e2.get(IntComponent.class).getInt());
            count++;
        }

        Assert.assertEquals(1, count);
    }

    @Test
    public void testIterateComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent ic = e.add(IntComponent.class);
        FloatComponent fc = e.add(FloatComponent.class);

        boolean intFound = false;
        boolean floatFound = false;
        for (Component c : e) {
            if (ic == c) {
                Assert.assertFalse(intFound);
                intFound = true;
            } else if (fc == c) {
                Assert.assertFalse(floatFound);
                floatFound = true;
            } else {
                Assert.fail();
            }
        }

        Assert.assertTrue(intFound);
        Assert.assertTrue(floatFound);
    }

    @Test
    public void testIterateRemoveComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent ic = e.add(IntComponent.class);
        FloatComponent fc = e.add(FloatComponent.class);

        Iterator<Component> it = e.iterator();
        while (it.hasNext()) {
            Component c = it.next();
            if (c.getType().equals(IntComponent.class)) {
                Assert.assertSame(ic, c);
                it.remove();

                Assert.assertNull(c.getEntity());
                Assert.assertEquals(0, c.getIndex());
            } else {
                Assert.assertSame(fc, c);
            }
        }

        Assert.assertNull(e.get(IntComponent.class));
        Assert.assertNotNull(e.get(FloatComponent.class));
        Assert.assertFalse(ic.isAlive());
    }
}
