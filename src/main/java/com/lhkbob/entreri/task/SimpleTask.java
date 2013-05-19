/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2013, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lhkbob.entreri.task;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;

import java.lang.annotation.*;
import java.lang.reflect.Method;

/**
 * <p/>
 * SimpleTask extends Task adds logic to simplify the creation of tasks that perform the
 * same operations on each entity that matches a specific component configuration.
 * Subclasses of SimpleTask should define a single method named 'processEntity' that takes
 * as its only parameters, any number of Component instances of specific types. An example
 * might be:
 * <p/>
 * <pre>
 * public class ExampleTask extends SimpleTask {
 *     // c1 and c2 are required types
 *     // c3 is an optional component type
 *     protected void processEntities(TypeA c1, TypeB c2, @Optional TypeC c3) {
 *         // perform operations on c1 and c2
 *         if (c3 != null) {
 *             // perform additional operations on c3
 *         }
 *     }
 *
 *     public Task process(EntitySystem system, Job job) {
 *         // this will invoke processEntities() for each entity in the system
 *         // that has a TypeA and TypeB component. If the entity also has
 *         // a TypeC component, it is passed in too, otherwise it's null
 *         processEntities(system);
 *         return null;
 *     }
 * }
 * </pre>
 * <p/>
 * In the task's {@link #process(EntitySystem, Job)} method, it can then invoke {@link
 * #processEntities(EntitySystem)} to perform the automated iteration over matching
 * entities within the system. SimpleTask will call the identified 'processEntity' method
 * for each matched entity.
 *
 * @author Michael Ludwig
 */
public abstract class SimpleTask implements Task {
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Optional {
    }

    private final Method processMethod;
    private final boolean[] optional;

    // filled with instances after first call to processEntities
    private final Component[] componentDatas;

    // "final" after the first call to processEntities() or until the system changes
    private ComponentIterator iterator;
    private EntitySystem lastSystem;

    public SimpleTask() {
        Method processMethod = null;

        Class<?> cls = getClass();
        while (!SimpleTask.class.equals(cls)) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("processEntity")) {
                    if (m.getParameterTypes().length > 0 &&
                        m.getReturnType().equals(boolean.class)) {
                        boolean paramsValid = true;
                        for (Class<?> p : m.getParameterTypes()) {
                            if (!Component.class.isAssignableFrom(p)) {
                                paramsValid = false;
                                break;
                            }
                        }

                        if (paramsValid) {
                            if (processMethod == null) {
                                processMethod = m;
                            } else {
                                throw new IllegalStateException(
                                        "More than one processEntity() method defined");
                            }
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }

        if (processMethod == null) {
            throw new IllegalStateException(
                    "SimpleTask subclasses must define a processEntity() method");
        }

        processMethod.setAccessible(true);

        this.processMethod = processMethod;
        optional = new boolean[processMethod.getParameterTypes().length];
        componentDatas = new Component[optional.length];

        for (int i = 0; i < optional.length; i++) {
            for (Annotation a : processMethod.getParameterAnnotations()[i]) {
                if (a instanceof Optional) {
                    optional[i] = true;
                    break;
                }
            }
        }
    }

    /**
     * The default implementation of process() just invokes {@link
     * #processEntities(com.lhkbob.entreri.EntitySystem)} immediately and returns no
     * future task.
     *
     * @param system The EntitySystem being processed, which will always be the same for a
     *               given Task instance
     * @param job    The Job this task belongs to
     *
     * @return Will return null unless overridden
     */
    @Override
    public Task process(EntitySystem system, Job job) {
        processEntities(system);
        return null;
    }

    /**
     * Process all entities that fit the component profile mandated by the defined
     * 'processEntity()' method in the subclass.
     *
     * @param system The system to process
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processEntities(EntitySystem system) {
        if (iterator == null || lastSystem != system) {
            iterator = system.fastIterator();
            for (int i = 0; i < optional.length; i++) {
                if (optional[i]) {
                    componentDatas[i] = iterator
                            .addOptional((Class) processMethod.getParameterTypes()[i]);
                } else {
                    componentDatas[i] = iterator
                            .addRequired((Class) processMethod.getParameterTypes()[i]);
                }
            }

            lastSystem = system;
        }

        try {
            Object[] invokeArgs = new Object[optional.length];
            iterator.reset();
            while (iterator.next()) {
                for (int i = 0; i < optional.length; i++) {
                    invokeArgs[i] = (optional[i] && !componentDatas[i].isAlive() ? null
                                                                                 : componentDatas[i]);
                }

                boolean iterate = ((Boolean) processMethod.invoke(this, invokeArgs));
                if (!iterate) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while invoking processEntity()", e);
        }
    }
}
