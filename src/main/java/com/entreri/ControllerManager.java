package com.entreri;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * ControllerManager is a utility that manages the list of Controllers that can
 * process an EntitySystem. It also exposes two different ways of storing
 * controller data: the first is by annotation key for sharing between
 * controller types, the second is by a private object key for storing data
 * dependent on a system but internal to a controller implementation.
 * </p>
 * <p>
 * Additionally, the ControllerManager is used to invoke the phase processing
 * methods on each Controller on its configured EntitySystem.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class ControllerManager {
    /**
     * The Phase enum represents the different phases of
     * processing that an EntitySystem can go through during
     * what is often considered a "frame".
     */
    public static enum Phase {
        /**
         * The PREPROCESS phase is invoked before all other phases. All
         * controllers in a manager will have their
         * {@link Controller#preProcess(EntitySystem, float)} method called
         * before moving to the next phase.
         */
        PREPROCESS,

        /**
         * The PROCESS phase is invoked between PREPROCESS and POSTPROCESS. All
         * controllers in the manager will have their
         * {@link Controller#process(EntitySystem, float)} method called before
         * moving to the next phase.
         */
        PROCESS,

        /**
         * The POSTPROCESS phase is invoked after PREPROCESS and POSTPROCESS.
         * All controllers in their manager will have their
         * {@link Controller#postProcess(EntitySystem, float)} method called
         * before the frame is completed.
         */
        POSTPROCESS,

        /**
         * ALL is a special PHASE that represents all three true phases invoked
         * in their proper order.
         */
        ALL
    }

    private float fixedDelta;
    private final List<Controller> controllers;
    
    // This is a concurrent map so that parallel controllers can access it efficiently
    // - the rest of the class is assumed to be single-threaded
    private final ConcurrentHashMap<Class<? extends Annotation>, Object> controllerData;
    private final ConcurrentHashMap<Object, Object> privateData;

    private final EntitySystem system;

    /**
     * Create a new ControllerManager that will store controllers and controller
     * data for processing the given EntitySystem.
     * 
     * @param system The EntitySystem that is managed by this controller manager
     * @throws NullPointerException if system is null
     */
    public ControllerManager(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        
        this.system = system;
        controllerData = new ConcurrentHashMap<Class<? extends Annotation>, Object>();
        privateData = new ConcurrentHashMap<Object, Object>();
        controllers = new ArrayList<Controller>();
        
        fixedDelta = 1 / 60f; // 60fps
    }

    /**
     * Set a new fixed time frame delta. This can take any value and represents
     * the number of seconds between each frame, so negative values may produce
     * undefined results with some controllers. This value is only used if
     * {@link #process()} is invoked, the other varieties take a delta value
     * that overrides this one.
     * 
     * @param dt The new time frame delta
     */
    public void setFixedDelta(float dt) {
        fixedDelta = dt;
    }

    /**
     * @return The current fixed time frame delta that is used if
     *         {@link #process()} is invoked
     */
    public float getFixedDelta() {
        return fixedDelta;
    }
    
    /**
     * Return the controller data that has been mapped to the given annotation
     * <tt>key</tt>. This will return if there has been no assigned data. This
     * can be used to store arbitrary data that must be shared between related
     * controllers.
     * 
     * @param key The annotation key
     * @return The object previously mapped to the annotation with
     *         {@link #setControllerData(Class, Object)}
     * @throws NullPointerException if key is null
     */
    public Object getControllerData(Class<? extends Annotation> key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return controllerData.get(key);
    }

    /**
     * Map <tt>value</tt> to the given annotation <tt>key</tt> so that future
     * calls to {@link #getControllerData(Class)} with the same key will return
     * the new value. If the value is null, any previous mapping is removed.
     * 
     * @param key The annotation key
     * @param value The new value to store
     * @throws NullPointerException if key is null
     */
    public void setControllerData(Class<? extends Annotation> key, Object value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            controllerData.remove(key);
        else
            controllerData.put(key, value);
    }

    /**
     * Retrieve a privately stored instance from this manager's cache. It is
     * similar to {@link #getControllerData(Class)} except the key is intended
     * to be something known only to the owner so the data is effectively hidden
     * from other controllers (unlike the annotation key which facilitates
     * cross-controller communication).
     * 
     * @param key The key to lookup
     * @return The value associated with the key, or null
     * @throws NullPointerException if key is null
     */
    public Object getPrivateData(Object key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return privateData.get(key);
    }

    /**
     * Store the given value into this manager's cache so that it can be
     * retrieved by the given key. It is intended that the key is not accessible
     * to other controllers so that this represents private data. If sharing is
     * desired, create an annotation description and use
     * {@link #setControllerData(Class, Object)}.
     * 
     * @param key The key to store the value to
     * @param value The value being stored, or null to remove the old value
     * @throws NullPointerException if key is null
     */
    public void setPrivateData(Object key, Object value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            privateData.remove(key);
        else
            privateData.put(key, value);
    }

    /**
     * <p>
     * Add a Controller to this manager so that subsequent calls to
     * {@link #process()} and its varieties will invoke the process hooks on the
     * controller. The new controller is invoked after all already added
     * controllers.
     * </p>
     * <p>
     * If this controller was already added to the manager, it will be moved to
     * the end of the list.
     * </p>
     * 
     * @param controller The controller to add
     * @throws NullPointerException if controller is null
     */
    public void addController(Controller controller) {
        if (controller == null)
            throw new NullPointerException("Controller cannot be null");
        
        // remove it first - which does nothing if not in the list
        controllers.remove(controller);
        // now add it to the end
        controllers.add(controller);
    }

    /**
     * Remove a controller from the manager so that it is no longer invoked when
     * {@link #process()} and its related functions are called. If the
     * controller has not been added to the manager, this does nothing.
     * 
     * @param controller The controller to remove
     * @throws NullPointerException if controller is null
     */
    public void removeController(Controller controller) {
        if (controller == null)
            throw new NullPointerException("Controller cannot be null");
        controllers.remove(controller);
    }
    
    /**
     * Run all phases of the manager using the current fixed frame time delta.
     */
    public void process() {
        process(fixedDelta);
    }

    /**
     * Run all phases of the manager using the specified frame time delta,
     * <tt>dt</tt>.
     * 
     * @param dt The time delta for the frame, or the amount of time since the
     *            start of the last frame and this one
     */
    public void process(float dt) {
        process(Phase.ALL, dt);
    }

    /**
     * Run the processing of a particular phase for this manager, using the
     * specified time delta. If the phase is {@link Phase#ALL}, all phases will
     * be run in their proper order.
     * 
     * @param phase The specific phase to run, or ALL to specify all phases
     * @param dt The time delta for the frame, or the amount of time since the
     *            start of the last frame and this one
     * @throws NullPointerException if phase is null
     */
    public void process(Phase phase, float dt) {
        if (phase == null)
            throw new NullPointerException("Phase cannot be null");
        
        switch(phase) {
        case PREPROCESS:
            doPreProcess(dt); break;
        case PROCESS:
            doProcess(dt); break;
        case POSTPROCESS:
            doPostProcess(dt); break;
        case ALL:
            // Perform all stages in one go
            doPreProcess(dt);
            doProcess(dt);
            doPostProcess(dt);
            break;
        }
    }
    
    private void doPreProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).preProcess(system, dt);
    }
    
    private void doProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).process(system, dt);
    }
    
    private void doPostProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).postProcess(system, dt);
    }
}
