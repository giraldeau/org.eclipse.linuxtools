package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;

public interface IntervalVisitor {

    public void visit(ITmfStateInterval interval);

}
