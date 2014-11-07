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

import com.lhkbob.entreri.property.Property;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MethodPattern
 * =============
 *
 * MethodPattern represents a class or pattern of method declarations in a Component class that it  knows
 * how to implement. Examples would be bean getters or bean setters that delegate to a property's `get()` and
 * `set()` methods. These patterns are responsible for making sure the method's signature is valid for the
 * pattern as well as providing the relevant metadata for the properties the method defines.
 *
 * MethodPatterns do not need to match every single method. If one pattern cannot match, another pattern
 * can. If no pattern is available then the component class is considered invalid and will result in
 * compilation failure.
 *
 * A necessary consequence of performing compile-time and runtime validation is that patterns must implement
 * equivalent logic using both the reflection and mirror APIs.
 *
 * @author Michael Ludwig
 */
public interface MethodPattern {
    /**
     * Return a map describing every property implicitly declared by the method. The map keys represent the
     * name of the property, used to combine multiple method's accessing or modifying the same property. If a
     * method pattern can identify that some property is declared, but is unsure of the type (such as a key or
     * element accessor of a type of collection, where the specific collection type is undefined by the
     * method) then a null Class value should be used.
     *
     * Must not return the empty set. If a method would not declare any properties according to this
     * pattern, `matches()` should return false instead. It can be assumed that `matches()` returns true for
     * the provided method.
     *
     * @param method The method whose properties are being analyzed
     * @return All properties declared as existing by this method
     */
    public Map<String, Class<?>> getDeclaredProperties(Method method);

    /**
     * As {@link #getDeclaredProperties(java.lang.reflect.Method)} but using the mirror API.
     *
     * @param method The method whose properties are being analyzed
     * @return All properties declared as existing by this method
     */
    public Map<String, TypeMirror> getDeclaredProperties(ExecutableElement method);

    /**
     * Get all property-level attribute annotations that apply to the given `property` that have been
     * declared on the method in question. A property's attributes are the union of property-level attributes
     * from all method patterns that declare the same property. The returned set of annotations should only
     * include the annotations of types present in `scope`.
     *
     * For some method patterns, this should include annotations applied on the method, or on the parameter
     * arguments, and sometimes both. It can be assumed that `matches()` returns true for the provided method.
     *
     * @param method   The method in question
     * @param property The property being queried
     * @param scope    The set of attribute annotations of interest
     * @return All property-level attribute annotations present in this method that affect `property`
     */
    public Set<Annotation> getPropertyLevelAttributes(Method method, String property,
                                                      Set<Class<? extends Annotation>> scope);

    /**
     * As {@link #getPropertyLevelAttributes(java.lang.reflect.Method, String, java.util.Set)} but  using
     * the mirror API.
     *
     * @param method   The method in question
     * @param property The property being queried
     * @param scope    The set of attribute annotations of interest
     * @return All property-level attribute annotations present in this method that affect `property`
     */
    public Set<Annotation> getPropertyLevelAttributes(ExecutableElement method, String property,
                                                      Set<Class<? extends Annotation>> scope);

    /**
     * Create a MethodDeclaration that is capable of implementing `method` according to the provided
     * property definitions. The order of `propClass` and `property` are arbitrary but consistent between
     * the two, such that the same index in either corresponds to the same logical property. The
     * property declarations in `property` are the almost-completed declarations of the properties specified
     * by `getDeclaredProperties()`. Everything is completed except for the list of method declarations.
     *
     * It can be assumed that `matches()` returns true for the provided method.
     *
     * @param method    The method to implement
     * @param propClass The Property class objects corresponding to each PropertyDeclaration
     * @param property  The near-completed property declarations specified by the method
     * @return A method declaration that can implement the method for a component proxy
     */
    public MethodDeclaration createMethodDeclaration(Method method, List<Class<? extends Property>> propClass,
                                                     List<? extends PropertyDeclaration> property);

    /**
     * As {@link #createMethodDeclaration(java.lang.reflect.Method, java.util.List, java.util.List)} but
     * using the mirror API.
     *
     * @param method   The method to implement
     * @param propType The Property TypeMirror objects corresponding to each PropertyDeclaration
     * @param property The near-completed property declarations specified by the method
     * @return A method declaration that can implement the method for a component proxy
     */
    public MethodDeclaration createMethodDeclaration(ExecutableElement method, List<TypeMirror> propType,
                                                     List<? extends PropertyDeclaration> property);

    /**
     * Return whether or not the given `method` matches this pattern. Matching must depend solely on
     * information available in the method. While an implementation of a matched method can take advantage of
     * property attributes declared elsewhere, matching must be done without it.
     *
     * @param method The method within the Component interface being analyzed
     * @return True if this pattern is capable of generating method declarations for the method
     */
    public boolean matches(Method method);

    /**
     * As {@link #matches(java.lang.reflect.Method)} but using the mirror API.
     *
     * @param method The method within the Component interface being analyzed
     * @return True if this pattern is capable of generating method declarations for the method
     */
    public boolean matches(ExecutableElement method);

    /**
     * Return a set of attribute annotation types that this pattern supports or relies upon to either
     * determine its method definition.
     *
     * @return The set of supported attribute classes
     */
    public Set<Class<? extends Annotation>> getSupportedAttributes();
}
