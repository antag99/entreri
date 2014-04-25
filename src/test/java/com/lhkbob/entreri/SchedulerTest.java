/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
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

import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.Result;
import com.lhkbob.entreri.task.Task;
import org.junit.Assert;
import org.junit.Test;

// NOTE: this does not test the thread-safety aspects of a job, because
// it's a little too difficult to right a unit test for that
public class SchedulerTest {
    @Test
    public void testResultsReportedToFutureTasksOnly() {
        EntitySystem system = EntitySystem.Factory.create();

        ResultReportingTask t1 = new ResultReportingTask(null, new ResultA(), new ResultA(), new ResultB());
        ResultAListeningTask t2 = new ResultAListeningTask(null);
        ResultBListeningTask t3 = new ResultBListeningTask(null);
        AllResultListeningTask t4 = new AllResultListeningTask(null);

        // note the reordering of the tasks, t1 doesn't report results until after
        // t2 and t3 are already run
        Job j = system.getScheduler().createJob("test", t2, t3, t1, t4);

        j.run();
        Assert.assertEquals(0, t2.aReceiveCount);
        Assert.assertEquals(0, t3.bReceiveCount);
        Assert.assertEquals(2, t4.aReceiveCount);
        Assert.assertEquals(1, t4.bReceiveCount);
        Assert.assertEquals(3, t4.receiveCount);

        // run the job a second time to make sure resubmitting results doesn't
        // screw anything up after a reset
        j.run();
        Assert.assertEquals(0, t2.aReceiveCount);
        Assert.assertEquals(0, t3.bReceiveCount);
        Assert.assertEquals(2, t4.aReceiveCount);
        Assert.assertEquals(1, t4.bReceiveCount);
        Assert.assertEquals(3, t4.receiveCount);
    }

    @Test
    public void testPostProcessTasksInvoked() {
        EntitySystem system = EntitySystem.Factory.create();

        BasicTask t1 = new BasicTask(null);
        BasicTask t2 = new BasicTask(t1);
        BasicTask t3 = new BasicTask(t2);

        Job j = system.getScheduler().createJob("test", t3);
        j.run();

        Assert.assertTrue(t1.invoked);
        Assert.assertTrue(t2.invoked);
        Assert.assertTrue(t3.invoked);
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleSingletonResultsReported() {
        EntitySystem system = EntitySystem.Factory.create();

        ResultReportingTask t1 = new ResultReportingTask(null, new ResultA(), new ResultB(), new ResultB());

        Job j = system.getScheduler().createJob("test", t1);
        j.run();
    }

    @Test
    public void testResultsReported() {
        EntitySystem system = EntitySystem.Factory.create();

        ResultReportingTask t1 = new ResultReportingTask(null, new ResultA(), new ResultA(), new ResultB());
        ResultAListeningTask t2 = new ResultAListeningTask(null);
        ResultBListeningTask t3 = new ResultBListeningTask(null);
        AllResultListeningTask t4 = new AllResultListeningTask(null);

        Job j = system.getScheduler().createJob("test", t1, t2, t3, t4);

        j.run();
        Assert.assertEquals(2, t2.aReceiveCount);
        Assert.assertEquals(1, t3.bReceiveCount);
        Assert.assertEquals(2, t4.aReceiveCount);
        Assert.assertEquals(1, t4.bReceiveCount);
        Assert.assertEquals(3, t4.receiveCount);

        // run the job a second time to make sure resubmitting results doesn't
        // screw anything up after a reset
        j.run();
        Assert.assertEquals(2, t2.aReceiveCount);
        Assert.assertEquals(1, t3.bReceiveCount);
        Assert.assertEquals(2, t4.aReceiveCount);
        Assert.assertEquals(1, t4.bReceiveCount);
        Assert.assertEquals(3, t4.receiveCount);
    }

    @Test
    public void testResetInvoked() {
        EntitySystem system = EntitySystem.Factory.create();

        BasicTask t1 = new BasicTask(null);
        Job j = system.getScheduler().createJob("test", t1);
        j.run();

        Assert.assertTrue(t1.invoked);
        Assert.assertTrue(t1.reset);
    }

    private static class ResultA extends Result {
        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    private static class ResultB extends Result {
        @Override
        public boolean isSingleton() {
            return true;
        }
    }

    private static class BasicTask implements Task {
        boolean invoked;
        boolean reset;

        final BasicTask postProcess;

        public BasicTask(BasicTask postProcess) {
            this.postProcess = postProcess;
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            invoked = true;
            return postProcess;
        }

        @Override
        public void reset(EntitySystem system) {
            reset = true;
        }
    }

    private static class ResultReportingTask extends BasicTask {
        private final Result[] toReport;

        public ResultReportingTask(BasicTask postProcess, Result... results) {
            super(postProcess);
            toReport = results;
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            for (Result r : toReport) {
                job.report(r);
            }
            return super.process(system, job);
        }
    }

    private static class ResultAListeningTask extends BasicTask {
        int aReceiveCount;

        public ResultAListeningTask(BasicTask parent) {
            super(parent);
            aReceiveCount = 0;
        }

        @Override
        public void reset(EntitySystem system) {
            aReceiveCount = 0;
            super.reset(system);
        }

        @SuppressWarnings("unused")
        public void report(ResultA r) {
            aReceiveCount++;
        }
    }

    private static class ResultBListeningTask extends BasicTask {
        int bReceiveCount;

        public ResultBListeningTask(BasicTask parent) {
            super(parent);
            bReceiveCount = 0;
        }

        @Override
        public void reset(EntitySystem system) {
            bReceiveCount = 0;
            super.reset(system);
        }

        @SuppressWarnings("unused")
        public void report(ResultB r) {
            bReceiveCount++;
        }
    }

    private static class AllResultListeningTask extends BasicTask {
        int aReceiveCount;
        int bReceiveCount;
        int receiveCount;

        public AllResultListeningTask(BasicTask parent) {
            super(parent);
        }

        @Override
        public void reset(EntitySystem system) {
            aReceiveCount = 0;
            bReceiveCount = 0;
            receiveCount = 0;
            super.reset(system);
        }

        @SuppressWarnings("unused")
        public void report(Result r) {
            receiveCount++;
        }

        @SuppressWarnings("unused")
        public void report(ResultA r) {
            aReceiveCount++;
        }

        @SuppressWarnings("unused")
        public void report(ResultB r) {
            bReceiveCount++;
        }
    }
}
