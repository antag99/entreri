package com.googlecode.entreri;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.component.IntComponent;
import com.googlecode.entreri.component.MultiPropertyComponent;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.FloatPropertyFactory;

public class ComponentRepositoryTest {
    @Test
    public void testFactorySetValue() {
        EntitySystem system = new EntitySystem();
        MultiPropertyComponent c = system.addEntity().add(TypeId.get(MultiPropertyComponent.class)).getData();
        Assert.assertEquals(FloatPropertyFactory.DEFAULT, c.getFactoryFloat(), .0001f);
    }
    
    @Test
    public void testDecorateProperty() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(TypeId.get(IntComponent.class)).getData();
        
        FloatProperty decorated = system.decorate(TypeId.get(IntComponent.class), new FloatPropertyFactory());
        decorated.getIndexedData()[c.getIndex()] = 1f;
        
        int count = 0;
        for (Entity entity: system) {
            Assert.assertTrue(entity.get(c));
            count++;
            
            Assert.assertEquals(1f, decorated.getIndexedData()[c.getIndex()], .0001f);
        }
        Assert.assertEquals(1, count);
    }
    
    @Test
    public void testDecoratePropertyAddComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(TypeId.get(IntComponent.class)).getData();
        
        FloatProperty decorated = system.decorate(TypeId.get(IntComponent.class), new FloatPropertyFactory());
        decorated.getIndexedData()[c.getIndex()] = 1f;
        
        Entity e2 = system.addEntity();
        IntComponent c2 = e2.add(TypeId.get(IntComponent.class)).getData();
        decorated.getIndexedData()[c2.getIndex()] = 2f;
        
        int count = 0;
        for (Entity entity: system) {
            IntComponent c3 = entity.get(TypeId.get(IntComponent.class)).getData();
            count++;
            
            if (c3.getIndex() == c.getIndex())
                Assert.assertEquals(1f, decorated.getIndexedData()[c3.getIndex()], .0001f);
            else
                Assert.assertEquals(2f, decorated.getIndexedData()[c3.getIndex()], .0001f);
        }
        Assert.assertEquals(2, count);
    }
    
    @Test
    public void testUndecorateValidProperty() {
        EntitySystem system = new EntitySystem();
        
        FloatProperty decorated = system.decorate(TypeId.get(IntComponent.class), new FloatPropertyFactory());
        system.undecorate(TypeId.get(IntComponent.class), decorated);
    }
    
    @Test
    public void testUndecorateInvalidProperty() {
        FloatProperty prop = new FloatProperty(2);
        EntitySystem system = new EntitySystem();
        system.undecorate(TypeId.get(IntComponent.class), prop);
        // should not fail
    }
}
