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

import org.junit.Assert;
import org.junit.Test;

import com.lhkbob.entreri.component.IntComponent;

public class IteratorTest {
    @Test
    public void testDisabledComponents() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));

        Entity e1 = system.addEntity();
        e1.add(TypeId.get(IntComponent.class)).setEnabled(true);
        Entity e2 = system.addEntity();
        e2.add(TypeId.get(IntComponent.class)).setEnabled(false);

        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(cd);
        it.reset();

        int count = 0;
        while(it.next()) {
            count++;
            Assert.assertSame(e1, cd.getEntity());
        }
        Assert.assertEquals(1, count);
    }

    @Test
    public void testIgnoreEnabledComponents() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));

        Entity e1 = system.addEntity();
        e1.add(TypeId.get(IntComponent.class)).setEnabled(true);
        Entity e2 = system.addEntity();
        e2.add(TypeId.get(IntComponent.class)).setEnabled(false);

        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(cd)
        .setIgnoreEnabled(true)
        .reset();

        boolean hasE1 = false;
        boolean hasE2 = false;
        while(it.next()) {
            if (e1 == cd.getEntity()) {
                Assert.assertFalse(hasE1);
                hasE1 = true;
            } else if (e2 == cd.getEntity()) {
                Assert.assertFalse(hasE2);
                hasE2 = true;
            } else {
                Assert.fail();
            }
        }
        Assert.assertTrue(hasE1);
        Assert.assertTrue(hasE2);
    }
}
