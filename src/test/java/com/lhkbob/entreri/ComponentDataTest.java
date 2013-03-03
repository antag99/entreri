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

import com.lhkbob.entreri.component.IntComponent;
import com.lhkbob.entreri.component.OnSetComponent;
import junit.framework.Assert;
import org.junit.Test;

public class ComponentDataTest {
    @Test
    public void testIsValidComponentRemove() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(IntComponent.class);
        IntComponent cd = c.getData();

        Assert.assertTrue(cd.isValid()); // sanity check
        e.remove(IntComponent.class);
        Assert.assertFalse(cd.isValid());
    }

    @Test
    public void testIsValidEntityRemove() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(IntComponent.class);
        IntComponent cd = c.getData();

        Assert.assertTrue(cd.isValid()); // sanity check
        system.removeEntity(e);
        Assert.assertFalse(cd.isValid());
    }

    @Test
    public void testIsValidCompact() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();
        Entity e3 = system.addEntity();

        e1.add(IntComponent.class); // removed
        e2.add(IntComponent.class); // will shift over
        e3.add(IntComponent.class); // will shift over
        IntComponent cd = e2.get(IntComponent.class).getData();

        Assert.assertTrue(cd.isValid()); // sanity check

        e1.remove(IntComponent.class);
        system.compact(); // since e1's component was moved, this shifts e2

        Assert.assertFalse(cd.isValid());
    }

    @Test
    public void testIsValid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(IntComponent.class);
        IntComponent cd = c.getData();

        Assert.assertTrue(cd.isValid()); // sanity check
    }

    @Test
    public void testIsValidNoopCompact() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();

        e1.add(IntComponent.class);
        e2.add(IntComponent.class);
        IntComponent cd = e2.get(IntComponent.class).getData();

        Assert.assertTrue(cd.isValid()); // sanity check
        system.compact(); // no changes
        Assert.assertTrue(cd.isValid());
    }

    @Test
    public void testSetValid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(IntComponent.class);

        IntComponent cd = system.createDataInstance(IntComponent.class);
        Assert.assertFalse(cd.isValid());
        Assert.assertTrue(cd.set(c));
        Assert.assertTrue(cd.isValid());
    }

    @Test
    public void testSetInvalid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();

        Component<IntComponent> c = e.add(IntComponent.class);
        IntComponent cd = system.createDataInstance(IntComponent.class);

        e.remove(IntComponent.class);

        Assert.assertFalse(cd.set(c));
        Assert.assertFalse(cd.isValid());
        Assert.assertFalse(cd.set(null));
        Assert.assertFalse(cd.isValid());

        cd.set(e.add(IntComponent.class));
        Assert.assertTrue(cd.isValid());
    }

    @Test
    public void testOnSetInvoked() {
        // must test both public set() and default setFast()
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        Component<OnSetComponent> c = e.add(OnSetComponent.class);
        OnSetComponent cd = system.createDataInstance(OnSetComponent.class);

        // sanity checks (onset gets called once during initialization)
        Assert.assertEquals(0, cd.onsetIndex);
        Assert.assertTrue(cd.onsetCalled);

        // reset
        cd.onsetCalled = false;
        cd.onsetIndex = 0;

        // trigger a regular set() call
        cd.set(c);
        Assert.assertEquals(c.index, cd.onsetIndex);
        Assert.assertEquals(cd.getIndex(), cd.onsetIndex);
        Assert.assertTrue(cd.onsetCalled);

        // reset
        cd.onsetCalled = false;
        cd.onsetIndex = 0;

        // trigger a setFast() call
        e.get(cd);
        Assert.assertEquals(c.index, cd.onsetIndex);
        Assert.assertEquals(cd.getIndex(), cd.onsetIndex);
        Assert.assertTrue(cd.onsetCalled);
    }

    @Test
    public void testVersionUpdate() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent cd = e.add(IntComponent.class).getData();

        Assert.assertEquals(0, cd.getVersion());
        cd.updateVersion();
        Assert.assertEquals(1, cd.getVersion());
    }

    @Test
    public void testUniqueVersionUpdate() {
        EntitySystem system = new EntitySystem();
        IntComponent cd1 = system.addEntity().add(IntComponent.class).getData();
        IntComponent cd2 = system.addEntity().add(IntComponent.class).getData();

        Assert.assertEquals(0, cd1.getVersion());
        Assert.assertEquals(1, cd2.getVersion());

        cd1.updateVersion();
        cd2.updateVersion();

        Assert.assertEquals(2, cd1.getVersion());
        Assert.assertEquals(3, cd2.getVersion());
    }

    @Test
    public void testInvalidComponentVersion() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(IntComponent.class);

        // sanity check
        Assert.assertEquals(0, cd.getIndex());

        int oldVersion = cd.getVersion();
        Assert.assertTrue(oldVersion < 0);
        cd.updateVersion();
        Assert.assertEquals(oldVersion, cd.getVersion());
    }

    @Test
    public void testInvalidComponentEnabled() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(IntComponent.class);

        // sanity check
        Assert.assertEquals(0, cd.getIndex());

        Assert.assertFalse(cd.isEnabled());
        cd.setEnabled(true);
        Assert.assertFalse(cd.isEnabled());
    }
}
