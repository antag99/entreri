package com.lhkbob.entreri;

import java.lang.annotation.*;

/**
 * Validate is a generic validation annotation that lets you specify a Java-like snippet to be inserted into
 * the generated proxy to perform validation on a setter method.  Unlike {@link NotNull} and {@link Within},
 * this annotation cannot be placed on setter parameters. Because of the flexibility this offers, Validate
 * allows you to perform validation between different properties of the same component (such as ensuring a
 * minimum is less than a maximum value).
 * <p/>
 * The Java-like validation snippet must evaluate to a boolean expression. When that expression is true, the
 * inputs are considered valid; otherwise, the proxy will throw an IllegalArgumentException. The snippet must
 * use valid Java syntax, except that the symbols {@code $1 - $n} should be used to refer to the first through
 * nth setter parameters. Those symbols will be replaced with the generated parameter name at compile time.
 * Additionally, the syntax {@code $propertyName} will be replaced with {@code getPropertyName()} to refer to
 * properties on a component. Validation is performed before the property values are assigned, so referencing
 * a property name with this syntax in the setter method for that property will produce the old value.
 * <p/>
 * After this syntax replacement, any other errors may produce Java syntax errors when the generated source is
 * compiled.
 * <p/>
 * This annotation is ignored if placed on the getter property method.
 *
 * @author Michael Ludwig
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Validate {
    /**
     * @return Get the Java-like validation snippet that represents a boolean expression evaluating to true
     *         when input parameters are valid
     */
    String value();

    /**
     * @return Optional error message to include in the thrown exception
     */
    String errorMsg() default "";
}
