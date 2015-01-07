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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * MethodDeclaration
 * =================
 *
 * A MethodDeclaration is a wrapper around a method declared in a Component interface and encapsulates the
 * necessary information and policy to implement such a method in a generated implementation of the Component.
 * MethodDeclarations are created and managed by the MethodPattern which matched the method the declaration
 * corresponds to.
 *
 * @author Michael Ludwig
 */
public interface MethodDeclaration extends Comparable<MethodDeclaration> {
    /**
     * @return The name of the method, suitable for inserting into Java code
     */
    public String getName();

    /**
     * @return A list of parameter variable names used in the method signature, paired with `getParameterTypes()`
     */
    public List<String> getParameterNames();

    /**
     * Get the parameter types of the method. The returned strings should be qualified canonical class names
     * so that they can be inserted directly into Java code without needing to import any packages. The list
     * is paired with `getParameterNames()`.
     *
     * @return A list of parameter class types used in the method signature
     */
    public List<TypeMirror> getParameterTypes();

    /**
     * @return The qualified canonical class type returned by the method, or 'void' or a primitive type
     */
    public TypeMirror getReturnType();

    /**
     * @return The method this declaration wraps
     */
    public ExecutableElement getMethod();

    /**
     * Validate all property declarations that the method implicitly defines to make sure that their chosen
     * Property implementations expose the required methods to implement the method body of this declaration.
     * This should make sure the Property's get or set methods take the exact type of the logically defined
     * property. This will only be called after the PropertyDeclarations have been updated to have valid
     * property implementations chosen.
     *
     * This should also validate the selected type of the property if the method declaration may have
     * started with a null property type and required other methods to help constrain the type.
     *
     * @param context The context of the component generation
     * @return True if all properties are valid
     */
    public boolean arePropertiesValid(Context context);

    /**
     * Often a method declaration must maintain a reference back to the properties that it created so that
     * the method body can be generated appropriately. However, when combining multiple PropertyDeclaration
     * instances that represent the same logical property, these back references must be carefully updated as
     * well.
     *
     * @param original    The original property declaration that owned this method declaration
     * @param replaceWith The new property declaration that the method now belongs to, which will be
     *                    equivalent in name and type
     */
    public void replace(PropertyDeclaration original, PropertyDeclaration replaceWith);

    /**
     * Get a snippet of valid Java syntax that loads the `attribute` annotation from this method so that it
     * can be made available to the constructor of the selected PropertyFactory for the properties referenced
     * by this method.
     *
     * @param context     The context that produced this declaration
     * @param forProperty The property from this method that is being queried (which may determine a particular parameter)
     * @param attribute   The attribute Annotation type that must be looked up
     * @return Null if this method does not provide the requested annotation, or valid syntax as described
     * above
     */
    public String getAnnotationSyntax(Context context, PropertyDeclaration forProperty, TypeMirror attribute);

    /**
     * Append valid Java syntax using the Generator API, where the syntax is expected to be declaring member
     * variables for use by the method implementation. The syntax will be inserted at the top of the Component
     * class's code block.
     *
     * @param generator The source code generator to append to
     */
    public void appendMembers(Generator generator);

    /**
     * Append valid Java syntax using the Generator API, where the syntax is expected to initialize all
     * members added by {@link #appendMembers(Generator)}. The syntax will be inserted into the constructor's
     * code block.
     *
     * @param generator The source code generator to append to
     */
    public void appendConstructorInitialization(Generator generator);

    /**
     * Append valid Java syntax using the Generator API, where the syntax is expected to fully implement the
     * method the declaration represents. This includes the return statement or expression. However, the
     * method signature is already added, having been automatically created from the reported method name,
     * return type, and parameter types. It can be assumed the signature of the method uses the parameter
     * variable names reported by {@link #getParameterNames()}.
     *
     * @param generator The source code generator to append to
     */
    public void appendMethodBody(Generator generator);
}
