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

/**
 * ElapsedTimeResult
 * =================
 *
 * A common utility result that can be used to report the amount of elapsed time between executions of a
 * job. Since this concept is so ubiquitous it is recommended to use this result unless additional data must
 * be passed with the elapsed time. This is a singleton result. Use the {@link com.lhkbob.entreri.task.Timers}
 * factory to create tasks that report these results for a variety of common scenarios.
 *
 * @author Michael Ludwig
 * @see Timers
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
