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

import com.lhkbob.entreri.property.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypePropertyMapping is an internal class used to maintain a thread-safe, shared, and
 * consistent mapping from Java type to an associated Property type that wraps that data.
 * Primitive and plain Object wrapping is built-in and it supports reading a
 * 'entreri-mapping.properties' file in the type's resources to discover new mappings at
 * runtime. Said properties file is a simple key-value map between basic class and
 * Property class.
 */
final class TypePropertyMapping {
    private static final ConcurrentHashMap<Class<?>, Class<? extends Property>> typeMapping;

    static {
        typeMapping = new ConcurrentHashMap<Class<?>, Class<? extends Property>>();

        // default mapping for primitive types
        typeMapping.put(byte.class, ByteProperty.class);
        typeMapping.put(short.class, ShortProperty.class);
        typeMapping.put(int.class, IntProperty.class);
        typeMapping.put(long.class, LongProperty.class);
        typeMapping.put(float.class, FloatProperty.class);
        typeMapping.put(double.class, DoubleProperty.class);
        typeMapping.put(char.class, CharProperty.class);
        typeMapping.put(boolean.class, BooleanProperty.class);
    }

    private TypePropertyMapping() {
    }

    /**
     * Attempt to determine a property class that wraps the corresponding Java type. If it
     * is a primitive type, it will use the corresponding primitive wrapper defined in
     * com.lhkbob.entreri.property.
     * <p/>
     * Unless the type has an available 'entreri-mapping.properties' file, it will
     * fallback to ObjectProperty.
     *
     * @param type The Java type that needs to be wrapped as a property
     *
     * @return The discovered or cached property type for the given type
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Property> getPropertyForType(Class<?> type) {
        Class<? extends Property> pType = typeMapping.get(type);
        if (pType != null) {
            return pType;
        }

        // otherwise check if we have a properties file to load
        // FIXME this requires the type to be aware of the wrapping property impl
        InputStream in = type.getResourceAsStream("/entreri-mapping.properties");
        if (in != null) {
            Properties p = new Properties();
            try {
                p.load(in);
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Error reading entreri-mapping.properties for class: " + type, e);
            }

            for (String key : p.stringPropertyNames()) {
                // only process the matching key, this causes more IO overhead but
                // makes the class loader pattern more consistent
                if (key.equals(type.getName())) {
                    // use the type's class loader so that the loaded property is tied to
                    // the same loader
                    try {
                        pType = (Class<? extends Property>) type.getClassLoader()
                                                                .loadClass(p.getProperty(
                                                                        key));

                        // store the mapping for later as well, this is safe because
                        // a class's loader is part of its identity and the property impl
                        // will use the same loader; even if we concurrently modify it,
                        // the same value will be stored
                        typeMapping.put(type, pType);
                        break;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(
                                "Unable to load mapped Property class for " + type, e);
                    }
                }
            }
        }

        if (pType == null) {
            // generic fallback
            pType = ObjectProperty.class;
        }

        return pType;
    }
}
