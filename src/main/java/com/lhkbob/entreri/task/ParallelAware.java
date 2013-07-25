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
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

import java.util.Set;

/**
 * <p/>
 * ParallelAware is an interface that {@link Task} implementations can implement. Tasks that are parallel
 * aware hold to the contract that they will only modify a limited and knowable set of component types, or
 * that they will or will not add or remove entities from the entity system.
 * <p/>
 * With that assumption based on the what is returned by {@link #getAccessedComponents()} and {@link
 * #isEntitySetModified()}, jobs will automatically guarantee thread safe execution of their tasks.
 * <p/>
 * It is highly recommended to implement ParallelAware if it's known what types of components or entities will
 * be modified at compile time.
 *
 * @author Michael Ludwig
 */
public interface ParallelAware {
    /**
     * <p/>
     * Get the set of all component data types that might have their data mutated, be added to, or removed
     * from an entity. This must always return the same types for a given instance, it cannot change based on
     * state of the task. Instead, it must return the maximal set of types that might be added, removed, or
     * modified by the task.
     * <p/>
     * If a task's component access is determined at runtime, then it should not be parallel aware, or it
     * should return true from {@link #isEntitySetModified()}.
     * <p/>
     * Jobs that do not share any access to the same component types (i.e. their intersection of the returned
     * set is empty), can be run in parallel if they both return false from {@link #isEntitySetModified()}.
     *
     * @return The set of all component types that might be added, removed, or modified by the task
     */
    public Set<Class<? extends Component>> getAccessedComponents();

    /**
     * <p/>
     * Return whether or not {@link Entity entities} are added or removed from an EntitySystem. Note that this
     * refers to using {@link EntitySystem#addEntity()} or {@link EntitySystem#removeEntity(Entity)}. When
     * true is returned, the job will be forced to execute with an exclusive lock that blocks other jobs.
     * <p/>
     * Thus it is a best practice to limit the run-time of tasks that require exclusive locks. It is better to
     * have a parallel aware task that accesses only specific component types to determine if an entity must
     * be added or removed.
     * <p/>
     * Once these are determined, it keeps track and returns a new task from {@link Task#process(EntitySystem,
     * Job)} that will get the exclusive lock and perform the determined additions or removals.
     * <p/>
     * Like {@link #getAccessedComponents()}, this must always return the same value for a given instance, and
     * cannot change during its lifetime.
     *
     * @return True if the task might add or remove entities from the system
     */
    public boolean isEntitySetModified();
}
