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

import org.junit.Assert;
import org.junit.Test;

import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.component.IntComponent;
import com.lhkbob.entreri.component.MultiPropertyComponent;
import com.lhkbob.entreri.property.FloatProperty;
import com.lhkbob.entreri.property.FloatPropertyFactory;

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
        FloatProperty prop = new FloatProperty();
        EntitySystem system = new EntitySystem();
        system.undecorate(TypeId.get(IntComponent.class), prop);
        // should not fail
    }
}
