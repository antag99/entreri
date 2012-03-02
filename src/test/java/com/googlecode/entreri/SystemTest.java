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
package com.googlecode.entreri;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.component.CustomFactoryComponent;
import com.googlecode.entreri.component.CustomFactoryComponent.CustomFactory;
import com.googlecode.entreri.component.DefaultFactoryComponent;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.IntComponent;
import com.googlecode.entreri.component.MultiPropertyComponent;

public class SystemTest {
    @Test
    public void testCustomFactory() {
        EntitySystem system = new EntitySystem();
        system.setFactory(TypeId.get(CustomFactoryComponent.class), new CustomFactory());
        
        // the default reflection factory will fail to create an instance
        // because the property is public. If it is created, and it's not null
        // we know the custom factory worked
        CustomFactoryComponent cd = system.createDataInstance(TypeId.get(CustomFactoryComponent.class));
        Assert.assertNotNull(cd.prop);
    }
    
    @Test
    public void testDefaultFactoryOverride() {
        EntitySystem system = new EntitySystem();
        
        // the default reflection factory will fail to create an instance
        // because the property is public. If it is created, and it's not null
        // we know the custom factory worked
        DefaultFactoryComponent cd = system.createDataInstance(TypeId.get(DefaultFactoryComponent.class));
        Assert.assertNotNull(cd.prop);
    }
    
    @Test
    public void testAddEntity() {
        // There really isn't much to test with this one, everything else
        // is validated by other tests in this package
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        int componentCount = 0;
        for (@SuppressWarnings("unused") Component<?> c: e) {
            componentCount++;
        }
        
        Assert.assertEquals(0, componentCount);
        Assert.assertEquals(system, e.getEntitySystem());
        Assert.assertTrue(e.isLive());
        
        int entityCount = 0;
        for (Entity entity: system) {
            entityCount++;
            Assert.assertSame(e, entity);
        }
        Assert.assertEquals(1, entityCount);
    }
    
    @Test
    public void testAddEntityFromTemplate() {
        EntitySystem system = new EntitySystem();
        Entity template = system.addEntity();
        
        Component<IntComponent> tc1 = template.add(TypeId.get(IntComponent.class));
        tc1.getData().setInt(0, 2);
        Component<FloatComponent> tc2 = template.add(TypeId.get(FloatComponent.class));
        tc2.getData().setFloat(0, 3f);
        
        Entity fromTemplate = system.addEntity(template);
        Component<IntComponent> c1 = fromTemplate.get(TypeId.get(IntComponent.class));
        Component<FloatComponent> c2 = fromTemplate.get(TypeId.get(FloatComponent.class));
        
        Assert.assertEquals(2, c1.getData().getInt(0));
        Assert.assertEquals(3f, c2.getData().getFloat(0), .0001f);
        Assert.assertNotSame(c1, tc1);
        Assert.assertNotSame(c2, tc2);
        Assert.assertNotSame(template, fromTemplate);
    }
    
    @Test
    public void testAddEntityFromTemplateInAnotherSystem() {
        EntitySystem systemTemplate = new EntitySystem();
        Entity template = systemTemplate.addEntity();
        
        EntitySystem system = new EntitySystem();
        try {
            system.addEntity(template);
            Assert.fail();
        } catch(IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testRemoveEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        
        system.removeEntity(e);
        Assert.assertFalse(e.isLive());
        Assert.assertFalse(c.isLive());
        Assert.assertEquals(1, e.getId()); // it's id should remain unchanged
        
        Assert.assertFalse(system.iterator().hasNext());
    }
    
    @Test
    public void testCompactNoOp() {
        EntitySystem system = new EntitySystem();
        for (int i = 0; i < 5; i++) 
            system.addEntity().add(TypeId.get(MultiPropertyComponent.class));
        
        system.compact();
        
        int count = 0;
        Iterator<Entity> it = system.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            MultiPropertyComponent m = e.get(TypeId.get(MultiPropertyComponent.class)).getData();
            Assert.assertTrue(m.getCompactProperty().wasCompacted());
            count++;
        }
        
        Assert.assertEquals(5, count);
    }
    
