package com.entreri;

import org.junit.Assert;
import org.junit.Test;

import com.entreri.ControllerManager.Phase;

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
        EntitySystem system = new EntitySystem();
        Object data = new Object();
        system.getControllerManager().setControllerData(Key.class, data);
        
        Assert.assertEquals(data, system.getControllerManager().getControllerData(Key.class));
        
        system.getControllerManager().setControllerData(Key.class, null);
        Assert.assertNull(system.getControllerManager().getControllerData(Key.class));
    }
    
    @Test
    public void testPrivateData() {
        EntitySystem system = new EntitySystem();
        Object data = new Object();
        Object key = new Object();
        
        system.getControllerManager().setPrivateData(key, data);
        
        Assert.assertEquals(data, system.getControllerManager().getPrivateData(key));
        
        system.getControllerManager().setPrivateData(key, null);
        Assert.assertNull(system.getControllerManager().getPrivateData(key));
    }
    
    private static @interface Key {
        
    }
    
    private static class ControllerImpl implements Controller {
        private boolean preprocessed;
        private boolean processed;
        private boolean postprocessed;
        
        private float dt;
        
        @Override
        public void preProcess(EntitySystem system, float dt) {
            preprocessed = true;
            this.dt = dt;
        }

        @Override
        public void process(EntitySystem system, float dt) {
            processed = true;
            this.dt = dt;
        }

        @Override
        public void postProcess(EntitySystem system, float dt) {
            postprocessed = true;
            this.dt = dt;
        }
    }
}
