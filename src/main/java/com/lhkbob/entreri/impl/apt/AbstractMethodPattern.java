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

import com.lhkbob.entreri.Named;
import com.lhkbob.entreri.property.Attribute;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
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

    /**
     * Create a new pattern that uses the provided annotation classes as its supported attributes. It
     * automatically includes the {@link com.lhkbob.entreri.Named} annotation.
     *
     * @param supportedAttributes The supported attributes
     */
    protected AbstractMethodPattern(List<Class<? extends Annotation>> supportedAttributes) {

        Set<Class<? extends Annotation>> annots = new HashSet<>();
        annots.addAll(supportedAttributes);
        annots.add(Named.class);
        attributes = Collections.unmodifiableSet(annots);
    }

    private static String getPropertyName(String methodName, String prefix) {
        return Character.toLowerCase(methodName.charAt(prefix.length())) +
               methodName.substring(prefix.length() + 1);
    }

    /**
     * Get all annotations of types that were provided to the constructor. The annotation source may be
     * an executable element (e.g. method) or a variable element (e.g. parameter to said method).
     *
     * @param annotSource The source of annotations
     * @return All annotations of interested types
     */
    protected Set<Annotation> getMethodAttributes(Element annotSource) {
        Set<Annotation> result = new HashSet<>();

        for (Class<? extends Annotation> aType : attributes) {
            Annotation a = annotSource.getAnnotation(aType);
            if (a != null) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Get all attribute annotations from the element.
     *
     * @param annotSource The source of annotations
     * @return All annotation mirrors that have been annotated with Attribute
     */
    protected Set<AnnotationMirror> getPropertyAttributes(TypeUtils tu, Element annotSource) {
        Set<AnnotationMirror> result = new HashSet<>();
        for (AnnotationMirror am : annotSource.getAnnotationMirrors()) {
            DeclaredType annotType = am.getAnnotationType();
            if (tu.asElement(annotType).getAnnotation(Attribute.class) != null) {
                result.add(am);
            }
        }
        return result;
    }

    /**
     * Get the property name from the method. If the method is annotated with `@Named` that is used.
     * Otherwise the if the method name starts with any of the possible prefixes, the prefix is removed and
     * the remaining first letter is made lower case. If the method name does not match any prefix, the method
     * name is returned unmodified.
     *
     * @param method           The method to extract the property name from
     * @param possiblePrefixes Possible prefixes the name may start with
     * @return The property name
     */
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

    /**
     * Get the property name as specified by the `param` argument to the given `method`. If the parameter is
     * annotated with `@Named` that is used. If not, and `useVarNameAsFallback` is true then the parameter
     * variable name from the source code is used. Otherwise null is returned, in which case the property name
     * ought to be determined by the method instead.
     *
     * @param method               The method defining the property
     * @param param                The specific parameter that defines the property
     * @param useVarNameAsFallback True if the variable name of the parameter can define the property name
     * @return The property name, or null if it couldn't be determined
     */
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
