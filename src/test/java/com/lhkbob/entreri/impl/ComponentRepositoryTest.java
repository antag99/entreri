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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.components.ComplexComponent;
import com.lhkbob.entreri.components.FloatPropertyFactory;
import com.lhkbob.entreri.components.IntComponent;
import com.lhkbob.entreri.property.FloatProperty;
import com.lhkbob.entreri.property.Property;
import org.junit.Assert;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ComponentRepositoryTest {
    @Test
    public void testFactorySetValue() {
        EntitySystem system = EntitySystem.Factory.create();
        ComplexComponent c = system.addEntity().add(ComplexComponent.class);
        Assert.assertEquals(FloatPropertyFactory.DEFAULT, c.getFactoryFloat(), .0001f);
    }

    @Test
    public void testDecorateProperty() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent c = e.add(IntComponent.class);

        FloatProperty decorated = system
                .decorate(IntComponent.class, new FloatPropertyFactory());
        decorated.getIndexedData()[c.getIndex()] = 1f;

        int count = 0;
        for (Entity entity : system) {
            Assert.assertNotNull(entity.get(IntComponent.class));
            count++;

            Assert.assertEquals(1f, decorated.getIndexedData()[c.getIndex()], .0001f);
        }
        Assert.assertEquals(1, count);
    }

    @Test
    public void testDecoratePropertyAddComponent() {
        EntitySystem system = EntitySystem.Factory.create();
        Entity e = system.addEntity();
        IntComponent c = e.add(IntComponent.class);

        FloatProperty decorated = system
                .decorate(IntComponent.class, new FloatPropertyFactory());
        decorated.getIndexedData()[c.getIndex()] = 1f;

        Entity e2 = system.addEntity();
        IntComponent c2 = e2.add(IntComponent.class);
        decorated.getIndexedData()[c2.getIndex()] = 2f;

        int count = 0;
        for (Entity entity : system) {
            IntComponent c3 = entity.get(IntComponent.class);
            count++;

            if (c3.getIndex() == c.getIndex()) {
                Assert.assertEquals(1f, decorated.getIndexedData()[c3.getIndex()],
                                    .0001f);
            } else {
                Assert.assertEquals(2f, decorated.getIndexedData()[c3.getIndex()],
                                    .0001f);
            }
        }
        Assert.assertEquals(2, count);
    }

    @Test
    @SuppressWarnings({ "unused", "UnusedAssignment" })
    public void testUndecorateValidProperty() throws Exception {
        // This is an ugly ugly test case since it has to verify that the
        // property gets garbage collected. The only way it can get at that
        // is to use reflection to inspect the component repository
        EntitySystemImpl system = (EntitySystemImpl) EntitySystem.Factory.create();
        ComponentRepository<IntComponent> cr = system.getRepository(IntComponent.class);
        int count = getDecoratedProperties(cr).size();

        FloatProperty decorated = system
                .decorate(IntComponent.class, new FloatPropertyFactory());

        Assert.assertEquals(count + 1, getDecoratedProperties(cr).size());

        decorated = null;
        System.gc();
        Thread.sleep(100);

        system.compact();

        Assert.assertEquals(count, getDecoratedProperties(cr).size());
    }

    @SuppressWarnings("unchecked")
    private static List<Property> getDecoratedProperties(ComponentRepository<?> cr)
            throws Exception {
        Field decorated = ComponentRepository.class
                .getDeclaredField("decoratedProperties");
        decorated.setAccessible(true);
        List<?> value = (List<?>) decorated.get(cr);

        List<Property> converted = new ArrayList<>();
        for (Object o : value) {
            Field ref = o.getClass().getDeclaredField("property");
            ref.setAccessible(true);
            converted.add(((WeakReference<? extends Property>) ref.get(o)).get());
        }
        return converted;
    }
}
