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
