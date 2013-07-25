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

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.Ownable;
import com.lhkbob.entreri.Owner;
import com.lhkbob.entreri.property.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;

/**
 * MirrorComponentSpecification is an implementation that extracts a component specification from the mirror
 * API defined in javax.lang.model.  This should only be used in the context of an annotation processor with a
 * valid processing environment.
 *
 * @author Michael Ludwig
 */
class MirrorComponentSpecification implements ComponentSpecification {
    private final String typeName;
    private final String packageName;
    private final List<MirrorPropertyDeclaration> properties;

    public MirrorComponentSpecification(TypeElement type, Types tu, Elements eu, Filer io) {
        TypeMirror baseComponentType = eu.getTypeElement(Component.class.getCanonicalName()).asType();
        TypeMirror ownerType = eu.getTypeElement(Owner.class.getCanonicalName()).asType();
        TypeMirror ownableType = eu.getTypeElement(Ownable.class.getCanonicalName()).asType();
        TypeMirror objectType = eu.getTypeElement(Object.class.getCanonicalName()).asType();

        if (!tu.isAssignable(type.asType(), baseComponentType)) {
            throw fail(type.asType(), "Class must extend Component");
        }
        if (!type.getKind().equals(ElementKind.INTERFACE)) {
            throw fail(type.asType(), "Component definition must be an interface");
        }

        List<MirrorPropertyDeclaration> properties = new ArrayList<>();

        // since this is an interface, we're only dealing with public methods
        // so getMethods() returns everything we're interested in plus the methods
        // declared in Component, which we'll have to exclude
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(eu.getAllMembers(type));
        Map<String, ExecutableElement> getters = new HashMap<>();
        Map<String, ExecutableElement> setters = new HashMap<>();
        Map<String, Integer> setterParameters = new HashMap<>();

        for (ExecutableElement m : methods) {
            // exclude methods defined in Component, Owner, and Ownable
            String name = m.getSimpleName().toString();
            TypeMirror declare = m.getEnclosingElement().asType();

            if (tu.isSameType(declare, baseComponentType) ||
                tu.isSameType(declare, ownableType) ||
                tu.isSameType(declare, ownerType) ||
                tu.isSameType(declare, objectType)) {
                continue;
            }

            if (name.startsWith("is")) {
                processGetter(m, "is", getters);
            } else if (name.startsWith("has")) {
                processGetter(m, "has", getters);
            } else if (name.startsWith("get")) {
                processGetter(m, "get", getters);
            } else if (name.startsWith("set")) {
                processSetter(m, setters, setterParameters, tu);
            } else {
                throw fail(declare, name + " is an illegal property method");
            }
        }

        for (String property : getters.keySet()) {
            ExecutableElement getter = getters.get(property);
            ExecutableElement setter = setters.remove(property);
            Integer param = setterParameters.remove(property);

            if (setter == null) {
                throw fail(type.asType(), property + " has no matching setter");
            } else if (!tu.isSameType(getter.getReturnType(), setter.getParameters().get(param).asType())) {
                throw fail(type.asType(), property + " has inconsistent type");
            }

            TypeMirror propertyType = getPropertyType(getter, tu, eu, io);
            properties.add(new MirrorPropertyDeclaration(property, getter, setter, param,
                                                         (TypeElement) tu.asElement(propertyType)));
        }

        if (!setters.isEmpty()) {
            throw fail(type.asType(), setters.keySet() + " have no matching getters");
        }

        // order the list of properties by their natural ordering
        Collections.sort(properties);

        String qualifiedName = type.getQualifiedName().toString();
        String packageName = eu.getPackageOf(type).getQualifiedName().toString();
        if (packageName.isEmpty()) {
            typeName = qualifiedName;
            this.packageName = "";
        } else {
            typeName = qualifiedName.substring(packageName.length() + 1);
            this.packageName = packageName;
        }

        this.properties = Collections.unmodifiableList(properties);
    }

    @Override
    public String getType() {
        return typeName;
    }

    @Override
    public String getPackage() {
        return packageName;
    }

    @Override
    public List<? extends PropertyDeclaration> getProperties() {
        return properties;
    }

