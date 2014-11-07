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
package com.lhkbob.entreri.attr;

import java.lang.annotation.*;

/**
 * Within
 * ======
 *
 * Within is a validation annotation for numeric properties to ensure values fall within a specific range.
 * For simplicity, the annotation expects minimum and maximum values in doubles but it works with any
 * primitive type that has `<` and `>` defined that can be lifted to a `double`. Specifying only one half of
 * the range is valid and produces an open-ended range. Inputs that fall outside the declared range will cause
 * an IllegalArgumentException to be thrown.
 *
 * Compilation failures will result if applied to non-primitive parameters. When applied to a setter method,
 * the range operates on the first parameter regardless of the number of method inputs. When applied to a
 * specific parameter, the proxy generates code for that parameter. In this way, multi-parameter setters can
 * have Within applied to each parameter.
 *
 * This annotation is ignored when placed on the property getter.
 *
 * @author Michael Ludwig
 */
@Documented
@Attribute(Attribute.Level.PROPERTY)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Within {
    /**
     * @return Minimum bound of input, inclusive, or leave unspecified for unbounded on the low side of the
     * range
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * @return Maximum bound of input, inclusive, or leave unspecified for unbounded on the high side of the
     * range
     */
    double max() default Double.POSITIVE_INFINITY;
}
