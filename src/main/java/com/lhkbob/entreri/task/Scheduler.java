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
import com.lhkbob.entreri.EntitySystem;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Scheduler
 * =========
 *
 * Scheduler coordinates the multi-threaded execution of jobs that process an EntitySystem. It is the factory
 * that creates jobs and contains convenience methods to schedule the execution of jobs.
 * As an example, here's how you can set up a 'rendering' job, assuming that all tasks necessary to perform
 * rendering are in an array called `renderTasks`:
 *
 * ```java
 * Job renderJob = system.getScheduler().createJob(&quot;rendering&quot;, renderTasks);
 * ExecutorService service = system.getScheduler().runEvery(1.0 / 60.0, renderJob);
 *
 * // ... perform game logic, and wait for exit request
 * service.shutdown();
 * ```
 *
 * @author Michael Ludwig
 */
public class Scheduler {
    private final ThreadGroup schedulerGroup;

    // write lock is for tasks that add/remove entities, 
    // read lock is for all other tasks
    private final ReentrantReadWriteLock exclusiveLock;

    // locks per component data type, this map is filled
    // dynamically the first time each type is requested
    private final ConcurrentHashMap<Class<? extends Component>, ReentrantReadWriteLock> typeLocks;

    private final EntitySystem system;

    /**
     * Create a new Scheduler for the given EntitySystem. It is recommended to use the scheduler provided by
     * the system. If multiple schedulers exist for the same entity system, they cannot guarantee thread
     * safety between each other, only within their own jobs.
     *
     * @param system The EntitySystem accessed by jobs created by this scheduler
     * @throws NullPointerException if system is null
     * @see EntitySystem#getScheduler()
     */
    public Scheduler(EntitySystem system) {
        if (system == null) {
            throw new NullPointerException("EntitySystem cannot be null");
        }
        this.system = system;

        schedulerGroup = new ThreadGroup("job-scheduler");
        exclusiveLock = new ReentrantReadWriteLock();
        typeLocks = new ConcurrentHashMap<>();
    }

    /**
     * @return The EntitySystem accessed by this scheduler
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * @return The read-write lock used to coordinate entity data access
     */
    ReentrantReadWriteLock getEntitySystemLock() {
        return exclusiveLock;
    }

    /**
     * @param id The component type to lock
     * @return The lock used to coordinate access to the particular componen type
     */
    ReentrantReadWriteLock getTypeLock(Class<? extends Component> id) {
        ReentrantReadWriteLock lock = typeLocks.get(id);
        if (lock == null) {
            ReentrantReadWriteLock newLock = new ReentrantReadWriteLock();
            // this will return the first lock stored into the map, or null if this thread created the first lock
            lock = typeLocks.putIfAbsent(id, newLock);
            if (lock == null) {
                // newLock was the assigned one
                lock = newLock;
            }
        }
        return lock;
    }

    /**
     * Create a new job with the given `name`, that will execute the provided tasks in order.
     *
     * @param name  The name of the new job
     * @param tasks The tasks of the job
     * @return The new job
     * @throws NullPointerException if name is null, tasks is null or contains null elements
     */
    public Job createJob(String name, Task... tasks) {
        return new Job(name, this, tasks);
    }

    /**
     * Execute the given job on the current thread. This will not return until after the job has completed
     * invoking all of its tasks, and any subsequently produced tasks.
     *
     * This is a convenience for invoking {@link Job#run()}, and exists primarily to parallel the other
     * runX(Job) methods.
     *
     * @param job The job to run
     * @throws NullPointerException     if job is null
     * @throws IllegalArgumentException if job was not created by this scheduler
     */
    public void runOnCurrentThread(Job job) {
        if (job == null) {
            throw new NullPointerException("Job cannot be null");
        }
        if (job.getScheduler() != this) {
            throw new IllegalArgumentException("Job was created by a different scheduler");
        }

        // the job will handle all locking logic
        job.run();
    }

