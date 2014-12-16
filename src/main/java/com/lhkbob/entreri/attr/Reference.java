package com.lhkbob.entreri.attr;

import java.lang.annotation.*;

/**
 * Reference
 * =========
 *
 * The Reference attribute overrides `entreri`'s default value semantics for a property, changing them to be
 * the normal Java reference semantics for Object types. This attribute is meaningless for primitive-typed
 * properties.
 *
 * Unlike the default value semantics, reference properties can be null. Unless a reference-supporting
 * PropertyFactory instantiates objects for the default values, the default value for a reference property is
 * null. If `nullable` is set to false on the attribute, this can cause the property to be in an invalid state
 * until the property has been assigned a value for the first time.
 *
 * The default Object PropertyFactory supports reference properties, and will invoke a no-argument
 * constructor if possible to construct a default value when `nullable` is set to false. It will use `null`
 * for the default value if `nullable` is set to true, or there is no default constructor to invoke.
 *
 * @author Michael Ludwig
 */
@Documented
@Attribute(influencePropertyChoice = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Reference {
    boolean nullable() default true;
}
