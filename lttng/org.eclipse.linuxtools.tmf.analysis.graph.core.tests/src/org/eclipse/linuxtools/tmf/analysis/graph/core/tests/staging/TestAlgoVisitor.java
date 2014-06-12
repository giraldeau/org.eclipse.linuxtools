package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.StateSystemFactory;
import org.eclipse.linuxtools.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.linuxtools.statesystem.core.backend.InMemoryBackend;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.StateSystemTaskListener;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalTraverse;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.TaskFlowTraverse;
import org.junit.Before;
import org.junit.Test;

public class TestAlgoVisitor {

    ITmfStateSystemBuilder ss;

    Task p1, p2, p3;
    String host = "host1";
    Integer cpu1 = 42;

    int p1q;
    int p2q;
    int cpu1q;

    private StateSystemTaskListener listener;

    @Before
    public void setup() throws StateValueTypeException {
        IStateHistoryBackend backend = new InMemoryBackend(0);
        ss = StateSystemFactory.newStateSystem("foo", backend);
        listener = new StateSystemTaskListener(ss);
        long now = 0;
        Ctx ctx = new Ctx();
        ctx.setHost(host).setCpu(cpu1).setTs(now);
        p1 = new Task(host, 111L, now);
        p2 = new Task(host, 222L, now);
        p3 = new Task(host, 333L, now);

        // initial state
        p1.setStateRaw(StateEnum.WAIT_CPU);
        p2.setStateRaw(StateEnum.RUNNING);
        p3.setStateRaw(StateEnum.WAIT_UNKNOWN);

        // p2 preempts p1
        notifyStateChange(ctx, p1, StateEnum.RUNNING);
        notifyStateChange(ctx, p2, StateEnum.WAIT_CPU);

        // p1 preempts p2
        ctx.setTs(10L);
        notifyStateChange(ctx, p2, StateEnum.RUNNING);
        notifyStateChange(ctx, p1, StateEnum.WAIT_CPU);

        // again
        ctx.setTs(20L);
        notifyStateChange(ctx, p1, StateEnum.RUNNING);
        notifyStateChange(ctx, p2, StateEnum.WAIT_CPU);

        // p1 wakes p3
        ctx.setTs(30L);
        ctx.setWup(p1);
        notifyStateChange(ctx, p3, StateEnum.WAIT_CPU);

        // p3 runs
        ctx.setTs(40L);
        notifyStateChange(ctx, p3, StateEnum.RUNNING);
        notifyStateChange(ctx, p1, StateEnum.WAIT_CPU);

        listener.stateFlush(p1);
        listener.stateFlush(p2);
        listener.stateFlush(p3);

    }

    private void notifyStateChange(Ctx ctx, Task task, StateEnum nextState) {
        listener.stateChange(ctx, task, nextState);
        task.setState(ctx.ts, nextState);
    }

    @Test
    public void testPreempt() throws StateValueTypeException {
        IntervalTraverse traverse = new TaskFlowTraverse();
        CountIntervalVisitor visitor = new CountIntervalVisitor();
        traverse.traverse(ss, p1, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
        assertEquals(4, visitor.getCount());
    }

}
