package com.entreri;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.entreri.component.BadConstructorComponent;
import com.entreri.component.BadParametersComponent;
import com.entreri.component.ExtraFieldComponent;
import com.entreri.component.FloatComponent;
import com.entreri.component.IntComponent;
import com.entreri.component.InvalidHierarchyComponent;
import com.entreri.component.MultiPropertyComponent;
import com.entreri.component.ObjectComponent;
import com.entreri.component.PublicConstructorComponent;
import com.entreri.component.PublicPropertyComponent;
import com.entreri.property.FloatProperty;
import com.entreri.property.FloatPropertyFactory;
import com.entreri.property.MultiParameterProperty;
import com.entreri.property.NoParameterProperty;
import com.entreri.property.Property;

public class ComponentTest {
    @Test
    public void testGetTypedId() {
        doGetTypedIdTest(FloatComponent.class);
        doGetTypedIdTest(IntComponent.class);
        doGetTypedIdTest(ObjectComponent.class);
        doGetTypedIdTest(MultiPropertyComponent.class);
    }
    
    private void doGetTypedIdTest(Class<? extends Component> type) {
        Component.getTypedId(type);
    }
    
    @Test
    public void testInvalidComponentDefinition() {
        doInvalidComponentDefinitionTest(BadConstructorComponent.class);
        doInvalidComponentDefinitionTest(BadParametersComponent.class);
        doInvalidComponentDefinitionTest(ExtraFieldComponent.class);
        doInvalidComponentDefinitionTest(InvalidHierarchyComponent.class);
        doInvalidComponentDefinitionTest(PublicConstructorComponent.class);
        doInvalidComponentDefinitionTest(PublicPropertyComponent.class);
    }
    
    private void doInvalidComponentDefinitionTest(Class<? extends Component> type) {
        try {
            Component.getTypedId(type);
            Assert.fail("Expected IllegalComponentDefinitionException");
        } catch(IllegalComponentDefinitionException e) {
            // expected
        }
    }
    
    @Test
    public void testPropertyLookup() {
        ComponentBuilder<MultiPropertyComponent> builder = Component.getBuilder(Component.getTypedId(MultiPropertyComponent.class));
        List<Property> props = builder.createProperties();
        
        Set<Class<? extends Property>> propTypeSet = new HashSet<Class<? extends Property>>();
        for (Property p: props) {
            propTypeSet.add(p.getClass());
        }
        
        Assert.assertEquals(3, propTypeSet.size());
        Assert.assertTrue(propTypeSet.contains(MultiParameterProperty.class));
        Assert.assertTrue(propTypeSet.contains(NoParameterProperty.class));
        Assert.assertTrue(propTypeSet.contains(FloatProperty.class));
    }
    
    @Test
    public void testDecorateProperty() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(Component.getTypedId(IntComponent.class));
        
        FloatProperty decorated = system.decorate(Component.getTypedId(IntComponent.class), new FloatPropertyFactory());
        
        decorated.getIndexedData()[c.getIndex()] = 1f;
        
        int count = 0;
        Iterator<IntComponent> it = system.fastIterator(Component.getTypedId(IntComponent.class));
        while(it.hasNext()) {
            c = it.next();
            count++;
            
            Assert.assertEquals(1f, decorated.getIndexedData()[c.getIndex()], .0001f);
        }
        Assert.assertEquals(1, count);
    }
    
    @Test
    public void testDecoratePropertyAddComponent() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(Component.getTypedId(IntComponent.class));
        
        FloatProperty decorated = system.decorate(Component.getTypedId(IntComponent.class), new FloatPropertyFactory());
        decorated.getIndexedData()[c.getIndex()] = 1f;
        
        Entity e2 = system.addEntity();
        IntComponent c2 = e2.add(Component.getTypedId(IntComponent.class));
        decorated.getIndexedData()[c2.getIndex()] = 2f;
        
        int count = 0;
        Iterator<IntComponent> it = system.fastIterator(Component.getTypedId(IntComponent.class));
        while(it.hasNext()) {
            IntComponent c3 = it.next();
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
        
        FloatProperty decorated = system.decorate(Component.getTypedId(IntComponent.class), new FloatPropertyFactory());
        system.undecorate(Component.getTypedId(IntComponent.class), decorated);
    }
    
    @Test
    public void testUndecorateInvalidProperty() {
        FloatProperty prop = new FloatProperty(2);
        EntitySystem system = new EntitySystem();
        system.undecorate(Component.getTypedId(IntComponent.class), prop);
        // should not fail
    }
    
    @Test
    public void testEquals() {
        EntitySystem system = new EntitySystem();
        Entity e = system.addEntity();
        IntComponent c = e.add(Component.getTypedId(IntComponent.class));
        
        int count = 0;
        Iterator<IntComponent> it = system.fastIterator(Component.getTypedId(IntComponent.class));
        while(it.hasNext()) {
            IntComponent c2 = it.next();
            count++;
            Assert.assertEquals(c, c2);
        }
        Assert.assertEquals(1, count);
    }
}