    @Test
    public void testCompactRepairRemoves() {
        EntitySystem system = new EntitySystem();
        List<Entity> es = new ArrayList<Entity>();
        List<Float> cs = new ArrayList<Float>();
        for (int i = 0; i < 100; i++) {
            es.add(system.addEntity());
            MultiPropertyComponent c = es.get(es.size() - 1).add(TypeId.get(MultiPropertyComponent.class)).getData();
            float f = (float) Math.random();
            float f2 = (float) Math.random();
            c.setFloat(f);
            c.setFactoryFloat(f2);
            
            cs.add(f);
            cs.add(f2);
        }
        
        int i = 0;
        Iterator<Entity> it = es.iterator();
        Iterator<Float> ft = cs.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            ft.next(); // always advance once
            if (i % 2 == 0) {
                it.remove();
                system.removeEntity(e);
                
                ft.remove();
                ft.next(); ft.remove(); // remove 2nd element
            } else {
                ft.next(); // advance past 2nd element
            }
            i++;
        }
        
        system.compact();
        
        it = es.iterator();
        Iterator<Entity> si = system.iterator();
        ft = cs.iterator();
        while(it.hasNext() && si.hasNext()) {
            Entity e = si.next();
            Assert.assertEquals(it.next(), e);
            Assert.assertEquals(ft.next().floatValue(), e.get(TypeId.get(MultiPropertyComponent.class)).getData().getFloat(), .0001f);
            Assert.assertEquals(ft.next().floatValue(), e.get(TypeId.get(MultiPropertyComponent.class)).getData().getFactoryFloat(), .0001f);
        }
        Assert.assertFalse(it.hasNext());
        Assert.assertFalse(si.hasNext());
    }
    
    @Test
    public void testCompactAddRemoveRepair() {
        EntitySystem system = new EntitySystem();
        List<Entity> es = new ArrayList<Entity>();
        List<Float> cs = new ArrayList<Float>();
        for (int i = 0; i < 100; i++) {
            es.add(system.addEntity());
            MultiPropertyComponent c = es.get(es.size() - 1).add(TypeId.get(MultiPropertyComponent.class)).getData();
            float f = (float) Math.random();
            float f2 = (float) Math.random();
            c.setFloat(f);
            c.setFactoryFloat(f2);
            
            cs.add(f);
            cs.add(f2);
        }
        
        // remove a bunch of components from the entities
        int i = 0;
        Iterator<Entity> it = es.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            if (i % 2 == 0) {
                e.remove(TypeId.get(MultiPropertyComponent.class));
            } 
            i++;
        }
        
        // now add back in component values for the previously removed entities
        i = 0;
        it = es.iterator();
        Iterator<Float> ft = cs.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            Component<MultiPropertyComponent> c = e.get(TypeId.get(MultiPropertyComponent.class));
            
            float f = ft.next();
            float f2 = ft.next();
            
            if (c == null) {
                c = e.add(TypeId.get(MultiPropertyComponent.class));
                c.getData().setFloat(f);
                c.getData().setFactoryFloat(f2);
            }
            i++;
        }
        
        system.compact();
        
        it = es.iterator();
        Iterator<Entity> si = system.iterator();
        ft = cs.iterator();
        while(it.hasNext() && si.hasNext()) {
            Entity e = si.next();
            Assert.assertEquals(it.next(), e);
            Assert.assertEquals(ft.next().floatValue(), e.get(TypeId.get(MultiPropertyComponent.class)).getData().getFloat(), .0001f);
            Assert.assertEquals(ft.next().floatValue(), e.get(TypeId.get(MultiPropertyComponent.class)).getData().getFactoryFloat(), .0001f);
        }
        Assert.assertFalse(it.hasNext());
        Assert.assertFalse(si.hasNext());
    }
}