    private static IllegalComponentDefinitionException fail(TypeMirror type, String msg) {
        return new IllegalComponentDefinitionException(type.toString(), msg);
    }

    private static class MirrorPropertyDeclaration implements PropertyDeclaration {
        private final String name;

        private final String setter;
        private final int setterParameter;
        private final boolean setterReturnsComponent;

        private final String getter;
        private final boolean isSharedInstance;
        private final boolean isGeneric;

        private final String type;
        private final String propertyType;

        public MirrorPropertyDeclaration(String name, ExecutableElement getter, ExecutableElement setter,
                                         int parameter, TypeElement propertyType) {
            this.name = name;
            this.getter = getter.getSimpleName().toString();
            this.setter = setter.getSimpleName().toString();
            this.propertyType = propertyType.toString();
            setterParameter = parameter;

            type = getter.getReturnType().toString();
            setterReturnsComponent = !setter.getReturnType().getKind().equals(TypeKind.VOID);

            isSharedInstance = getter.getAnnotation(SharedInstance.class) != null;
            isGeneric = propertyType.getAnnotation(GenericProperty.class) != null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getPropertyImplementation() {
            return propertyType;
        }

        @Override
        public String getSetterMethod() {
            return setter;
        }

        @Override
        public String getGetterMethod() {
            return getter;
        }

        @Override
        public int getSetterParameter() {
            return setterParameter;
        }

        @Override
        public boolean getSetterReturnsComponent() {
            return setterReturnsComponent;
        }

        @Override
        public boolean isShared() {
            return isSharedInstance;
        }

        @Override
        public boolean isPropertyGeneric() {
            return isGeneric;
        }

        @Override
        public PropertyFactory<?> getPropertyFactory() {
            throw new UnsupportedOperationException("Cannot create PropertyFactory with mirror API");
        }

        @Override
        public int compareTo(PropertyDeclaration o) {
            return name.compareTo(o.getName());
        }
    }

    private static void processSetter(ExecutableElement m, Map<String, ExecutableElement> setters,
                                      Map<String, Integer> parameters, Types tu) {
        TypeMirror declaringClass = m.getEnclosingElement().asType();
        if (!tu.isSameType(m.getReturnType(), m.getEnclosingElement().asType()) &&
            !m.getReturnType().getKind().equals(TypeKind.VOID)) {
            throw fail(declaringClass, m + " has invalid return type for setter");
        }

        List<? extends VariableElement> params = m.getParameters();
        if (params.isEmpty()) {
            throw fail(declaringClass, m + " must have at least one parameter");
        }

        if (params.size() == 1) {
            String name = getNameFromParameter(params.get(0));
            if (name != null) {
                // verify absence of @Named on actual setter
                if (m.getAnnotation(Named.class) != null) {
                    throw fail(declaringClass, m + ", @Named cannot be on both parameter and method");
                }
            } else {
                name = getName(m, "set");
            }

            if (setters.containsKey(name)) {
                throw fail(declaringClass, name + " already declared on a setter");
            }
            setters.put(name, m);
            parameters.put(name, 0);
        } else {
            // verify absence of @Named on actual setter
            if (m.getAnnotation(Named.class) != null) {
                throw fail(declaringClass,
                           m + ", @Named cannot be applied to setter method with multiple parameters");
            }

            int i = 0;
            for (VariableElement p : params) {
                String name = getNameFromParameter(p);
                if (name == null) {
                    throw fail(declaringClass, m +
                                               ", @Named must be applied to each parameter for multi-parameter setter methods");
                }

                if (setters.containsKey(name)) {
                    throw fail(declaringClass, name + " already declared on a setter");
                }

                setters.put(name, m);
                parameters.put(name, i++);
            }
        }
    }

    private static void processGetter(ExecutableElement m, String prefix,
                                      Map<String, ExecutableElement> getters) {
        TypeMirror declaringClass = m.getEnclosingElement().asType();

        String name = getName(m, prefix);
        if (getters.containsKey(name)) {
            throw fail(declaringClass, name + " already declared on a getter");
        }
        if (!m.getParameters().isEmpty()) {
            throw fail(declaringClass, m + ", getter must not take arguments");
        }
        if (m.getReturnType().getKind().equals(TypeKind.VOID)) {
            throw fail(declaringClass, m + ", getter must have non-void return type");
        }

        getters.put(name, m);
    }

