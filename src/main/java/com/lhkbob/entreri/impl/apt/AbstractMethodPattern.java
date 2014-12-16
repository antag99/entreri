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
package com.lhkbob.entreri.impl.apt;

import com.lhkbob.entreri.attr.Attribute;
import com.lhkbob.entreri.attr.Named;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AbstractMethodPattern
 * =====================
 *
 * An abstract base class for {@link com.lhkbob.entreri.impl.apt.MethodPattern} implementations that provides
 * useful utility methods for filtering annotations, or extracting names from methods.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractMethodPattern implements MethodPattern {
    private final Set<Class<? extends Annotation>> attributes;

    protected AbstractMethodPattern(List<Class<? extends Annotation>> supportedAttributes) {

        Set<Class<? extends Annotation>> annots = new HashSet<>();
        for (Class<? extends Annotation> a : supportedAttributes) {
            if (a.getAnnotation(Attribute.class) != null) {
                annots.add(a);
            }
        }
        annots.add(Named.class);
        attributes = Collections.unmodifiableSet(annots);
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAttributes() {
        return attributes;
    }

    private static String getPropertyName(String methodName, String prefix) {
        return Character.toLowerCase(methodName.charAt(prefix.length())) +
               methodName.substring(prefix.length() + 1);
    }

    protected Set<Annotation> getAttributes(Attribute.Level level, Element annotSource,
                                                   Set<Class<? extends Annotation>> scope) {
        Set<Annotation> result = new HashSet<>();

        for (Class<? extends Annotation> aType : scope) {
            if (aType.getAnnotation(Attribute.class).level() == level) {
                Annotation a = annotSource.getAnnotation(aType);
                if (a != null) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    protected String getPropertyName(ExecutableElement method, String... possiblePrefixes) {
        Named name = method.getAnnotation(Named.class);
        if (name != null) {
            return name.value();
        } else {
            // look for prefix
            String methodName = method.getSimpleName().toString();
            for (String prefix : possiblePrefixes) {
                if (methodName.startsWith(prefix)) {
                    return getPropertyName(methodName, prefix);
                }
            }
            return methodName;
        }
    }

    protected String getPropertyName(ExecutableElement method, int param, boolean useVarNameAsFallback) {
        VariableElement p = method.getParameters().get(param);
        Named name = p.getAnnotation(Named.class);
        if (name != null) {
            return name.value();
        } else {
            return useVarNameAsFallback ? p.getSimpleName().toString() : null;
        }
    }
}
