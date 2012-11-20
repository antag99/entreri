package com.lhkbob.entreri.task;

/**
 * ElapsedTimeResult is a utility result that can be used to report and received
 * the amount of elapsed time between executions of a job.
 * 
 * @see Timers
 * @author Michael Ludwig
 * 
 */
public class ElapsedTimeResult extends Result {
    private final double dt;

    /**
     * Create a new result that delivers the given time delta, in seconds.
     * 
     * @param dt The elapsed time in seconds
     */
    public ElapsedTimeResult(double dt) {
        this.dt = dt;
    }

    /**
     * @return The elapsed time in seconds
     */
    public double getTimeDelta() {
        return dt;
    }

    /**
     * @return True, ElapsedTimeResults are singletons
     */
    @Override
    public boolean isSingleton() {
        return true;
    }
}
