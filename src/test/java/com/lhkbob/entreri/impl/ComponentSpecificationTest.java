/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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

public class ComponentSpecificationTest {
   /* @Test
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

    private boolean isShared(PropertyDeclaration prop) {
        List<MethodDeclaration> methods = prop.getMethods();
        for (MethodDeclaration m : methods) {
            Set<Annotation> attrs = m.getAttributes();
            for (Annotation a : attrs) {
                if (a instanceof SharedInstance) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isGeneric(PropertyDeclaration prop) throws Exception {
        Class<?> propClass = Class.forName(prop.getPropertyImplementation());
        return propClass.getAnnotation(GenericProperty.class) != null;
    }

    private void assertSimpleGetter(String name, PropertyDeclaration prop) {
        for (MethodDeclaration m : prop.getMethods()) {
            if (m.getName().equals(name)) {
                Assert.assertEquals(prop.getType(), m.getReturnType());
                Assert.assertTrue(m.getParameterNames().isEmpty());
                return;
            }
        }
        Assert.fail("method could not be found");
    }

    private void assertSimpleSetter(String name, PropertyDeclaration prop) {
        for (MethodDeclaration m : prop.getMethods()) {
            if (m.getName().equals(name)) {
                if (!"void".equals(m.getReturnType())) {
                    Assert.assertEquals("com.lhkbob.entreri.components.ComplexComponent", m.getReturnType());
                }
                Assert.assertEquals(1, m.getParameterNames().size());
                Assert.assertEquals(prop.getType(), m.getParameterTypes().get(0));
                return;
            }
        }
        Assert.fail("method could not be found");
    }

    private void assertSetter(String name, int totalArgs, int propParam, PropertyDeclaration prop) {
        Assert.assertTrue(propParam < totalArgs && propParam >= 0);
        for (MethodDeclaration m : prop.getMethods()) {
            if (m.getName().equals(name)) {
                if (!"void".equals(m.getReturnType())) {
                    Assert.assertEquals("com.lhkbob.entreri.components.ComplexComponent", m.getReturnType());
                }
                Assert.assertEquals(totalArgs, m.getParameterNames().size());
                Assert.assertEquals(prop.getType(), m.getParameterTypes().get(propParam));
                return;
            }
        }
        Assert.fail("method could not be found");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyLookup() throws Exception {
        ComponentSpecification spec = ComponentSpecification.Factory.fromClass(ComplexComponent.class);
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
        PropertyDeclaration intSpec = props.remove("int");
        Assert.assertNotNull(intSpec);
        Assert.assertFalse(isShared(intSpec));
        Assert.assertFalse(isGeneric(intSpec));
        IntProperty intProp = (IntProperty) intSpec.getPropertyFactory().create();
        ((IntProperty.Factory) intSpec.getPropertyFactory()).setDefaultValue(intProp, 0);
        Assert.assertEquals(0, intProp.get(0));
        assertSimpleGetter("getInt", intSpec);
        assertSimpleSetter("setInt", intSpec);

        // property - float from FloatComponent
        PropertyDeclaration floatSpec = props.remove("float");
        Assert.assertNotNull(floatSpec);
        Assert.assertFalse(isShared(floatSpec));
        Assert.assertFalse(isGeneric(floatSpec));
        FloatProperty floatProp = (FloatProperty) floatSpec.getPropertyFactory().create();
        ((FloatProperty.Factory) floatSpec.getPropertyFactory()).setDefaultValue(floatProp, 0);
        Assert.assertEquals(0f, floatProp.get(0), 0.0001f);
        assertSimpleGetter("getFloat", floatSpec);
        assertSimpleSetter("setFloat", floatSpec);

        // property - long
        PropertyDeclaration longSpec = props.remove("long");
        Assert.assertNotNull(longSpec);
        Assert.assertFalse(isShared(longSpec));
        Assert.assertFalse(isGeneric(longSpec));
        LongProperty longProp = (LongProperty) longSpec.getPropertyFactory().create();
        ((LongProperty.Factory) longSpec.getPropertyFactory()).setDefaultValue(longProp, 0);
        Assert.assertEquals(Long.MAX_VALUE, longProp.get(0));
        assertSimpleGetter("getLong", longSpec);
        assertSimpleSetter("setLong", longSpec);

        // property - factoryFloat
        PropertyDeclaration facFloatSpec = props.remove("factoryFloat");
        Assert.assertNotNull(facFloatSpec);
        Assert.assertFalse(isShared(facFloatSpec));
        Assert.assertFalse(isGeneric(facFloatSpec));
        FloatProperty floatFactoryProp = (FloatProperty) facFloatSpec.getPropertyFactory().create();
        ((FloatPropertyOverride) facFloatSpec.getPropertyFactory()).setDefaultValue(floatFactoryProp, 0);
        Assert.assertEquals(FloatPropertyOverride.DEFAULT, floatFactoryProp.get(0), 0.0001f);
        assertSimpleGetter("getFactoryFloat", facFloatSpec);
        assertSimpleSetter("setFactoryFloat", facFloatSpec);

        // property - param1
        PropertyDeclaration param1Spec = props.remove("param1");
        Assert.assertNotNull(param1Spec);
        Assert.assertFalse(isShared(param1Spec));
        Assert.assertFalse(isGeneric(param1Spec));
        ShortProperty param1Prop = (ShortProperty) param1Spec.getPropertyFactory().create();
        ((ShortProperty.Factory) param1Spec.getPropertyFactory()).setDefaultValue(param1Prop, 0);
        Assert.assertEquals((short) 0, param1Prop.get(0));
        assertSimpleGetter("getParam1", param1Spec);
        assertSetter("setParams", 2, 0, param1Spec);

        // property - param2
        PropertyDeclaration param2Spec = props.remove("param2");
        Assert.assertNotNull(param2Spec);
        Assert.assertFalse(isShared(param2Spec));
        Assert.assertFalse(isGeneric(param2Spec));
        ShortProperty param2Prop = (ShortProperty) param2Spec.getPropertyFactory().create();
        ((ShortProperty.Factory) param2Spec.getPropertyFactory()).setDefaultValue(param2Prop, 0);
        Assert.assertEquals((short) 0, param2Prop.get(0));
        assertSimpleGetter("getParam2", param2Spec);
        assertSetter("setParams", 2, 1, param2Spec);

        // property - foo-blah
        PropertyDeclaration fooblahSpec = props.remove("foo-blah");
        Assert.assertNotNull(fooblahSpec);
        Assert.assertFalse(isShared(fooblahSpec));
        Assert.assertFalse(isGeneric(fooblahSpec));
        BooleanProperty fooblahProp = (BooleanProperty) fooblahSpec.getPropertyFactory().create();
        ((BooleanProperty.Factory) fooblahSpec.getPropertyFactory()).setDefaultValue(fooblahProp, 0);
        Assert.assertEquals(false, fooblahProp.get(0));
        assertSimpleGetter("isNamedParamGetter", fooblahSpec);
        assertSimpleSetter("setNamedParamSetter", fooblahSpec);

        // property - bletch
        PropertyDeclaration bletchSpec = props.remove("bletch");
        Assert.assertNotNull(bletchSpec);
        Assert.assertTrue(isShared(bletchSpec));
        Assert.assertFalse(isGeneric(bletchSpec));
        CustomProperty bletchProp = (CustomProperty) bletchSpec.getPropertyFactory().create();
        ((CustomProperty.CustomFactoryWithAttributes) bletchSpec.getPropertyFactory())
                .setDefaultValue(bletchProp, 0);
        Assert.assertEquals(14, bletchProp.get(0).value);
        assertSimpleGetter("hasBletch", bletchSpec);
        assertSimpleSetter("setBletch", bletchSpec);

        // property - superValue
        PropertyDeclaration superSpec = props.remove("superValue");
        Assert.assertNotNull(superSpec);
        Assert.assertFalse(isShared(superSpec));
        Assert.assertFalse(isGeneric(superSpec));
        IntProperty superProp = (IntProperty) superSpec.getPropertyFactory().create();
        ((IntProperty.Factory) superSpec.getPropertyFactory()).setDefaultValue(superProp, 0);
        Assert.assertEquals(0, superProp.get(0));
        assertSimpleGetter("getSuperValue", superSpec);
        assertSimpleSetter("setSuperValue", superSpec);

        // property - enum
        PropertyDeclaration enumSpec = props.remove("enum");
        Assert.assertNotNull(enumSpec);
        Assert.assertFalse(isShared(enumSpec));
        Assert.assertTrue(isGeneric(enumSpec));
        EnumProperty enumProp = (EnumProperty) enumSpec.getPropertyFactory().create();
        ((EnumProperty.Factory) enumSpec.getPropertyFactory()).setDefaultValue(enumProp, 0);
        Assert.assertEquals(ComplexComponent.TestEnum.V1, enumProp.get(0));
        assertSimpleGetter("getEnum", enumSpec);
        assertSimpleSetter("setEnum", enumSpec);

        // no other properties
        Assert.assertTrue(props.isEmpty());
    }
    */
}
