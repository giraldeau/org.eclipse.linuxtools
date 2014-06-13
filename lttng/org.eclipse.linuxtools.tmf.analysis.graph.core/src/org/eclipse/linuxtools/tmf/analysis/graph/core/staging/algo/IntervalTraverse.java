package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;

public interface IntervalTraverse {

    public void traverse(ITmfStateSystem ss, int qTask, long start, long end, IntervalVisitor visitor);

}
