package com.lhkbob.entreri.task;

import com.lhkbob.entreri.EntitySystem;

public final class Timers {

    public static Task fixedDelta(double dt) {
        return new FixedDeltaTask(dt);
    }

    public static Task measuredDelta() {
        return new MeasuredDeltaTask();
    }

    private static class FixedDeltaTask implements Task {
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
    }

    private static class MeasuredDeltaTask implements Task {
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
    }
}
