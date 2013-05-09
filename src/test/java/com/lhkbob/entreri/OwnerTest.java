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

import com.lhkbob.entreri.component.FloatComponent;
import com.lhkbob.entreri.component.IntComponent;
import junit.framework.Assert;
import org.junit.Test;

public class OwnerTest {

    @Test
    public void testEntitySetOwner() {
        EntitySystem system = new EntitySystem();

        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();

        Assert.assertNull(e1.getOwner());
        Assert.assertNull(e2.getOwner());

        e1.setOwner(e2);

        Assert.assertSame(e2, e1.getOwner());
        Assert.assertNull(e2.getOwner());

        IntComponent c2 = e2.add(IntComponent.class);
        e1.setOwner(c2);

        Assert.assertSame(c2, e1.getOwner());
    }

    @Test
    public void testComponentSetOwner() {
        EntitySystem system = new EntitySystem();

        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();

        IntComponent c1a = e1.add(IntComponent.class);
        FloatComponent c1b = e1.add(FloatComponent.class);

        Assert.assertNull(c1a.getOwner());
        Assert.assertNull(c1b.getOwner());

        c1a.setOwner(c1b);

        Assert.assertSame(c1b, c1a.getOwner());
        Assert.assertNull(c1b.getOwner());

        c1a.setOwner(e2);

        Assert.assertSame(e2, c1a.getOwner());
    }

    @Test
    public void testOwnedEntityRemoval() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();

        final boolean[] revoked = new boolean[1];

        e1.setOwner(new Owner() {
            @Override
            public Owner notifyOwnershipGranted(Ownable obj) {
                return this;
            }

            @Override
            public void notifyOwnershipRevoked(Ownable obj) {
                revoked[0] = true;
            }
        });

        system.removeEntity(e1);
        Assert.assertTrue(revoked[0]);
    }

    @Test
    public void testOwnedComponentRemoval() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();
        IntComponent c1 = e1.add(IntComponent.class);

        final boolean[] revoked = new boolean[1];

        c1.setOwner(new Owner() {
            @Override
            public Owner notifyOwnershipGranted(Ownable obj) {
                return this;
            }

            @Override
            public void notifyOwnershipRevoked(Ownable obj) {
                revoked[0] = true;
            }
        });

        e1.remove(IntComponent.class);
        Assert.assertTrue(revoked[0]);
    }

    @Test
    public void testOwnedComponentAdd() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();

        IntComponent c1 = e1.add(IntComponent.class);

        final boolean[] revoked = new boolean[1];

        c1.setOwner(new Owner() {
            @Override
            public Owner notifyOwnershipGranted(Ownable obj) {
                return this;
            }

            @Override
            public void notifyOwnershipRevoked(Ownable obj) {
                revoked[0] = true;
            }
        });

        IntComponent c2 = e1.add(IntComponent.class);
        Assert.assertTrue(revoked[0]);
        Assert.assertNull(c2.getOwner());
    }

    @Test
    public void testEntityRemovalCleanup() {
        EntitySystem system = new EntitySystem();

        Entity owner = system.addEntity();

        Entity spare = system.addEntity();
        IntComponent ownedC = spare.add(IntComponent.class);
        ownedC.setOwner(owner);

        Entity ownedE = system.addEntity();
        ownedE.setOwner(owner);

        system.removeEntity(owner);
        Assert.assertFalse(ownedC.isAlive());
        Assert.assertFalse(ownedE.isAlive());
        Assert.assertTrue(spare.isAlive());
    }

    @Test
    public void testComponentRemovalCleanup() {
        EntitySystem system = new EntitySystem();

        Entity e1 = system.addEntity();

        IntComponent owner = e1.add(IntComponent.class);
        FloatComponent ownedC = e1.add(FloatComponent.class);
        ownedC.setOwner(owner);

        Entity ownedE = system.addEntity();
        ownedE.setOwner(owner);

        e1.remove(IntComponent.class);
        Assert.assertFalse(ownedC.isAlive());
        Assert.assertFalse(ownedE.isAlive());
    }

    @Test
    public void testComplexOwnershipHierarchyCleanup() {
        EntitySystem system = new EntitySystem();

        Entity e1 = system.addEntity();
        IntComponent c1 = e1.add(IntComponent.class);

        Entity e2 = system.addEntity();
        IntComponent c2 = e2.add(IntComponent.class);

        Entity e3 = system.addEntity();
        IntComponent c3 = e3.add(IntComponent.class);

        e1.setOwner(e2);
        e2.setOwner(e3);
        e3.setOwner(c1);
        c1.setOwner(c2);
        c2.setOwner(c3);

        e3.remove(IntComponent.class);

        Assert.assertFalse(e1.isAlive());
        Assert.assertFalse(e2.isAlive());
        Assert.assertFalse(e3.isAlive());
        Assert.assertFalse(c1.isAlive());
        Assert.assertFalse(c2.isAlive());
        Assert.assertFalse(c3.isAlive());
    }

    @Test
    public void testComponentOwningParentEntityRemoval() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(IntComponent.class);

        e.setOwner(c);

        system.removeEntity(e);
        Assert.assertFalse(e.isAlive());
        Assert.assertFalse(c.isAlive());
    }

    @Test
    public void testFlyweightComponentOwnership() {
        Assert.fail();
    }
}
