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

import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.attr.Factory;
import com.lhkbob.entreri.property.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypePropertyMapping
 * ===================
 *
 * TypePropertyMapping is an internal class used to maintain a thread-safe, shared, and consistent mapping
 * from Java type to an associated Property type that wraps that data. Primitive and plain Object wrapping is
 * built-in. A property can be overridden for a class by placing the file
 *
 * ```
 * META-INF/entreri/mapping/<CANONICAL CLASS NAME>;
 * ```
 *
 * in the class path, with a single string `BINARY CLASS NAME OF PROPERTY` as its contents. The class name
 * must either be a Property implementation that specifies an @Factory annotation, or the class of a
 * PropertyFactory supporting the mapped type.
 *
 * @author Michael Ludwig
 */
public final class TypePropertyMapping {
    public static final String MAPPING_DIR = "META-INF/entreri/mapping/";

    private static final ConcurrentHashMap<Class<?>, Class<? extends PropertyFactory<?>>> typeMapping;

    static {
        typeMapping = new ConcurrentHashMap<>();

        // default mapping for primitive types
        typeMapping.put(byte.class, ByteProperty.Factory.class);
        typeMapping.put(short.class, ShortProperty.Factory.class);
        typeMapping.put(int.class, IntProperty.Factory.class);
        typeMapping.put(long.class, LongProperty.Factory.class);
        typeMapping.put(float.class, FloatProperty.Factory.class);
        typeMapping.put(double.class, DoubleProperty.Factory.class);
        typeMapping.put(char.class, CharProperty.Factory.class);
        typeMapping.put(boolean.class, BooleanProperty.Factory.class);
    }

    private TypePropertyMapping() {
    }

