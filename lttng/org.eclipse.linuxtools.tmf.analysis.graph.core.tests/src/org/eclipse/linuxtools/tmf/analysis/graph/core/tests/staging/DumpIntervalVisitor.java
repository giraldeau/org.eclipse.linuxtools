package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class DumpIntervalVisitor  implements IntervalVisitor {

    @Override
    public void visit(Task task, StateEnum state, long t1, long t2) {
        System.out.println(String.format("[%d,%d] %d %s ", t1, t2, task.getTID(), state.toString()));
    }

}
