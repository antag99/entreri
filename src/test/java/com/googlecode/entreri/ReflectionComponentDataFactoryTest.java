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
package com.googlecode.entreri;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.component.BadConstructorComponent;
import com.googlecode.entreri.component.ExtraFieldComponent;
import com.googlecode.entreri.component.FloatComponent;
import com.googlecode.entreri.component.IntComponent;
import com.googlecode.entreri.component.InvalidFactoryComponent;
import com.googlecode.entreri.component.InvalidFactoryMethodComponent;
import com.googlecode.entreri.component.InvalidHierarchyComponent;
import com.googlecode.entreri.component.MultiPropertyComponent;
import com.googlecode.entreri.component.NoFactoryMethodComponent;
import com.googlecode.entreri.component.ObjectComponent;
import com.googlecode.entreri.component.PublicConstructorComponent;
import com.googlecode.entreri.component.UnmanagedFieldComponent;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.FloatPropertyFactory;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.LongProperty;
import com.googlecode.entreri.property.NoParameterProperty;
import com.googlecode.entreri.property.PropertyFactory;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

public class ReflectionComponentDataFactoryTest {
    @Test
    public void testValidComponentDefinition() {
        doValidComponentDefinitionTest(FloatComponent.class);
        doValidComponentDefinitionTest(IntComponent.class);
        doValidComponentDefinitionTest(ObjectComponent.class);
        doValidComponentDefinitionTest(MultiPropertyComponent.class);
        doValidComponentDefinitionTest(UnmanagedFieldComponent.class);
    }
    
    @Test
    public void testInvalidComponentDefinition() {
        doInvalidComponentDefinitionTest(BadConstructorComponent.class);
        doInvalidComponentDefinitionTest(InvalidFactoryComponent.class);
        doInvalidComponentDefinitionTest(ExtraFieldComponent.class);
        doInvalidComponentDefinitionTest(InvalidHierarchyComponent.class);
        doInvalidComponentDefinitionTest(InvalidFactoryComponent.class);
        doInvalidComponentDefinitionTest(InvalidFactoryMethodComponent.class);
        doInvalidComponentDefinitionTest(NoFactoryMethodComponent.class);
        doInvalidComponentDefinitionTest(PublicConstructorComponent.class);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doValidComponentDefinitionTest(Class type) {
        new ReflectionComponentDataFactory(type).getPropertyFactories();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doInvalidComponentDefinitionTest(Class type) {
        try {
            new ReflectionComponentDataFactory(type);
            Assert.fail("Expected IllegalComponentDefinitionException");
        } catch(IllegalComponentDefinitionException e) {
            // expected
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyLookup() {
        ReflectionComponentDataFactory<MultiPropertyComponent> builder = new ReflectionComponentDataFactory<MultiPropertyComponent>(MultiPropertyComponent.class);
        Map<?, PropertyFactory<?>> origProps = builder.getPropertyFactories();
        
        // for this test convert it to a string key map
        Map<String, PropertyFactory<?>> props = new HashMap<String, PropertyFactory<?>>();
        for (Entry<?, PropertyFactory<?>> e: origProps.entrySet()) {
            props.put(((Field) e.getKey()).getName(), e.getValue());
        }
        
        // verify that fields are mapped properly
        Assert.assertTrue(props.containsKey("longProp"));
        Assert.assertTrue(props.get("longProp").create() instanceof LongProperty);
        LongProperty longProp = (LongProperty) props.get("longProp").create();
        ((PropertyFactory<LongProperty>) props.get("longProp")).setDefaultValue(longProp, 0);
        long[] longData = longProp.getIndexedData();
        Assert.assertEquals(3, longData.length);
        Assert.assertEquals(Long.MAX_VALUE, longData[0]);
        Assert.assertEquals(Long.MAX_VALUE, longData[1]);
        Assert.assertEquals(Long.MAX_VALUE, longData[2]);
        
        Assert.assertTrue(props.containsKey("floatProp"));
        Assert.assertTrue(props.get("floatProp").create() instanceof FloatProperty);
        FloatProperty floatProp = (FloatProperty) props.get("floatProp").create();
        ((PropertyFactory<FloatProperty>) props.get("floatProp")).setDefaultValue(floatProp, 0);
        float[] floatData = floatProp.getIndexedData();
        Assert.assertEquals(1, floatData.length);
        Assert.assertEquals(0.5f, floatData[0], .0001f);
        
        Assert.assertTrue(props.containsKey("intProp"));
        Assert.assertTrue(props.get("intProp").create() instanceof IntProperty);
        IntProperty intProp = (IntProperty) props.get("intProp").create();
        ((PropertyFactory<IntProperty>) props.get("intProp")).setDefaultValue(intProp, 0);
        int[] intData = intProp.getIndexedData();
        Assert.assertEquals(2, intData.length);
        Assert.assertEquals(0, intData[0]);
        Assert.assertEquals(0, intData[1]);
        
        Assert.assertTrue(props.containsKey("fromFactory"));
        Assert.assertTrue(props.get("fromFactory") instanceof FloatPropertyFactory);
        
        Assert.assertTrue(props.containsKey("noparams"));
        Assert.assertTrue(props.get("noparams").create() instanceof NoParameterProperty);
    }
    
    @Test
    public void testUnmanagedField() {
        TypeId<UnmanagedFieldComponent> id = TypeId.get(UnmanagedFieldComponent.class);
        
        EntitySystem system = new EntitySystem();
        for (int i = 0; i < 4; i++) {
            UnmanagedFieldComponent c = system.addEntity().add(id).getData();
            c.setObject(i);
            c.setFloat(i);
        }
        
        int i = 0;
        UnmanagedFieldComponent c = system.createDataInstance(id);
        for (Entity e: system) {
            Assert.assertTrue(e.get(c));
            float f = c.getFloat();
            Assert.assertEquals(0, f, .0001f);
            i++;
        }
    }
}
