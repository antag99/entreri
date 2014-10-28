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
package com.lhkbob.entreri.property;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Attributes
 * ==========
 *
 * Attributes represents the collection of attributes that have been provided on a property declaration within
 * a ComponentData definition. To use attributes, {@link PropertyFactory} implementations should have a
 * constructor that takes a single Attributes instance.
 *
 * @author Michael Ludwig
 * @see PropertyFactory
 */
public class Attributes {
    private final Class<?> propertyType;
    private final Map<Class<? extends Annotation>, Annotation> attrs;

    /**
     * Construct a new set of attributes from the given annotations. Only annotations that have the Attribute
     * annotation are kept.
     *
     * @param propertyType The class of the declared property that produced these attributes
     * @param attrs        The annotations from the method, or field, etc.
     * @throws NullPointerException if attrs is null or contains null elements
     */
    public Attributes(Class<?> propertyType, Annotation... attrs) {
        if (propertyType == null) {
            throw new NullPointerException("Property type cannot be null");
        }
        if (attrs == null) {
            throw new NullPointerException("Attributes cannot be null");
        }
        this.propertyType = propertyType;
        this.attrs = new HashMap<>();

        for (Annotation a : attrs) {
            if (a.annotationType().getAnnotation(Attribute.class) != null) {
                // the attribute is an annotation
                this.attrs.put(a.annotationType(), a);
            }
        }
    }

    /**
     * @return The class of the property this set of attributes belongs to, where property refers to the
     * logical property declared in a Component and not the selected `Property` class
     */
    public Class<?> getPropertyType() {
        return propertyType;
    }

    /**
     * Get the attribute annotation of type T. If there is no attribute for the given type, then null is
     * returned.
     *
     * @param cls The attribute annotation class type
     * @return The associated attribute instance
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAttribute(Class<T> cls) {
        if (cls == null) {
            throw new NullPointerException("Annotation class cannot be null");
        }
        return (T) attrs.get(cls);
    }

    /**
     * Get whether or not this set of attributes has an attribute of the given type. If an attribute does not
     * have any variables, this is sufficient instead of getting the actual instance.
     *
     * @param cls The annotation class type
     * @return True if the associated attribute exists
     */
    public boolean hasAttribute(Class<? extends Annotation> cls) {
        if (cls == null) {
            throw new NullPointerException("Annotation class cannot be null");
        }
        return attrs.containsKey(cls);
    }

    /**
     * @return All annotation attributes in this set
     */
    public Collection<Annotation> getAttributes() {
        return attrs.values();
    }
}
