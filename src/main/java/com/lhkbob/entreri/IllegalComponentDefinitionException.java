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
package com.lhkbob.entreri;

/**
 * IllegalComponentDefinitionException
 * ===================================
 *
 * IllegalComponentDefinitionException is an exception thrown if a Component implementation does not follow
 * the class hierarchy or field rules defined in {@link Component}. This will generally be handled by the
 * annotation processor that outputs proxy implementations of components. However, if a discrepency in the
 * class file occurs between compilation after generation and before class loading, this may be thrown at
 * runtime.
 *
 * @author Michael Ludwig
 */
public class IllegalComponentDefinitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String componentTypeName;

    /**
     * Create an exception that specifies the leaf-level class in a Component type hierarchy has some problem
     * with its definition.
     *
     * @param componentTypeName The canonical class name of the component type
     * @param problem           A generic error message to be tacked to the end of the final error message
     */
    public IllegalComponentDefinitionException(String componentTypeName, String problem) {
        super(componentTypeName + " is invalid, error: " + problem);
        this.componentTypeName = componentTypeName;
    }

    /**
     * Create an exception that specifies a problem was detected, but at a point where the current Component
     * type was unknown or unavailable.
     *
     * @param problem The error message
     */
    public IllegalComponentDefinitionException(String problem) {
        super(problem);
        componentTypeName = "[?]";
    }

    /**
     * @return The canonical name of the invalid component type
     */
    public String getComponentTypeName() {
        return componentTypeName;
    }
}
