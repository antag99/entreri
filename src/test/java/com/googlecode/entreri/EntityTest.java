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

import com.googlecode.entreri.ComponentData;
import com.googlecode.entreri.Entity;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.InitParamsComponent;
import com.googlecode.entreri.component.IntComponent;

public class EntityTest {
    @Test
    public void testGetEntitySystem() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Assert.assertEquals(system, e.getEntitySystem());
    }
    
    @Test
    public void testValidInitParams() {
        TypeId<InitParamsComponent> id = ComponentData.getTypedId(InitParamsComponent.class);
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        // First test a simple add
        Object val = new Object();
        InitParamsComponent c = e.add(id, 4f, val);
        Assert.assertEquals(4f, c.getFloat(), .0001f);
        Assert.assertEquals(val, c.getObject());
    }
    
    @Test
    public void testBadInitParams() {
        // empty args when expected some
        doTestBadInitParams(ComponentData.getTypedId(InitParamsComponent.class), new Object[0], new Object[] {4f, new Object()});
        // not enough args when expected some
        doTestBadInitParams(ComponentData.getTypedId(InitParamsComponent.class), new Object[] {5f}, new Object[] {4f, new Object()});
        // swapped type list
        doTestBadInitParams(ComponentData.getTypedId(InitParamsComponent.class), new Object[] {new Object(), 5f}, new Object[] {4f, new Object()});
        // bad type list
        doTestBadInitParams(ComponentData.getTypedId(InitParamsComponent.class), new Object[] {"", 2}, new Object[] {4f, new Object()});
        // parameters when none are expected
        doTestBadInitParams(ComponentData.getTypedId(FloatComponent.class), new Object[] { 4f, 3f }, new Object[0]);
    }
    
    private void doTestBadInitParams(TypeId<? extends ComponentData> id, Object[] use, Object[] valid) {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        try {
            e.add(id, use);
            Assert.fail();
        } catch(IllegalArgumentException iae) {
            // expected
        }
        
        // verify that this still works
        ComponentData c = e.add(id, valid);
        Iterator<? extends ComponentData> it = system.iterator(id);
        while(it.hasNext()) {
            Assert.assertEquals(c, it.next());
            break;
        }
        Assert.assertFalse(it.hasNext());
    }
    
    @Test
    public void testComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        IntComponent c = e.add(ComponentData.getTypedId(IntComponent.class));
        
        c.setInt(0, 1);
        Assert.assertEquals(1, c.getInt(0));
        
        Assert.assertEquals(c, e.get(ComponentData.getTypedId(IntComponent.class)));
        Assert.assertEquals(1, e.get(ComponentData.getTypedId(IntComponent.class)).getInt(0));
        
        e.remove(ComponentData.getTypedId(IntComponent.class));
        
        Assert.assertNull(e.get(ComponentData.getTypedId(IntComponent.class)));
        Assert.assertNull(e.get(ComponentData.getTypedId(FloatComponent.class)));
    }
    
    @Test
    public void testGetComponentFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        IntComponent c = e.add(ComponentData.getTypedId(IntComponent.class));
        c.setInt(0, 2);
        
        int count = 0;
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            Entity e2 = it.next();
            Assert.assertEquals(2, e2.get(ComponentData.getTypedId(IntComponent.class)).getInt(0));
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
            IntComponent c = it.next().add(ComponentData.getTypedId(IntComponent.class));
            c.setInt(0, 3);
        }
        
        Assert.assertEquals(3, e.get(ComponentData.getTypedId(IntComponent.class)).getInt(0));
    }
    
    @Test
    public void testRemoveComponentFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        e.add(ComponentData.getTypedId(IntComponent.class));

        
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            Entity e2 = it.next();
            Assert.assertNotNull(e2.get(ComponentData.getTypedId(IntComponent.class)));
            e2.remove(ComponentData.getTypedId(IntComponent.class));
        }
        
        Assert.assertNull(e.get(ComponentData.getTypedId(IntComponent.class)));
    }
    
    @Test
    public void testIterateComponents() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        e.add(ComponentData.getTypedId(IntComponent.class));
        e.add(ComponentData.getTypedId(FloatComponent.class));
        
        boolean intFound = false;
        for(ComponentData c: e) {
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
        e.add(ComponentData.getTypedId(IntComponent.class));
        e.add(ComponentData.getTypedId(FloatComponent.class));
        
        Iterator<ComponentData> it = e.iterator();
        while(it.hasNext()) {
            ComponentData c = it.next();
            if (c instanceof IntComponent) {
                it.remove();
                
                Assert.assertNull(c.getEntity());
                Assert.assertEquals(0, c.getIndex());
            }
        }
        
        Assert.assertNull(e.get(ComponentData.getTypedId(IntComponent.class)));
        Assert.assertNotNull(e.get(ComponentData.getTypedId(FloatComponent.class)));
    }
}
