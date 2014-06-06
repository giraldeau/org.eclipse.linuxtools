package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;

public interface IntervalTraverse {

    public void traverse(ITmfStateSystem ss, int quark, long start, long end, IntervalVisitor visitor);

}
