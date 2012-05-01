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
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.lhkbob.entreri.ControllerManager.Phase;
import com.lhkbob.entreri.component.IntComponent;

public class ControllerManagerTest {
    @Test
    public void testPreProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.PREPROCESS, 0);
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertFalse(controller.processed);
        Assert.assertFalse(controller.postprocessed);
    }
    
    @Test
    public void testProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.PROCESS, 0);
        
        Assert.assertFalse(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertFalse(controller.postprocessed);
    }
    
    @Test
    public void testPostProcessPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(Phase.POSTPROCESS, 0);
        
        Assert.assertFalse(controller.preprocessed);
        Assert.assertFalse(controller.processed);
        Assert.assertTrue(controller.postprocessed);
    }
    
    @Test
    public void testProcessAllPhase() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process();
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
    }
    
    @Test
    public void testComputedDelta() throws Exception {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process();
        // 1st process() uses a dt of 0
        Assert.assertEquals(0.0, controller.dt, .0001);
        
        Thread.sleep(10);
        
        system.getControllerManager().process();
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
        // must be at least 10 ms, within 2 ms error either direction
        Assert.assertTrue(controller.dt > .008);
        Assert.assertTrue(controller.dt < .012);
    }
    
    @Test
    public void testDeltaOverride() {
        ControllerImpl controller = new ControllerImpl();
        EntitySystem system = new EntitySystem();
        system.getControllerManager().addController(controller);
        
        system.getControllerManager().process(1f);
        
        Assert.assertTrue(controller.preprocessed);
        Assert.assertTrue(controller.processed);
        Assert.assertTrue(controller.postprocessed);
        Assert.assertEquals(1f, controller.dt, .0001);
    }
    
    @Test
    public void testReportResult() {
        doTestReportResult(new ResultImpl());
    }
    
    @Test
    public void testReportMultipleResults() {
        doTestReportResult(new ResultImpl(), new ResultImpl());
    }
    
    @Test
    public void testReportSingletonResult() {
        doTestReportResult(new SingletonResultImpl());
    }
    
    @Test
    public void testReportMixedResults() {
        doTestReportResult(new SingletonResultImpl(), new ResultImpl());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testReportMultipleSingletonResults() {
        doTestReportResult(new SingletonResultImpl(), new SingletonResultImpl());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testReportOutOfPhase() {
        Result result = new ResultImpl();
        
        EntitySystem system = new EntitySystem();
        ResultSupplyingController supplier = new ResultSupplyingController(result);
        ControllerImpl receiver = new ControllerImpl();
        
        system.getControllerManager().addController(supplier);
        system.getControllerManager().addController(receiver);
        
        system.getControllerManager().report(result);
    }
    
    @Test
    public void testSingletonResultReset() {
        Result result = new SingletonResultImpl();
        
        EntitySystem system = new EntitySystem();
        ResultSupplyingController supplier = new ResultSupplyingController(result);
        ControllerImpl receiver = new ControllerImpl();
        
        system.getControllerManager().addController(supplier);
        system.getControllerManager().addController(receiver);
        
        Assert.assertArrayEquals(new Result[] { result }, supplier.resultsToReport);
        Assert.assertTrue(receiver.reportedResults.isEmpty());
        
        // process twice
        system.getControllerManager().process();
        system.getControllerManager().process();
        
        // result list should contain 2 elements to the same result
        Assert.assertEquals(2, receiver.reportedResults.size());
        Assert.assertSame(result, receiver.reportedResults.get(0));
        Assert.assertSame(result, receiver.reportedResults.get(1));
    }
    
    private void doTestReportResult(Result... results) {
        EntitySystem system = new EntitySystem();
        ResultSupplyingController supplier = new ResultSupplyingController(results);
        ControllerImpl receiver = new ControllerImpl();
        
        system.getControllerManager().addController(supplier);
        system.getControllerManager().addController(receiver);
        
        Assert.assertArrayEquals(results, supplier.resultsToReport);
        Assert.assertTrue(receiver.reportedResults.isEmpty());
        
        system.getControllerManager().process();
        
        Assert.assertEquals(Arrays.asList(results), receiver.reportedResults);
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
    
    private static class ResultSupplyingController extends SimpleController {
        private final Result[] resultsToReport;
        
        public ResultSupplyingController(Result... results) {
            resultsToReport = results;
        }
        
        @Override
        public void process(double dt) {
            for (Result r: resultsToReport) {
                getEntitySystem().getControllerManager().report(r);
            }
        }
    }
    
    private static class ResultImpl implements Result {
        @Override
        public boolean isSingleton() {
            return false;
        }
    }
    
    private static class SingletonResultImpl implements Result {
        @Override
        public boolean isSingleton() {
            return true;
        }
    }
    
    // explicitly not a listener
    private static class ControllerImpl implements Controller {
        private boolean preprocessed;
        private boolean processed;
        private boolean postprocessed;
        
        private double dt;
        
        private boolean added;
        private boolean removed;
        
        private Entity lastAddedEntity;
        private Entity lastRemovedEntity;
        
        private Component<?> lastAddedComponent;
        private Component<?> lastRemovedComponent;
        
        private EntitySystem system;
        
        private final List<Result> reportedResults = new ArrayList<Result>();
        
        @Override
        public void report(Result result) {
            reportedResults.add(result);
        }
        
        
        @Override
        public EntitySystem getEntitySystem() {
            return system;
        }
        
        @Override
        public void preProcess(double dt) {
            preprocessed = true;
            this.dt = dt;
        }

        @Override
        public void process(double dt) {
            processed = true;
            this.dt = dt;
        }

        @Override
        public void postProcess(double dt) {
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
