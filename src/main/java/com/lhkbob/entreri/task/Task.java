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

import com.lhkbob.entreri.EntitySystem;

/**
 * Task
 * ====
 *
 * Tasks are functional processors of the entities and components within an EntitySystem. Different Task
 * implementations have different purposes, such as updating transforms, computing physics or AI, or rendering
 * a scene. Generally task implementations should be as small and as independent as possible to allow for
 * reuse and easy compose-ability.
 *
 * The {@link Job} and {@link Scheduler} work together to coordinate the execution of collections of tasks.
 * Tasks are grouped into jobs, where a job represents a logical step such as "render frame", or "compute
 * physics", even if it is split into multiple tasks. Within a job, tasks are executed serially, but the
 * scheduler can run different jobs simultaneously. The interface {@link ParallelAware} can be used with tasks
 * classes to restrict the contention points of a job. This is important because a job must acquire the
 * component locks for each of its tasks before invoking the first task. The locks are not released until the
 * last task has terminated.
 *
 * A task can communicate with the remaining tasks of a job by {@link Job#report(Result) reporting} results.
 * The results are only reported to tasks within the owning job. Results are reported to tasks that have
 * already completed and to those that must still be processed within that job. Tasks that are returned from a
 * call to `process()` are in a new job and so do not receive result reports from the original job (they can
 * still report and receive results generated during the post-process job execution). In order for a
 * post-processing task to receive results from the original job, the owning task in the job must receive the
 * results and manually expose them to the later task.
 *
 * A task receives results by defining any number of methods with the signature:
 *
 * ```java
 * public void report(T extends Result)
 * ```
 *
 * When a result is reported to the job, it will invoke using reflection any `report()` method that takes a
 * result of the compatible type.
 *
 * Task instances should only ever be used with a single job and entity system. If the task needs to be
 * performed in multiple jobs or systems, new instances of the same type should be created.
 *
 * @author Michael Ludwig
 */
public interface Task {
    /**
     * Invoke task specific operations to process the EntitySystem. This will be invoked on the task's parent
     * job's thread, after the job has acquired any locks mandated by {@link ParallelAware}. If this task is
     * not ParallelAware, the job acquires an exclusive lock with the assumption that it could modify
     * anything.
     *
     * A task can return another task to be invoked after the owning job completes. These returned tasks are
     * executed within their own set of locks, so it can be used to segment how long, and which locks are held
     * by a job.
     *
     * @param system The EntitySystem being processed, which will always be the same for a given Task
     *               instance
     * @param job    The Job this task belongs to
     * @return A nullable task that is executed after the job is completed
     */
    public Task process(EntitySystem system, Job job);

    /**
     * Reset any internal storage within this Task in preparation for the next execution of its owning job.
     * This is used when jobs are scheduled to repeat at a given rate. Instead of instantiating new tasks
     * every time, they can reset their tasks. All tasks in a job will be reset before the first task is
     * processed.
     *
     * Examples of how this can be used is if a spatial index is built up per-frame, or if decorated
     * properties cache computed results. By using reset(), it's not necessary to re-decorate or allocate
     * again.
     *
     * @param system The EntitySystem that will be processed, which will always be the same for a given Task
     *               instance
     */
    public void reset(EntitySystem system);
}
