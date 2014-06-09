package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.*;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.StateSystemFactory;
import org.eclipse.linuxtools.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.linuxtools.statesystem.core.backend.InMemoryBackend;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalTraverse;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.PreemptTraverse;
import org.junit.Before;
import org.junit.Test;

public class TestAlgoVisitor {

    ITmfStateSystemBuilder ss;

    @Before
    public void setup() {
        IStateHistoryBackend backend = new InMemoryBackend(0);
        ss = StateSystemFactory.newStateSystem("foo", backend);

    }

    @Test
    public void testPreempt() throws StateValueTypeException, AttributeNotFoundException {
        String p1 = "1234";
        String p2 = "5678";
        String host = "host1";
        String state = "state";
        String task = "task";
        String cpu = "cpu";
        String cpu1 = "42";
        int p1q = ss.getQuarkAbsoluteAndAdd(host, task, p1, state);
        int p2q = ss.getQuarkAbsoluteAndAdd(host, task, p2, state);
        int cpu1q = ss.getQuarkAbsoluteAndAdd(host, cpu, cpu1, state);

        // p1 runs
        ss.modifyAttribute(0, val(StateEnum.RUN.value(), 0), p1q);
        ss.modifyAttribute(0, val(Integer.parseInt(p1)), cpu1q);

        // p2 preempts p1
        ss.modifyAttribute(10, val(StateEnum.PREEMPTED.value(), cpu1q), p1q);
        ss.modifyAttribute(10, val(StateEnum.RUN.value(), 0), p2q);
        ss.modifyAttribute(10, val(Integer.parseInt(p2)), cpu1q);

        // p1 runs again
        ss.modifyAttribute(20, val(StateEnum.RUN.value(), 0), p1q);
        ss.modifyAttribute(20, val(StateEnum.PREEMPTED.value(), 0), p2q);
        ss.modifyAttribute(20, val(Integer.parseInt(p1)), cpu1q);

        Task t = new Task(host, Integer.parseInt(p1), 0);
        IntervalTraverse traverse = new PreemptTraverse();
        CountIntervalVisitor visitor = new CountIntervalVisitor();
        traverse.traverse(ss, t, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
        assertEquals(3, visitor.getCount());

    }

    private static TmfStateValue val(int a, int b) {
        return TmfStateValue.newValueLong(PackedLongValue.pack(a, b));
    }

    private static TmfStateValue val(int a) {
        return TmfStateValue.newValueInt(a);
    }

}
