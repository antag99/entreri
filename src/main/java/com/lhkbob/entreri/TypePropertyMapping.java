package com.lhkbob.entreri;

import com.lhkbob.entreri.property.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
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

    public static Class<? extends Property> getPropertyForType(Class<?> type) {
        Class<? extends Property> pType = typeMapping.get(type);
        if (pType != null) {
            return pType;
        }

        // otherwise check if we have a properties file to load
        InputStream in = type.getResourceAsStream("entreri-mapping.properties");
        if (in != null) {
            Properties p = new Properties();
            try {
                p.load(in);
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Error reading entreri-mapping.properties for class: " + type, e);
            }

            for (String key: p.stringPropertyNames()) {
                // only process the matching key, this causes more IO overhead but
                // makes the class loader pattern more consistent
                if (key.equals(type.getName())) {
                    // use the type's class loader so that the loaded property is tied to
                    // the same loader
                    try {
                        pType = (Class<? extends Property>) type.getClassLoader().loadClass(
                                p.getProperty(key));

                        // store the mapping for later as well, this is safe because
                        // a class's loader is part of its identity and the property impl
                        // will use the same loader; even if we concurrently modify it,
                        // the same value will be stored
                        typeMapping.put(type, pType);
                        break;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unable to load mapped Property class for " + type, e);
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
