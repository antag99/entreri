package com.googlecode.entreri;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.entreri.component.IntComponent;

public class ComponentDataTest {
    @Test
    public void testInvalidComponentRemove() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        IntComponent cd = c.getData();
        
        Assert.assertTrue(cd.isValid()); // sanity check
        e.remove(TypeId.get(IntComponent.class));
        Assert.assertFalse(cd.isValid());
    }
    
    @Test
    public void testInvalidEntityRemove() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        IntComponent cd = c.getData();
        
        Assert.assertTrue(cd.isValid()); // sanity check
        system.removeEntity(e);
        Assert.assertFalse(cd.isValid());
    }
    
    @Test
    public void testInvalidCompact() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();
        Entity e3 = system.addEntity();
        
        e1.add(TypeId.get(IntComponent.class)); // removed
        e2.add(TypeId.get(IntComponent.class)); // will shift over 
        e3.add(TypeId.get(IntComponent.class)); // will shift over 
        IntComponent cd = e2.get(TypeId.get(IntComponent.class)).getData();
        
        Assert.assertTrue(cd.isValid()); // sanity check
        
        e1.remove(TypeId.get(IntComponent.class));
        system.compact(); // since e1's component was moved, this shifts e2
        
        Assert.assertFalse(cd.isValid());
    }
    
    @Test
    public void testIsValid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        IntComponent cd = c.getData();
        
        Assert.assertTrue(cd.isValid()); // sanity check
    }
    
    @Test
    public void testIsValidNoopCompact() {
        EntitySystem system = new EntitySystem();
        Entity e1 = system.addEntity();
        Entity e2 = system.addEntity();
        
        e1.add(TypeId.get(IntComponent.class));
        e2.add(TypeId.get(IntComponent.class));
        IntComponent cd = e2.get(TypeId.get(IntComponent.class)).getData();
        
        Assert.assertTrue(cd.isValid()); // sanity check
        system.compact(); // no changes
        Assert.assertTrue(cd.isValid());
    }
    
    @Test
    public void testSetValid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class)); 
        
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));
        Assert.assertFalse(cd.isValid());
        Assert.assertTrue(cd.set(c));
        Assert.assertTrue(cd.isValid());
    }
    
    @Test
    public void testSetInvalid() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        
        Component<IntComponent> c = e.add(TypeId.get(IntComponent.class));
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));
        
        e.remove(TypeId.get(IntComponent.class));
        
        Assert.assertFalse(cd.set(c));
        Assert.assertFalse(cd.isValid());
        Assert.assertFalse(cd.set(null));
        Assert.assertFalse(cd.isValid());
        
        cd.set(e.add(TypeId.get(IntComponent.class)));
        Assert.assertTrue(cd.isValid());
    }
}
