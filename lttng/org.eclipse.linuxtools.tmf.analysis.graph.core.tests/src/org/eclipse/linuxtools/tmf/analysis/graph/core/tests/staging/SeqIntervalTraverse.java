package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

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
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalTraverse;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class SeqIntervalTraverse implements IntervalTraverse {

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
            ITmfStateValue stateValue = i.getStateValue();
            if (stateValue.getType() == Type.LONG) {
                long val = stateValue.unboxLong();
                StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, val));
                visitor.visit(task, state, i.getStartTime(), i.getEndTime());
            }
        }
    }

}
