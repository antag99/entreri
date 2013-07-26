package com.lhkbob.entreri;

import java.lang.annotation.*;

/**
 * Within is a validation annotation for numeric properties to ensure values fall within a specific range. For
 * simplicity, the annotation expects minimum and maximum values in doubles but it works with any primitive
 * type that has {@code &lt;} and {@code &gt;} defined. Specifying only one half of the range is valid and
 * produces an open-ended range. Inputs that fall outside the declared range will cause an
 * IllegalArgumentException to be thrown.
 * <p/>
 * Compilation failures will result if applied to non-primitive parameters. When applied to a setter method,
 * the range operates on the first parameter regardless of the number of method inputs. When applied to a
 * specific parameter, the proxy generates code for that parameter. In this way, multi-parameter setters can
 * have Within applied to each parameter.
 * <p/>
 * This annotation is ignored when placed on the property getter.
 *
 * @author Michael Ludwig
 */
@Documented
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Within {
    /**
     * @return Minimum bound of input, inclusive, or leave unspecified for unbounded on the low side of the
     *         range
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * @return Maximum bound of input, inclusive, or leave unspecified for unbounded on the high side of the
     *         range
     */
    double max() default Double.POSITIVE_INFINITY;
}
