package com.lhkbob.entreri;

import java.lang.annotation.*;

/**
 * NotNull is a validation annotation that can be added to the setter method or setter parameters for a
 * property to declare that the references cannot be null. The proxy implementation will generate code to
 * check for null and throw a NullPointerException as necessary. Applying NotNull to the entire method
 * declares all parameters to be non-null and checks will be generated for each.
 * <p/>
 * This should not be used with primitive typed data because they cannot be null. Some property
 * implementations may implicitly enforce a non-null rule already in which case this is unnecessary.
 * <p/>
 * This annotation is ignored when placed on the getter for a property
 *
 * @author Michael Ludwig
 */
@Documented
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNull {
}
