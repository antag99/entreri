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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * ComponentSpecification provides an interface to access the information encoded in a Component sub-interface
 * in order to generate a proxy implementation. The specification can be extracted at runtime using
 * reflection, or at compile time using the annotation processor mirror API. The two factory methods, {@link
 * Factory#fromClass(Class)} and {@link Factory#fromTypeElement(javax.lang.model.element.TypeElement,
 * javax.annotation.processing.ProcessingEnvironment)} correspond to these two scenarios.
 *
 * @author Michael Ludwig
 */
public interface ComponentSpecification {
    /**
     * Get the qualified name of the component type, with the package prefix removed. Thus, the returned
     * string should be valid to insert into source code within the same package of the component and refer to
     * that exact type.
     *
     * @return The component type
     */
    public String getType();

    /**
     * @return The package the component type resides in
     */
    public String getPackage();

    /**
     * Get all properties that must be implemented for this component type. This will include all properties
     * defined in a parent component type if the type does not directly extend Component.
     * <p/>
     * The returned list will be immutable and sorted by logical property name.
     *
     * @return The list of all properties for the component
     */
    public List<? extends PropertyDeclaration> getProperties();

    /**
     * Get all validation annotations applied directly to the setter with the given method name. Included
     * annotations will be instances of {@link com.lhkbob.entreri.NotNull}, {@link com.lhkbob.entreri.Within},
     * and {@link com.lhkbob.entreri.Validate}.
     *
     * @param setterName The setter method name to look up
     *
     * @return All annotations present on the given method
     */
    public List<Annotation> getValidationAnnotations(String setterName);

    public static final class Factory {
        private Factory() {
        }

        /**
         * Produce a ComponentSpecification for the given Component class type.
         *
         * @param type The component type
         *
         * @return The extracted ComponentSpecification
         *
         * @throws com.lhkbob.entreri.IllegalComponentDefinitionException
         *          if the class does not follow the specification defined in Component, or if referenced
         *          Property classes do not meet their requirements
         */
        public static ComponentSpecification fromClass(Class<? extends Component> type) {
            return new ReflectionComponentSpecification(type);
        }

        /**
         * Produce a ComponentSpecification for the given Component type element, within the context of the
         * ProcessingEnvironment used by the active annotation processor. The returned specification maintains
         * no dependencies to the processing environment but no guarantee is made on its validity after the
         * processing round completes.
         *
         * @param type The component type
         * @param env  The ProcessingEnvironment for the current round
         *
         * @return The extracted ComponentSpecification
         *
         * @throws com.lhkbob.entreri.IllegalComponentDefinitionException
         *          if the class does not follow the specification defined in Component, or if referenced
         *          Property classes do not meet ther requirements
         */
        public static ComponentSpecification fromTypeElement(TypeElement type, ProcessingEnvironment env) {
            return new MirrorComponentSpecification(type, env.getTypeUtils(), env.getElementUtils(),
                                                    env.getFiler());
        }
    }
}
