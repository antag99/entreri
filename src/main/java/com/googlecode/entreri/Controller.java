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

/**
 * <p>
 * Controllers are functional processors of the entities and components within
 * an EntitySystem. Different Controller implementations have different
 * purposes, such as updating transforms, computing physics or AI, or rendering
 * a scene. Generally controller implementations should be as small and as
 * independent as possible to allow for reuse and easy composability.
 * </p>
 * <p>
 * Controllers also have event listener method hooks so that they can be
 * notified when they are added or removed from an EntitySystem, or when an
 * Entity or component in a system they're attached to is added or removed.
 * </p>
 * <p>
 * Controller instances should only be added to a single EntitySystem at a time.
 * If a controller is later removed, it can safely be added to a new
 * EntitySystem. Controller implementations are responsible for ensuring this in
 * their {@link #init(EntitySystem)} and {@link #destroy()} methods. It is
 * recommended to extend {@link SimpleController} because it handles the
 * necessary logic.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Controller {
    /**
     * Invoke operations on the controller's EntitySystem that must occur before
     * the main processing. All controllers in a system will have their
     * preProcess() method called before the first process() method is called.
     * 
     * @param dt The elapsed time since the last processing
     */
    public void preProcess(float dt);

    /**
     * Invoke controller specific operations to process the EntitySystem. All
     * controllers will have already had their preProcess() method invoked and
     * none will have postProcess() invoked until process() has completed for
     * all controllers in the system.
     * 
     * @param dt The elapsed time since the last processing
     */
    public void process(float dt);

    /**
     * Invoked at the end of a processing phase after all controllers in a
     * system have completed their process() methods.
     * 
     * @param dt The elapsed time since the last processing
     */
    public void postProcess(float dt);

    /**
     * Invoked when a Controller is added to an EntitySystem. This method
     * should initialize any system-specific state used by the controller,
     * such as ComponentIterators, ComponentData instances, or decorated
     * properties.
     * 
     * @param system The new EntitySystem this controller is attached to
     * @throws IllegalStateException if the controller is already attached to
     *             another entity system
     */
    public void init(EntitySystem system);

    /**
     * Invoked when a Controller is detached from its EntitySystem. This should
     * clean up any decorated properties added to the system. It should also
     * update its internal state to make it safe to call
     * {@link #init(EntitySystem)} later.
     */
    public void destroy();

    /**
     * Invoked when an Entity is added to the Controller's EntitySystem. If the
     * Entity was added with the {@link EntitySystem#addEntity(Entity)} method,
     * this is invoked before template's components have been copied to the
     * entity.
     * 
     * @param e The new entity
     */
    public void onEntityAdd(Entity e);

    /**
     * <p>
     * Invoked when an Entity is removed from the Controller's EntitySystem. The
     * Entity is still alive when this is called, but will be removed completely
     * after all Controllers have processed the remove.
     * </p>
     * <p>
     * When an entity is removed, all of its components are removed first, so
     * {@link #onComponentRemove(Component)} will be invoked on all attached
     * components before this listener method is caled.
     * </p>
     * 
     * @param e The entity being removed
     */
    public void onEntityRemove(Entity e);

    /**
     * Invoked when the given Component is added to an Entity. The component is
     * alive so its Entity can be queried in the usual manner.
     * 
     * @param c The new component
     */
    public void onComponentAdd(Component<?> c);

    /**
     * Invoked when the given Component is about to be removed. At the time this
     * is invoked, the Component is still alive. This may be invoked in response
     * to removing the Component from the entity, or if the component's owning
     * entity is removed.
     * 
     * @param c The component being removed
     */
    public void onComponentRemove(Component<?> c);

    /**
     * @return The current EntitySystem processed by this controller, or null if
     *         it is not added to any EntitySystem
     */
    public EntitySystem getEntitySystem();
}
