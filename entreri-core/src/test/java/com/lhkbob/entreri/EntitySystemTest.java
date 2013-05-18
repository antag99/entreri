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

import com.lhkbob.entreri.components.ComplexComponent;
import com.lhkbob.entreri.components.FloatComponent;
import com.lhkbob.entreri.components.IntComponent;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntitySystemTest {
    @Test
    public void testAddEntity() {
        // There really isn't much to test with this one, everything else
        // is validated by other tests in this package
        EntitySystem system = EntitySystem.create();
        Entity e = system.addEntity();

        int componentCount = 0;
        for (@SuppressWarnings("unused") Component c : e) {
            componentCount++;
        }

        Assert.assertEquals(0, componentCount);
        Assert.assertEquals(system, e.getEntitySystem());
        Assert.assertTrue(e.isAlive());

        int entityCount = 0;
        for (Entity entity : system) {
            entityCount++;
            Assert.assertSame(e, entity);
        }
        Assert.assertEquals(1, entityCount);
    }

    @Test
    public void testAddEntityFromTemplate() {
        EntitySystem system = EntitySystem.create();
        Entity template = system.addEntity();

        IntComponent tc1 = template.add(IntComponent.class);
        tc1.setInt(2);
        FloatComponent tc2 = template.add(FloatComponent.class);
        tc2.setFloat(3f);

        Entity fromTemplate = system.addEntity(template);
        IntComponent c1 = fromTemplate.get(IntComponent.class);
        FloatComponent c2 = fromTemplate.get(FloatComponent.class);

        Assert.assertEquals(2, c1.getInt());
        Assert.assertEquals(3f, c2.getFloat(), .0001f);
        Assert.assertFalse(c1.equals(tc1));
        Assert.assertFalse(c2.equals(tc2));
        Assert.assertNotSame(template, fromTemplate);
    }

    @Test
    public void testAddEntityFromTemplateInAnotherSystem() {
        EntitySystem system1 = EntitySystem.create();
        Entity template = system1.addEntity();

        IntComponent tc1 = template.add(IntComponent.class);
        tc1.setInt(2);
        FloatComponent tc2 = template.add(FloatComponent.class);
        tc2.setFloat(3f);

        EntitySystem system2 = EntitySystem.create();
        Entity fromTemplate = system2.addEntity(template);
        IntComponent c1 = fromTemplate.get(IntComponent.class);
        FloatComponent c2 = fromTemplate.get(FloatComponent.class);

        Assert.assertEquals(2, c1.getInt());
        Assert.assertEquals(3f, c2.getFloat(), .0001f);
        Assert.assertFalse(c1.equals(tc1));
        Assert.assertFalse(c2.equals(tc2));
        Assert.assertNotSame(template, fromTemplate);
    }

    @Test
    public void testRemoveEntity() {
        EntitySystem system = EntitySystem.create();
        Entity e = system.addEntity();
        IntComponent c = e.add(IntComponent.class);

        system.removeEntity(e);
        Assert.assertFalse(e.isAlive());
        Assert.assertFalse(c.isAlive());
        Assert.assertEquals(1, e.getId()); // it's id should remain unchanged

        Assert.assertFalse(system.iterator().hasNext());
    }

    @Test
    public void testIteratorRemoveEntity() {
        EntitySystem system = EntitySystem.create();
        List<Entity> original = new ArrayList<Entity>();

        List<Entity> removed = new ArrayList<Entity>();

        for (int i = 0; i < 10; i++) {
            original.add(system.addEntity());
        }

        Iterator<Entity> it = system.iterator();
        while (it.hasNext()) {
            removed.add(it.next());
            it.remove();
        }

        Assert.assertEquals(original, removed);
    }

    @Test
    public void testIteratorExternalRemoveEntity() {
        EntitySystem system = EntitySystem.create();
        List<Entity> original = new ArrayList<Entity>();

        List<Entity> removed = new ArrayList<Entity>();

        for (int i = 0; i < 10; i++) {
            original.add(system.addEntity());
        }

        Iterator<Entity> it = system.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            removed.add(e);
            system.removeEntity(e);
        }

        Assert.assertEquals(original, removed);
    }

    @Test
    public void testCompactNoOp() {
        EntitySystem system = EntitySystem.create();
        for (int i = 0; i < 5; i++) {
            system.addEntity().add(ComplexComponent.class);
        }

        system.compact();

        int count = 0;
        Iterator<Entity> it = system.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            Assert.assertNotNull(e.get(ComplexComponent.class));
            count++;
        }

        Assert.assertEquals(5, count);
    }

    @Test
    public void testCompactRepairRemoves() {
        EntitySystem system = EntitySystem.create();
        List<Entity> es = new ArrayList<Entity>();
        List<Float> cs = new ArrayList<Float>();
        for (int i = 0; i < 100; i++) {
            es.add(system.addEntity());
            ComplexComponent c = es.get(es.size() - 1).add(ComplexComponent.class);
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
        while (it.hasNext()) {
            Entity e = it.next();
            ft.next(); // always advance once
            if (i % 2 == 0) {
                it.remove();
                system.removeEntity(e);

                ft.remove();
                ft.next();
                ft.remove(); // remove 2nd element
            } else {
                ft.next(); // advance past 2nd element
            }
            i++;
        }

        system.compact();

        it = es.iterator();
        Iterator<Entity> si = system.iterator();
        ft = cs.iterator();
        while (it.hasNext() && si.hasNext()) {
            Entity e = si.next();
            Assert.assertEquals(it.next(), e);
            Assert.assertEquals(ft.next(), e.get(ComplexComponent.class).getFloat(),
                                .0001f);
            Assert.assertEquals(ft.next(),
                                e.get(ComplexComponent.class).getFactoryFloat(), .0001f);
        }
        Assert.assertFalse(it.hasNext());
        Assert.assertFalse(si.hasNext());
    }

    @Test
    public void testCompactAddRemoveRepair() {
        EntitySystem system = EntitySystem.create();
        List<Entity> es = new ArrayList<Entity>();
        List<Float> cs = new ArrayList<Float>();
        for (int i = 0; i < 100; i++) {
            es.add(system.addEntity());
            ComplexComponent c = es.get(es.size() - 1).add(ComplexComponent.class);
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
        while (it.hasNext()) {
            Entity e = it.next();
            if (i % 2 == 0) {
                e.remove(ComplexComponent.class);
            }
            i++;
        }

        // now add back in component values for the previously removed entities
        i = 0;
        it = es.iterator();
        Iterator<Float> ft = cs.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            ComplexComponent c = e.get(ComplexComponent.class);

            float f = ft.next();
            float f2 = ft.next();

            if (c == null) {
                c = e.add(ComplexComponent.class);
                c.setFloat(f);
                c.setFactoryFloat(f2);
            }
            i++;
        }

        system.compact();

        it = es.iterator();
        Iterator<Entity> si = system.iterator();
        ft = cs.iterator();
        while (it.hasNext() && si.hasNext()) {
            Entity e = si.next();
            Assert.assertEquals(it.next(), e);
            Assert.assertEquals(ft.next(), e.get(ComplexComponent.class).getFloat(),
                                .0001f);
            Assert.assertEquals(ft.next(),
                                e.get(ComplexComponent.class).getFactoryFloat(), .0001f);
        }
        Assert.assertFalse(it.hasNext());
        Assert.assertFalse(si.hasNext());
    }
}
