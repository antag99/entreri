package com.googlecode.entreri.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * DefaultValue is an annotation that can be added to {@link Property} fields in
 * a component definition to automatically configure their creating
 * PropertyFactories.
 * </p>
 * <p>
 * The default {@link ReflectionComponentDataFactory} will look for a static
 * method named 'factory()' that takes two parameters. The first must be an int
 * and represents the element size. If the {@link ElementSize} is not present,
 * the ComponentDataFactory should use a value of 1. The second parameter is the
 * default value to use. The parameter type determines which annotation method
 * is checked (e.g. {@link #defaultByte()} or {@link #defaultDouble()}).
 * </p>
 * <p>
 * All Properties defined in com.googlecode.entreri.property support this
 * annotation, except for ObjectProperty because it is not possible to specify a
 * default object via annotation.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DefaultValue {
    boolean defaultBoolean() default false;
    char defaultChar() default '\0';
    byte defaultByte() default 0;
    short defaultShort() default 0;
    int defaultInt() default 0;
    long defaultLong() default 0L;
    float defaultFloat() default 0f;
    double defaultDouble() default 0.0;
}
