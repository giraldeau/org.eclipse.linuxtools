package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;

public interface IntervalVisitor {

    public void visit(Task task, StateEnum state, long t1, long t2);

}
