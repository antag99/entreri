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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class Attributes {
    private final Map<Class<? extends Annotation>, Annotation> attrs;

    public Attributes(Field f) {
        if (f == null) {
            throw new NullPointerException("Field cannot be null");
        }

        attrs = new HashMap<Class<? extends Annotation>, Annotation>();

        for (Annotation a: f.getAnnotations()) {
            if (a.annotationType().getAnnotation(Attribute.class) != null) {
                // the attribute is an annotation
                attrs.put(a.annotationType(), a);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAttribute(Class<T> cls) {
        if (cls == null) {
            throw new NullPointerException("Annotation class cannot be null");
        }
        return (T) attrs.get(cls);
    }

    public boolean hasAttribute(Class<? extends Annotation> cls) {
        if (cls == null) {
            throw new NullPointerException("Annotation class cannot be null");
        }
        return attrs.containsKey(cls);
    }

    public Collection<Annotation> getAttributes() {
        return attrs.values();
    }
}