    /**
     * Attempt to determine a PropertyFactory class that wraps the corresponding Java type. If it is a primitive
     * type, it will use the corresponding primitive wrapper defined in com.lhkbob.entreri.property.
     *
     * Unless the type has an available mapping file, it will fallback to ObjectProperty.Factory.
     *
     * @param type The Java type that needs to be wrapped as a property
     * @return The discovered or cached property type for the given type
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends PropertyFactory<?>> getPropertyFactory(Class<?> type) {
        Class<? extends PropertyFactory<?>> pType = typeMapping.get(type);
        if (pType != null) {
            // already mapped file exists (statically or from previous META-INF load)
            return pType;
        }

        ClassLoader loader = type.getClassLoader();
        if (loader != null) {
            try {
                // otherwise check if we have a properties file to load
                Enumeration<URL> urls = loader.getResources(MAPPING_DIR + type.getCanonicalName());
                if (urls.hasMoreElements()) {
                    URL mapping = urls.nextElement();
                    if (urls.hasMoreElements()) {
                        throw new RuntimeException("Multiple mapping files for " + type +
                                                   " present in classpath");
                    }


                    BufferedReader in = new BufferedReader(new InputStreamReader(mapping.openStream()));
                    String line;
                    StringBuilder className = new StringBuilder();
                    // be somewhat permissive of whitespace (any other input most likely
                    // will fail to load a class)
                    while ((line = in.readLine()) != null) {
                        className.append(line);
                    }
                    in.close();

                    // use the type's class loader so that the loaded property is tied to
                    // the same loader
                    try {
                        Class<?> mappedType = type.getClassLoader().loadClass(className.toString().trim());
                        if (PropertyFactory.class.isAssignableFrom(mappedType)) {
                            pType = (Class<? extends PropertyFactory<?>>) mappedType;
                        } else if (Property.class.isAssignableFrom(mappedType)) {
                            if (mappedType.getAnnotation(Factory.class) != null) {
                                pType = mappedType.getAnnotation(Factory.class).value();
                            } else {
                                throw new IllegalComponentDefinitionException(type.getName(),
                                                                              "Property does not have @Factory annotation: " +
                                                                              className);
                            }
                        } else {
                            throw new IllegalComponentDefinitionException(type.getName(),
                                                                          "Type mapping must be a Property or PropertyFactory, not: " +
                                                                          className);
                        }

                        // store the mapping for later as well, this is safe because
                        // a class's loader is part of its identity and the property impl
                        // will use the same loader; even if we concurrently modify it,
                        // the same value will be stored
                        typeMapping.put(type, pType);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unable to load mapped Property class for " + type, e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading META-INF mapping for class: " + type, e);
            }
        }

        if (pType == null) {
            // generic fallbacks
            if (Enum.class.isAssignableFrom(type)) {
                pType = EnumProperty.Factory.class;
            } else {
                pType = ObjectProperty.Factory.class;
            }
        }

        return pType;
    }

    /**
     * Find the PropertyFactory mapping given `baseType`. For all intents and purposes this is equivalent to
     * {@link #getPropertyFactory(Class)} except that it uses Java's mirror API instead of its reflection
     * API. This does not cache the mappings read from any `META-INF` files.
     *
     * @param baseType The base type the property muse support
     * @param tu       The Types utility object used during annotation processing
     * @param eu       The Elements utility object used during annotation processing
     * @param io       The Filer utility object used during annotation processing
     * @return A TypeMirror that represents a subclass of PropertyFactory
     */
    public static TypeMirror getPropertyFactory(TypeMirror baseType, Types tu, Elements eu, Filer io) {
        // note that we don't cache these because the type mirrors and elements are specific to the annotation
        // processing context
        TypeMirror propertyType = eu.getTypeElement(Property.class.getCanonicalName()).asType();
        TypeMirror propertyFactoryType = tu.erasure(eu.getTypeElement(PropertyFactory.class
                                                                              .getCanonicalName()).asType());

        // try to find a default property type
        TypeMirror mappedType;
        switch (baseType.getKind()) {
        case BOOLEAN:
            mappedType = eu.getTypeElement(BooleanProperty.Factory.class.getCanonicalName()).asType();
            break;
        case BYTE:
            mappedType = eu.getTypeElement(ByteProperty.Factory.class.getCanonicalName()).asType();
            break;
        case CHAR:
            mappedType = eu.getTypeElement(CharProperty.Factory.class.getCanonicalName()).asType();
            break;
        case DOUBLE:
            mappedType = eu.getTypeElement(DoubleProperty.Factory.class.getCanonicalName()).asType();
            break;
        case FLOAT:
            mappedType = eu.getTypeElement(FloatProperty.Factory.class.getCanonicalName()).asType();
            break;
        case INT:
            mappedType = eu.getTypeElement(IntProperty.Factory.class.getCanonicalName()).asType();
            break;
        case LONG:
            mappedType = eu.getTypeElement(LongProperty.Factory.class.getCanonicalName()).asType();
            break;
        case SHORT:
            mappedType = eu.getTypeElement(ShortProperty.Factory.class.getCanonicalName()).asType();
            break;
        default:
            FileObject mapping;
            try {
                mapping = io.getResource(StandardLocation.CLASS_PATH, "", MAPPING_DIR + baseType.toString());
            } catch (IOException e) {
                // if an IO is thrown here, it means it couldn't find the file
                mapping = null;
            }

            if (mapping != null) {
                try {
                    String content = mapping.getCharContent(true).toString().trim();
                    mappedType = eu.getTypeElement(content).asType();
                    if (tu.isAssignable(mappedType, propertyType)) {
                        // get factory from @Factory annotation on property class
                        mappedType = getFactory(tu.asElement(mappedType));
                        if (mappedType == null) {
                            throw new IllegalComponentDefinitionException(baseType.toString(),
                                                                          "Property does not have @Factory annotation: " +
                                                                          content);
                        }
                    } else if (!tu.isAssignable(mappedType, propertyFactoryType)) {
                        throw new IllegalComponentDefinitionException(baseType.toString(),
                                                                      "Type mapping must be a Property or PropertyFactory, not: " +
                                                                      mappedType.toString());
                    }
                } catch (IOException e) {
                    // if an IO is thrown here, however, it means errors accessing
                    // the file, which we can't recover from
                    throw new RuntimeException(e);
                }
            } else {
                TypeMirror enumType = tu.erasure(eu.getTypeElement(Enum.class.getCanonicalName()).asType());

                if (tu.isAssignable(baseType, enumType)) {
                    mappedType = eu.getTypeElement(EnumProperty.Factory.class.getCanonicalName()).asType();
                } else {
                    mappedType = eu.getTypeElement(ObjectProperty.Factory.class.getCanonicalName()).asType();
                }
            }
        }

        return mappedType;
    }

    private static TypeMirror getFactory(Element e) {
        try {
            Factory factory = e.getAnnotation(Factory.class);
            if (factory != null) {
                factory.value(); // will throw an exception
            }
            return null;
        } catch (MirroredTypeException te) {
            return te.getTypeMirror();
        }
    }
}
