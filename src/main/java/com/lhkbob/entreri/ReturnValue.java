package com.lhkbob.entreri;

import java.lang.annotation.*;

/**
 * ReturnValue
 * ===========
 *
 * ReturnValue is an annotation used to disambiguate getter methods that take a single parameter. The two
 * method patterns that match a method starting with `get` and taking an argument are:
 *
 * 1. Key-querying  methods for Map-typed properties.
 * 2. "Regular" bean getters that take an input object that is modified to equal the property's value, as an
 * efficient way of preserving value semantics.
 *
 * The key-value query method is the default assumed pattern when a `get(Foo)` method is encountered. To
 * select the second pattern, the parameter must be annotated with `@ReturnValue`.
 *
 * @author Michael Ludwig
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface ReturnValue {
}
