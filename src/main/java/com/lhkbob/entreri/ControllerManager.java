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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * ControllerManager is a utility that manages the list of Controllers that can
 * process an EntitySystem.
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
     * The Phase enum represents the different phases of processing that an
     * EntitySystem can go through during what is often considered a "frame".
     */
    public static enum Phase {
        /**
         * The PREPROCESS phase is invoked before all other phases. All
         * controllers in a manager will have their
         * {@link Controller#preProcess(double)} method called before moving to
         * the next phase.
         */
        PREPROCESS,

        /**
         * The PROCESS phase is invoked between PREPROCESS and POSTPROCESS. All
         * controllers in the manager will have their
         * {@link Controller#process(double)} method called before moving to the
         * next phase.
         */
        PROCESS,

        /**
         * <p>
         * The POSTPROCESS phase is invoked after PREPROCESS and POSTPROCESS.
         * All controllers in their manager will have their
         * {@link Controller#postProcess(double)} method called before the frame
         * is completed.
         * </p>
         * <p>
         * Singleton result tracking is reset after this phase completes so that
         * singleton results can be reported at the start of the next frame.
         * Execution timing is also completed at the end of this phase.
         * </p>
         */
        POSTPROCESS
    }

    private final List<Controller> controllers;
    private final Map<Controller, ProfileData> profile;

    private final EntitySystem system;

    private long lastProcessTime;
    private final Set<Class<?>> singletonResults;

    private Phase currentPhase;

    /**
     * Create a new ControllerManager that will store controllers and controller
     * data for processing the given EntitySystem.
     * 
     * @param system The EntitySystem that is managed by this controller manager
     * @throws NullPointerException if system is null
     */
    public ControllerManager(EntitySystem system) {
        if (system == null) {
            throw new NullPointerException("EntitySystem cannot be null");
        }

        this.system = system;
        controllers = new ArrayList<Controller>();

        profile = new HashMap<Controller, ProfileData>();
        singletonResults = new HashSet<Class<?>>();
        lastProcessTime = -1L;

        currentPhase = null;
    }

    /**
     * Get an unmodifiable view of the controllers registered with this manager.
     * 
     * @return The controllers in the manager
     */
    public List<Controller> getControllers() {
        return Collections.unmodifiableList(controllers);
    }

    /**
     * Report the given Result to all registered Controllers in this manager.
     * This can only be called while a Phase is being processed.
     * 
     * @see Result
     * @param result The result to supply to registered controllers
     * @throws NullPointerException if result is null
     * @throws IllegalStateException if not processing a phase, or if a
     *             singleton result of the same type has already been reported
     *             this frame
     */
    public void report(Result result) {
        if (currentPhase == null) {
            throw new IllegalStateException("Can only report results while processing a phase");
        }
        if (result.isSingleton()) {
            if (!singletonResults.add(result.getClass())) {
                throw new IllegalStateException("Singleton result of type " + result.getClass() + " already reported this frame");
            }
        }

        int ct = controllers.size();
        for (int i = 0; i < ct; i++) {
            controllers.get(i).report(result);
        }
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
        if (controller == null) {
            throw new NullPointerException("Controller cannot be null");
        }

        // remove it first - which does nothing if not in the list
        boolean removed = controllers.remove(controller);
        // now add it to the end
        controllers.add(controller);

        if (!removed) {
            // perform initialization steps if we've never seen the controller
            profile.put(controller, new ProfileData());
            controller.init(system);
        }
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
        if (controller == null) {
            throw new NullPointerException("Controller cannot be null");
        }
        boolean removed = controllers.remove(controller);
        if (removed) {
            controller.destroy();
            profile.remove(controller);
        }
    }

    /**
     * Return the last execution time for the given controller and phase. This
     * will return 0 if the controller has not been added to the manager.
     * 
     * @param controller The controller whose time is looked up
     * @param phase The phase that whose timing is returned
     * @return The last execution time of the controller in nanoseconds.
     */
    public long getExecutionTime(Controller controller, Phase phase) {
        if (controller == null || phase == null) {
            throw new NullPointerException("Arguments cannot be null");
        }

        ProfileData c = profile.get(controller);

        if (c != null) {
            switch (phase) {
            case POSTPROCESS:
                return c.postprocessTime;
            case PREPROCESS:
                return c.preprocessTime;
            case PROCESS:
                return c.processTime;
            }
        }

        return 0L;
    }

    /**
     * Return the last execution time for the given controller, for all its
     * phases, in nanoseconds.
     * 
     * @param controller The controller whose time is looked up
     * @return The last total execution time of the controller
     */
    public long getExecutionTime(Controller controller) {
        return getExecutionTime(controller, Phase.PREPROCESS) + getExecutionTime(controller,
                                                                                 Phase.POSTPROCESS) + getExecutionTime(controller,
                                                                                                                       Phase.PROCESS);
    }

    /**
     * Run all phases of the manager using the time delta from the last time the
     * post-process phase was executed. This means that the time delta is
     * reasonably defined even if {@link #process(double)} and
     * {@link #process(Phase, double)} are used in addition to this process()
     * call.
     */
    public void process() {
        if (lastProcessTime < 0) {
            process(0);
        } else {
            process((System.nanoTime() - lastProcessTime) / 1e9);
        }
    }

    /**
     * Run all phases of the manager using the specified frame time delta,
     * <tt>dt</tt>.
     * 
     * @param dt The time delta for the frame, or the amount of time since the
     *            start of the last frame and this one
     */
    public void process(double dt) {
        process(Phase.PREPROCESS, dt);
        process(Phase.PROCESS, dt);
        process(Phase.POSTPROCESS, dt);
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
    public void process(Phase phase, double dt) {
        if (phase == null) {
            throw new NullPointerException("Phase cannot be null");
        }

        currentPhase = phase;
        switch (phase) {
        case PREPROCESS:
            firePreProcess(dt);
            break;
        case PROCESS:
            fireProcess(dt);
            break;
        case POSTPROCESS:
            firePostProcess(dt);
            break;
        }
        currentPhase = null;
    }

    /**
     * Invoke onEntityAdd() for all controllers.
     * 
     * @param e The entity being added
     */
    void fireEntityAdd(Entity e) {
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).onEntityAdd(e);
        }
    }

    /**
     * Invoke onEntityRemove() for all controllers.
     * 
     * @param e The entity being removed
     */
    void fireEntityRemove(Entity e) {
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).onEntityRemove(e);
        }
    }

    /**
     * Invoke onComponentAdd() for all controllers.
     * 
     * @param c The component being added
     */
    void fireComponentAdd(Component<?> c) {
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).onComponentAdd(c);
        }
    }

    /**
     * Invoke onComponentRemove() for all controllers.
     * 
     * @param c The component being removed
     */
    void fireComponentRemove(Component<?> c) {
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).onComponentRemove(c);
        }
    }

    private void firePreProcess(double dt) {
        lastProcessTime = System.nanoTime();

        for (int i = 0; i < controllers.size(); i++) {
            long start = System.nanoTime();
            controllers.get(i).preProcess(dt);
            profile.get(controllers.get(i)).preprocessTime = System.nanoTime() - start;
        }
    }

    private void fireProcess(double dt) {
        for (int i = 0; i < controllers.size(); i++) {
            long start = System.nanoTime();
            controllers.get(i).process(dt);
            profile.get(controllers.get(i)).processTime = System.nanoTime() - start;
        }
    }

    private void firePostProcess(double dt) {
        for (int i = 0; i < controllers.size(); i++) {
            long start = System.nanoTime();
            controllers.get(i).postProcess(dt);
            profile.get(controllers.get(i)).postprocessTime = System.nanoTime() - start;
        }

        singletonResults.clear();
    }

    private static class ProfileData {
        private long processTime;
        private long preprocessTime;
        private long postprocessTime;

        public ProfileData() {
            processTime = 0;
            preprocessTime = 0;
            postprocessTime = 0;
        }
    }
}
