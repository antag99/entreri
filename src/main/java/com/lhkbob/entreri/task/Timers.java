package com.lhkbob.entreri.task;

import java.util.Collections;
import java.util.Set;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.EntitySystem;

/**
 * Timers is a utility class that provides factory methods for creating Tasks
 * that report {@link ElapsedTimeResult} for use with other tasks that might
 * have time-dependent behavior.
 * 
 * @author Michael Ludwig
 * 
 */
public final class Timers {
    private Timers() {}

    /**
     * <p>
     * Create a new Task that reports an {@link ElapsedTimeResult} with the
     * provided fixed time delta. It is generally recommended for this task to
     * be one of the first to execute within a job.
     * <p>
     * The created task always reports the fixed time delta, regardless of true
     * elapsed time between invocations, and performs no other action.
     * 
     * @param dt The fixed time delta to always report
     * @return Return a task that measures and reports a fixed delta
     * @throws IllegalArgumentException if dt is less than or equal to zero
     */
    public static Task fixedDelta(double dt) {
        if (dt <= 0) {
            throw new IllegalArgumentException("Fixed delta must be positive: " + dt);
        }
        return new FixedDeltaTask(dt);
    }

    /**
     * <p>
     * Create a new Task that reports an {@link ElapsedTimeResult} with the
     * elapsed time since the start of the task's last invocation. It is
     * generally recommended for this task to be one of the first to execute
     * within a job.
     * <p>
     * The created task always reports the measured elapsed time and performs no
     * other action.
     * 
     * @return Return a task that measures and reports delta time
     */
    public static Task measuredDelta() {
        return new MeasuredDeltaTask();
    }

    /*
     * Task that reports the same delta each invocation, even if it does equal
     * the elapsed wall time
     */
    private static class FixedDeltaTask implements Task, ParallelAware {
        private final ElapsedTimeResult delta;

        public FixedDeltaTask(double dt) {
            delta = new ElapsedTimeResult(dt);
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            job.report(delta);
            return null;
        }

        @Override
        public void reset(EntitySystem system) {
            // do nothing
        }

        @Override
        public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEntitySetModified() {
            return false;
        }
    }

    /*
     * Task that measures the time delta from its last invocation
     */
    private static class MeasuredDeltaTask implements Task, ParallelAware {
        private long lastStart = -1L;

        @Override
        public Task process(EntitySystem system, Job job) {
            long now = System.nanoTime();
            if (lastStart <= 0) {
                job.report(new ElapsedTimeResult(0));
            } else {
                job.report(new ElapsedTimeResult((now - lastStart) / 1e9));
            }
            lastStart = now;

            return null;
        }

        @Override
        public void reset(EntitySystem system) {
            // do nothing
        }

        @Override
        public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEntitySetModified() {
            return false;
        }
    }
}
