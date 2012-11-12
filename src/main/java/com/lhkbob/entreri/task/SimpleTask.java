/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.TypeId;

public abstract class SimpleTask implements Task {
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Optional {}

    private final Method processMethod;
    private final boolean[] optional;

    // filled with instances after first call to processEntities
    private final ComponentData<?>[] componentDatas;

    // "final" after the first call to processEntities()
    private ComponentIterator iterator;

    public SimpleTask() {
        Method processMethod = null;

        Class<?> cls = getClass();
        while (!SimpleTask.class.equals(cls)) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("processEntity")) {
                    if (m.getParameterTypes().length > 0 && m.getReturnType()
                                                             .equals(boolean.class)) {
                        boolean paramsValid = true;
                        for (Class<?> p : m.getParameterTypes()) {
                            if (!ComponentData.class.isAssignableFrom(p)) {
                                paramsValid = false;
                                break;
                            }
                        }

                        if (paramsValid) {
                            if (processMethod == null) {
                                processMethod = m;
                            } else {
                                throw new IllegalStateException("More than one processEntity() method defined");
                            }
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }

        if (processMethod == null) {
            throw new IllegalStateException("SimpleTask subclasses must define a processEntity() method");
        }

        processMethod.setAccessible(true);

        this.processMethod = processMethod;
        optional = new boolean[processMethod.getParameterTypes().length];
        componentDatas = new ComponentData<?>[optional.length];

        for (int i = 0; i < optional.length; i++) {
            for (Annotation a : processMethod.getParameterAnnotations()[i]) {
                if (a instanceof Optional) {
                    optional[i] = true;
                    break;
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void processEntities(EntitySystem system) {
        if (iterator == null) {
            iterator = new ComponentIterator(system);
            for (int i = 0; i < optional.length; i++) {
                ComponentData<?> data = system.createDataInstance(TypeId.get((Class) processMethod.getParameterTypes()[i]));
                if (optional[i]) {
                    iterator.addOptional(data);
                } else {
                    iterator.addRequired(data);
                }
                componentDatas[i] = data;
            }
        }

        try {
            Object[] invokeArgs = new Object[optional.length];
            iterator.reset();
            while (iterator.next()) {
                for (int i = 0; i < optional.length; i++) {
                    invokeArgs[i] = (optional[i] && !componentDatas[i].isEnabled() ? null : componentDatas[i]);
                }

                boolean iterate = ((Boolean) processMethod.invoke(this, invokeArgs)).booleanValue();
                if (!iterate) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while invoking processEntity()", e);
        }
    }
}