    private static String getNameFromParameter(VariableElement parameter) {
        Named n = parameter.getAnnotation(Named.class);
        if (n != null) {
            return n.value();
        } else {
            return null;
        }
    }

    private static String getName(ExecutableElement m, String prefix) {
        Named n = m.getAnnotation(Named.class);
        if (n != null) {
            return n.value();
        } else {
            String name = m.getSimpleName().toString();
            return Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
        }
    }

    private static TypeMirror getPropertyType(ExecutableElement getter, Types tu, Elements eu, Filer io) {
        TypeMirror baseType = getter.getReturnType();

        TypeMirror factory = getFactory(getter);
        if (factory != null) {
            // prefer getter specification to allow default overriding
            return validateFactory(getter, factory, null, tu, eu);
        } else {
            // try to find a default property type
            TypeElement mappedType;
            switch (baseType.getKind()) {
            case BOOLEAN:
                mappedType = eu.getTypeElement(BooleanProperty.class.getCanonicalName());
                break;
            case BYTE:
                mappedType = eu.getTypeElement(ByteProperty.class.getCanonicalName());
                break;
            case CHAR:
                mappedType = eu.getTypeElement(CharProperty.class.getCanonicalName());
                break;
            case DOUBLE:
                mappedType = eu.getTypeElement(DoubleProperty.class.getCanonicalName());
                break;
            case FLOAT:
                mappedType = eu.getTypeElement(FloatProperty.class.getCanonicalName());
                break;
            case INT:
                mappedType = eu.getTypeElement(IntProperty.class.getCanonicalName());
                break;
            case LONG:
                mappedType = eu.getTypeElement(LongProperty.class.getCanonicalName());
                break;
            case SHORT:
                mappedType = eu.getTypeElement(ShortProperty.class.getCanonicalName());
                break;
            default:
                FileObject mapping;
                try {
                    mapping = io.getResource(StandardLocation.CLASS_PATH, "",
                                             TypePropertyMapping.MAPPING_DIR + baseType.toString());
                } catch (IOException e) {
                    // if an IO is thrown here, it means it couldn't find the file
                    mapping = null;
                }

                if (mapping != null) {
                    try {
                        String content = mapping.getCharContent(true).toString().trim();
                        mappedType = eu.getTypeElement(content);
                    } catch (IOException e) {
                        // if an IO is thrown here, however, it means errors accessing
                        // the file, which we can't recover from
                        throw new RuntimeException(e);
                    }
                } else {
                    TypeMirror enumType = tu
                            .erasure(eu.getTypeElement(Enum.class.getCanonicalName()).asType());

                    if (tu.isAssignable(baseType, enumType)) {
                        mappedType = eu.getTypeElement(EnumProperty.class.getCanonicalName());
                    } else {
                        mappedType = eu.getTypeElement(ObjectProperty.class.getCanonicalName());
                    }
                }
            }

            factory = getFactory(mappedType);
            if (factory == null) {
                throw fail(getter.getEnclosingElement().asType(), mappedType + " has no @Factory annotation");
            } else {
                return validateFactory(getter, factory, mappedType, tu, eu);
            }
        }
    }

    private static TypeMirror getFactory(Element e) {
        try {
            com.lhkbob.entreri.property.Factory factory = e
                    .getAnnotation(com.lhkbob.entreri.property.Factory.class);
            if (factory != null) {
                factory.value(); // will throw an exception
            }
            return null;
        } catch (MirroredTypeException te) {
            return te.getTypeMirror();
        }
    }

    private static TypeMirror getGenericPropertySuperclass(Element e) {
        try {
            GenericProperty generic = e.getAnnotation(GenericProperty.class);
            if (generic != null) {
                generic.superClass(); // will throw an exception
            }
            return null;
        } catch (MirroredTypeException te) {
            return te.getTypeMirror();
        }
    }

