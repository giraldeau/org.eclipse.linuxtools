package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class DumpIntervalVisitor  implements IntervalVisitor {

    @Override
    public void visit(ITmfStateSystem ss, int quark, ITmfStateInterval interval) {
        StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, interval.getStateValue().unboxLong()));
        System.out.println(String.format("/%s %-15s [%d,%d]", ss.getFullAttributePath(quark),
                state, interval.getStartTime(), interval.getEndTime()));
    }

}
