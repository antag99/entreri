package com.lhkbob.entreri;

import java.lang.annotation.*;

/**
 * Annotation applied to getters, setters, and setter parameters in a Component definition
 * to specify an exact property name instead of using Java naming conventions to infer it
 * from the getter or setter.
 * <p/>
 * When a setter takes multiple parameters, this annotation is required for each parameter
 * because method parameter names are not always available via reflection at runtime.
 *
 * @author Michael Ludwig
 * @see Component
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface Named {
    /**
     * @return The property name
     */
    String value();
}
