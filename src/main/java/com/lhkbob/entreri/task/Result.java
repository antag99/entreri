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

/**
 * <p>
 * Result represents a computed result, preferably that of a bulk computation,
 * performed by a Task. Results allow Taskss to easily pass data to other tasks
 * in the same job that might be interested in their computations.
 * </p>
 * <p>
 * Tasks that wish to expose results define classes that extend Result and
 * provide the actual result data. During processing of a task, result instances
 * are created and supplied to all future tasks by calling
 * {@link Job#report(Result)}
 * </p>
 * <p>
 * To receive computed results, Tasks define
 * <code>public void report(T extends Result)</code> methods that are invoked
 * through reflection by the job when a compatible result is reported.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Result {
    /**
     * <p>
     * Return true if this result is a "singleton" result. A singleton result is
     * a type of result that is can only be reported once per execution of a
     * job. The job verifies that singleton results are supplied at most once
     * per job execution. Most results should return false. The returned value
     * should be the same for every instance of a type, it should not depend on
     * the state of the instance.
     * </p>
     * <p>
     * Singleton results should only be used when the computation of the result
     * produces all of the result data. As an example, a 3D engine might assign
     * entities to lights, and each unique configuration of lights on an entity
     * is a "light group". It makes more sense to provide a single result that
     * describes all light groups than individual results for each group. They
     * are packed into a single result because each group is dependent on the
     * others to guarantee its uniqueness.
     * </p>
     * <p>
     * As a counter example, computing potentially visible sets for a 3D engine
     * should not be a singleton result. A result that contains the view and set
     * of visible entities is a self-contained result, other views or cameras do
     * not affect the PVS results.
     * </p>
     * <p>
     * The default implementation returns false. Subtypes are free to override
     * it, but it must return the same value for all instances of a given type.
     * </p>
     * 
     * @return True if this result should only be supplied at most once during
     *         each frame
     */
    public boolean isSingleton() {
        return false;
    }
}
