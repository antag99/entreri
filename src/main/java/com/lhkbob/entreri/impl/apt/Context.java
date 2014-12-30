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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

/**
 * Context
 * =======
 *
 * Context bundles up the commonly needed information when processing a specific component type. This
 * includes the actual component type, the APT processing environment, the attribute annotation scope that all
 * method patterns are interested in, and the type mappers for both value and reference semantics. It also
 * provides utility methods for dealing with complexities of the Java mirror API.
 *
 * @author Michael Ludwig
 */
public class Context extends TypeUtils {
    private final TypeMirror componentType;
    private final TypePropertyMapper valueMapper;
    private final TypePropertyMapper referenceMapper;

    /**
     * Create a new Context that uses the provided ProcessingEnvironment, is for the given component type (which
     * is assumed to be a sub-interface of Component), and is configured to use the given reference
     * and value type mappers.
     *
     * @param env             The processing environment of the APT compilation unit processing the component type
     * @param componentType   The component sub-interface being processed
     * @param valueMapper     The property type mapper used for properties with value semantics
     * @param referenceMapper The property type mapper used for properties with reference semantics
     */
    public Context(ProcessingEnvironment env, TypeMirror componentType, TypePropertyMapper valueMapper,
                   TypePropertyMapper referenceMapper) {
        super(env);
        this.componentType = componentType;
        this.valueMapper = valueMapper;
        this.referenceMapper = referenceMapper;
    }

    // FIXME get rid of method level scope as a thing because:
    // 1. for method level attributes, the method pattern knows and will get them all
    // 2. for property level attributes, the pattern now has to update after implementation assignment
    //    in which case it gets the scope from the implementation
    // 3. howerver these are already represented as annotation mirrors, so why not just require the patterns
    //    to grab all property-level annotation mirrors on the methods and proeprties the first time through
    // 4. that will make accessing property-level annotation mirrors from a method dec or pattern more of a pain
    //    but it won't be that bad and it makes the API a lot simpler since there's no more dealing with
    //    who's interested in what. everyone can have access to anything

    /**
     * @return The Component sub-interface being processed
     */
    public TypeMirror getComponentType() {
        return componentType;
    }

    /**
     * @return The type mapper for properties with value semantics
     */
    public TypePropertyMapper getValueTypeMapper() {
        return valueMapper;
    }

    /**
     * @return The type mapper for properties with reference semantics
     */
    public TypePropertyMapper getReferenceTypeMapper() {
        return referenceMapper;
    }
}
