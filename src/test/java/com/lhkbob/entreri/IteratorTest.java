package com.lhkbob.entreri;

import org.junit.Assert;
import org.junit.Test;

import com.lhkbob.entreri.component.IntComponent;

public class IteratorTest {
    @Test
    public void testDisabledComponents() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));
        
        Entity e1 = system.addEntity();
        e1.add(TypeId.get(IntComponent.class)).setEnabled(true);
        Entity e2 = system.addEntity();
        e2.add(TypeId.get(IntComponent.class)).setEnabled(false);
        
        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(cd);
        it.reset();
        
        int count = 0;
        while(it.next()) {
            count++;
            Assert.assertSame(e1, cd.getEntity());
        }
        Assert.assertEquals(1, count);
    }
    
    @Test
    public void testIgnoreEnabledComponents() {
        EntitySystem system = new EntitySystem();
        IntComponent cd = system.createDataInstance(TypeId.get(IntComponent.class));
        
        Entity e1 = system.addEntity();
        e1.add(TypeId.get(IntComponent.class)).setEnabled(true);
        Entity e2 = system.addEntity();
        e2.add(TypeId.get(IntComponent.class)).setEnabled(false);
        
        ComponentIterator it = new ComponentIterator(system);
        it.addRequired(cd)
          .setIgnoreEnabled(true)
          .reset();

        boolean hasE1 = false;
        boolean hasE2 = false;
        while(it.next()) {
            if (e1 == cd.getEntity()) {
                Assert.assertFalse(hasE1);
                hasE1 = true;
            } else if (e2 == cd.getEntity()) {
                Assert.assertFalse(hasE2);
                hasE2 = true;
            } else {
                Assert.fail();
            }
        }
        Assert.assertTrue(hasE1);
        Assert.assertTrue(hasE2);
    }
}
