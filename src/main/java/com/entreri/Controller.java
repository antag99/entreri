package com.entreri;

/**
 * Controllers are functional processors of the entities and components within
 * an EntitySystem. Different Controller implementations have different
 * purposes, such as updating transforms, computing physics or AI, or rendering
 * a scene. Generally controller implementations should be as small and as
 * independent as possible to allow for reuse and easy composability.
 * 
 * @author Michael Ludwig
 */
public interface Controller {
    /**
     * Invoke operations on the given EntitySystem that must occur before the
     * main processing of the frame. All controllers in a system will have their
     * preProcess() method called before the first process() method is called.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void preProcess(EntitySystem system, float dt);

    /**
     * Invoke controller specific operations to process the EntitySystem. All
     * controllers will have already had their preProcess() method invoked and
     * none will have postProcess() invoked until process() has completed for
     * all controllers in the system.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void process(EntitySystem system, float dt);

    /**
     * Invoked at the end of a processing phase after all controllers in a
     * system have completed their process() methods.
     * 
     * @param system The entity system to process
     * @param dt The elapsed time since the last processing
     */
    public void postProcess(EntitySystem system, float dt);
}
