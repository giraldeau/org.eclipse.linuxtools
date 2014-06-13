package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;

public interface IntervalVisitor {

    public void visit(ITmfStateSystem ss, int quark, ITmfStateInterval interval);

}
