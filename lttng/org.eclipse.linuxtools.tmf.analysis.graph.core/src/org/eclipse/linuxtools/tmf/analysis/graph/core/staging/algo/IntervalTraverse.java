package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;

public interface IntervalTraverse {

    public void traverse(ITmfStateSystem ss, Task task, long start, long end, IntervalVisitor visitor);

}
