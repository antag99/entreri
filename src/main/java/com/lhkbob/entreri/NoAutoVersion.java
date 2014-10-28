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

import java.lang.annotation.*;

/**
 * NoAutoVersion
 * =============
 *
 * Disable the default automatic incrementing of a component's version when property values are modified. By
 * default, all modifications to properties declared in a component will automatically increment that
 * component's version. This is useful when components are largely unchanging and the version change can be
 * used to kick off a more expensive computation.  However, when a property stores a cached value that
 * frequently changes or is not part of the common identity of the component, it can be useful to disable this
 * behavior.
 *
 * It is highly recommended to avoid the use this annotation until performance profiling suggests that
 * disabling the version increment is necessary to take advantage of it.  An example of this is in Ferox, the
 * Renderable component stores local bounds for visibility checks, but it also caches the world bounds. The
 * world bounds is a cached value that combines the local bounds and the matrix from the Transform component.
 * The Transform's version can change very often, which would force the Renderable's version to change when
 * the world bounds was updated to match. By disabling the auto-update on the world bounds, the rest of the
 * Renderable could be easily versioned.
 *
 * The annotation must be placed on the getter, it is ignored when placed on the setter.
 *
 * @author Michael Ludwig
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAutoVersion {
}