    /**
     * Execute the given job once on a new thread. This will create a new thread that will invoke the job once
     * and then terminate once the job returns. This method will return after the thread starts and will not
     * block the calling thread while the job is executed.
     *
     * This should be used as a convenience to invoke one-off jobs that should not block a performance
     * sensitive thread.
     *
     * @param job The job to run
     * @throws NullPointerException     if job is null
     * @throws IllegalArgumentException if job was not created by this scheduler
     */
    public void runOnSeparateThread(Job job) {
        if (job == null) {
            throw new NullPointerException("Job cannot be null");
        }
        if (job.getScheduler() != this) {
            throw new IllegalArgumentException("Job was created by a different scheduler");
        }

        // spawn a new thread that will terminate when the job completes
        Thread jobThread = new Thread(schedulerGroup, job, "job-" + job.getName());
        jobThread.start();
    }

    /**
     * Create an ExecutorService that is configured to execute the given job every `dt` seconds. Assuming
     * that the job terminates in under `dt` seconds, it will not be invoked until `dt` seconds after it was
     * first started. To schedule a rendering job to run at 60 FPS, you could call `runEvery(1.0 / 60.0,
     * renderJob)`.
     *
     * The returned ExecutorService should have its {@link ExecutorService#shutdown() shutdown()} method
     * called when the job no longer needs to be invoked. Scheduling timing is undefined if new Runnables or
     * Callables are submitted to the returned service.
     *
     * @param dt  The amount of time between the start of each job execution
     * @param job The job to be repeatedly executed
     * @return An unconfigurable executor service that performs the scheduling, and owns the execution thread
     * @throws NullPointerException     if job is null
     * @throws IllegalArgumentException if job was not created by this scheduler, or if dt is negative
     */
    public ExecutorService runEvery(double dt, Job job) {
        if (job == null) {
            throw new NullPointerException("Job cannot be null");
        }
        if (job.getScheduler() != this) {
            throw new IllegalArgumentException("Job was created by a different scheduler");
        }
        if (dt < 0) {
            throw new IllegalArgumentException("Time between jobs cannot be negative: " + dt);
        }

        final String name = String.format("job-%s-every-%.2fs", job.getName(), dt);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(schedulerGroup, r, name);
            }
        });
        service.scheduleAtFixedRate(job, 0L, (long) (dt * 1e9), TimeUnit.NANOSECONDS);
        return Executors.unconfigurableExecutorService(service);
    }

    /**
     * Create an ExecutorService that is configured to execute the given job back to back as fast as the job
     * executes.
     * This effectively performs the following logic on a separate thread:
     *
     * ```java
     * while (true) {
     * job.run();
     * }
     * ```
     *
     * The returned ExecutorService should have its {@link ExecutorService#shutdown() shutdown()} method
     * called when the job no longer needs to be invoked. Scheduling timing is undefined if new Runnables or
     * Callables are submitted to the returned service.
     *
     * @param job The job to be repeatedly executed
     * @return An unconfigurable executor service that performs the scheduling, and owns the execution thread
     * @throws NullPointerException     if job is null
     * @throws IllegalArgumentException if job was not created by this scheduler
     */
    public ExecutorService runContinuously(Job job) {
        if (job == null) {
            throw new NullPointerException("Job cannot be null");
        }
        if (job.getScheduler() != this) {
            throw new IllegalArgumentException("Job was created by a different scheduler");
        }

        final String name = String.format("job-%s-as-fast-as-possible", job.getName());
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(schedulerGroup, r, name);
            }
        });

        // ScheduledExecutorService has no way to just specify run-as-fast-as-possible.
        // However, if a task takes longer than its fixed-rate, that is the resulting,
        // behavior. There is a strong probability that all jobs will take longer
        // than a single nanosecond, so this should do the trick.
        service.scheduleAtFixedRate(job, 0L, 1L, TimeUnit.NANOSECONDS);
        return Executors.unconfigurableExecutorService(service);
    }
}
