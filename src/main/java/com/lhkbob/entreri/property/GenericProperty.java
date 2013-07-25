package com.lhkbob.entreri.property;

import java.lang.annotation.*;

/**
 * GenericProperty is an annotation that can be added to Property definitions to specify that they support
 * more permissive types. The canonical examples of this are {@link ObjectProperty} and {@link EnumProperty}.
 * When generic, a property must declare the same {@code set(int, Foo)} and {@code Foo get(int)} methods,
 * except that the value type is the super class declared in this annotation.
 * <p/>
 * The proxy code generation will then insert casts into the component implementations so that the property
 * works correctly with a more restricted interface exposed by the component (e.g. instead of returning an
 * Object, it is cast to whatever Object subclass that is used by the component).
 *
 * @author Michael Ludwig
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenericProperty {
    Class<?> superClass();
}
