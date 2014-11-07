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

/**
 * Generator
 * =========
 *
 * Generator hides the nesting structure of a Java source file from MethodDeclarations that must  insert
 * syntax at various key points. Properly forming the nested structure and invoking the method declarations at
 * the appropriate times is the responsibility of Generator implementations (i.e. the one used in {@link
 * com.lhkbob.entreri.impl.ComponentFactoryProvider}.
 *
 * It also provides methods to produce member variable names that either correspond to a `Property` field of
 * the Component proxy, or are uniquely named and safely usable by the method declaration to add meta-fields.
 *
 * @author Michael Ludwig
 */
public interface Generator {
    /**
     * @param propertyName The logical name of the property as determined by the method pattern defining the
     *                     property
     * @return The field name of the associated `Property` instance in the Component implementation class
     */
    public String getPropertyMemberName(String propertyName);

    /**
     * Get a unique name that can be used to declare a new member variable. `name` is a meaningful variable
     * name that will be included in the returned member name after uniqueness is guaranteed. `owner` is
     * (often) the MethodDeclaration that must use the member.
     *
     * Invoking `getMemberName()` with the exact same name and owner will result in the same field name
     * being returned. Using the same name with a different owner will have a separate field name.
     *
     * @param name  The meaningful variable name
     * @param owner The owner/user of the member field
     * @return A unique name usable as a Java field name
     */
    public String getMemberName(String name, Object owner);

    /**
     * @return Valid syntax for accessing the component's current index, which can be used to access
     * Property data
     */
    public String getComponentIndex();

    /**
     * Add syntax to the class being generated. Each line will be tabbed properly given the nesting depth of
     * the generator. If syntax must be nested further, such as for `if` and `for` blocks, then those single
     * tabs should be included in the provided syntax strings.
     *
     * @param blobLines The syntax to append
     */
    public void appendSyntax(String... blobLines);
}
