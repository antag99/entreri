/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

import java.lang.annotation.*;

/**
 * ParallelAware
 * =============
 *
 * ParallelAware is an annotation that {@link Task} implementations can add. Tasks that are parallel aware
 * hold to the contract that they will only modify or read from a limited and knowable set of component types,
 * or that they will or will not add or remove entities from the entity system.
 *
 * With that assumption, and the values returned by {@link #modifiedComponents()}, {@link
 * #readOnlyComponents()} and {@link #entitySetModified()}, jobs can automatically guarantee thread safe
 * execution of their tasks.
 *
 * It is highly recommended to add ParallelAware if it's known what types of components or entities will be
 * modified at compile time. The absence of ParallelAware on a task type forces the job execution to acquire
 * the system's exclusive lock.
 *
 * @author Michael Ludwig
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParallelAware {
    /**
     * Return the set of all component data types that might have their data mutated, be added to, or removed
     * from an entity. It must return the maximal set of types that might be added, removed, or
     * modified by the task. If a task's component access is completely determined at runtime, then it should
     * not be parallel aware, or it should return true from {@link #entitySetModified()}.
     *
     * Jobs that do not share any access to the same component types (i.e. their intersection of the returned
     * set is empty), can be run in parallel if they both return false from {@link #entitySetModified()}.
     * Exclusive locks are used for the types returned from this method so a task can be guaranteed no other
     * task is reading or writing to the component types.
     *
     * @return The set of all component types that might be added, removed, or modified by the task
     */
    public Class<? extends Component>[] modifiedComponents();

    /**
     * Return the set of all component data types that might be read during task execution. Types that are
     * already included in {@link #modifiedComponents()} do not need to be duplicated in this method. It must
     * return the maximal set of types that might be read.  If a task's component access is completely
     * determined at runtime, then it should not be parallel aware, or it should return true from {@link
     * #entitySetModified()}.
     *
     * Jobs that do not share any access to the same component types (i.e. their intersection of the
     * returned set is empty), can be run in parallel if they both return false from {@link
     * #entitySetModified()}. Read-only locks are used for components returned from this method so a task can
     * be guaranteed no other task is writing to the component types, although other tasks may also be reading
     * data.
     *
     * @return The set of all component types that might be added, removed, or modified by the task
     */
    public Class<? extends Component>[] readOnlyComponents();

    /**
     * Return whether or not {@link Entity entities} are added or removed from an EntitySystem. Note that
     * this refers to using {@link EntitySystem#addEntity()} or {@link EntitySystem#removeEntity(Entity)}, or
     * iterators that trigger the same effect. When true is returned, the job will be forced to execute with
     * an exclusive lock that blocks other jobs.
     *
     * Thus it is a best practice to limit the run-time of tasks that require exclusive locks. It is better
     * to have a parallel aware task that accesses only specific component types to determine if an entity
     * must be added or removed. Once these are determined, it keeps track and returns a new task from {@link
     * Task#process(EntitySystem, Job)} that will get the exclusive lock and perform the determined additions
     * or removals.
     *
     * @return True if the task might add or remove entities from the system
     */
    public boolean entitySetModified();
}
