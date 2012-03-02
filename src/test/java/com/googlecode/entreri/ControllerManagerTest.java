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

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.entreri.ControllerManager.Key;
import com.googlecode.entreri.ControllerManager.Phase;
import com.googlecode.entreri.component.IntComponent;

public class ControllerManagerTest {
    @Test
    public void testPreProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.PREPROCESS, 0f);
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertFalse(controller.processed);
        Assert.assertFalse(controller.postprocessed);
    }
    
    @Test
    public void testProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.PROCESS, 0f);
        
        Assert.assertFalse(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertFalse(controller.postprocessed);
    }
    
    @Test
    public void testPostProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.POSTPROCESS, 0f);
        
        Assert.assertFalse(controller.preprocessed);
        Assert.assertFalse(controller.processed);
        Assert.assertTrue(controller.postprocessed);
    }
    
    @Test
    public void testProcessAllPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.ALL, 0f);
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
    }
    
    @Test
    public void testFixedDelta() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().setFixedDelta(1f);
        
        system.getControllerManager().process();
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
        Assert.assertEquals(1f, controller.dt, .0001f);
    }
    
    @Test
    public void testDeltaOverride() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().setFixedDelta(-1f);
        
        system.getControllerManager().process(1f);
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
        Assert.assertEquals(1f, controller.dt, .0001f);
    }
    
    @Test
    public void testControllerData() {
        Key<Object> key = new Key<Object>();
        EntitySystem system = new EntitySystem();
        Object data = new Object();
        system.getControllerManager().setData(key, data);
        
        Assert.assertEquals(data, system.getControllerManager().getData(key));
        
        system.getControllerManager().setData(key, null);
        Assert.assertNull(system.getControllerManager().getData(key));
    }
    
    @Test
    public void testControllerAddRemoveListener() {
        EntitySystem system = new EntitySystem();
        ControllerImpl ctrl = new ControllerImpl();
        
        system.getControllerManager().addController(ctrl);
        Assert.assertTrue(ctrl.added);
        system.getControllerManager().removeController(ctrl);
        Assert.assertTrue(ctrl.removed);
    }
    
    @Test
    public void testEntityAddRemoveListener() {
        EntitySystem system = new EntitySystem();
        ControllerImpl ctrl = new ControllerImpl();
        system.getControllerManager().addController(ctrl);
        
        Entity e = system.addEntity();
        Assert.assertEquals(e, ctrl.lastAddedEntity);
        system.removeEntity(e);
        Assert.assertTrue(e == ctrl.lastRemovedEntity);
    }
    
    @Test
    public void testComponentAddRemoveListener() {
        EntitySystem system = new EntitySystem();
        ControllerImpl ctrl = new ControllerImpl();
        system.getControllerManager().addController(ctrl);
        
        Entity e = system.addEntity();
        Component<IntComponent> i = e.add(TypeId.get(IntComponent.class));
        
        Assert.assertEquals(i, ctrl.lastAddedComponent);
        e.remove(TypeId.get(IntComponent.class));
        Assert.assertTrue(i == ctrl.lastRemovedComponent);
    }
    
    private static class ControllerImpl implements Controller {
        private boolean preprocessed;
        private boolean processed;
        private boolean postprocessed;
        
        private float dt;
        
        private boolean added;
        private boolean removed;
        
        private Entity lastAddedEntity;
        private Entity lastRemovedEntity;
        
        private Component<?> lastAddedComponent;
        private Component<?> lastRemovedComponent;
        
        private EntitySystem system;
        
        @Override
        public EntitySystem getEntitySystem() {
            return system;
        }
        
        @Override
        public void preProcess(float dt) {
            preprocessed = true;
            this.dt = dt;
        }

        @Override
        public void process(float dt) {
            processed = true;
            this.dt = dt;
        }

        @Override
        public void postProcess(float dt) {
            postprocessed = true;
            this.dt = dt;
        }

        @Override
        public void init(EntitySystem system) {
            added = true;
        }

        @Override
        public void destroy() {
            removed = true;
        }

        @Override
        public void onEntityAdd(Entity e) {
            lastAddedEntity = e;
        }

        @Override
        public void onEntityRemove(Entity e) {
            lastRemovedEntity = e;
        }

        @Override
        public void onComponentAdd(Component<?> c) {
            lastAddedComponent = c;
        }

        @Override
        public void onComponentRemove(Component<?> c) {
            lastRemovedComponent = c;
        }
    }
}
