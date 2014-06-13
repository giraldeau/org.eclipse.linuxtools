package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import java.util.List;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue.Type;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalTraverse;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class SeqIntervalTraverse implements IntervalTraverse {

    @Override
    public void traverse(ITmfStateSystem s, int qTask, long start, long end, IntervalVisitor visitor) {
        try {
            doTraverse(s, qTask, start, end, visitor);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            e.printStackTrace();
        }
    }

    private static void doTraverse(ITmfStateSystem s, int qTask, long start, long end, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
        List<ITmfStateInterval> hist = s.queryHistoryRange(qTask, start, end);
        for (ITmfStateInterval i: hist) {
            ITmfStateValue stateValue = i.getStateValue();
            if (stateValue.getType() == Type.LONG) {
                visitor.visit(s, qTask, i);
            }
        }
    }

}
