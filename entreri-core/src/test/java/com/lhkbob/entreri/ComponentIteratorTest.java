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

import com.lhkbob.entreri.components.FloatComponent;
import com.lhkbob.entreri.components.IntComponent;
import com.lhkbob.entreri.components.ObjectComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ComponentIteratorTest {
    private static final int ENTITY_COUNT = 5;

    private EntitySystem system;
    private List<Integer> entityIds;
    private List<ObjectComponent.FooBlah> entityObjValues;
    private List<Float> entityFloatValues;

    private List<ObjectComponent.FooBlah> entityCombinedObjValues;
    private List<Float> entityCombinedFloatValues;

    private int countWithObj;
    private int countWithFloat;
    private int countWithBoth;

    private ObjectComponent objData;
    private FloatComponent floatData;

    @Before
    public void setup() {
        entityIds = new ArrayList<Integer>();
        entityObjValues = new ArrayList<ObjectComponent.FooBlah>();
        entityFloatValues = new ArrayList<Float>();
        entityCombinedObjValues = new ArrayList<ObjectComponent.FooBlah>();
        entityCombinedFloatValues = new ArrayList<Float>();

        system = EntitySystem.create();

        for (int i = 0; i < ENTITY_COUNT; i++) {
            Entity e = system.addEntity();

            entityIds.add(e.getId());

            double c = Math.random();
            if (c > .8) {
                // both components to add
                objData = e.add(ObjectComponent.class);
                ObjectComponent.FooBlah v = new ObjectComponent.FooBlah();
                entityObjValues.add(v);
                entityCombinedObjValues.add(v);
                objData.setObject(v);

                floatData = e.add(FloatComponent.class);
                float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                entityCombinedFloatValues.add(fv);
                floatData.setFloat(fv);

                countWithBoth++;
                countWithObj++;
                countWithFloat++;
            } else if (c > .4) {
                // just float component
                floatData = e.add(FloatComponent.class);
                float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                floatData.setFloat(fv);

                countWithFloat++;
            } else {
                // just object component
                objData = e.add(ObjectComponent.class);
                ObjectComponent.FooBlah v = new ObjectComponent.FooBlah();
                entityObjValues.add(v);
                objData.setObject(v);

                countWithObj++;
            }
        }
    }

    private void doTestIterator(Iterator<Entity> it) {
        int i = 0;
        while (it.hasNext()) {
            Assert.assertEquals(entityIds.get(i), Integer.valueOf(it.next().getId()));
            i++;
        }

        Assert.assertEquals(entityIds.size(), i);
    }

    // assumes it has objData in it
    private void doTestObjectComponentIterator(ComponentIterator it) {
        int i = 0;
        while (it.next()) {
            Assert.assertEquals(entityObjValues.get(i), objData.getObject());
            i++;
        }

        Assert.assertEquals(countWithObj, i);
    }

    // assumes it has floatData in it
    private void doTestFloatComponentIterator(ComponentIterator it) {
        int i = 0;
        while (it.next()) {
            Assert.assertEquals(entityFloatValues.get(i), floatData.getFloat(), .0001f);
            i++;
        }

        Assert.assertEquals(countWithFloat, i);
    }

    // assumes it has floatData and objData as required
    private void doTestBulkComponentIterator(ComponentIterator it) {
        int i = 0;
        while (it.next()) {
            Assert.assertEquals(entityCombinedObjValues.get(i), objData.getObject());
            Assert.assertEquals(entityCombinedFloatValues.get(i), floatData.getFloat(),
                                .0001f);
            i++;
        }

        Assert.assertEquals(countWithBoth, i);
    }

    private void doIteratorRemove(Iterator<Entity> it) {
        int i = 0;
        Iterator<Integer> ids = entityIds.iterator();
        while (it.hasNext()) {
            it.next();
            ids.next();
            if (i > ENTITY_COUNT / 2) {
                it.remove();
                ids.remove();
            }

            i++;
        }

        // this invalidates all of the value lists, but that is okay
    }

    @Test
    public void testEntityIterator() {
        doTestIterator(system.iterator());
    }

    @Test
    public void testComponentIterator() {
        ComponentIterator ft = system.fastIterator();
        floatData = ft.addRequired(FloatComponent.class);
        ComponentIterator it = system.fastIterator();
        objData = it.addRequired(ObjectComponent.class);

        doTestObjectComponentIterator(it);
        it.reset();
        doTestObjectComponentIterator(it);

        doTestFloatComponentIterator(ft);
        ft.reset();
        doTestFloatComponentIterator(ft);
    }

    @Test
    public void testBulkComponentIterator() {
        ComponentIterator it = system.fastIterator();
        floatData = it.addRequired(FloatComponent.class);
        objData = it.addRequired(ObjectComponent.class);

        doTestBulkComponentIterator(it);
        it.reset();
        doTestBulkComponentIterator(it);
    }

    @Test
    public void testEntityIteratorRemove() {
        doIteratorRemove(system.iterator());
        doTestIterator(system.iterator());
    }

    @Test
    public void testFlyweightComponent() {
        ComponentIterator it = system.fastIterator();
        IntComponent c = it.addRequired(IntComponent.class);

        Assert.assertTrue(c.isFlyweight());
        Assert.assertFalse(c.isAlive());

        int count = 0;
        system.addEntity().add(IntComponent.class);
        while (it.next()) {
            Assert.assertTrue(c.isFlyweight());
            Assert.assertTrue(c.isAlive());
            count++;
        }

        Assert.assertEquals(1, count);
    }
}
