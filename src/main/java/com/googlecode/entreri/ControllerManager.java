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
package com.googlecode.entreri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * ControllerManager is a utility that manages the list of Controllers that can
 * process an EntitySystem. It also provides a mechanism to share data between
 * controllers by storing objects associated with {@link Key Keys}. If a
 * Controller type publically exposes a static key, other controllers can then
 * look up the associated value.
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
     * Key represents a typed key into controller data managed by a
     * ControllerManager. Equality is defined by reference, so it is generally
     * best to store keys for reuse in private or public fields (possibly static
     * if the key is shared).
     * 
     * @param <T> The type of data associated with the key
     */
    public static class Key<T> { }
    
    /**
     * The Phase enum represents the different phases of
     * processing that an EntitySystem can go through during
     * what is often considered a "frame".
     */
    public static enum Phase {
        /**
         * The PREPROCESS phase is invoked before all other phases. All
         * controllers in a manager will have their
         * {@link Controller#preProcess(float)} method called
         * before moving to the next phase.
         */
        PREPROCESS,

        /**
         * The PROCESS phase is invoked between PREPROCESS and POSTPROCESS. All
         * controllers in the manager will have their
         * {@link Controller#process(float)} method called before
         * moving to the next phase.
         */
        PROCESS,

        /**
         * The POSTPROCESS phase is invoked after PREPROCESS and POSTPROCESS.
         * All controllers in their manager will have their
         * {@link Controller#postProcess(float)} method called
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
    private final ConcurrentHashMap<Key<?>, Object> controllerData;

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
        controllerData = new ConcurrentHashMap<Key<?>, Object>();
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
     * Return the controller data that has been mapped to the given <tt>key</tt>
     * . This will return null if there has been no assigned data. The getData()
     * and setData() methods should be used by controllers to share data between
     * themselves, and to store system-dependent state so that their
     * implementations are properly indepdent of the system.
     * 
     * @param key The annotation key
     * @return The object previously mapped to the annotation with
     *         {@link #setData(Key, Object)}
     * @throws NullPointerException if key is null
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(Key<T> key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return (T) controllerData.get(key);
    }

    /**
     * Store <tt>value</tt> to the given <tt>key</tt> so that future
     * calls to {@link #getData(Key)} with the same key will return
     * the new value. If the value is null, any previous mapping is removed.
     * 
     * @param key The key
     * @param value The new value to store
     * @throws NullPointerException if key is null
     */
    public <T> void setData(Key<T> key, T value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            controllerData.remove(key);
        else
            controllerData.put(key, value);
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
        boolean removed = controllers.remove(controller);
        // now add it to the end
        controllers.add(controller);
        
        if (!removed)
            controller.init(system);
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
        boolean removed = controllers.remove(controller);
        if (removed)
            controller.destroy();
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
            firePreProcess(dt); break;
        case PROCESS:
            fireProcess(dt); break;
        case POSTPROCESS:
            firePostProcess(dt); break;
        case ALL:
            // Perform all phases in one go
            firePreProcess(dt);
            fireProcess(dt);
            firePostProcess(dt);
            break;
        }
    }

    /**
     * Invoke onEntityAdd() for all controllers.
     * 
     * @param e The entity being added
     */
    void fireEntityAdd(Entity e) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).onEntityAdd(e);
    }
    
    /**
     * Invoke onEntityRemove() for all controllers.
     * 
     * @param e The entity being removed
     */
    void fireEntityRemove(Entity e) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).onEntityRemove(e);
    }
    
    /**
     * Invoke onComponentAdd() for all controllers.
     * 
     * @param c The component being added
     */
    void fireComponentAdd(Component<?> c) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).onComponentAdd(c);
    }
    
    /**
     * Invoke onComponentRemove() for all controllers.
     * 
     * @param c The component being removed
     */
    void fireComponentRemove(Component<?> c) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).onComponentRemove(c);
    }
    
    private void firePreProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).preProcess(dt);
    }
    
    private void fireProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).process(dt);
    }
    
    private void firePostProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).postProcess(dt);
    }
}
