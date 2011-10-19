package com.entreri.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The Parameters annotation is used to specify a multi-argument constructor
 * when instantiating a Property for a Component type in an EntitySystem, and to
 * provide the constant values for the constructor.
 * </p>
 * <p>
 * See {@link Parameter} for more details on what values can be described by the
 * annotation and used as constructor arguments for a Property. If a Property
 * cannot be instantiated with the Parameters annotation, a custom
 * {@link PropertyFactory} can be used in conjunction with the {@link Factory}
 * annotation.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameters {
    /**
     * @return An array of Parameter annotations describing the constructor
     *         arguments to use when instantiating the property.
     */
    Parameter[] value();
}
