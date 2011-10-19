package com.entreri;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.entreri.component.FloatComponent;
import com.entreri.component.ObjectComponent;

public class IteratorTest {
    private static final int ENTITY_COUNT = 10000;
    
    private EntitySystem system;
    private List<Integer> entityIds;
    private List<Object> entityObjValues;
    private List<Float> entityFloatValues;
    
    private List<Object> entityCombinedObjValues;
    private List<Float> entityCombinedFloatValues;
    
    private int countWithObj;
    private int countWithFloat;
    private int countWithBoth;
    
    private TypedId<ObjectComponent> objId;
    private TypedId<FloatComponent> floatId;
    
    @Before
    public void setup() {
        objId = Component.getTypedId(ObjectComponent.class);
        floatId = Component.getTypedId(FloatComponent.class);
        
        
        entityIds = new ArrayList<Integer>();
        entityObjValues = new ArrayList<Object>();
        entityFloatValues = new ArrayList<Float>();
        entityCombinedObjValues = new ArrayList<Object>();
        entityCombinedFloatValues = new ArrayList<Float>();
        
        system = new EntitySystem();
        
        for (int i = 0; i < ENTITY_COUNT; i++) {
            Entity e = system.addEntity();
            
            entityIds.add(e.getId());
            
            double c = Math.random();
            if (c > .8) {
                // both components to add
                ObjectComponent o = e.add(objId);
                Object v = new Object();
                entityObjValues.add(v);
                entityCombinedObjValues.add(v);
                o.setObject(0, v);
                
                FloatComponent f = e.add(floatId);
                Float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                entityCombinedFloatValues.add(fv);
                f.setFloat(0, fv);
                
                countWithBoth++;
                countWithObj++;
                countWithFloat++;
            } else if (c > .4) {
                // just float component
                FloatComponent f = e.add(floatId);
                Float fv = (float) (Math.random() * 1000);
                entityFloatValues.add(fv);
                f.setFloat(0, fv);
                
                countWithFloat++;
            } else {
                // just object component
                ObjectComponent o = e.add(objId);
                Object v = new Object();
                entityObjValues.add(v);
                o.setObject(0, v);
                
                countWithObj++;
            }
        }
    }
    
    private void doTestIterator(Iterator<Entity> it) {
        int i = 0;
        while(it.hasNext()) {
            Assert.assertEquals(entityIds.get(i), Integer.valueOf(it.next().getId()));
            i++;
        }
        
        Assert.assertEquals(entityIds.size(), i);
    }
    
    private void doTestObjectComponentIterator(Iterator<ObjectComponent> it) {
        int i = 0;
        while(it.hasNext()) {
            Assert.assertEquals(entityObjValues.get(i), it.next().getObject(0));
            i++;
        }
        
        Assert.assertEquals(countWithObj, i);
    }
    
    private void doTestFloatComponentIterator(Iterator<FloatComponent> it) {
        int i = 0;
        while(it.hasNext()) {
            Assert.assertEquals(entityFloatValues.get(i).floatValue(), it.next().getFloat(0), .0001f);
            i++;
        }
        
        Assert.assertEquals(countWithFloat, i);
    }
    
    private void doTestBulkComponentIterator(Iterator<IndexedComponentMap> it) {
        int i = 0;
        while(it.hasNext()) {
            IndexedComponentMap map = it.next();
            Assert.assertEquals(entityCombinedObjValues.get(i), map.get(objId, 0).getObject(0));
            Assert.assertEquals(entityCombinedObjValues.get(i), map.get(objId).getObject(0));
            Assert.assertEquals(entityCombinedFloatValues.get(i).floatValue(), map.get(floatId, 1).getFloat(0), .0001f);
            Assert.assertEquals(entityCombinedFloatValues.get(i).floatValue(), map.get(floatId).getFloat(0), .0001f);
            i++;
        }
        
        Assert.assertEquals(countWithBoth, i);
    }
    
    private void doIteratorRemove(Iterator<Entity> it) {
        int i = 0;
        Iterator<Integer> ids = entityIds.iterator();
        while(it.hasNext()) {
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
    
    private void doIteratorObjectComponentRemove(Iterator<ObjectComponent> it) {
        int i = 0;
        Iterator<Object> vs = entityObjValues.iterator();
        while(it.hasNext()) {
            it.next();
            vs.next();
            if (i > countWithObj / 2) {
                it.remove();
                vs.remove();
                countWithObj--;
            }
            
            i++;
        }
        
        // this invalidates the combined value lists, but that is okay
    }
    
    private void doIteratorFloatComponentRemove(Iterator<FloatComponent> it) {
        int i = 0;
        Iterator<Float> fs = entityFloatValues.iterator();
        while(it.hasNext()) {
            it.next();
            fs.next();
            if (i > countWithFloat / 2) {
                it.remove();
                fs.remove();
                countWithFloat--;
            }
            
            i++;
        }
        
        // this invalidates the combined value lists, but that is okay
    }
    
    @Test
    public void testEntityIterator() {
        doTestIterator(system.iterator());
    }
    
    @Test
    public void testFastEntityIterator() {
        doTestIterator(system.fastIterator());
    }
    
    @Test
    public void testComponentIterator() {
        doTestObjectComponentIterator(system.iterator(objId));
        doTestFloatComponentIterator(system.iterator(floatId));
    }
    
    @Test
    public void testFastComponentIterator() {
        doTestObjectComponentIterator(system.fastIterator(objId));
        doTestFloatComponentIterator(system.fastIterator(floatId));
    }
    
    @Test
    public void testBulkComponentIterator() {
        doTestBulkComponentIterator(system.iterator(objId, floatId));
    }
    
    @Test
    public void testFastBulkComponentIterator() {
        doTestBulkComponentIterator(system.fastIterator(objId, floatId));
    }
    
    @Test
    public void testEntityIteratorRemove() {
        doIteratorRemove(system.iterator());
        doTestIterator(system.iterator());
    }
    
    @Test
    public void testFastEntityIteratorRemove() {
        doIteratorRemove(system.fastIterator());
        doTestIterator(system.fastIterator());
    }
    
    @Test
    public void testComponentIteratorRemove() {
        doIteratorObjectComponentRemove(system.iterator(objId));
        doIteratorFloatComponentRemove(system.iterator(floatId));
        
        doTestObjectComponentIterator(system.iterator(objId));
        doTestFloatComponentIterator(system.iterator(floatId));
        
        // make sure no entities were removed
        doTestIterator(system.iterator());
    }
    
    @Test
    public void testFastComponentIteratorRemove() {
        doIteratorObjectComponentRemove(system.fastIterator(objId));
        doIteratorFloatComponentRemove(system.fastIterator(floatId));
        
        doTestObjectComponentIterator(system.fastIterator(objId));
        doTestFloatComponentIterator(system.fastIterator(floatId));
        
        // make sure no entities were removed
        doTestIterator(system.fastIterator());
    }
    
    @Test
    public void testBulkComponentIteratorRemove() {
        Iterator<IndexedComponentMap> it = system.iterator(objId, floatId);
        while(it.hasNext()) {
            it.next();
            try {
                it.remove();
                Assert.fail();
            } catch(UnsupportedOperationException e) {
                // expected
                break;
            }
        }
    }
    
    @Test
    public void testFastBulkComponentIteratorRemove() {
        Iterator<IndexedComponentMap> it = system.fastIterator(objId, floatId);
        while(it.hasNext()) {
            it.next();
            try {
                it.remove();
                Assert.fail();
            } catch(UnsupportedOperationException e) {
                // expected
                break;
            }
        }
    }
}
