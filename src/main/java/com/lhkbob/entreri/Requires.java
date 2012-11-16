package com.lhkbob.entreri;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The Requires annotation can be used to specify dependencies within a package
 * of components. If one component requires the use of another, such as a scene
 * or light component needing a transform, this can be a convenient way to
 * specify this relationship and reduce the amount of explicit configuration
 * during entity initialization.
 * <p>
 * If a component type extends an additional abstract component type that also
 * is annotated with Requires, the union of the required types is the effective
 * set of types.
 * <p>
 * When a component type that has been annotated with this annotation is added
 * to an entity, all required component types that are not already attached to
 * the entity are added. All newly added components have their owner set to the
 * initial component so that when it's removed, the required components are
 * cleaned up as well.
 * 
 * @author Michael Ludwig
 * 
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requires {
    /**
     * @return All ComponentData types required by the annotated component type
     */
    Class<? extends ComponentData<?>>[] value();
}
