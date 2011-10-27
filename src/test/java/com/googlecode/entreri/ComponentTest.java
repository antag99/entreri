/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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
package com.googlecode.entreri;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.ComponentBuilder;
import com.googlecode.entreri.Entity;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.IllegalComponentDefinitionException;
import com.googlecode.entreri.component.BadConstructorComponent;
import com.googlecode.entreri.component.BadParametersComponent;
import com.googlecode.entreri.component.ExtraFieldComponent;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.IntComponent;
import com.googlecode.entreri.component.InvalidHierarchyComponent;
import com.googlecode.entreri.component.MultiPropertyComponent;
import com.googlecode.entreri.component.ObjectComponent;
import com.googlecode.entreri.component.PublicConstructorComponent;
import com.googlecode.entreri.component.PublicPropertyComponent;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.FloatPropertyFactory;
import com.googlecode.entreri.property.MultiParameterProperty;
import com.googlecode.entreri.property.NoParameterProperty;
import com.googlecode.entreri.property.Property;

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