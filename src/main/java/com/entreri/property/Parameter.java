package com.entreri.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Parameter specifies a single argument to a constructor of a Property. If a
 * Property has a single-argument constructor, Parameter can be used directly on
 * the Property field. Otherwise {@link Parameters} can be used to select a
 * multiple-argument constructor.
 * </p>
 * <p>
 * The definition of a Property must be constant for all Component instances of
 * the same type because they share a Property instance for each declared
 * property (and access the indexed data as needed). Because of this, a
 * Parameter can only use primitive and boxed primitive values, Strings, and
 * Classes. The primitives and Classes are encoded in strings and are parsed
 * when Properties are instantiated.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameter {
    /**
     * <p>
     * Return the value of the parameter, converted to a string. Depending on
     * the type of the parameter, this will be converted to its final form in
     * different ways.
     * </p>
     * <p>
     * If the type is a primitive or boxed primitive, it is parsed using the
     * appropriate parseX() method (e.g. {@link Integer#parseInt(String)}).
     * String parameters take the value as is; Class parameters attempt to load
     * the Class via {@link Class#forName(String)}.
     * </p>
     * 
     * @return The constant value of the parameter
     */
    String value();

    /**
     * @return The class type of the parameter argument, must match the
     *         parameter to the constructor of the Property
     */
    Class<?> type();
}
