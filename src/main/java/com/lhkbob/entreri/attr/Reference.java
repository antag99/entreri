package com.lhkbob.entreri.attr;

import java.lang.annotation.*;

/**
 * Reference
 * =========
 *
 * The Reference attribute overrides `entreri`'s default value semantics for a property, changing them to be
 * the normal Java reference semantics for Object types. This attribute is meaningless on primitive-typed
 * properties.
 *
 * Unlike the default value semantics, reference properties can be null. Unless a reference-supporting
 * Property instantiates objects for the default values, the default value for a reference property is null.
 * If `nullable` is set to false on the attribute, this can cause the property to be in an invalid state until
 * the property has been assigned a value for the first time. This is the behavior of the default reference
 * property {@link com.lhkbob.entreri.property.ObjectProperty} that supports any subclass of `Object`.
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
