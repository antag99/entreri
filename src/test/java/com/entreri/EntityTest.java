package com.entreri;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.entreri.component.FloatComponent;
import com.entreri.component.IntComponent;

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
