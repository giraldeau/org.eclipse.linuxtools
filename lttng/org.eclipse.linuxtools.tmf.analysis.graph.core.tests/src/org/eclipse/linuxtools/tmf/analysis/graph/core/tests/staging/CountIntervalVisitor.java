package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class CountIntervalVisitor implements IntervalVisitor {

    private int count = 0;

    public CountIntervalVisitor() {
        count = 0;
    }

    @Override
    public void visit(ITmfStateSystem ss, int quark, ITmfStateInterval interval) {
        count++;
    }

    public int getCount() {
        return count;
    }

}