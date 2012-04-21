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
package com.lhkbob.entreri;

/**
 * <p>
 * Result represents a computed result, preferably that of a bulk computation,
 * performed by a Controller. Results allow Controllers to easily pass data to
 * other controllers that might be interested in their computations.
 * </p>
 * <p>
 * Controllers that wish to expose results must define an interface that is
 * capable of receiving their computations. The signature of this method is
 * entirely up to the Controller. Then, the controller wraps the computed
 * results in an internal Result implementation that knows how to invoke the
 * listener interface defined previously with the just computed data. To provide
 * to all interested Controllers, the {@link ControllerManager#supply(Result)}
 * method can be used.
 * </p>
 * <p>
 * To receive computed results, Controllers merely have to implement the
 * listener interfaces defined by the controllers of interest and store the
 * supplied results.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The listener type
 */
public interface Result<T> {
    /**
     * <p>
     * Supply this result to the provided listener of type T. It is expected
     * that a listener interface will define some method that enables compatible
     * Result implementations to inject the computed data.
     * </p>
     * <p>
     * This injection or supplying is performed by this method. Controllers that
     * compute results will define interfaces as appropriate to receive their
     * results, and result implementations to provide those results.
     * </p>
     * 
     * @param listener The listener to receive the event
     */
    public void supply(T listener);
    
    /**
     * @return The listener interface this result is supplied to
     */
    public Class<T> getListenerType();
}
