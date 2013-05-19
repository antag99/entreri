/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.components.*;
import com.lhkbob.entreri.property.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentSpecificationTest {
    @Test
    public void testValidComponentDefinition() {
        doValidComponentDefinitionTest(FloatComponent.class);
        doValidComponentDefinitionTest(IntComponent.class);
        doValidComponentDefinitionTest(ObjectComponent.class);
        doValidComponentDefinitionTest(ComplexComponent.class);
        doValidComponentDefinitionTest(RequiresAComponent.class);
        doValidComponentDefinitionTest(RequiresBComponent.class);
    }

    @Test
    public void testInvalidComponentDefinition() {
        // these are invalid component definitions but because they're not interfaces
        // they're not picked up by the annotation processor
        doInvalidComponentDefinitionTest(AbstractComponent.class);
        doInvalidComponentDefinitionTest(BadConstructorComponent.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doValidComponentDefinitionTest(Class type) {
        ComponentSpecification.Factory.fromClass(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doInvalidComponentDefinitionTest(Class type) {
        try {
            ComponentSpecification.Factory.fromClass(type);
            Assert.fail("Expected IllegalComponentDefinitionException");
        } catch (IllegalComponentDefinitionException e) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyLookup() throws Exception {
        ComponentSpecification spec = ComponentSpecification.Factory.fromClass(
                ComplexComponent.class);
        Assert.assertEquals("com.lhkbob.entreri.components", spec.getPackage());
        Assert.assertEquals("ComplexComponent", spec.getType());


        // for this test, convert it to a string key map
        List<? extends PropertyDeclaration> origProps = spec.getProperties();
        Map<String, PropertyDeclaration> props = new HashMap<>();
        for (PropertyDeclaration e : origProps) {
            props.put(e.getName(), e);
        }

        // verify that properties are mapped properly

        // property - int from IntComponent
        PropertyDeclaration intSpec = props.get("int");
        Assert.assertNotNull(intSpec);
        Assert.assertFalse(intSpec.isShared());
        IntProperty intProp = (IntProperty) intSpec.getPropertyFactory().create();
        ((IntProperty.Factory) intSpec.getPropertyFactory()).setDefaultValue(intProp, 0);
        Assert.assertEquals(0, intProp.get(0));
        Assert.assertEquals("getInt", intSpec.getGetterMethod());
        Assert.assertEquals("setInt", intSpec.getSetterMethod());
        Assert.assertEquals(0, intSpec.getSetterParameter());

        // property - float from FloatComponent
        PropertyDeclaration floatSpec = props.get("float");
        Assert.assertNotNull(floatSpec);
        Assert.assertFalse(floatSpec.isShared());
        FloatProperty floatProp = (FloatProperty) floatSpec.getPropertyFactory().create();
        ((FloatProperty.Factory) floatSpec.getPropertyFactory())
                .setDefaultValue(floatProp, 0);
        Assert.assertEquals(0f, floatProp.get(0), 0.0001f);
        Assert.assertEquals("getFloat", floatSpec.getGetterMethod());
        Assert.assertEquals("setFloat", floatSpec.getSetterMethod());
        Assert.assertEquals(0, floatSpec.getSetterParameter());

        // property - long
        PropertyDeclaration longSpec = props.get("long");
        Assert.assertNotNull(longSpec);
        Assert.assertFalse(longSpec.isShared());
        LongProperty longProp = (LongProperty) longSpec.getPropertyFactory().create();
        ((LongProperty.Factory) longSpec.getPropertyFactory())
                .setDefaultValue(longProp, 0);
        Assert.assertEquals(Long.MAX_VALUE, longProp.get(0));
        Assert.assertEquals("getLong", longSpec.getGetterMethod());
        Assert.assertEquals("setLong", longSpec.getSetterMethod());
        Assert.assertEquals(0, longSpec.getSetterParameter());

        // property - factoryFloat
        PropertyDeclaration facFloatSpec = props.get("factoryFloat");
        Assert.assertNotNull(facFloatSpec);
        Assert.assertFalse(facFloatSpec.isShared());
        FloatProperty floatFactoryProp = (FloatProperty) facFloatSpec.getPropertyFactory()
                                                                     .create();
        ((FloatPropertyFactory) facFloatSpec.getPropertyFactory())
                .setDefaultValue(floatFactoryProp, 0);
        Assert.assertEquals(FloatPropertyFactory.DEFAULT, floatFactoryProp.get(0),
                            0.0001f);
        Assert.assertEquals("getFactoryFloat", facFloatSpec.getGetterMethod());
        Assert.assertEquals("setFactoryFloat", facFloatSpec.getSetterMethod());
        Assert.assertEquals(0, facFloatSpec.getSetterParameter());

        // property - param1
        PropertyDeclaration param1Spec = props.get("param1");
        Assert.assertNotNull(param1Spec);
        Assert.assertFalse(param1Spec.isShared());
        ShortProperty param1Prop = (ShortProperty) param1Spec.getPropertyFactory()
                                                             .create();
        ((ShortProperty.Factory) param1Spec.getPropertyFactory())
                .setDefaultValue(param1Prop, 0);
        Assert.assertEquals((short) 0, param1Prop.get(0));
        Assert.assertEquals("getParam1", param1Spec.getGetterMethod());
        Assert.assertEquals("setParams", param1Spec.getSetterMethod());
        Assert.assertEquals(0, param1Spec.getSetterParameter());

        // property - param2
        PropertyDeclaration param2Spec = props.get("param2");
        Assert.assertNotNull(param2Spec);
        Assert.assertFalse(param2Spec.isShared());
        ShortProperty param2Prop = (ShortProperty) param2Spec.getPropertyFactory()
                                                             .create();
        ((ShortProperty.Factory) param2Spec.getPropertyFactory())
                .setDefaultValue(param2Prop, 0);
        Assert.assertEquals((short) 0, param2Prop.get(0));
        Assert.assertEquals("getParam2", param2Spec.getGetterMethod());
        Assert.assertEquals("setParams", param2Spec.getSetterMethod());
        Assert.assertEquals(1, param2Spec.getSetterParameter());

        // property - foo-blah
        PropertyDeclaration fooblahSpec = props.get("foo-blah");
        Assert.assertNotNull(fooblahSpec);
        Assert.assertFalse(fooblahSpec.isShared());
        BooleanProperty fooblahProp = (BooleanProperty) fooblahSpec.getPropertyFactory()
                                                                   .create();
        ((BooleanProperty.Factory) fooblahSpec.getPropertyFactory())
                .setDefaultValue(fooblahProp, 0);
        Assert.assertEquals(false, fooblahProp.get(0));
        Assert.assertEquals("isNamedParamGetter", fooblahSpec.getGetterMethod());
        Assert.assertEquals("setNamedParamSetter", fooblahSpec.getSetterMethod());
        Assert.assertEquals(0, fooblahSpec.getSetterParameter());

        // property - bletch
        PropertyDeclaration bletchSpec = props.get("bletch");
        Assert.assertNotNull(bletchSpec);
        Assert.assertTrue(bletchSpec.isShared());
        CustomProperty bletchProp = (CustomProperty) bletchSpec.getPropertyFactory()
                                                               .create();
        ((CustomProperty.CustomFactoryWithAttributes) bletchSpec.getPropertyFactory())
                .setDefaultValue(bletchProp, 0);
        Assert.assertEquals(14, bletchProp.get(0).value);
        Assert.assertEquals("hasBletch", bletchSpec.getGetterMethod());
        Assert.assertEquals("setBletch", bletchSpec.getSetterMethod());
        Assert.assertEquals(0, bletchSpec.getSetterParameter());
    }
}
