package com.googlecode.entreri.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.googlecode.entreri.property.Property;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * ElementSize is an annotation that can be added to {@link Property} fields in
 * a component definition to automatically configure their creating
 * PropertyFactories.
 * </p>
 * <p>
 * The default {@link ReflectionComponentDataFactory} will look for a static
 * method named 'factory()' that takes a single int as a parameter. ElementSize
 * can be combined with the {@link DefaultValue} if there is a factory() that
 * takes an int and a second argument for the default value. It is not necessary
 * to use the {@link ElementSize} if the element size should be 1.
 * </p>
 * <p>
 * All Property implementations in com.googlecode.entreri.property support the
 * use of this annotation.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ElementSize {
    /**
     * @return The number of primitive elements allocated for each component
     */
    int value();
}
