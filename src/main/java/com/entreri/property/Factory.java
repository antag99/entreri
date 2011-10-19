package com.entreri.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Factory annotation can be declared on a Property field in a Component
 * definition to specify the type of PropertyFactory to use when creating
 * instances of the Property for the component. The factory type must have a
 * no-argument constructor in order to be instantiated correctly. This
 * annotation can be used in place of the {@link Parameters} or
 * {@link Parameter} annotations.
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Factory {
    /**
     * @return Class of the PropertyFactory to instantiate, must have an
     *         accessible no-argument constructor
     */
    Class<? extends PropertyFactory<?>> value();
}
