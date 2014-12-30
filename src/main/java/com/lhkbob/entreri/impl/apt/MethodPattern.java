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
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
 * @author Michael Ludwig
 */
public interface MethodPattern {
    /**
     * Scan through the provided `methods` and match all possible. The provided methods are a subset of the
     * methods of the component type of the `context`, where methods previously matched by patterns of higher
     * precedence have been removed. The returned map should use the matched method element as a key, and the
     * corresponding map value be the collection of all properties implicitly defined by the method. Often
     * this is likely to be a singleton set.
     *
     * The properties defined for a method do not need to have their property implementation decided. This
     * will be handled automatically after the property declarations have been combined with those from all
     * other method patterns. The returned properties need only have attributes and methods known by this
     * pattern, it will be updated appropriately with state from other patterns afterwards based on its
     * reported name and type.
     *
     * @param context The context of the component generation
     * @param methods The remaining methods to match
     * @return All matched methods with the property declarations they define
     */
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods);
}
