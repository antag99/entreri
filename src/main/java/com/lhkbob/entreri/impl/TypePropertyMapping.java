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

import com.lhkbob.entreri.property.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypePropertyMapping is an internal class used to maintain a thread-safe, shared, and
 * consistent mapping from Java type to an associated Property type that wraps that data.
 * Primitive and plain Object wrapping is built-in. A property can be overridden for a
 * class by placing the file META-INF/entreri/mapping/&lt;CANONICAL CLASS NAME&gt; in the
 * class path, with a single string &lt;BINARY CLASS NAME OF PROPERTY&gt;.
 *
 * @author Michael Ludwig
 */
public final class TypePropertyMapping {
    public static final String MAPPING_DIR = "META-INF/entreri/mapping/";

    private static final ConcurrentHashMap<Class<?>, Class<? extends Property>> typeMapping;

    static {
        typeMapping = new ConcurrentHashMap<>();

        // default mapping for primitive types
        typeMapping.put(byte.class, ByteProperty.class);
        typeMapping.put(short.class, ShortProperty.class);
        typeMapping.put(int.class, IntProperty.class);
        typeMapping.put(long.class, LongProperty.class);
        typeMapping.put(float.class, FloatProperty.class);
        typeMapping.put(double.class, DoubleProperty.class);
        typeMapping.put(char.class, CharProperty.class);
        typeMapping.put(boolean.class, BooleanProperty.class);
        typeMapping.put(Object.class, ObjectProperty.class);
    }

    private TypePropertyMapping() {
    }

    /**
     * Attempt to determine a property class that wraps the corresponding Java type. If it
     * is a primitive type, it will use the corresponding primitive wrapper defined in
     * com.lhkbob.entreri.property.
     * <p/>
     * Unless the type has an available mapping file, it will fallback to ObjectProperty.
     *
     * @param type The Java type that needs to be wrapped as a property
     *
     * @return The discovered or cached property type for the given type
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Property> getPropertyForType(Class<?> type) {
        Class<? extends Property> pType = typeMapping.get(type);
        if (pType != null) {
            // already mapped file exists (statically or from previous META-INF load)
            return pType;
        }

        ClassLoader loader = type.getClassLoader();
        if (loader != null) {
            try {
                // otherwise check if we have a properties file to load
                Enumeration<URL> urls = loader
                        .getResources(MAPPING_DIR + type.getCanonicalName());
                if (urls.hasMoreElements()) {
                    URL mapping = urls.nextElement();
                    if (urls.hasMoreElements()) {
                        throw new RuntimeException("Multiple mapping files for " + type +
                                                   " present in classpath");
                    }


                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(mapping.openStream()));
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
                        pType = (Class<? extends Property>) type.getClassLoader()
                                                                .loadClass(className
                                                                                   .toString()
                                                                                   .trim());

                        // store the mapping for later as well, this is safe because
                        // a class's loader is part of its identity and the property impl
                        // will use the same loader; even if we concurrently modify it,
                        // the same value will be stored
                        typeMapping.put(type, pType);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(
                                "Unable to load mapped Property class for " + type, e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Error reading META-INF mapping for class: " + type, e);
            }
        }

        if (pType == null) {
            // generic fallback
            pType = ObjectProperty.class;
        }

        return pType;
    }
}
