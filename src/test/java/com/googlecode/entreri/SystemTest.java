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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.Entity;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.IntComponent;
import com.googlecode.entreri.component.MultiPropertyComponent;

public class SystemTest {
    @Test
    public void testAddEntity() {
        // There really isn't much to test with this one, everything else
        // is validated by other tests in this package
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        int componentCount = 0;
        for (@SuppressWarnings("unused") Component c: e) {
            componentCount++;
        }
        
        Assert.assertEquals(0, componentCount);
        Assert.assertEquals(system, e.getEntitySystem());
        Assert.assertTrue(e.isLive());
    }
    
    @Test
    public void testAddEntityFromTemplate() {
        EntitySystem system = new EntitySystem();
        Entity template = system.addEntity();
        
        IntComponent tc1 = template.add(Component.getTypedId(IntComponent.class));
        tc1.setInt(0, 2);
        FloatComponent tc2 = template.add(Component.getTypedId(FloatComponent.class));
        tc2.setFloat(0, 3f);
        
        Entity fromTemplate = system.addEntity(template);
        IntComponent c1 = fromTemplate.get(Component.getTypedId(IntComponent.class));
        FloatComponent c2 = fromTemplate.get(Component.getTypedId(FloatComponent.class));
        
        Assert.assertEquals(2, c1.getInt(0));
        Assert.assertEquals(3f, c2.getFloat(0), .0001f);
        Assert.assertNotSame(template, fromTemplate);
    }
    
    @Test
    public void testAddEntityFromTemplateInAnotherSystem() {
        EntitySystem systemTemplate = new EntitySystem();
        Entity template = systemTemplate.addEntity();
        
        IntComponent tc1 = template.add(Component.getTypedId(IntComponent.class));
        tc1.setInt(0, 2);
        FloatComponent tc2 = template.add(Component.getTypedId(FloatComponent.class));
        tc2.setFloat(0, 3f);
        
        EntitySystem system = new EntitySystem();
        Entity fromTemplate = system.addEntity(template);
        IntComponent c1 = fromTemplate.get(Component.getTypedId(IntComponent.class));
        FloatComponent c2 = fromTemplate.get(Component.getTypedId(FloatComponent.class));
        
        Assert.assertEquals(2, c1.getInt(0));
        Assert.assertEquals(3f, c2.getFloat(0), .0001f);
        Assert.assertNotSame(template, fromTemplate);
        
        Assert.assertEquals(system, fromTemplate.getEntitySystem());
        Assert.assertEquals(systemTemplate, template.getEntitySystem());
    }
    
    @Test
    public void testRemoveEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        system.removeEntity(e);
        Assert.assertFalse(e.isLive());
        Assert.assertEquals(0, e.getId());
        
        Assert.assertFalse(system.iterator().hasNext());
    }
    
    @Test
    public void testRemoveFastEntity() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Iterator<Entity> it = system.fastIterator();
        while(it.hasNext()) {
            system.removeEntity(it.next());
        }
        
        Assert.assertFalse(e.isLive());
        Assert.assertEquals(0, e.getId());
        
        Assert.assertFalse(system.iterator().hasNext());
    }
    
    @Test
    public void testCompactNoOp() {
        EntitySystem system = new EntitySystem();
        for (int i = 0; i < 5; i++) 
            system.addEntity().add(Component.getTypedId(MultiPropertyComponent.class));
        
        system.compact();
        
        int count = 0;
        Iterator<Entity> it = system.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            MultiPropertyComponent m = e.get(Component.getTypedId(MultiPropertyComponent.class));
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
            MultiPropertyComponent c = es.get(es.size() - 1).add(Component.getTypedId(MultiPropertyComponent.class));
            float f = (float) Math.random();
            float f2 = (float) Math.random();
            c.setFloat(0, f);
            c.setFloat(1, f2);
            
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
            Assert.assertEquals(ft.next().floatValue(), e.get(Component.getTypedId(MultiPropertyComponent.class)).getFloat(0), .0001f);
            Assert.assertEquals(ft.next().floatValue(), e.get(Component.getTypedId(MultiPropertyComponent.class)).getFloat(1), .0001f);
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
            MultiPropertyComponent c = es.get(es.size() - 1).add(Component.getTypedId(MultiPropertyComponent.class));
            float f = (float) Math.random();
            float f2 = (float) Math.random();
            c.setFloat(0, f);
            c.setFloat(1, f2);
            
            cs.add(f);
            cs.add(f2);
        }
        
        
        // remove a bunch of components from the entities
        int i = 0;
        Iterator<Entity> it = es.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            if (i % 2 == 0) {
                e.remove(Component.getTypedId(MultiPropertyComponent.class));
            } 
            i++;
        }
        
        // now add back in component values for the previously removed entities
        i = 0;
        it = es.iterator();
        Iterator<Float> ft = cs.iterator();
        while(it.hasNext()) {
            Entity e = it.next();
            MultiPropertyComponent c = e.get(Component.getTypedId(MultiPropertyComponent.class));
            
            float f = ft.next();
            float f2 = ft.next();
            
            if (c == null) {
                c = e.add(Component.getTypedId(MultiPropertyComponent.class));
                c.setFloat(0, f);
                c.setFloat(1, f2);
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
            Assert.assertEquals(ft.next().floatValue(), e.get(Component.getTypedId(MultiPropertyComponent.class)).getFloat(0), .0001f);
            Assert.assertEquals(ft.next().floatValue(), e.get(Component.getTypedId(MultiPropertyComponent.class)).getFloat(1), .0001f);
        }
        Assert.assertFalse(it.hasNext());
        Assert.assertFalse(si.hasNext());
    }
    
    @Test
    public void testGetEntityById() {
        EntitySystem system = new EntitySystem();
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < 100; i++) {
            ids.add(system.addEntity().getId());
        }
        
        for (Integer id: ids) {
            Assert.assertEquals(id.intValue(), system.getEntity(id).getId());
        }
        
        // remove some entities to create gaps that need repairs
        int i = 0;
        Iterator<Entity> it = system.iterator();
        Iterator<Integer> idit = ids.iterator();
        while(it.hasNext()) {
            it.next();
            idit.next();
            if (i % 2 == 0) {
                it.remove();
                idit.remove();
            }
            i++;
        }
        
        // now add some new entities
        for (i = 0; i < 50; i++) {
            ids.add(system.addEntity().getId());
        }
        
        // double-check - this should run a compact on the first try
        for (Integer id: ids) {
            Assert.assertEquals(id.intValue(), system.getEntity(id).getId());
        }
    }
}
