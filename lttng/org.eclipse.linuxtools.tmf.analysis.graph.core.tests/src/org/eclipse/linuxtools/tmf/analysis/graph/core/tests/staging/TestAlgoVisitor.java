package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.StateSystemFactory;
import org.eclipse.linuxtools.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.linuxtools.statesystem.core.backend.InMemoryBackend;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.StateSystemTaskListener;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.StateSystemTaskListener.Attributes;
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
    Integer cpu1 = 24;
    Integer cpu2 = 42;

    int p1q;
    int p2q;

    private StateSystemTaskListener listener;

    private Ctx ctx;

    @Before
    public void setup() throws StateValueTypeException {
        IStateHistoryBackend backend = new InMemoryBackend(0);
        ss = StateSystemFactory.newStateSystem("foo", backend);
        listener = new StateSystemTaskListener(ss);
        long now = 0;
        ctx = new Ctx();
        ctx.setHost(host).setCpu(cpu1).setTs(now);
        p1 = new Task(host, 111L, now);
        p2 = new Task(host, 222L, now);
        p3 = new Task(host, 333L, now);

    }

    private void notifyStateChange(Ctx context, Task task, StateEnum nextState) {
        listener.stateChange(context, task, nextState);
        task.setState(context.ts, nextState);
    }

    @Test
    public void testPreempt() throws StateValueTypeException, AttributeNotFoundException {
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

        listener.stateFlush(p1);
        listener.stateFlush(p2);
        listener.stateFlush(p3);

        IntervalTraverse traverse = new TaskFlowTraverse();
        CountIntervalVisitor visitor = new CountIntervalVisitor();
        p1q = getQuarkTask(p1);
        traverse.traverse(ss, p1q, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
        assertEquals(3, visitor.getCount());
    }

    private int getQuarkTask(Task task) throws AttributeNotFoundException {
        return ss.getQuarkAbsolute(task.getHostID(), Attributes.TASK.label(), task.getTID().toString(), Attributes.STATE.label());
    }

    /* one level blocking
     *  ====-----====
     *   =======|=
     */
    @Test
    public void testWaitTaskDepth1() throws AttributeNotFoundException {
        // initial state (time=0)
        p1.setStateRaw(StateEnum.RUNNING);
        p2.setStateRaw(StateEnum.RUNNING);

        // p1 runs and block
        ctx.setTs(10L);
        notifyStateChange(ctx, p1, StateEnum.WAIT_UNKNOWN);

        // p2 runs and wake-up p1
        ctx.setTs(20L);
        ctx.setWup(p2);
        p1.setStateRaw(StateEnum.WAIT_TASK); // we now know that wait was due to a task
        notifyStateChange(ctx, p1, StateEnum.WAIT_CPU);
        ctx.setWup(null);

        // p1 is scheduled
        ctx.setTs(30L);
        notifyStateChange(ctx, p1, StateEnum.RUNNING);

        // p2 exits
        ctx.setTs(40L);
        notifyStateChange(ctx, p2, StateEnum.EXIT);

        // p1 exits
        ctx.setTs(50L);
        notifyStateChange(ctx, p1, StateEnum.EXIT);

        listener.stateFlush(p1);
        listener.stateFlush(p2);

        p1q = getQuarkTask(p1);
        p2q = getQuarkTask(p2);

        DumpIntervalVisitor visitor = new DumpIntervalVisitor();
        IntervalTraverse traverse = new SeqIntervalTraverse();
        traverse.traverse(ss, p1q, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
        traverse.traverse(ss, p2q, ss.getStartTime(), ss.getCurrentEndTime(), visitor);

        System.out.println("task flow");
        traverse = new TaskFlowTraverse();
        traverse.traverse(ss, p1q, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
    }

}
