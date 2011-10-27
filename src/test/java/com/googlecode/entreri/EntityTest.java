/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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
package com.googlecode.entreri;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.Entity;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.IntComponent;

public class EntityTest {
    @Test
    public void testGetEntitySystem() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Assert.assertEquals(system, e.getEntitySystem());
    }
    
    @Test
    public void testComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        IntComponent c = e.add(Component.getTypedId(IntComponent.class));
        
        c.setInt(0, 1);
        Assert.assertEquals(1, c.getInt(0));
        
        Assert.assertEquals(c, e.get(Component.getTypedId(IntComponent.class)));
        Assert.assertEquals(1, e.get(Component.getTypedId(IntComponent.class)).getInt(0));
        
        e.remove(Component.getTypedId(IntComponent.class));
        
        Assert.assertNull(e.get(Component.getTypedId(IntComponent.class)));
        Assert.assertNull(e.get(Component.getTypedId(FloatComponent.class)));
    }
    
    @Test
    public void testGetComponentFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        IntComponent c = e.add(Component.getTypedId(IntComponent.class));
        c.setInt(0, 2);
        
        int count = 0;
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            Entity e2 = it.next();
            Assert.assertEquals(2, e2.get(Component.getTypedId(IntComponent.class)).getInt(0));
            count++;
        }
        
        Assert.assertEquals(1, count);
    }
    
    @Test
    public void testAddComponentFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            IntComponent c = it.next().add(Component.getTypedId(IntComponent.class));
            c.setInt(0, 3);
        }
        
        Assert.assertEquals(3, e.get(Component.getTypedId(IntComponent.class)).getInt(0));
    }
    
    @Test
    public void testRemoveComponentFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        e.add(Component.getTypedId(IntComponent.class));

        
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            Entity e2 = it.next();
            Assert.assertNotNull(e2.get(Component.getTypedId(IntComponent.class)));
            e2.remove(Component.getTypedId(IntComponent.class));
        }
        
        Assert.assertNull(e.get(Component.getTypedId(IntComponent.class)));
    }
    
    @Test
    public void testIterateComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        e.add(Component.getTypedId(IntComponent.class));
        e.add(Component.getTypedId(FloatComponent.class));
        
        boolean intFound = false;
        for(Component c: e) {
            if (intFound) {
                Assert.assertTrue(c instanceof FloatComponent);
            } else {
                Assert.assertTrue(c instanceof IntComponent || c instanceof FloatComponent);
                if (c instanceof IntComponent)
                    intFound = true;
            }
        }
        
        Assert.assertTrue(intFound);
    }
    
    @Test
    public void testIterateRemoveComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        e.add(Component.getTypedId(IntComponent.class));
        e.add(Component.getTypedId(FloatComponent.class));
        
        Iterator<Component> it = e.iterator();
        while(it.hasNext()) {
            Component c = it.next();
            if (c instanceof IntComponent) {
                it.remove();
                
                Assert.assertNull(c.getEntity());
                Assert.assertEquals(0, c.getIndex());
            }
        }
        
        Assert.assertNull(e.get(Component.getTypedId(IntComponent.class)));
        Assert.assertNotNull(e.get(Component.getTypedId(FloatComponent.class)));
    }
}
