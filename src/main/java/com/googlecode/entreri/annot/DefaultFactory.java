package com.googlecode.entreri.annot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.googlecode.entreri.ComponentDataFactory;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.property.ReflectionComponentDataFactory;

/**
 * <p>
 * DefaultFactory is a type-level annotation that can be added to ComponentData
 * definitions to declare a different ComponentDataFactory than
 * {@link ReflectionComponentDataFactory} as the default. This default will be
 * used by any EntitySystem unless it has a per-system factory override that was
 * set with
 * {@link EntitySystem#setFactory(com.googlecode.entreri.TypeId, ComponentDataFactory)}
 * </p>
 * <p>
 * Runtime exceptions will be thrown if the factory type declared by the
 * annotation does not have an accessible, supported constructor. The currently
 * supported constructors are:
 * <ol>
 * <li><code>ComponentDataFactory()</code></li>
 * <li><code>ComponentDataFactory(TypeId&lt;T&gt;)</code></li>
 * <li>
 * <code>ComponentDataFactory(Class&lt;T extends ComponentData&lt;T&gt;&gt;)</code>
 * </li>
 * </ol>
 * </p>
 * 
 * @author Michael Ludwig
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultFactory {
    /**
     * @return The ComponentDataFactory implementation used to create
     *         ComponentData's
     */
    Class<? extends ComponentDataFactory<?>> value();
}
