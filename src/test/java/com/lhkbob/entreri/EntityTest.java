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

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.lhkbob.entreri.component.FloatComponent;
import com.lhkbob.entreri.component.IntComponent;
import com.lhkbob.entreri.component.ObjectComponent;

public class EntityTest {
    @Test
    public void testGetEntitySystem() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Assert.assertEquals(system, e.getEntitySystem());
    }

    @Test
    public void testAddRemoveComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));

        c.getData().setInt(1);
        Assert.assertEquals(1, c.getData().getInt());

        Assert.assertEquals(c, e.get(TypeId.get(IntComponent.class)));
        Assert.assertEquals(1, e.get(TypeId.get(IntComponent.class)).getData().getInt());

        Assert.assertTrue(e.remove(TypeId.get(IntComponent.class)));

        Assert.assertNull(e.get(TypeId.get(IntComponent.class)));
        Assert.assertNull(e.get(TypeId.get(FloatComponent.class)));

        Assert.assertFalse(c.isLive());
        Assert.assertFalse(e.get(system.createDataInstance(TypeId.get(IntComponent.class))));
    }

    @Test
    public void testReAddComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        Component<IntComponent> c2 = e.add(TypeId.get(IntComponent.class));

        Assert.assertNotSame(c, c2);
        Assert.assertFalse(c.isLive());
        Assert.assertTrue(c2.isLive());
        Assert.assertSame(c2, e.get(TypeId.get(IntComponent.class)));
    }

    @Test
    public void testGetComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        c.getData().setInt(2);

        int count = 0;
        for (Entity e2 : system) {
            Assert.assertSame(e, e2);
            Assert.assertSame(c, e2.get(TypeId.get(IntComponent.class)));
            Assert.assertEquals(2, e2.get(TypeId.get(IntComponent.class)).getData()
                                     .getInt());
            count++;
        }

        Assert.assertEquals(1, count);
    }

    @Test
    public void testDisabledComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        c.setEnabled(false);

        Assert.assertNull(e.get(TypeId.get(IntComponent.class)));
        Assert.assertSame(c, e.get(TypeId.get(IntComponent.class), true));

        // test removing a disabled component
        Assert.assertTrue(e.remove(TypeId.get(IntComponent.class)));
    }

    @Test
    public void testGetComponentData() {
        EntitySystem system = new EntitySystem();

        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();

        IntComponent data = system.createDataInstance(TypeId.get(IntComponent.class));

        Assert.assertTrue(data.set(e1.add(TypeId.get(IntComponent.class))));
        data.setInt(1);

        Assert.assertTrue(data.set(e2.add(TypeId.get(IntComponent.class))));
        data.setInt(2);

        Assert.assertTrue(e1.get(data));
        Assert.assertEquals(1, data.getInt());
        Assert.assertTrue(e2.get(data));
        Assert.assertEquals(2, data.getInt());

        // now test disabled'ness
        e1.get(TypeId.get(IntComponent.class)).setEnabled(false);
        Assert.assertFalse(e1.get(data));
        Assert.assertTrue(data.isValid());
        Assert.assertFalse(data.isEnabled());
    }

    @Test
    public void testIterateComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        Component<IntComponent> ic = e.add(TypeId.get(IntComponent.class));
        Component<FloatComponent> fc = e.add(TypeId.get(FloatComponent.class));

        e.add(TypeId.get(ObjectComponent.class)).setEnabled(false);

        boolean intFound = false;
        boolean floatFound = false;
        for (Component<?> c : e) {
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
    public void testIterateDisabledComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        Component<IntComponent> ic = e.add(TypeId.get(IntComponent.class));
        Component<FloatComponent> fc = e.add(TypeId.get(FloatComponent.class));

        Component<ObjectComponent> oc = e.add(TypeId.get(ObjectComponent.class));
        oc.setEnabled(false);

        boolean intFound = false;
        boolean floatFound = false;
        boolean objFound = false;
        Iterator<Component<?>> it = e.iterator(true);
        while (it.hasNext()) {
            Component<?> c = it.next();
            if (ic == c) {
                Assert.assertFalse(intFound);
                intFound = true;
            } else if (fc == c) {
                Assert.assertFalse(floatFound);
                floatFound = true;
            } else if (oc == c) {
                Assert.assertFalse(objFound);
                objFound = true;
            } else {
                Assert.fail();
            }
        }

        Assert.assertTrue(intFound);
        Assert.assertTrue(floatFound);
        Assert.assertTrue(objFound);
    }

    @Test
    public void testIterateRemoveComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        Component<IntComponent> ic = e.add(TypeId.get(IntComponent.class));
        Component<FloatComponent> fc = e.add(TypeId.get(FloatComponent.class));

        Iterator<Component<?>> it = e.iterator();
        while (it.hasNext()) {
            Component<?> c = it.next();
            if (c.getTypeId() == TypeId.get(IntComponent.class)) {
                Assert.assertSame(ic, c);
                it.remove();

                Assert.assertNull(c.getEntity());
                Assert.assertEquals(0, c.index);
            } else {
                Assert.assertSame(fc, c);
            }
        }

        Assert.assertNull(e.get(TypeId.get(IntComponent.class)));
        Assert.assertNotNull(e.get(TypeId.get(FloatComponent.class)));
        Assert.assertFalse(ic.isLive());
        Assert.assertFalse(e.get(system.createDataInstance(TypeId.get(IntComponent.class))));
    }
}
