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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.TypeId;

public class Scheduler {
    private final ThreadGroup schedulerGroup;

    // write lock is for tasks that add/remove entities, 
    // read lock is for all other tasks
    private final ReentrantReadWriteLock exclusiveLock;

    // locks per component data type, this map is filled
    // dynamically the first time each type is requested
    private final ConcurrentHashMap<TypeId<?>, ReentrantLock> typeLocks;

    private final EntitySystem system;

    public Scheduler(EntitySystem system) {
        this.system = system;

        schedulerGroup = new ThreadGroup("job-scheduler");
        exclusiveLock = new ReentrantReadWriteLock();
        typeLocks = new ConcurrentHashMap<TypeId<?>, ReentrantLock>();
    }

    public EntitySystem getEntitySystem() {
        return system;
    }

    ReentrantReadWriteLock getEntitySystemLock() {
        return exclusiveLock;
    }

    ReentrantLock getTypeLock(TypeId<?> id) {
        ReentrantLock lock = typeLocks.get(id);
        if (lock == null) {
            // this will either return the newly constructed lock, or 
            // the lock inserted from another thread after we tried to fetch it,
            // in either case, the lock is valid
            lock = typeLocks.putIfAbsent(id, new ReentrantLock());
        }
        return lock;
    }

    public Job createJob(String name, Task... tasks) {
        return new Job(name, this, tasks);
    }

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

    public ExecutorService runEvery(double dt, Job job) {
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

    public ExecutorService runContinuously(Job job) {
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
