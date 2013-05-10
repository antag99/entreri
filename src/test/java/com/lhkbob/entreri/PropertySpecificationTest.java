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

import com.lhkbob.entreri.component.*;
import com.lhkbob.entreri.property.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertySpecificationTest {
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
        doInvalidComponentDefinitionTest(AbstractComponent.class);
        doInvalidComponentDefinitionTest(BadConstructorComponent.class);
        doInvalidComponentDefinitionTest(InvalidFactoryComponent.class);
        doInvalidComponentDefinitionTest(MissingSetterComponent.class);
        doInvalidComponentDefinitionTest(MissingGetterComponent.class);
        doInvalidComponentDefinitionTest(MismatchedNameComponent.class);
        doInvalidComponentDefinitionTest(MismatchedTypeComponent.class);
        doInvalidComponentDefinitionTest(NonBeanComponent.class);
        doInvalidComponentDefinitionTest(InvalidPropertyComponent.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doValidComponentDefinitionTest(Class type) {
        PropertySpecification.getSpecification(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doInvalidComponentDefinitionTest(Class type) {
        try {
            PropertySpecification.getSpecification(type);
            Assert.fail("Expected IllegalComponentDefinitionException");
        } catch (IllegalComponentDefinitionException e) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyLookup() throws Exception {
        List<PropertySpecification> origProps = PropertySpecification
                .getSpecification(ComplexComponent.class);

        // for this test, convert it to a string key map
        Map<String, PropertySpecification> props = new HashMap<String, PropertySpecification>();
        for (PropertySpecification e : origProps) {
            props.put(e.getName(), e);
        }

        // verify that properties are mapped properly

        // property - int from IntComponent
        PropertySpecification intSpec = props.get("int");
        Assert.assertNotNull(intSpec);
        Assert.assertFalse(intSpec.isSharedInstance());
        IntProperty intProp = (IntProperty) intSpec.getFactory().create();
        ((IntProperty.Factory) intSpec.getFactory()).setDefaultValue(intProp, 0);
        Assert.assertEquals(0, intProp.get(0));
        Assert.assertEquals(IntComponent.class.getMethod("getInt"),
                            intSpec.getGetterMethod());
        Assert.assertEquals(IntComponent.class.getMethod("setInt", int.class),
                            intSpec.getSetterMethod());
        Assert.assertEquals(0, intSpec.getSetterParameter());

        // property - float from FloatComponent
        PropertySpecification floatSpec = props.get("float");
        Assert.assertNotNull(floatSpec);
        Assert.assertFalse(floatSpec.isSharedInstance());
        FloatProperty floatProp = (FloatProperty) floatSpec.getFactory().create();
        ((FloatProperty.Factory) floatSpec.getFactory()).setDefaultValue(floatProp, 0);
        Assert.assertEquals(0f, floatProp.get(0), 0.0001f);
        Assert.assertEquals(FloatComponent.class.getMethod("getFloat"),
                            floatSpec.getGetterMethod());
        Assert.assertEquals(FloatComponent.class.getMethod("setFloat", float.class),
                            floatSpec.getSetterMethod());
        Assert.assertEquals(0, floatSpec.getSetterParameter());

        // property - long
        PropertySpecification longSpec = props.get("long");
        Assert.assertNotNull(longSpec);
        Assert.assertFalse(longSpec.isSharedInstance());
        LongProperty longProp = (LongProperty) longSpec.getFactory().create();
        ((LongProperty.Factory) longSpec.getFactory()).setDefaultValue(longProp, 0);
        Assert.assertEquals(Long.MAX_VALUE, longProp.get(0));
        Assert.assertEquals(ComplexComponent.class.getMethod("getLong"),
                            longSpec.getGetterMethod());
        Assert.assertEquals(ComplexComponent.class.getMethod("setLong", long.class),
                            longSpec.getSetterMethod());
        Assert.assertEquals(0, longSpec.getSetterParameter());

        // property - factoryFloat
        PropertySpecification facFloatSpec = props.get("factoryFloat");
        Assert.assertNotNull(facFloatSpec);
        Assert.assertFalse(facFloatSpec.isSharedInstance());
        FloatProperty floatFactoryProp = (FloatProperty) facFloatSpec.getFactory()
                                                                     .create();
        ((FloatPropertyFactory) facFloatSpec.getFactory())
                .setDefaultValue(floatFactoryProp, 0);
        Assert.assertEquals(FloatPropertyFactory.DEFAULT, floatFactoryProp.get(0),
                            0.0001f);
        Assert.assertEquals(ComplexComponent.class.getMethod("getFactoryFloat"),
                            facFloatSpec.getGetterMethod());
        Assert.assertEquals(
                ComplexComponent.class.getMethod("setFactoryFloat", float.class),
                facFloatSpec.getSetterMethod());
        Assert.assertEquals(0, facFloatSpec.getSetterParameter());

        // property - param1
        PropertySpecification param1Spec = props.get("param1");
        Assert.assertNotNull(param1Spec);
        Assert.assertFalse(param1Spec.isSharedInstance());
        ShortProperty param1Prop = (ShortProperty) param1Spec.getFactory().create();
        ((ShortProperty.Factory) param1Spec.getFactory()).setDefaultValue(param1Prop, 0);
        Assert.assertEquals((short) 0, param1Prop.get(0));
        Assert.assertEquals(ComplexComponent.class.getMethod("getParam1"),
                            param1Spec.getGetterMethod());
        Assert.assertEquals(
                ComplexComponent.class.getMethod("setParams", short.class, short.class),
                param1Spec.getSetterMethod());
        Assert.assertEquals(0, param1Spec.getSetterParameter());

        // property - param2
        PropertySpecification param2Spec = props.get("param2");
        Assert.assertNotNull(param2Spec);
        Assert.assertFalse(param2Spec.isSharedInstance());
        ShortProperty param2Prop = (ShortProperty) param2Spec.getFactory().create();
        ((ShortProperty.Factory) param2Spec.getFactory()).setDefaultValue(param2Prop, 0);
        Assert.assertEquals((short) 0, param2Prop.get(0));
        Assert.assertEquals(ComplexComponent.class.getMethod("getParam2"),
                            param2Spec.getGetterMethod());
        Assert.assertEquals(
                ComplexComponent.class.getMethod("setParams", short.class, short.class),
                param2Spec.getSetterMethod());
        Assert.assertEquals(1, param2Spec.getSetterParameter());

        // property - foo-blah
        PropertySpecification fooblahSpec = props.get("foo-blah");
        Assert.assertNotNull(fooblahSpec);
        Assert.assertFalse(fooblahSpec.isSharedInstance());
        BooleanProperty fooblahProp = (BooleanProperty) fooblahSpec.getFactory().create();
        ((BooleanProperty.Factory) fooblahSpec.getFactory())
                .setDefaultValue(fooblahProp, 0);
        Assert.assertEquals(false, fooblahProp.get(0));
        Assert.assertEquals(ComplexComponent.class.getMethod("isNamedParamGetter"),
                            fooblahSpec.getGetterMethod());
        Assert.assertEquals(
                ComplexComponent.class.getMethod("setNamedParamSetter", boolean.class),
                fooblahSpec.getSetterMethod());
        Assert.assertEquals(0, fooblahSpec.getSetterParameter());

        // property - bletch
        PropertySpecification bletchSpec = props.get("bletch");
        Assert.assertNotNull(bletchSpec);
        Assert.assertTrue(bletchSpec.isSharedInstance());
        CustomProperty bletchProp = (CustomProperty) bletchSpec.getFactory().create();
        ((CustomProperty.CustomFactoryWithAttributes) bletchSpec.getFactory())
                .setDefaultValue(bletchProp, 0);
        Assert.assertEquals(14, bletchProp.get(0).value);
        Assert.assertEquals(ComplexComponent.class.getMethod("hasBletch"),
                            bletchSpec.getGetterMethod());
        Assert.assertEquals(ComplexComponent.class
                                    .getMethod("setBletch", CustomProperty.Bletch.class),
                            bletchSpec.getSetterMethod());
        Assert.assertEquals(0, bletchSpec.getSetterParameter());
    }
}