    private static TypeMirror validateFactory(ExecutableElement getter, TypeMirror factory,
                                              TypeElement propertyType, Types tu, Elements eu) {
        TypeMirror declaringClass = getter.getEnclosingElement().asType();
        boolean isShared = getter.getAnnotation(SharedInstance.class) != null;
        TypeMirror baseType = getter.getReturnType();

        TypeMirror createdType = null;
        List<? extends ExecutableElement> factoryMethods = ElementFilter
                .methodsIn(eu.getAllMembers((TypeElement) tu.asElement(factory)));
        for (ExecutableElement m : factoryMethods) {
            if (m.getSimpleName().contentEquals("create")) {
                createdType = m.getReturnType();
                break;
            }
        }
        if (createdType == null) {
            throw fail(declaringClass, factory + " is missing create() method");
        }

        if (propertyType == null) {
            // rely on factory to determine property type
            propertyType = (TypeElement) tu.asElement(createdType);
        } else {
            // make sure factory returns an assignable type
            if (!tu.isAssignable(createdType, propertyType.asType())) {
                throw fail(declaringClass, "Factory creates " + createdType +
                                           ", which is incompatible with expected type " +
                                           propertyType);
            }
        }

        // verify contract of property
        TypeMirror intType = tu.getPrimitiveType(TypeKind.INT);
        TypeMirror voidType = tu.getNoType(TypeKind.VOID);
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(eu.getAllMembers(propertyType));

        TypeMirror asType = propertyType.asType();
        TypeMirror genericType = getGenericPropertySuperclass(propertyType);
        if (genericType != null) {
            // special case for properties that claim to support more permissive assignments
            if (isShared) {
                throw fail(declaringClass,
                           propertyType + " can't be used with @SharedInstance, it is declared generic");
            }

            // ensure that the getter and setter methods exis with the declared super class type
            // and that the base value type is assignable to the super type
            if (!findMethod(methods, tu, "get", genericType, intType)) {
                throw fail(declaringClass,
                           propertyType + " does not implement generic " + genericType + " get(int)");
            }
            if (!findMethod(methods, tu, "set", voidType, intType, genericType)) {
                throw fail(declaringClass,
                           propertyType + " does not implement generic void set(int, " + baseType + ")");
            }

            if (!tu.isAssignable(baseType, genericType)) {
                throw fail(declaringClass,
                           propertyType + " cannot be used with " + baseType + ", type must extend from " +
                           genericType);
            }
        } else {
            if (!findMethod(methods, tu, "get", baseType, intType)) {
                throw fail(declaringClass, propertyType + " does not implement " + baseType + " get(int)");
            }
            if (!findMethod(methods, tu, "set", voidType, intType, baseType)) {
                throw fail(declaringClass, propertyType + " does not implement void set(int, " +
                                           baseType + ")");
            }

            if (isShared) {
                // we could instantiate the declared type, but that crashes if the parameter
                // type must be a primitive, so the erased type gives us a good enough check
                TypeMirror share = tu
                        .erasure(eu.getTypeElement(ShareableProperty.class.getCanonicalName()).asType());
                if (!tu.isAssignable(asType, share)) {
                    throw fail(declaringClass, propertyType + " can't be used with @SharedInstance");
                }

                // verify additional shareable property contract
                if (!findMethod(methods, tu, "get", voidType, intType, baseType)) {
                    throw fail(declaringClass, propertyType + " does not implement void get(int, " +
                                               baseType + ")");
                }
                if (!findMethod(methods, tu, "createShareableInstance", baseType)) {
                    throw fail(declaringClass, propertyType + " does not implement " + baseType +
                                               " createShareableInstance()");
                }
            }
        }

        return tu.erasure(asType);
    }

    private static boolean findMethod(List<? extends ExecutableElement> methods, Types tu, String name,
                                      TypeMirror returnType, TypeMirror... params) {
        for (ExecutableElement m : methods) {
            if (m.getSimpleName().contentEquals(name) && tu.isSameType(returnType, m.getReturnType())) {
                // now check parameters
                List<? extends VariableElement> realParams = m.getParameters();
                if (params.length == realParams.size()) {
                    boolean found = true;
                    for (int i = 0; i < params.length; i++) {
                        if (!tu.isSameType(params[i], realParams.get(i).asType())) {
                            found = false;
                            break;
                        }
                    }

                    if (found) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
