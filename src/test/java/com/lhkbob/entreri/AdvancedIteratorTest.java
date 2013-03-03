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
import com.lhkbob.entreri.component.ObjectComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdvancedIteratorTest {
    private static final int ENTITY_COUNT = 5;

    private EntitySystem system;
    private List<Integer> entityIds;
    private List<Object> entityObjValues;
    private List<Float> entityFloatValues;

    private List<Object> entityCombinedObjValues;
    private List<Float> entityCombinedFloatValues;

    private int countWithObj;
    private int countWithFloat;
    private int countWithBoth;

    private ObjectComponent objData;
    private FloatComponent floatData;

    @Before
    public void setup() {
        entityIds = new ArrayList<Integer>();
        entityObjValues = new ArrayList<Object>();
        entityFloatValues = new ArrayList<Float>();
        entityCombinedObjValues = new ArrayList<Object>();
        entityCombinedFloatValues = new ArrayList<Float>();

        system = new EntitySystem();

        objData = system.createDataInstance(ObjectComponent.class);
        floatData = system.createDataInstance(FloatComponent.class);

        for (int i = 0; i < ENTITY_COUNT; i++) {
            Entity e = system.addEntity();

            entityIds.add(e.getId());

            double c = Math.random();
            if (c > .8) {
                // both components to add
                objData.set(e.add(ObjectComponent.class));
                Object v = new Object();
                entityObjValues.add(v);
                entityCombinedObjValues.add(v);
                objData.setObject(v);

                floatData.set(e.add(FloatComponent.class));
                float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                entityCombinedFloatValues.add(fv);
                floatData.setFloat(fv);

                countWithBoth++;
                countWithObj++;
                countWithFloat++;
            } else if (c > .4) {
                // just float component
                floatData.set(e.add(FloatComponent.class));
                float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                floatData.setFloat(fv);

                countWithFloat++;
            } else {
                // just object component
                objData.set(e.add(ObjectComponent.class));
                Object v = new Object();
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
            Assert.assertEquals(entityFloatValues.get(i).floatValue(),
                                floatData.getFloat(), .0001f);
            i++;
        }

        Assert.assertEquals(countWithFloat, i);
    }

    // assumes it has floatData and objData as required
    private void doTestBulkComponentIterator(ComponentIterator it) {
        int i = 0;
        while (it.next()) {
            Assert.assertEquals(entityCombinedObjValues.get(i), objData.getObject());
            Assert.assertEquals(entityCombinedFloatValues.get(i).floatValue(),
                                floatData.getFloat(), .0001f);
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
        ComponentIterator ft = new ComponentIterator(system);
        ft.addRequired(floatData);
        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(objData);

        doTestObjectComponentIterator(it);
        it.reset();
        doTestObjectComponentIterator(it);

        doTestFloatComponentIterator(ft);
        ft.reset();
        doTestFloatComponentIterator(ft);
    }

    @Test
    public void testBulkComponentIterator() {
        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(floatData);
        it.addRequired(objData);

        doTestBulkComponentIterator(it);
        it.reset();
        doTestBulkComponentIterator(it);
    }

    @Test
    public void testEntityIteratorRemove() {
        doIteratorRemove(system.iterator());
        doTestIterator(system.iterator());
    }
}
