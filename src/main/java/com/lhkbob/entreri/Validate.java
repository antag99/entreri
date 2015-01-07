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
 * Validate
 * ========
 *
 * Validate is a generic validation annotation that lets you specify a Java-like snippet to be inserted into
 * the generated proxy to perform validation on a setter method.  Unlike {@link com.lhkbob.entreri.property.Within}, this annotation
 * validates all parameters of a method, and not a property. Because of the flexibility this offers, Validate
 * allows you to perform validation between different properties of the same component (such as ensuring a
 * minimum is less than a maximum value).
 *
 * This annotation does nothing if placed on a method that does not accept input arguments.
 *
 * ## Validation expressions
 *
 * The Java-like validation snippet must evaluate to a boolean expression. When that expression evaluates to
 * true, the inputs are considered valid; otherwise, the generated setter method will throw an
 * IllegalArgumentException. The snippet must use valid Java syntax, except that the symbols `${1}` to `${n}`
 * should be used to refer to the first through `n`th method parameters. Those symbols will be replaced with
 * the generated parameter name at compile time.
 *
 * After this syntax replacement, any other errors in the validation expression may produce Java syntax
 * errors when the generated source is compiled.
 *
 * @author Michael Ludwig
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
public @interface Validate {
    /**
     * @return Get the Java-like validation snippet that represents a boolean expression evaluating to true
     * when input parameters are valid
     */
    String value();

    /**
     * @return Optional error message to include in the thrown exception
     */
    String errorMsg() default "";
}
