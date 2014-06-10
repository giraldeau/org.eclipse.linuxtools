package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import java.util.List;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue.Type;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;

public class PreemptTraverse implements IntervalTraverse {

    @Override
    public void traverse(ITmfStateSystem s, Task task, long start, long end, IntervalVisitor visitor) {
        try {
            doTraverse(s, task, start, end, visitor);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            e.printStackTrace();
        }
    }

    private static void doTraverse(ITmfStateSystem s, Task task, long start, long end, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
        int q = s.getQuarkAbsolute(task.getHostID(), "task", task.getTID().toString(), "state");
        List<ITmfStateInterval> hist = s.queryHistoryRange(q, start, end);
        for (ITmfStateInterval i: hist) {
            if (i.getStateValue().getType() == Type.LONG) {
                // visit the interval
                long val = i.getStateValue().unboxLong();
                StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, val));
                visitor.visit(task, state, i.getStartTime(), i.getEndTime());
                switch(state) {
                case EXIT:
                    break;
                case WAIT_CPU:
                    int cpuQuark = PackedLongValue.unpack(1, val);
                    resolvePreempt(s, task, i.getStartTime(), i.getEndTime(), cpuQuark, visitor);
                    break;
                case RUNNING:
                    break;
                case UNKNOWN:
                    break;
                case WAIT_BLOCKED:
                case WAIT_BLOCK_DEV:
                case WAIT_NETWORK:
                case WAIT_TASK:
                case WAIT_TIMER:
                case WAIT_USER_INPUT:
                case INTERRUPTED:
                default:
                    break;
                }
            }
        }
    }

    private static void resolvePreempt(ITmfStateSystem s, Task preemptedTask, long t1, long t2, int cpuQuark, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
        List<ITmfStateInterval> preemptRange = s.queryHistoryRange(cpuQuark, t1, t2);
        for (ITmfStateInterval preempt: preemptRange) {
            ITmfStateValue stateValue = preempt.getStateValue();
            if (stateValue.getType() == Type.INTEGER) {
                long tid = stateValue.unboxLong();
                Task t = new Task(preemptedTask.getHostID(), tid, 0);
                visitor.visit(t, StateEnum.RUNNING, preempt.getStartTime(), preempt.getEndTime());
            }
        }
    }
}
