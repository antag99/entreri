package com.entreri.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.annotation.Retention;


/**
 * A source-level annotation to add to {@link Property} fields to describe the
 * 'true' type of the property if it has been condensed into primitives for use
 * a property such as {@link FloatProperty} or {@link IntProperty}.
 * 
 * @author Michael Ludwig
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapsTo {
    /**
     * The Class type that the property values eventually represent,
     * as exposed by the public API of the Component definition.
     * @return Final Class type of a property
     */
    Class<?> value();
}
